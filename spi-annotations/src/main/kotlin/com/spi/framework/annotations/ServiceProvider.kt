package com.spi.framework.annotations

import kotlin.reflect.KClass

/**
 * Marks a class as a service provider implementation for the specified service interface.
 * 标记一个类作为指定服务接口的服务提供者实现。
 *
 * The KSP processor discovers annotated classes at compile time and generates
 * a [com.spi.framework.core.ServiceIndexProvider] that registers them into the
 * [com.spi.framework.core.ServiceRegistry] for runtime discovery.
 * KSP 处理器在编译时发现被注解的类，并生成 [ServiceIndexProvider] 实现，
 * 将其注册到 [ServiceRegistry] 中，供运行时发现使用。
 *
 * This annotation is repeatable — a single class can implement multiple
 * service interfaces.
 * 该注解是可重复的——单个类可以实现多个服务接口。
 *
 * Example usage / 使用示例:
 * ```kotlin
 * @ServiceProvider(service = GreetingService::class, priority = 10, name = "english")
 * class EnglishGreeting : GreetingService {
 *     override fun greet() = "Hello"
 * }
 * ```
 *
 * @property service  The service interface this class implements. 该类实现的服务接口。
 * @property priority Ordering priority. Higher values are returned first
 *                    by [com.spi.framework.core.SpiLoader.loadAll].
 *                    Default is 0.
 *                    排序优先级。值越高的实现由 [SpiLoader.loadAll] 优先返回。默认值为 0。
 * @property name     An optional logical name for this implementation, enabling
 *                    retrieval by name via [com.spi.framework.core.SpiLoader.loadByName].
 *                    Default is empty string (unnamed).
 *                    该实现的可选逻辑名称，可通过 [SpiLoader.loadByName] 按名称检索。默认值为空字符串（未命名）。
 * @property lazy     Whether the implementation should be instantiated lazily
 *                    (on first access). When false, the instance is created
 *                    eagerly at initialization time. Default is true.
 *                    实现是否应延迟实例化（首次访问时）。若为 false，则在初始化时立即创建实例。默认值为 true。
 * @property factory  An optional [ServiceFactory] class for custom instantiation logic.
 *                    Use this when the implementation class requires constructor parameters.
 *                    Default is [ServiceFactory.None] (no factory, uses no-arg constructor).
 *                    可选的 [ServiceFactory] 类，用于自定义实例化逻辑。
 *                    当实现类需要构造函数参数时使用此选项。
 *                    默认为 [ServiceFactory.None]（无工厂，使用无参构造函数）。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class ServiceProvider(
    val service: KClass<*>,
    val priority: Int = 0,
    val name: String = "",
    val lazy: Boolean = true,
    val factory: KClass<out ServiceFactory<*>> = ServiceFactory.None::class
)
