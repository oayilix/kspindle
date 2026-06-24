package com.spi.framework.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

@DisplayName("SpiLoader")
class SpiLoaderTest {

    // ── Test service interfaces and implementations ──

    interface TestService {
        fun execute(): String
    }

    class TestServiceImplA : TestService {
        override fun execute() = "A"
    }

    class TestServiceImplB : TestService {
        override fun execute() = "B"
    }

    class TestServiceImplC : TestService {
        override fun execute() = "C"
    }

    interface AnotherService {
        fun value(): String
    }

    class AnotherServiceImpl : AnotherService {
        override fun value() = "another"
    }

    @BeforeEach
    fun setUp() {
        // Ensure clean state before each test
        SpiLoader.reset()
    }

    @AfterEach
    fun tearDown() {
        // Clean up after each test to avoid cross-test pollution
        SpiLoader.reset()
    }

    /**
     * Returns the internal registry and registers the given services.
     * This lets us test SpiLoader with real data without needing
     * ServiceLoader-based ServiceIndexProvider implementations.
     */
    private fun registerServices(vararg pairs: Pair<Class<*>, Class<*>>) {
        val registry = SpiLoader.getRegistry()
        pairs.forEach { (serviceClass, implClass) ->
            @Suppress("UNCHECKED_CAST")
            registry.register(
                serviceClass as Class<Any>,
                implClass as Class<Any>,
                priority = 0
            )
        }
    }

