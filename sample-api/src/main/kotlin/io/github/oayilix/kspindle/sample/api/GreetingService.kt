package io.github.oayilix.kspindle.sample.api

import io.github.oayilix.kspindle.annotations.ServiceProviderInterface

/**
 * Service interface for greeting functionality.
 * 问候功能的服务接口。
 *
 * This interface lives in the **sample-api** module, which is the **API module**.
 * Business/application modules (e.g., sample-app) depend ONLY on this module.
 * Implementation modules (e.g., sample-impl) also depend on this module to
 * provide concrete service implementations.
 * 该接口位于 **sample-api** 模块中，即 **API 模块**。
 * 业务/应用模块（如 sample-app）仅依赖于此模块。
 * 实现模块（如 sample-impl）也依赖于此模块来提供具体的服务实现。
 *
 * The SPI framework handles discovery of implementations at runtime, so the
 * business code never needs to import or instantiate implementation classes
 * directly. This enforces true decoupling between API and implementation.
 * KSPindle 在运行时处理实现的发现，因此业务代码无需直接导入或实例化实现类。
 * 这确保了 API 与实现之间的真正解耦。
 *
 * @see io.github.oayilix.kspindle.sample.impl.EnglishGreeting
 * @see io.github.oayilix.kspindle.sample.impl.SpanishGreeting
 */
@ServiceProviderInterface(name = "Greeting Service")
interface GreetingService {

    /**
     * Return a greeting string.
     * 返回问候字符串。
     */
    fun greet(): String
}
