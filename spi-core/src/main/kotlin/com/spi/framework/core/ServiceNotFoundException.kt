package com.spi.framework.core

/**
 * Exception thrown when no implementation is registered for a requested service.
 * 当请求的服务没有注册任何实现时抛出的异常。
 *
 * This typically indicates that / 这通常表示：
 * - The service interface has no classes annotated with
 *   [com.spi.framework.annotations.ServiceProvider].
 *   服务接口没有使用 [ServiceProvider] 注解的类。
 * - The spi-compiler KSP processor was not applied to the module containing
 *   the annotated classes.
 *   包含注解类的模块未应用 spi-compiler KSP 处理器。
 * - [SpiLoader.initialize] was not called before attempting to load services.
 *   在尝试加载服务之前未调用 [SpiLoader.initialize]。
 *
 * @property message a human-readable description of the error.
 *                   人类可读的错误描述信息。
 */
class ServiceNotFoundException(message: String) : RuntimeException(message)
