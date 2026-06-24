package com.spi.framework.annotations

/**
 * Factory interface for creating service instances with custom initialization logic.
 * Use this when service implementations require constructor dependencies.
 *
 * 服务实例工厂接口，用于自定义初始化逻辑。
 * 当服务实现需要构造函数依赖时使用此接口。
 *
 * @param T the service interface type. 服务接口类型。
 */
interface ServiceFactory<T : Any> {
    /**
     * Create a new instance of the service.
     * 创建服务的新实例。
     */
    fun create(): T

    /**
     * Default sentinel value indicating "no factory, use no-arg constructor".
     * 默认标记值，表示"无工厂，使用无参构造函数"。
     */
    class None : ServiceFactory<Nothing> {
        override fun create(): Nothing =
            throw UnsupportedOperationException("ServiceFactory.None is a sentinel")
    }
}
