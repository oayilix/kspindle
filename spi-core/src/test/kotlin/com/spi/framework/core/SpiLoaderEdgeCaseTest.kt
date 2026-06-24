package com.spi.framework.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

/**
 * Edge case tests for SpiLoader.
 * SpiLoader 的边界情况测试。
 */
class SpiLoaderEdgeCaseTest {

    // ── Test service interfaces and implementations ──

    interface EdgeService {
        fun execute(): String
    }

    class EdgeServiceImpl : EdgeService {
        override fun execute() = "result"
    }

    interface EdgeNamedService {
        fun label(): String
    }

    class EdgeNamedAlpha : EdgeNamedService {
        override fun label() = "alpha"
    }

    class EdgeNamedBeta : EdgeNamedService {
        override fun label() = "beta"
    }

    class EdgeNamedGamma : EdgeNamedService {
        override fun label() = "gamma"
    }

    interface EdgeServiceA {
        fun doA(): String
    }

    interface EdgeServiceB {
        fun doB(): String
    }

    class EdgeDualServiceImpl : EdgeServiceA, EdgeServiceB {
        override fun doA() = "serviceA"
        override fun doB() = "serviceB"
    }

    @AfterEach
    fun tearDown() {
        SpiLoader.reset()
    }

    // 加载不存在的服务会抛出 ServiceNotFoundException
    @Test
    fun `load non-existent service throws ServiceNotFoundException`() {
        SpiLoader.initialize()

        assertThatThrownBy { SpiLoader.load(EdgeService::class.java) }
            .isInstanceOf(ServiceNotFoundException::class.java)
            .hasMessageContaining(EdgeService::class.java.name)
    }

    // 使用不存在的名称调用 loadByName 返回 null
    @Test
    fun `loadByName with non-existent name returns null`() {
        val registry = SpiLoader.getRegistry()
        registry.register(
            EdgeNamedService::class.java,
            EdgeNamedAlpha::class.java,
            name = "alpha"
        )

        val result = SpiLoader.loadByName(EdgeNamedService::class.java, "nonexistent")
        assertThat(result).isNull()
    }

    // loadByName 的空名称匹配未命名服务（空字符串名称匹配）
    @Test
    fun `loadByName with empty name matches unnamed service`() {
        val registry = SpiLoader.getRegistry()
        registry.register(
            EdgeNamedService::class.java,
            EdgeNamedAlpha::class.java,
            name = ""
        )

        val service = SpiLoader.loadByName(EdgeNamedService::class.java, "")
        assertThat(service).isNotNull
        assertThat(service!!.label()).isEqualTo("alpha")
    }

    // 无注册时 loadAll 返回空列表（不为 null）
    @Test
    fun `loadAll with no registrations returns empty list`() {
        SpiLoader.initialize()

        val all = SpiLoader.loadAll(EdgeService::class.java)
        assertThat(all).isNotNull
        assertThat(all).isEmpty()
    }

    // 无注册时 isRegistered 返回 false
    @Test
    fun `isRegistered with no registrations returns false`() {
        SpiLoader.initialize()

        assertThat(SpiLoader.isRegistered(EdgeService::class.java)).isFalse
    }

    // 多次注册相同服务（未命名、不同优先级），loadAll 返回所有并按优先级排序
    @Test
    fun `register same service multiple times returns both sorted`() {
        val registry = SpiLoader.getRegistry()
        registry.register(
            EdgeNamedService::class.java,
            EdgeNamedBeta::class.java,
            priority = 5
        )
        registry.register(
            EdgeNamedService::class.java,
            EdgeNamedAlpha::class.java,
            priority = 10
        )

        val all = SpiLoader.loadAll(EdgeNamedService::class.java)
        assertThat(all).hasSize(2)
        assertAll(
            { assertThat(all[0].label()).isEqualTo("alpha") },
            { assertThat(all[1].label()).isEqualTo("beta") }
        )
    }

    // 使用极值优先级注册不应抛出异常
    @Test
    fun `register with extreme priority values does not throw`() {
        val registry = SpiLoader.getRegistry()

        registry.register(
            EdgeNamedService::class.java,
            EdgeNamedAlpha::class.java,
            priority = Int.MAX_VALUE
        )
        registry.register(
            EdgeNamedService::class.java,
            EdgeNamedBeta::class.java,
            priority = Int.MIN_VALUE
        )

        val all = SpiLoader.loadAll(EdgeNamedService::class.java)
        assertThat(all).hasSize(2)
        // Int.MAX_VALUE should come first
        assertThat(all[0].label()).isEqualTo("alpha")
        assertThat(all[1].label()).isEqualTo("beta")
    }

    // 重复调用 SpiLoader.initialize() 是幂等的
    @Test
    fun `initialize called twice is idempotent`() {
        SpiLoader.reset()

        // Call initialize multiple times
        SpiLoader.initialize()
        SpiLoader.initialize()
        SpiLoader.initialize()

        // After idempotent initialize, loading should work
        val registry = SpiLoader.getRegistry()
        registry.register(EdgeService::class.java, EdgeServiceImpl::class.java)

        val service = SpiLoader.load(EdgeService::class.java)
        assertThat(service.execute()).isEqualTo("result")
    }

    // 不显式调用 initialize 时，getRegistry() 会自动初始化
    @Test
    fun `getRegistry auto-initializes without explicit initialize`() {
        SpiLoader.reset()

        // Do NOT call SpiLoader.initialize() explicitly
        val registry = SpiLoader.getRegistry()
        assertThat(registry).isNotNull
        assertThat(registry.serviceCount()).isZero()
    }

    // 同一类实现多个服务接口，每个接口都能正确返回对应类型
    @Test
    fun `multiple service interfaces on same class return correct types`() {
        val registry = SpiLoader.getRegistry()
        registry.register(EdgeServiceA::class.java, EdgeDualServiceImpl::class.java)
        registry.register(EdgeServiceB::class.java, EdgeDualServiceImpl::class.java)

        val serviceA = SpiLoader.load(EdgeServiceA::class.java)
        val serviceB = SpiLoader.load(EdgeServiceB::class.java)

        assertAll(
            { assertThat(serviceA.doA()).isEqualTo("serviceA") },
            { assertThat(serviceB.doB()).isEqualTo("serviceB") },
            { assertThat(serviceA).isInstanceOf(EdgeDualServiceImpl::class.java) },
            { assertThat(serviceB).isInstanceOf(EdgeDualServiceImpl::class.java) },
            { assertThat(serviceA).isNotSameAs(serviceB) }
        )
    }
}
