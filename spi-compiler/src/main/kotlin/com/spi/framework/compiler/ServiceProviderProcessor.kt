package com.spi.framework.compiler

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.spi.framework.annotations.ServiceProvider
import java.io.OutputStreamWriter

/**
 * KSP SymbolProcessor that discovers [ServiceProvider]-annotated classes and generates:
 * KSP SymbolProcessor，用于发现带有 [ServiceProvider] 注解的类并生成：
 * 1. A [com.spi.framework.core.ServiceIndexProvider] implementation that registers
 *    all discovered services into the [com.spi.framework.core.ServiceRegistry].
 *    一个 [ServiceIndexProvider] 实现，将所有发现的服务注册到 [ServiceRegistry]。
 * 2. A META-INF/services file for [com.spi.framework.core.ServiceIndexProvider]
 *    discovery via [java.util.ServiceLoader].
 *    一个 META-INF/services 文件，用于通过 [ServiceLoader] 发现 [ServiceIndexProvider]。
 */
class ServiceProviderProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var invocationCount = 0

    override fun process(resolver: Resolver): List<KSAnnotated> {
        invocationCount++

        // Find all classes annotated with @ServiceProvider.
        // 查找所有带有 @ServiceProvider 注解的类。
        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(ServiceProvider::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        if (annotatedSymbols.isEmpty()) {
            logger.info("[SPI Processor] No @ServiceProvider annotations found")
            return emptyList()
        }

        logger.info("[SPI Processor] Found ${annotatedSymbols.size} @ServiceProvider-annotated class(es)")

        val validProviders = mutableListOf<ProviderInfo>()

        for (declaration in annotatedSymbols) {
            val results = processAnnotation(declaration)
            validProviders.addAll(results)
        }

        // --- Phase 2: Duplicate detection & summary ---

        // Detect duplicate non-empty (service, name) pairs and log warnings.
        val seen = mutableSetOf<Pair<String, String>>()
        for (provider in validProviders.filter { it.name.isNotEmpty() }) {
            val key = provider.serviceQualifiedName to provider.name
            if (!seen.add(key)) {
                logger.warn(
                    "[SPI Processor] Duplicate @ServiceProvider registration found: " +
                    "service='${provider.serviceQualifiedName}', name='${provider.name}' " +
                    "(impl=${provider.implQualifiedName}). The later registration will " +
                    "replace the earlier named registration in the registry."
                )
            }
        }

        // Summary log grouping providers by service with counts.
        val groupedByService = validProviders.groupBy { it.serviceQualifiedName }
        logger.info("[SPI Processor] Provider summary by service interface:")
        for ((serviceName, impls) in groupedByService) {
            logger.info(
                "[SPI Processor]   ${impls.size} provider(s) for $serviceName: " +
                impls.joinToString(", ") { it.implQualifiedName }
            )
        }

        if (validProviders.isEmpty()) {
            logger.warn("[SPI Processor] No valid @ServiceProvider declarations to process")
            return emptyList()
        }

        // Generate a unique suffix based on the provider list content.
        // 基于提供者列表内容生成唯一后缀。
        val suffix = generateSuffix(validProviders)

        // Generate the ServiceIndexProvider implementation.
        // 生成 ServiceIndexProvider 实现。
        generateServiceIndexClass(validProviders, suffix)

        // Generate/update the META-INF/services discovery file.
        // 生成/更新 META-INF/services 发现文件。
        generateServiceDescriptorFile(suffix)

        logger.info("[SPI Processor] Generated ServiceIndex_$suffix with ${validProviders.size} registration(s)")

        return emptyList()
    }

    /**
     * Process a single [ServiceProvider] annotation on a class declaration.
     * Handles [Repeatable] annotations — a single class may have multiple
     * [ServiceProvider] annotations for different service interfaces.
     * 处理类声明上的单个 [ServiceProvider] 注解。
     * 处理 [Repeatable] 注解——单个类可能有多个针对不同服务接口的 [ServiceProvider] 注解。
     */
    private fun processAnnotation(
        declaration: KSClassDeclaration
    ): List<ProviderInfo> {
        val annotations = declaration.annotations.filter {
            it.shortName.asString() == ServiceProvider::class.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                ServiceProvider::class.qualifiedName
        }.toList()

        val results = mutableListOf<ProviderInfo>()

        for (annotation in annotations) {
            val args = annotation.arguments

            // Extract annotation arguments / 提取注解参数
            val serviceType = args.firstOrNull { it.name?.asString() == "service" }?.value as? KSType
            if (serviceType == null) {
                logger.error(
                    "Missing required 'service' argument in @ServiceProvider on ${declaration.qualifiedName?.asString()}",
                    declaration
                )
                continue
            }

            val priority = (args.firstOrNull { it.name?.asString() == "priority" }?.value as? Int) ?: 0
            val name = (args.firstOrNull { it.name?.asString() == "name" }?.value as? String) ?: ""
            val lazy = (args.firstOrNull { it.name?.asString() == "lazy" }?.value as? Boolean) ?: true

            // Extract factory argument / 提取 factory 参数
            val factoryType = args.firstOrNull { it.name?.asString() == "factory" }?.value as? KSType
            val usesFactory = factoryType != null &&
                factoryType.declaration.qualifiedName?.asString() != "com.spi.framework.annotations.ServiceFactory.None"

            val serviceDeclaration = serviceType.declaration
            val serviceQualifiedName = serviceDeclaration.qualifiedName?.asString()

            if (serviceQualifiedName == null) {
                logger.error(
                    "Cannot resolve service type '${serviceType.declaration.simpleName.asString()}' " +
                    "in @ServiceProvider on ${declaration.qualifiedName?.asString()}",
                    declaration
                )
                continue
            }

            // Validate: service type must be an interface or abstract class.
            // 验证：服务类型必须是接口或抽象类。
            if (serviceDeclaration is KSClassDeclaration &&
                serviceDeclaration.classKind != ClassKind.INTERFACE &&
                !serviceDeclaration.modifiers.contains(Modifier.ABSTRACT)) {
                logger.error(
                    "Service type '$serviceQualifiedName' must be an interface or abstract class. " +
                    "Found: ${serviceDeclaration.classKind}",
                    declaration
                )
                continue
            }

            // Validate: annotated class must implement the declared service interface.
            // 验证：被注解的类必须实现声明的服务接口。
            val implQualifiedName = declaration.qualifiedName?.asString() ?: continue
            if (!implementsInterface(declaration, serviceQualifiedName)) {
                logger.error(
                    "'$implQualifiedName' does not implement '$serviceQualifiedName'. " +
                    "All @ServiceProvider classes must implement their declared service interface.",
                    declaration
                )
                continue
            }

            val isObject = declaration.classKind == ClassKind.OBJECT

            // Validate: if no factory is specified, non-object implementations must have a no-arg constructor.
            // 验证：如果没有指定工厂，非 object 实现类必须有一个无参构造函数。
            if (!usesFactory && !isObject) {
                val primaryCtor = declaration.primaryConstructor
                if (primaryCtor != null && primaryCtor.parameters.isNotEmpty()) {
                    logger.error(
                        "'$implQualifiedName' has constructor parameters but no factory specified. " +
                        "Either add a no-arg constructor or provide a ServiceFactory via the 'factory' parameter. " +
                        "要么添加无参构造函数，要么通过 'factory' 参数提供 ServiceFactory。",
                        declaration
                    )
                    continue
                }
            }

            results.add(
                ProviderInfo(
                    serviceQualifiedName = serviceQualifiedName,
                    implQualifiedName = implQualifiedName,
                    priority = priority.coerceIn(Int.MIN_VALUE, Int.MAX_VALUE - 1),
                    name = name,
                    lazy = lazy,
                    isObject = isObject,
                    factoryQualifiedName = if (usesFactory)
                        factoryType!!.declaration.qualifiedName?.asString() else null
                )
            )
        }

        return results
    }

    /**
     * Check whether [classDecl] implements [interfaceQualifiedName].
     * 检查 [classDecl] 是否实现了 [interfaceQualifiedName]。
     */
    private fun implementsInterface(
        classDecl: KSClassDeclaration,
        interfaceQualifiedName: String
    ): Boolean {
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<KSClassDeclaration>()
        stack.add(classDecl)

        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            val currentName = current.qualifiedName?.asString() ?: current.simpleName.asString()
            if (!visited.add(currentName)) {
                continue
            }

            for (superType in current.superTypes) {
                val resolved = superType.resolve()
                val decl = resolved.declaration
                if (decl.qualifiedName?.asString() == interfaceQualifiedName) {
                    return true
                }
                if (decl is KSClassDeclaration) {
                    stack.add(decl)
                }
            }
        }
        return false
    }

    /**
     * Generate a deterministic suffix for the generated class name.
     * 为生成的类名生成确定性后缀。
     */
    private fun generateSuffix(providers: List<ProviderInfo>): String {
        val content = providers
            .sortedWith(compareBy({ it.serviceQualifiedName }, { it.implQualifiedName }))
            .joinToString("|") { "${it.serviceQualifiedName}->${it.implQualifiedName}" }
        val hash = content.hashCode().toUInt().toString(16).take(6).uppercase()
        return hash
    }

    /**
     * Generate the ServiceIndexProvider implementation Kotlin source file.
     * 生成 ServiceIndexProvider 实现的 Kotlin 源文件。
     */
    private fun generateServiceIndexClass(
        providers: List<ProviderInfo>,
        suffix: String
    ) {
        val className = "ServiceIndex_$suffix"
        val packageName = "com.spi.framework.generated"

        // Build import list for all implementation and factory classes.
        // 构建所有实现类和工厂类的导入列表。
        val implPackages = providers
            .map { it.implQualifiedName.substringBeforeLast('.') }
        val factoryPackages = providers
            .mapNotNull { it.factoryQualifiedName?.substringBeforeLast('.') }
        val allPackages = (implPackages + factoryPackages)
            .distinct()
            .sorted()
            .joinToString("\n") { "import $it.*" }

        // Determine whether any provider uses a factory.
        // 确定是否有任何提供者使用了工厂。
        val anyFactory = providers.any { it.factoryQualifiedName != null }

        // Build registration calls, grouped by service.
        // 构建按服务分组的注册调用。
        val groupedByService = providers.groupBy { it.serviceQualifiedName }

        val registrationBlocks = groupedByService.entries.joinToString("\n\n") { (serviceName, impls) ->
            val comment = "        // --- Registration: ${serviceName.substringAfterLast('.')} ---"
            val anyObject = impls.any { it.isObject && it.factoryQualifiedName == null }
            val noArgComment = if (!anyFactory && !anyObject) {
                "        // IMPORTANT: Each implementation class MUST have a public no-argument constructor.\n" +
                        "        // 重要：每个实现类必须有一个公开的无参构造函数。"
            } else {
                "        // NOTE: Some providers use a ServiceFactory or Kotlin object singleton for instantiation.\n" +
                        "        // 注意：部分提供者使用 ServiceFactory 或 Kotlin object 单例进行实例化。"
            }
            val registrations = impls.joinToString("\n") { provider ->
                val nameArg = provider.name.toKotlinStringLiteral()
                if (provider.factoryQualifiedName != null || provider.isObject) {
                    val factoryExpression = if (provider.factoryQualifiedName != null) {
                        "${provider.factoryQualifiedName}().create()"
                    } else {
                        provider.implQualifiedName
                    }
                    """        registry.register(
            serviceClass = ${provider.serviceQualifiedName}::class.java,
            implClass = ${provider.implQualifiedName}::class.java,
            priority = ${provider.priority},
            name = $nameArg,
            lazy = ${provider.lazy},
            factory = { $factoryExpression }
        )"""
                } else {
                    """        registry.register(
            serviceClass = ${provider.serviceQualifiedName}::class.java,
            implClass = ${provider.implQualifiedName}::class.java,
            priority = ${provider.priority},
            name = $nameArg,
            lazy = ${provider.lazy}
        )"""
                }
            }
            "$comment\n$noArgComment\n$registrations"
        }

        val sourceCode = """
            |package $packageName
            |
            |import com.spi.framework.core.ServiceIndexProvider
            |import com.spi.framework.core.ServiceRegistry
            |$allPackages
            |
            |/**
            | * Auto-generated by SPI Framework KSP Processor.
            | * 由 SPI 框架 KSP 处理器自动生成。
            | *
            | * Registers ${providers.size} service implementation(s) for
            | * ${groupedByService.size} service interface(s).
            | * 为 ${groupedByService.size} 个服务接口注册 ${providers.size} 个服务实现。
            | *
            | * DO NOT EDIT — changes will be overwritten.
            | * 请勿编辑——更改将被覆盖。
            | */
            |public class $className : ServiceIndexProvider {
            |
            |    override fun initialize(registry: ServiceRegistry) {
            |$registrationBlocks
            |    }
            |}
        """.trimMargin()

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true),
            packageName = packageName,
            fileName = className
        )
        OutputStreamWriter(file).use { writer ->
            writer.write(sourceCode)
        }
    }

    /**
     * Generate or append to the META-INF/services file for ServiceIndexProvider discovery.
     * 生成或追加到 META-INF/services 文件，用于 ServiceIndexProvider 发现。
     */
    private fun generateServiceDescriptorFile(suffix: String) {
        val className = "ServiceIndex_$suffix"
        val serviceLine = "com.spi.framework.generated.$className"

        try {
            val file = codeGenerator.createNewFileByPath(
                dependencies = Dependencies(aggregating = true),
                path = "META-INF/services/com.spi.framework.core.ServiceIndexProvider",
                extensionName = ""
            )
            OutputStreamWriter(file).use { writer ->
                writer.write("$serviceLine\n")
            }
        } catch (e: Exception) {
            logger.warn("[SPI Processor] Could not create META-INF/services file: ${e.message}")
        }
    }
}

/**
 * Internal data class holding parsed provider information.
 * 内部数据类，保存解析后的提供者信息。
 *
 * @property factoryQualifiedName Fully qualified name of the [ServiceFactory] class,
 *                                 or null if no factory is specified (use no-arg constructor).
 *                                 [ServiceFactory] 类的全限定名，如果未指定工厂则为 null（使用无参构造函数）。
 */
internal data class ProviderInfo(
    val serviceQualifiedName: String,
    val implQualifiedName: String,
    val priority: Int,
    val name: String,
    val lazy: Boolean,
    val isObject: Boolean,
    val factoryQualifiedName: String? = null
)

internal fun String.toKotlinStringLiteral(): String {
    val escaped = buildString {
        for (char in this@toKotlinStringLiteral) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> {
                    append('\\')
                    append('$')
                }
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                else -> {
                    if (char < ' ') {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
        }
    }
    return "\"$escaped\""
}
