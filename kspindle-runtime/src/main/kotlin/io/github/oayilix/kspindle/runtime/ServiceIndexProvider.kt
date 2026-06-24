package io.github.oayilix.kspindle.runtime

/**
 * Contract for generated code that populates the [ServiceRegistry].
 * 为填充 [ServiceRegistry] 的生成代码定义的契约。
 *
 * The KSP processor generates one implementation of this interface per module
 * that uses [io.github.oayilix.kspindle.annotations.ServiceProvider] annotations.
 * KSP 处理器为每个使用了 [ServiceProvider] 注解的模块生成该接口的一个实现。
 *
 * At runtime, [Kspindle] discovers all implementations via [java.util.ServiceLoader]
 * and calls [initialize] on each to populate the shared [ServiceRegistry].
 * 在运行时，[Kspindle] 通过 [java.util.ServiceLoader] 发现所有实现，
 * 并在每个实现上调用 [initialize] 以填充共享的 [ServiceRegistry]。
 */
interface ServiceIndexProvider {
    /**
     * Called by [Kspindle] during initialization to register all
     * service implementations discovered by the KSP processor.
     * 由 [Kspindle] 在初始化期间调用，用于注册 KSP 处理器发现的所有服务实现。
     *
     * @param registry the shared [ServiceRegistry] to populate. 要填充的共享 [ServiceRegistry]。
     */
    fun initialize(registry: ServiceRegistry)
}
