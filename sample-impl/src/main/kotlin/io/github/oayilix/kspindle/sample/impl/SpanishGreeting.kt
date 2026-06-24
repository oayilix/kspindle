package io.github.oayilix.kspindle.sample.impl

import io.github.oayilix.kspindle.annotations.ServiceProvider
import io.github.oayilix.kspindle.sample.api.GreetingService

/**
 * Spanish implementation of [GreetingService].
 * [GreetingService] 的西班牙语实现。
 *
 * This class lives in the **sample-impl** module, which is the **implementation module**.
 * Business/application code (e.g., sample-app) never imports this class directly.
 * It is discovered through SPI at runtime via the [@ServiceProvider] annotation.
 * 该类位于 **sample-impl** 模块中，即 **实现模块**。
 * 业务/应用代码（如 sample-app）从不直接导入此类。
 * 它通过 [@ServiceProvider] 注解在运行时通过 SPI 发现。
 *
 * Registered with lower priority (5) than [EnglishGreeting] for demonstration purposes.
 * 优先级（5）比 [EnglishGreeting] 低，用于演示目的。
 *
 * @see io.github.oayilix.kspindle.sample.api.GreetingService The interface this implements / 该类实现的接口
 * @see io.github.oayilix.kspindle.runtime.Kspindle How implementations are discovered at runtime / 实现如何在运行时被发现
 */
@ServiceProvider(service = GreetingService::class, priority = 5, name = "spanish")
class SpanishGreeting : GreetingService {

    override fun greet(): String {
        return "!Hola desde SPI!"
    }
}
