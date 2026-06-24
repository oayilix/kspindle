package io.github.oayilix.kspindle.runtime

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("ServiceRegistry")
class ServiceRegistryTest {

    private lateinit var registry: ServiceRegistry

    @BeforeEach
    fun setUp() {
        registry = ServiceRegistry()
    }

    @AfterEach
    fun tearDown() {
        registry.clear()
    }

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

    // Concrete named classes for concurrent registration tests.
    // These have public no-arg constructors, required by RegistryEntry.createInstance().
    class ThreadSafeImpl0 : TestService { override fun execute() = "ts0" }
    class ThreadSafeImpl1 : TestService { override fun execute() = "ts1" }
    class ThreadSafeImpl2 : TestService { override fun execute() = "ts2" }
    class ThreadSafeImpl3 : TestService { override fun execute() = "ts3" }
    class ThreadSafeImpl4 : TestService { override fun execute() = "ts4" }
    class ThreadSafeImpl5 : TestService { override fun execute() = "ts5" }
    class ThreadSafeImpl6 : TestService { override fun execute() = "ts6" }
    class ThreadSafeImpl7 : TestService { override fun execute() = "ts7" }
    class ThreadSafeImpl8 : TestService { override fun execute() = "ts8" }
    class ThreadSafeImpl9 : TestService { override fun execute() = "ts9" }

    private val threadSafeImpls: List<Class<out TestService>> = listOf(
        ThreadSafeImpl0::class.java,
        ThreadSafeImpl1::class.java,
        ThreadSafeImpl2::class.java,
        ThreadSafeImpl3::class.java,
        ThreadSafeImpl4::class.java,
        ThreadSafeImpl5::class.java,
        ThreadSafeImpl6::class.java,
        ThreadSafeImpl7::class.java,
        ThreadSafeImpl8::class.java,
        ThreadSafeImpl9::class.java,
    )

    interface AnotherTestService {
        fun process(): String
    }

    class AnotherTestServiceImpl : AnotherTestService {
        override fun process() = "done"
    }

    interface YetAnotherService {
        fun run(): String
    }

    class YetAnotherServiceImpl : YetAnotherService {
        override fun run() = "running"
    }

    interface LazyTestService {
        fun ping(): String
    }

    class LazyTestServiceImpl : LazyTestService {
        override fun ping() = "pong"
    }

    class CountedLazyTestServiceImpl : LazyTestService {
        companion object {
            val constructorCallCount = AtomicInteger(0)

            fun resetCount() {
                constructorCallCount.set(0)
            }
        }

        init {
            constructorCallCount.incrementAndGet()
        }

        override fun ping() = "counted-pong"
    }

    // ── Tests ──

    @Nested
    @DisplayName("register()")
    inner class Register {

        @Test
        @DisplayName("should register a single implementation")
        fun `register single implementation`() {
            registry.register(TestService::class.java, TestServiceImplA::class.java)
            assertThat(registry.hasService(TestService::class.java)).isTrue
        }

        @Test
        @DisplayName("should register multiple implementations with priority ordering")
        fun `register multiple implementations with priority ordering`() {
            registry.register(
                TestService::class.java, TestServiceImplC::class.java,
                priority = 10
            )
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                priority = 30
            )
            registry.register(
                TestService::class.java, TestServiceImplB::class.java,
                priority = 20
            )

            val services = registry.getServices(TestService::class.java)
            assertThat(services).hasSize(3)
            val results = services.map { (it as TestService).execute() }
            assertThat(results).containsExactly("A", "B", "C")
        }

        @Test
        @DisplayName("should use class name as tiebreaker when priorities are equal")
        fun `register same priority uses class name tiebreaker`() {
            registry.register(
                TestService::class.java, TestServiceImplB::class.java,
                priority = 0
            )
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                priority = 0
            )

