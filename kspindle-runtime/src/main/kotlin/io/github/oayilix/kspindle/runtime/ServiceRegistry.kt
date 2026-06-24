package io.github.oayilix.kspindle.runtime

import io.github.oayilix.kspindle.runtime.internal.RegistryEntry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe registry that stores and retrieves service provider instances.
 * 线程安全的注册表，用于存储和检索服务提供者实例。
 *
 * Each service interface maps to a sorted list of [RegistryEntry] instances.
 * Entries are sorted by priority descending, then by implementation class name
 * for deterministic ordering.
 * 每个服务接口映射到一个排序的 [RegistryEntry] 实例列表。
 * 条目按优先级降序排序，然后按实现类名排序以确保确定性顺序。
 *
 * All methods are safe for concurrent access. Registration uses
 * [ConcurrentHashMap.compute] for atomic updates, and the entry list uses
 * [CopyOnWriteArrayList] to allow safe concurrent reads during writes.
 * 所有方法都支持并发访问安全。注册使用 [ConcurrentHashMap.compute] 进行原子更新，
 * 条目列表使用 [CopyOnWriteArrayList] 以允许在写入时进行安全的并发读取。
 */
class ServiceRegistry {

    private val registry: ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<RegistryEntry<*>>> =
        ConcurrentHashMap()

    /**
     * Register a service implementation.
     * 注册一个服务实现。
     *
     * @param T the service interface type. 服务接口类型。
     * @param serviceClass the service interface class. 服务接口类。
     * @param implClass the concrete implementation class (must be assignable to [serviceClass]).
     *                  具体实现类（必须可赋值给 [serviceClass]）。
     * @param priority ordering priority. Higher values are returned first. Default is 0.
     *                 排序优先级。值越高的越先返回。默认值为 0。
     * @param name an optional logical name for named lookups. Default is empty.
     *             可选逻辑名称，用于按名称查找。默认值为空。
     * @param lazy whether to instantiate lazily. Default is true.
     *             是否延迟实例化。默认值为 true。
     * @param factory an optional factory lambda for custom instantiation.
     *                When non-null, it is invoked to create instances instead of
     *                using reflection-based no-arg construction. Default is null.
     *                可选的工厂 lambda，用于自定义实例化。
     *                当非 null 时，将调用它来创建实例，而不是使用基于反射的无参构造。默认值为 null。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> register(
        serviceClass: Class<T>,
        implClass: Class<out T>,
        priority: Int = 0,
        name: String = "",
        lazy: Boolean = true,
        factory: (() -> T)? = null
    ) {
        val entry = RegistryEntry(
            implClass = implClass,
            priority = priority,
            name = name,
            lazy = lazy,
            factory = factory
        )

        if (!lazy) {
            entry.initializeEagerly()
        }

        registry.compute(serviceClass) { _, existing ->
            val list = existing ?: CopyOnWriteArrayList()
            if (name.isNotEmpty()) {
                list.removeIf { it.name == name }
            }
            list.add(entry as RegistryEntry<*>)
            list.sortWith(entryComparator)
            list
        }
    }

    /**
     * Register all entries described by a [ProviderRegistration].
     * 注册 [ProviderRegistration] 描述的所有条目。
     */
    fun <T : Any> register(registration: ProviderRegistration<T>) {
        register(
            serviceClass = registration.serviceClass,
            implClass = registration.implClass,
            priority = registration.priority,
            name = registration.name,
            lazy = registration.lazy,
            factory = registration.factory
        )
    }

