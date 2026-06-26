package io.github.oayilix.kspindle.sample.impl

import io.github.oayilix.kspindle.annotations.ServiceProvider
import io.github.oayilix.kspindle.sample.api.GreetingService

/**
 * French implementation of [GreetingService].
 * [GreetingService] 的法语实现。
 *
 * This sample intentionally has a parameterized primary constructor plus a
 * no-argument secondary constructor. It verifies that the KSP processor accepts
 * service implementations that are still compatible with reflective
 * `getDeclaredConstructor()` instantiation.
 * 该示例有一个带参数的主构造函数，同时提供无参 secondary constructor。
 * 它用于验证 KSP 处理器允许这种仍可通过 `getDeclaredConstructor()` 反射创建的实现类。
 */
@ServiceProvider(service = GreetingService::class, priority = 1, name = "french")
class FrenchGreeting(private val greeting: String) : GreetingService {

    constructor() : this("Bonjour depuis SPI!")

    override fun greet(): String {
        return greeting
    }
}