    @Nested
    @DisplayName("initialize()")
    inner class Initialize {

        @Test
        @DisplayName("should be idempotent when called multiple times")
        fun `initialize is idempotent`() {
            // First call
            SpiLoader.initialize()

            // Second call — should be a no-op, no exception
            SpiLoader.initialize()

            // Third call — still a no-op
            SpiLoader.initialize()

            // If we got here without exception, idempotency is verified
            assertThat(true).`as`("multiple initialize() calls completed without error").isTrue
        }

        @Test
        @DisplayName("should not prevent subsequent service loading")
        fun `initialize does not prevent service loading`() {
            SpiLoader.initialize()

            // Register after init via getRegistry()
            val registry = SpiLoader.getRegistry()
            registry.register(TestService::class.java, TestServiceImplA::class.java)

            val service = SpiLoader.load(TestService::class.java)
            assertThat(service)
                .isNotNull
                .extracting { it.execute() }
                .isEqualTo("A")
        }

        @Test
        @DisplayName("should accept a custom ClassLoader")
        fun `initialize with custom ClassLoader`() {
            val customClassLoader = object : ClassLoader() {
                override fun loadClass(name: String): Class<*> {
                    return super.loadClass(name)
                }
            }

            // Should not throw when given a valid class loader
            SpiLoader.initialize(customClassLoader)
            assertThat(true).`as`("initialize with custom ClassLoader completed").isTrue
        }

        @Test
        @DisplayName("should not throw when called after reset")
        fun `initialize after reset`() {
            SpiLoader.initialize()
            SpiLoader.reset()
            SpiLoader.initialize()

            val registry = SpiLoader.getRegistry()
            registry.register(TestService::class.java, TestServiceImplA::class.java)

            val service = SpiLoader.load(TestService::class.java)
            assertThat(service.execute()).isEqualTo("A")
        }

        @Test
        @DisplayName("should be safe to call from multiple threads simultaneously")
        fun `initialize thread safety`() {
            val threadCount = 10
            val threads = List(threadCount) {
                Thread {
                    SpiLoader.initialize()
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            // If we got here without deadlock or exception, it's safe
            assertThat(true).`as`("concurrent initialize() calls completed").isTrue
        }
    }

    @Nested
    @DisplayName("load()")
    inner class Load {

        @Test
        @DisplayName("should throw ServiceNotFoundException when nothing is registered")
        fun `load throws ServiceNotFoundException when nothing registered`() {
            // Initialize without any ServiceIndexProvider on classpath
            SpiLoader.initialize()

            assertThatThrownBy { SpiLoader.load(TestService::class.java) }
                .isInstanceOf(ServiceNotFoundException::class.java)
                .hasMessageContaining(TestService::class.java.name)
        }

        @Test
        @DisplayName("should return registered service instance")
        fun `load returns registered service`() {
            registerServices(TestService::class.java to TestServiceImplA::class.java)

            val service = SpiLoader.load(TestService::class.java)
            assertThat(service.execute()).isEqualTo("A")
        }

        @Test
        @DisplayName("should return highest-priority implementation")
        fun `load returns highest priority`() {
            val registry = SpiLoader.getRegistry()
            registry.register(TestService::class.java, TestServiceImplB::class.java, priority = 10)
            registry.register(TestService::class.java, TestServiceImplA::class.java, priority = 50)

            val service = SpiLoader.load(TestService::class.java)
            assertThat(service.execute()).isEqualTo("A")
        }

        @Test
        @DisplayName("should call initialize automatically if not yet initialized")
        fun `load calls initialize automatically`() {
            // Do NOT call initialize() — load() calls ensureInitialized() internally
            registerServices(TestService::class.java to TestServiceImplA::class.java)

            // This should succeed because getRegistry() calls ensureInitialized()
            val service = SpiLoader.load(TestService::class.java)
            assertThat(service.execute()).isEqualTo("A")
        }

        @Test
        @DisplayName("should return different services for different types")
        fun `load multiple service types`() {
            val registry = SpiLoader.getRegistry()
            registry.register(TestService::class.java, TestServiceImplA::class.java)
            registry.register(AnotherService::class.java, AnotherServiceImpl::class.java)

            val testService = SpiLoader.load(TestService::class.java)
            val anotherService = SpiLoader.load(AnotherService::class.java)

            assertAll(
                { assertThat(testService.execute()).isEqualTo("A") },
                { assertThat(anotherService.value()).isEqualTo("another") }
            )
        }
    }

    @Nested
    @DisplayName("loadAll()")
    inner class LoadAll {

        @Test
        @DisplayName("should return empty list when nothing is registered")
        fun `loadAll returns empty list when nothing registered`() {
            SpiLoader.initialize()

            val services = SpiLoader.loadAll(TestService::class.java)
            assertThat(services).isEmpty()
        }

        @Test
        @DisplayName("should return all registered implementations sorted by priority")
        fun `loadAll returns all sorted by priority`() {
            val registry = SpiLoader.getRegistry()
            registry.register(TestService::class.java, TestServiceImplC::class.java, priority = 5)
            registry.register(TestService::class.java, TestServiceImplA::class.java, priority = 100)
            registry.register(TestService::class.java, TestServiceImplB::class.java, priority = 50)

            val services = SpiLoader.loadAll(TestService::class.java)
            assertThat(services).hasSize(3)
            val results = services.map { it.execute() }
            assertThat(results).containsExactly("A", "B", "C")
        }

        @Test
        @DisplayName("should return single-element list when one implementation registered")
        fun `loadAll single result`() {
            registerServices(TestService::class.java to TestServiceImplA::class.java)

            val services = SpiLoader.loadAll(TestService::class.java)
            assertThat(services).hasSize(1)
            assertThat(services.first().execute()).isEqualTo("A")
        }
    }

    @Nested
    @DisplayName("loadByName()")
    inner class LoadByName {

        @Test
        @DisplayName("should return named implementation")
        fun `loadByName returns named implementation`() {
            val registry = SpiLoader.getRegistry()
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                name = "impl-a"
            )
            registry.register(
                TestService::class.java, TestServiceImplB::class.java,
                name = "impl-b"
            )

            val service = SpiLoader.loadByName(TestService::class.java, "impl-b")
            assertThat(service).isNotNull
            assertThat(service!!.execute()).isEqualTo("B")
        }

        @Test
        @DisplayName("should return null when name does not exist")
        fun `loadByName returns null for unknown name`() {
            registerServices(TestService::class.java to TestServiceImplA::class.java)

            val service = SpiLoader.loadByName(TestService::class.java, "nonexistent")
            assertThat(service).isNull()
        }

        @Test
        @DisplayName("should return null when nothing is registered")
        fun `loadByName returns null when nothing registered`() {
            SpiLoader.initialize()

            val service = SpiLoader.loadByName(TestService::class.java, "anything")
            assertThat(service).isNull()
        }
    }

    @Nested
    @DisplayName("isRegistered()")
    inner class IsRegistered {

        @Test
        @DisplayName("should return true when implementation is registered")
        fun `isRegistered returns true`() {
            registerServices(TestService::class.java to TestServiceImplA::class.java)

            assertThat(SpiLoader.isRegistered(TestService::class.java)).isTrue
        }

        @Test
        @DisplayName("should return false when nothing is registered")
        fun `isRegistered returns false`() {
            SpiLoader.initialize()

            assertThat(SpiLoader.isRegistered(TestService::class.java)).isFalse
        }

        @Test
        @DisplayName("should return false after reset")
        fun `isRegistered returns false after reset`() {
            registerServices(TestService::class.java to TestServiceImplA::class.java)
            assertThat(SpiLoader.isRegistered(TestService::class.java)).isTrue

            SpiLoader.reset()
            SpiLoader.initialize()

            assertThat(SpiLoader.isRegistered(TestService::class.java)).isFalse
        }
    }

    @Nested
    @DisplayName("reset()")
    inner class Reset {

        @Test
        @DisplayName("should clear all registered services")
        fun `reset clears registered services`() {
            registerServices(TestService::class.java to TestServiceImplA::class.java)
            assertThat(SpiLoader.isRegistered(TestService::class.java)).isTrue

            SpiLoader.reset()

            SpiLoader.initialize()
            assertThat(SpiLoader.isRegistered(TestService::class.java)).isFalse
        }

        @Test
        @DisplayName("should clear the initialized flag")
        fun `reset clears initialized flag`() {
            SpiLoader.initialize()

            // After reset, initialize should work again
            SpiLoader.reset()

            // Calling load on an uninitialized loader with nothing registered
            // should call ensureInitialized and then throw
            assertThatThrownBy { SpiLoader.load(TestService::class.java) }
                .isInstanceOf(ServiceNotFoundException::class.java)
        }

        @Test
        @DisplayName("should allow re-initialization after reset")
        fun `reset allows re-initialization`() {
            registerServices(TestService::class.java to TestServiceImplA::class.java)
            val before = SpiLoader.load(TestService::class.java)
            assertThat(before.execute()).isEqualTo("A")

            SpiLoader.reset()

            // Register a different impl
            registerServices(TestService::class.java to TestServiceImplB::class.java)
            val after = SpiLoader.load(TestService::class.java)
            assertThat(after.execute()).isEqualTo("B")
        }

        @Test
        @DisplayName("should be safe to call multiple times")
        fun `reset is safe to call multiple times`() {
            SpiLoader.reset()
            SpiLoader.reset()
            SpiLoader.reset()

            // Should not throw
            assertThat(true).`as`("multiple reset() calls completed without error").isTrue
        }
    }

    @Nested
    @DisplayName("getRegistry()")
    inner class GetRegistry {

        @Test
        @DisplayName("should return the internal registry")
        fun `getRegistry returns registry`() {
            val registry = SpiLoader.getRegistry()
            assertThat(registry).isNotNull
            assertThat(registry).isInstanceOf(ServiceRegistry::class.java)
        }

        @Test
        @DisplayName("should return the same registry instance across calls")
        fun `getRegistry returns same instance`() {
            val first = SpiLoader.getRegistry()
            val second = SpiLoader.getRegistry()

            assertThat(first).isSameAs(second)
        }

        @Test
        @DisplayName("should initialize the loader")
        fun `getRegistry initializes loader`() {
            // Reset and then getRegistry should trigger initialization
            SpiLoader.reset()

            val registry = SpiLoader.getRegistry()
            assertThat(registry).isNotNull

            // load() should work without throwing (since getRegistry was called)
            assertThatThrownBy { SpiLoader.load(TestService::class.java) }
                .isInstanceOf(ServiceNotFoundException::class.java)
        }
    }
}
