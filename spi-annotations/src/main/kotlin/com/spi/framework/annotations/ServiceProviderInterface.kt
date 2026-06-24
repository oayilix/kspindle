package com.spi.framework.annotations

/**
 * Marks an interface as a service provider interface (SPI).
 * 标记一个接口为服务提供者接口（SPI）。
 *
 * This annotation is optional — any interface or abstract class can be used
 * as a service type without this annotation. It primarily serves as documentation
 * and enables the KSP processor to perform additional validation.
 * 此注解是可选的——任何接口或抽象类都可以作为服务类型使用，无需此注解。
 * 它主要用于文档目的，并使 KSP 处理器能够执行额外的验证。
 *
 * Example usage / 使用示例:
 * ```kotlin
 * @ServiceProviderInterface(name = "Greeting Service")
 * interface GreetingService {
 *     fun greet(): String
 * }
 * ```
 *
 * @property name A human-readable name for this service interface. 该服务接口的可读名称。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ServiceProviderInterface(
    val name: String = ""
)