    /**
     * Get the highest-priority service instance for the given service class,
     * or null if no implementation is registered.
     * 获取给定服务类的最高优先级服务实例，如果没有注册任何实现则返回 null。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getService(serviceClass: Class<T>): T? {
        val entries = registry[serviceClass] ?: return null
        return entries.firstOrNull()?.getInstance() as T?
    }

    /**
     * Get all registered service instances for the given service class,
     * ordered by priority descending (then by class name).
     * Returns an empty list if no implementations are registered.
     * 获取给定服务类的所有已注册服务实例，按优先级降序排列（然后按类名排序）。
     * 如果没有注册任何实现则返回空列表。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getServices(serviceClass: Class<T>): List<T> {
        val entries = registry[serviceClass] ?: return emptyList()
        return entries.map { it.getInstance() as T }
    }

    /**
     * Get a named service instance for the given service class,
     * or null if no implementation with the given name is registered.
     * 获取给定服务类的指定名称服务实例，如果没有注册具有该名称的实现则返回 null。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getService(serviceClass: Class<T>, name: String): T? {
        val entries = registry[serviceClass] ?: return null
        return entries.firstOrNull { it.name == name }?.getInstance() as T?
    }

    /**
     * Check whether any implementation is registered for the given service class.
     * 检查给定服务类是否注册了任何实现。
     */
    fun <T : Any> hasService(serviceClass: Class<T>): Boolean {
        val entries = registry[serviceClass]
        return entries != null && entries.isNotEmpty()
    }

    /**
     * Check whether a named implementation is registered for the given service class.
     * 检查给定服务类是否注册了指定名称的实现。
     */
    fun <T : Any> hasService(serviceClass: Class<T>, name: String): Boolean {
        val entries = registry[serviceClass] ?: return false
        return entries.any { it.name == name }
    }

    /**
     * Get the total number of registered service interfaces.
     * 获取已注册的服务接口总数。
     */
    fun serviceCount(): Int = registry.size

    /**
     * Get the total number of registered implementations across all services.
     * 获取所有服务中已注册的实现总数。
     */
    fun implementationCount(): Int = registry.values.sumOf { it.size }

    /**
     * Clear all registrations. Useful for testing.
     * 清除所有注册。主要用于测试。
     */
    fun clear() {
        registry.clear()
    }

    /**
     * Dump a human-readable snapshot of all registered services and their
     * implementations for debugging purposes.
     * 输出所有已注册服务及其实现的人类可读快照，用于调试目的。
     *
     * Example output / 输出示例:
     * ```
     * === KSPindle Registry ===
     * Registered Services: 2 interfaces, 3 implementations
     *
     *   com.example.GreetingService (2 implementations):
     *     [10] com.example.EnglishGreeting (name="english", lazy=true, NOT initialized)
     *     [5]  com.example.SpanishGreeting (name="spanish", lazy=true, NOT initialized)
     *
     *   com.example.PaymentService (1 implementations):
     *     [0]  com.example.StripePayment (name="stripe", lazy=true, hasFactory, NOT initialized)
     * ```
     *
     * @return a formatted string representing the current state of the registry.
     *         表示注册表当前状态的格式化字符串。
     */
    fun dump(): String {
        val sb = StringBuilder()
        sb.appendLine("=== KSPindle Registry ===")
        val svcCount = serviceCount()
        val implCount = implementationCount()
        sb.appendLine("Registered Services: $svcCount interfaces, $implCount implementations")
        sb.appendLine()

        if (registry.isEmpty()) {
            sb.appendLine("  (empty / 空)")
        } else {
            // Sort service classes by name for consistent output
            // 按服务类名排序以确保输出一致
            val sortedEntries = registry.entries.sortedBy { it.key.name }
            for ((serviceClass, entries) in sortedEntries) {
                sb.appendLine("  ${serviceClass.name} (${entries.size} implementations):")
                for (entry in entries) {
                    sb.appendLine("    $entry")
                }
                sb.appendLine()
            }
        }

        return sb.toString().trimEnd()
    }

    private companion object {
        // Comparator: sort by priority descending, then by implementation class name for deterministic ordering.
        // 比较器：按优先级降序排序，然后按实现类名排序以确保确定性顺序。
        val entryComparator: Comparator<RegistryEntry<*>> =
            compareByDescending<RegistryEntry<*>> { it.priority }
                .thenBy { it.implClass.name }
    }
}
