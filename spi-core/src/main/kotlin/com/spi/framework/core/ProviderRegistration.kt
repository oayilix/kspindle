package com.spi.framework.core

/**
 * Describes a single service provider registration.
 * 描述单个服务提供者的注册信息。
 *
 * Used by generated [ServiceIndexProvider] implementations to communicate
 * registrations to the [ServiceRegistry].
 * 由生成的 [ServiceIndexProvider] 实现使用，用于将注册信息传递给 [ServiceRegistry]。
 *
 * @param T the service interface type. 服务接口类型。
 * @property serviceClass the service interface [Class]. 服务接口的 [Class]。
 * @property implClass the concrete implementation [Class] (must extend [serviceClass]).
 *                     具体实现 [Class]（必须继承 [serviceClass]）。
 * @property priority the ordering priority (higher = returned first).
 *                    排序优先级（值越高越先返回）。
 * @property name an optional logical name for named lookups.
 *                用于按名称查找的可选逻辑名称。
 * @property lazy whether to instantiate lazily.
 *                是否延迟实例化。
 * @property factory an optional factory lambda for custom instantiation.
 *                   When non-null, it is invoked to create instances instead of
 *                   using reflection-based no-arg construction. Default is null.
 *                   可选的工厂 lambda，用于自定义实例化。
 *                   当非 null 时，将调用它来创建实例，而不是使用基于反射的无参构造。默认值为 null。
 */
data class ProviderRegistration<T : Any>(
    val serviceClass: Class<T>,
    val implClass: Class<out T>,
    val priority: Int = 0,
    val name: String = "",
    val lazy: Boolean = true,
    val factory: (() -> T)? = null
)