            val services = registry.getServices(TestService::class.java)
            assertThat(services).hasSize(2)
            val results = services.map { (it as TestService).execute() }
            assertThat(results).containsExactly("A", "B")
        }

        @Test
        @DisplayName("should accept ProviderRegistration overload")
        fun `register with ProviderRegistration`() {
            val registration = ProviderRegistration(
                serviceClass = TestService::class.java,
                implClass = TestServiceImplA::class.java,
                priority = 42,
                name = "named-impl",
                lazy = false
            )
            registry.register(registration)

            assertThat(registry.hasService(TestService::class.java)).isTrue
            assertThat(registry.getService(TestService::class.java, "named-impl"))
                .isNotNull
                .extracting { (it as TestService).execute() }
                .isEqualTo("A")
        }
    }

    @Nested
    @DisplayName("getService()")
    inner class GetService {

        @Test
        @DisplayName("should return highest-priority implementation")
        fun `getService returns highest priority`() {
            registry.register(
                TestService::class.java, TestServiceImplB::class.java,
                priority = 10
            )
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                priority = 50
            )

            val service = registry.getService(TestService::class.java)
            assertThat(service)
                .isNotNull
                .extracting { (it as TestService).execute() }
                .isEqualTo("A")
        }

        @Test
        @DisplayName("should return null when no implementation registered")
        fun `getService returns null for empty registry`() {
            val service = registry.getService(TestService::class.java)
            assertThat(service).isNull()
        }

        @Test
        @DisplayName("should return null when different service type is registered")
        fun `getService returns null for unregistered type`() {
            registry.register(TestService::class.java, TestServiceImplA::class.java)

            // Register with a different type
            val service = registry.getService(Comparable::class.java)
            assertThat(service).isNull()
        }
    }

    @Nested
    @DisplayName("getServices()")
    inner class GetServices {

        @Test
        @DisplayName("should return all implementations sorted by priority descending")
        fun `getServices returns all sorted by priority descending`() {
            registry.register(
                TestService::class.java, TestServiceImplC::class.java,
                priority = 5
            )
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                priority = 100
            )
            registry.register(
                TestService::class.java, TestServiceImplB::class.java,
                priority = 50
            )

            val services = registry.getServices(TestService::class.java)
            assertThat(services).hasSize(3)
            val results = services.map { (it as TestService).execute() }
            assertThat(results).containsExactly("A", "B", "C")
        }

        @Test
        @DisplayName("should return empty list when no implementation registered")
        fun `getServices returns empty list for empty registry`() {
            val services = registry.getServices(TestService::class.java)
            assertThat(services).isEmpty()
        }

        @Test
        @DisplayName("should return empty list when different service type is registered")
        fun `getServices returns empty list for unregistered type`() {
            registry.register(TestService::class.java, TestServiceImplA::class.java)

            val services = registry.getServices(Comparable::class.java)
            assertThat(services).isEmpty()
        }

        @Test
        @DisplayName("should return single-element list when one implementation registered")
        fun `getServices with single implementation`() {
            registry.register(
                TestService::class.java, TestServiceImplA::class.java
            )

            val services = registry.getServices(TestService::class.java)
            assertThat(services)
                .hasSize(1)
                .first()
                .extracting { (it as TestService).execute() }
                .isEqualTo("A")
        }
    }

    @Nested
    @DisplayName("getService(name)")
    inner class GetServiceByName {

        @Test
        @DisplayName("should return named implementation")
        fun `getService(name) returns named implementation`() {
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                name = "impl-a"
            )
            registry.register(
                TestService::class.java, TestServiceImplB::class.java,
                name = "impl-b"
            )

            val service = registry.getService(TestService::class.java, "impl-b")
            assertThat(service)
                .isNotNull
                .extracting { (it as TestService).execute() }
                .isEqualTo("B")
        }

        @Test
        @DisplayName("should return null when name does not exist")
        fun `getService(name) returns null for unknown name`() {
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                name = "impl-a"
            )

            val service = registry.getService(TestService::class.java, "nonexistent")
            assertThat(service).isNull()
        }

        @Test
        @DisplayName("should return null when registry is empty")
        fun `getService(name) returns null for empty registry`() {
            val service = registry.getService(TestService::class.java, "anything")
            assertThat(service).isNull()
        }

        @Test
        @DisplayName("should return highest-priority named match")
        fun `getService(name) returns correct instance when names overlap with priorities`() {
            registry.register(
                TestService::class.java, TestServiceImplB::class.java,
                priority = 5, name = "same-name"
            )
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                priority = 10, name = "same-name"
            )

            // Should return the first entry matching the name
            val service = registry.getService(TestService::class.java, "same-name")
            assertThat(service)
                .isNotNull
                .extracting { (it as TestService).execute() }
                .isEqualTo("A")
        }

        @Test
        @DisplayName("should replace an existing non-empty named registration")
        fun `getService(name) returns latest non-empty named registration`() {
            registry.register(
                TestService::class.java, TestServiceImplB::class.java,
                priority = 100, name = "same-name"
            )
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                priority = 1, name = "same-name"
            )

            val service = registry.getService(TestService::class.java, "same-name")
            assertThat(service)
                .isNotNull
                .extracting { (it as TestService).execute() }
                .isEqualTo("A")
            assertThat(registry.implementationCount()).isEqualTo(1)
        }

        @Test
        @DisplayName("should return null when name is empty string")
        fun `getService(name) returns null when name is empty and no unnamed registered`() {
            // Register with a non-empty name only
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                name = "impl-a"
            )

            val service = registry.getService(TestService::class.java, "")
            assertThat(service).isNull()
        }
    }

    @Nested
    @DisplayName("hasService()")
    inner class HasService {

        @Test
        @DisplayName("should return true when implementation is registered")
        fun `hasService returns true when registered`() {
            registry.register(TestService::class.java, TestServiceImplA::class.java)

            assertThat(registry.hasService(TestService::class.java)).isTrue
        }

        @Test
        @DisplayName("should return false when nothing is registered")
        fun `hasService returns false when nothing registered`() {
            assertThat(registry.hasService(TestService::class.java)).isFalse
        }

        @Test
        @DisplayName("should return false after clear()")
        fun `hasService returns false after clear`() {
            registry.register(TestService::class.java, TestServiceImplA::class.java)
            registry.clear()

            assertThat(registry.hasService(TestService::class.java)).isFalse
        }

        @Test
        @DisplayName("should return true for named implementation that exists")
        fun `hasService(name) returns true when name exists`() {
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                name = "my-impl"
            )

            assertThat(registry.hasService(TestService::class.java, "my-impl")).isTrue
        }

        @Test
        @DisplayName("should return false for named implementation that does not exist")
        fun `hasService(name) returns false when name does not exist`() {
            registry.register(
                TestService::class.java, TestServiceImplA::class.java,
                name = "my-impl"
            )

            assertThat(registry.hasService(TestService::class.java, "other")).isFalse
        }
    }

    @Nested
    @DisplayName("empty registry")
    inner class EmptyRegistry {

        @Test
        @DisplayName("getService returns null")
        fun `getService returns null`() {
            assertThat(registry.getService(TestService::class.java)).isNull()
        }

        @Test
        @DisplayName("getServices returns empty list")
        fun `getServices returns empty list`() {
            assertThat(registry.getServices(TestService::class.java)).isEmpty()
        }

        @Test
        @DisplayName("hasService returns false")
        fun `hasService returns false`() {
            assertThat(registry.hasService(TestService::class.java)).isFalse()
        }

        @Test
        @DisplayName("serviceCount returns 0")
        fun `serviceCount returns 0`() {
            assertThat(registry.serviceCount()).isZero()
        }

        @Test
        @DisplayName("implementationCount returns 0")
        fun `implementationCount returns 0`() {
            assertThat(registry.implementationCount()).isZero()
        }
    }

    @Nested
    @DisplayName("clear()")
    inner class Clear {

        @Test
        @DisplayName("should reset the registry to empty state")
        fun `clear resets registry`() {
            registry.register(TestService::class.java, TestServiceImplA::class.java)
            registry.register(AnotherTestService::class.java, AnotherTestServiceImpl::class.java)

            assertThat(registry.serviceCount()).isEqualTo(2)
            assertThat(registry.implementationCount()).isEqualTo(2)

            registry.clear()

            assertAll(
                { assertThat(registry.serviceCount()).isZero() },
                { assertThat(registry.implementationCount()).isZero() },
                { assertThat(registry.hasService(TestService::class.java)).isFalse },
                { assertThat(registry.hasService(AnotherTestService::class.java)).isFalse }
            )
        }

        @Test
        @DisplayName("should allow re-registration after clear")
        fun `clear allows re-registration`() {
            registry.register(TestService::class.java, TestServiceImplA::class.java)
            registry.clear()

            registry.register(TestService::class.java, TestServiceImplB::class.java)
            val service = registry.getService(TestService::class.java)
            assertThat(service)
                .extracting { (it as TestService).execute() }
                .isEqualTo("B")
        }

        @Test
        @DisplayName("should be safe to call on an already empty registry")
        fun `clear on empty registry is safe`() {
            registry.clear()
            assertThat(registry.serviceCount()).isZero()
        }
    }

    @Nested
    @DisplayName("thread safety")
    inner class ThreadSafety {

        @Test
        @DisplayName("should handle concurrent registrations from multiple threads")
        fun `concurrent registrations`() {
            val threadCount = 10
            val registrationsPerThread = 100
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val errors = ConcurrentHashMap.newKeySet<Throwable>()

            repeat(threadCount) { threadIndex ->
                executor.submit {
                    try {
                        repeat(registrationsPerThread) { regIndex ->
                            val implClass = threadSafeImpls[regIndex % threadSafeImpls.size]
                            registry.register(
                                TestService::class.java,
                                implClass,
                                priority = regIndex
                            )
                        }
                    } catch (e: Exception) {
                        errors.add(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            val completed = latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()

            assertThat(completed).`as`("all threads completed within timeout").isTrue
            assertThat(errors).`as`("no registration errors").isEmpty()

            // Verify registry state is consistent
            val totalExpected = threadCount * registrationsPerThread
            assertThat(registry.serviceCount()).isEqualTo(1)
            assertThat(registry.implementationCount()).isEqualTo(totalExpected)
        }

        @Test
        @DisplayName("should handle concurrent reads and writes")
        fun `concurrent reads and writes`() {
            val executor = Executors.newFixedThreadPool(8)
            val latch = CountDownLatch(8)
            val readerErrors = ConcurrentHashMap.newKeySet<Throwable>()
            val writerErrors = ConcurrentHashMap.newKeySet<Throwable>()
            val registrationCount = AtomicInteger(0)

            // 4 writer threads
            repeat(4) { threadIndex ->
                executor.submit {
                    try {
                        repeat(50) { regIndex ->
                            val implClass = threadSafeImpls[regIndex % threadSafeImpls.size]
                            registry.register(
                                TestService::class.java,
                                implClass,
                                priority = regIndex
                            )
                            registrationCount.incrementAndGet()
                        }
                    } catch (e: Exception) {
                        writerErrors.add(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            // 4 reader threads
            repeat(4) {
                executor.submit {
                    try {
                        repeat(100) {
                            @Suppress("unused")
                            val services = registry.getServices(TestService::class.java)
                            @Suppress("unused")
                            val single = registry.getService(TestService::class.java)
                            @Suppress("unused")
                            val has = registry.hasService(TestService::class.java)
                        }
                    } catch (e: Exception) {
                        readerErrors.add(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            val completed = latch.await(15, TimeUnit.SECONDS)
            executor.shutdown()

            assertThat(completed).`as`("all threads completed within timeout").isTrue
            assertThat(writerErrors).`as`("no writer errors").isEmpty()
            assertThat(readerErrors).`as`("no reader errors").isEmpty()
            assertThat(registry.implementationCount()).isEqualTo(registrationCount.get())
        }

        @Test
        @DisplayName("should handle concurrent registrations across multiple service types")
        fun `concurrent registrations for multiple service types`() {
            val executor = Executors.newFixedThreadPool(8)
            val latch = CountDownLatch(8)
            val errors = ConcurrentHashMap.newKeySet<Throwable>()

            val serviceTypes = listOf(
                TestService::class.java,
                AnotherTestService::class.java,
                YetAnotherService::class.java,
                LazyTestService::class.java
            )

            repeat(8) {
                executor.submit {
                    try {
                        repeat(25) { i ->
                            val type = serviceTypes[i % serviceTypes.size]
                            val implClass = threadSafeImpls[i % threadSafeImpls.size]
                            @Suppress("UNCHECKED_CAST")
                            registry.register(type as Class<Any>, implClass, priority = i)
                        }
                    } catch (e: Exception) {
                        errors.add(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            val completed = latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()

            assertThat(completed).`as`("all threads completed").isTrue
            assertThat(errors).`as`("no errors").isEmpty()
            assertThat(registry.serviceCount()).isEqualTo(serviceTypes.size)
        }

    }

    @Nested
    @DisplayName("serviceCount and implementationCount")
    inner class Counts {

        @Test
        @DisplayName("should track service interface count")
        fun `serviceCount tracks distinct interfaces`() {
            assertThat(registry.serviceCount()).isZero()

            registry.register(TestService::class.java, TestServiceImplA::class.java)
            assertThat(registry.serviceCount()).isEqualTo(1)

            registry.register(AnotherTestService::class.java, AnotherTestServiceImpl::class.java)
            assertThat(registry.serviceCount()).isEqualTo(2)

            // Adding more implementations to existing type doesn't change service count
            registry.register(TestService::class.java, TestServiceImplB::class.java)
            assertThat(registry.serviceCount()).isEqualTo(2)
        }

        @Test
        @DisplayName("should track total implementation count")
        fun `implementationCount tracks total implementations`() {
            assertThat(registry.implementationCount()).isZero()

            registry.register(TestService::class.java, TestServiceImplA::class.java)
            assertThat(registry.implementationCount()).isEqualTo(1)

            registry.register(TestService::class.java, TestServiceImplB::class.java)
            assertThat(registry.implementationCount()).isEqualTo(2)

            registry.register(TestService::class.java, TestServiceImplC::class.java)
            assertThat(registry.implementationCount()).isEqualTo(3)

            registry.clear()
            assertThat(registry.implementationCount()).isZero()
        }
    }

    @Nested
    @DisplayName("lazy parameter propagation")
    inner class LazyPropagation {

        @Test
        @DisplayName("should create lazy RegistryEntry when lazy=true")
        fun `lazy true creates lazy entry`() {
            CountedLazyTestServiceImpl.resetCount()
            registry.register(
                LazyTestService::class.java, CountedLazyTestServiceImpl::class.java,
                lazy = true
            )

            assertThat(CountedLazyTestServiceImpl.constructorCallCount.get()).isZero()

            // getService should still work
            val service = registry.getService(LazyTestService::class.java)
            assertThat(service).isNotNull
            assertThat((service as LazyTestService).ping()).isEqualTo("counted-pong")
            assertThat(CountedLazyTestServiceImpl.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("should create eager RegistryEntry when lazy=false")
        fun `lazy false creates eager entry`() {
            CountedLazyTestServiceImpl.resetCount()
            registry.register(
                LazyTestService::class.java, CountedLazyTestServiceImpl::class.java,
                lazy = false
            )

            assertThat(CountedLazyTestServiceImpl.constructorCallCount.get()).isEqualTo(1)

            val service = registry.getService(LazyTestService::class.java)
            assertThat(service).isNotNull
            assertThat((service as LazyTestService).ping()).isEqualTo("counted-pong")
            assertThat(CountedLazyTestServiceImpl.constructorCallCount.get()).isEqualTo(1)
        }
    }
}
