package com.spi.sample.impl

import com.spi.framework.annotations.ServiceProvider
import com.spi.sample.api.GreetingService

/**
 * English implementation of [GreetingService].
 * [GreetingService] 的英文实现。
 *
 * This class lives in the **sample-impl** module, which is the **implementation module**.
 * Business/application code (e.g., sample-app) never imports this class directly.
 * It is discovered through SPI at runtime via the [@ServiceProvider] annotation.
 * 该类位于 **sample-impl** 模块中，即 **实现模块**。
 * 业务/应用代码（如 sample-app）从不直接导入此类。
 * 它通过 [@ServiceProvider] 注解在运行时通过 SPI 发现。
 *
 * Registered with high priority (10) to be returned first by [com.spi.framework.core.SpiLoader.load].
 * 以高优先级（10）注册，以便被 [SpiLoader.load] 优先返回。
 *
 * @see com.spi.sample.api.GreetingService The interface this implements / 该类实现的接口
 * @see com.spi.framework.core.SpiLoader How implementations are discovered at runtime / 实现如何在运行时被发现
 */
@ServiceProvider(service = GreetingService::class, priority = 10, name = "english")
class EnglishGreeting : GreetingService {

    override fun greet(): String {
        return "Hello from SPI!"
    }
}
