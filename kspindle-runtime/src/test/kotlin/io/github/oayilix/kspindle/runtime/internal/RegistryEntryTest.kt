package io.github.oayilix.kspindle.runtime.internal

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

@DisplayName("RegistryEntry")
class RegistryEntryTest {

    // ── Test service interface and implementations ──

    interface TestService {
        fun execute(): String
    }

    class TestServiceImpl : TestService {
        override fun execute() = "impl"
    }

    class AnotherServiceImpl : TestService {
        override fun execute() = "another"
    }

    /**
     * A tracked service that counts how many times its constructor has been invoked.
     * Used to verify lazy vs eager instantiation semantics.
     */
    class TrackedService {
        companion object {
            val constructorCallCount = AtomicInteger(0)

            fun resetCount() {
                constructorCallCount.set(0)
            }
        }

        init {
            constructorCallCount.incrementAndGet()
        }

        fun ping() = "pong"
    }

    @BeforeEach
    fun setUp() {
        TrackedService.resetCount()
    }

    @AfterEach
    fun tearDown() {
        TrackedService.resetCount()
    }

    // ── Tests ──

    @Nested
    @DisplayName("constructor")
    inner class Constructor {

        @Test
        @DisplayName("should store implClass property")
        fun `stores implClass`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = true
            )
            assertThat(entry.implClass).isEqualTo(TestServiceImpl::class.java)
        }

        @Test
        @DisplayName("should store priority property")
        fun `stores priority`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 42,
                name = "",
                lazy = true
            )
            assertThat(entry.priority).isEqualTo(42)
        }

        @Test
        @DisplayName("should store name property")
        fun `stores name`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "my-service",
                lazy = true
            )
            assertThat(entry.name).isEqualTo("my-service")
        }

        @Test
        @DisplayName("should not create instance during construction for lazy=true")
        fun `does not create instance during construction for lazy=true`() {
            RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = true
            )
            assertThat(TrackedService.constructorCallCount.get())
                .`as`("constructor should not have been called during construction")
                .isZero()
        }

        @Test
        @DisplayName("should not create instance during construction for lazy=false")
        fun `does not create instance during construction for lazy=false`() {
            RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = false
            )
            assertThat(TrackedService.constructorCallCount.get())
                .`as`("constructor should not have been called during construction")
                .isZero()
        }
    }

    @Nested
    @DisplayName("lazy=true")
    inner class LazyTrue {

        @Test
        @DisplayName("isLazy should return true")
        fun `isLazy returns true`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = true
            )
            assertThat(entry.isLazy).isTrue
        }

        @Test
        @DisplayName("should defer instantiation until getInstance() is called")
        fun `defers instantiation`() {
            TrackedService.resetCount()
            val entry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            // Constructor should not have been called yet
            assertThat(TrackedService.constructorCallCount.get()).isZero()

            // First access — instance should be created
            val instance = entry.getInstance()
            assertThat(instance).isNotNull
            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("should return same instance on repeated calls (singleton)")
        fun `returns same instance`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            val first = entry.getInstance()
            val second = entry.getInstance()
            val third = entry.getInstance()

            assertThat(first).isSameAs(second)
            assertThat(second).isSameAs(third)
        }

        @Test
        @DisplayName("should only construct once (singleton)")
        fun `constructs only once`() {
            val entry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            repeat(10) {
                entry.getInstance()
            }

            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("should return correct type")
        fun `returns correct type`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            val instance = entry.getInstance()
            assertThat(instance).isInstanceOf(TestServiceImpl::class.java)
        }
    }

    @Nested
    @DisplayName("lazy=false")
    inner class LazyFalse {

        @Test
        @DisplayName("isLazy should return false")
        fun `isLazy returns false`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = false
            )
            assertThat(entry.isLazy).isFalse
        }

        @Test
        @DisplayName("should instantiate on first getInstance() call")
        fun `instantiates on first getInstance`() {
            TrackedService.resetCount()
            val entry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = false
            )

            // Not yet created
            assertThat(TrackedService.constructorCallCount.get()).isZero()

            // First call creates the instance
            val instance = entry.getInstance()
            assertThat(instance).isNotNull
            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("should return same instance on repeated calls (singleton)")
        fun `returns same instance`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = false
            )

            val first = entry.getInstance()
            val second = entry.getInstance()
            val third = entry.getInstance()

            assertThat(first).isSameAs(second)
            assertThat(second).isSameAs(third)
        }

        @Test
        @DisplayName("should only construct once (singleton)")
        fun `constructs only once`() {
            val entry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = false
            )

            repeat(10) {
                entry.getInstance()
            }

            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("should return correct type")
        fun `returns correct type`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = false
            )

            val instance = entry.getInstance()
            assertThat(instance).isInstanceOf(TestServiceImpl::class.java)
        }
    }

    @Nested
    @DisplayName("singleton behavior")
    inner class Singleton {

        @Test
        @DisplayName("getInstance() returns same instance for lazy=true")
        fun `singleton lazy`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            val instances = (1..100).map { entry.getInstance() }
            val allSame = instances.all { it === instances.first() }
            assertThat(allSame).`as`("all getInstance() calls return the same object").isTrue
        }

        @Test
        @DisplayName("getInstance() returns same instance for lazy=false")
        fun `singleton eager`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = false
            )

            val instances = (1..100).map { entry.getInstance() }
            val allSame = instances.all { it === instances.first() }
            assertThat(allSame).`as`("all getInstance() calls return the same object").isTrue
        }
    }

    @Nested
    @DisplayName("thread safety")
    inner class ThreadSafety {

        @Test
        @DisplayName("getInstance() is thread-safe for lazy=true")
        fun `thread safety lazy`() {
            val entry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            val threadCount = 20
            val callsPerThread = 500
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val instances = ConcurrentHashMap.newKeySet<TrackedService>()
            val errors = ConcurrentHashMap.newKeySet<Throwable>()

            repeat(threadCount) {
                executor.submit {
                    try {
                        repeat(callsPerThread) {
                            instances.add(entry.getInstance() as TrackedService)
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
            assertThat(errors).`as`("no exceptions during concurrent access").isEmpty()
            assertThat(instances).hasSize(1)
            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("getInstance() is thread-safe for lazy=false")
        fun `thread safety eager`() {
            val entry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = false
            )

            val threadCount = 20
            val callsPerThread = 500
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val instances = ConcurrentHashMap.newKeySet<TrackedService>()
            val errors = ConcurrentHashMap.newKeySet<Throwable>()

            repeat(threadCount) {
                executor.submit {
                    try {
                        repeat(callsPerThread) {
                            instances.add(entry.getInstance() as TrackedService)
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
            assertThat(errors).`as`("no exceptions during concurrent access").isEmpty()
            assertThat(instances).hasSize(1)
            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("concurrent getInstance() from mixed lazy and non-lazy entries is safe")
        fun `concurrent getInstance from mixed entries`() {
            val lazyEntry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = true
            )
            val eagerEntry = RegistryEntry(
                implClass = AnotherServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = false
            )

            val executor = Executors.newFixedThreadPool(8)
            val latch = CountDownLatch(8)
            val errors = ConcurrentHashMap.newKeySet<Throwable>()

            repeat(8) { index ->
                executor.submit {
                    try {
                        repeat(200) {
                            if (index % 2 == 0) {
                                lazyEntry.getInstance()
                            } else {
                                eagerEntry.getInstance()
                            }
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
            assertThat(errors).`as`("no exceptions").isEmpty()
        }
    }

    @Nested
    @DisplayName("initializeEagerly()")
    inner class InitializeEagerly {

        @Test
        @DisplayName("should initialize lazy entry")
        fun `initializes lazy entry`() {
            val entry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            assertThat(TrackedService.constructorCallCount.get()).isZero()

            entry.initializeEagerly()
            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)

            // Subsequent calls should be no-op
            entry.initializeEagerly()
            entry.initializeEagerly()
            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("should initialize eager entry")
        fun `initializes eager entry`() {
            val entry = RegistryEntry(
                implClass = TrackedService::class.java,
                priority = 0,
                name = "",
                lazy = false
            )

            assertThat(TrackedService.constructorCallCount.get()).isZero()

            entry.initializeEagerly()
            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)

            entry.initializeEagerly()
            assertThat(TrackedService.constructorCallCount.get()).isEqualTo(1)
        }

        @Test
        @DisplayName("getInstance() after initializeEagerly() returns same instance")
        fun `getInstance after initializeEagerly returns same instance`() {
            val entry = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            entry.initializeEagerly()
            val instance = entry.getInstance()

            assertThat(instance).isInstanceOf(TestServiceImpl::class.java)
            assertThat((instance as TestServiceImpl).execute()).isEqualTo("impl")
        }
    }

    @Nested
    @DisplayName("multiple entries")
    inner class MultipleEntries {

        @Test
        @DisplayName("different entries produce different instances")
        fun `different entries produce different instances`() {
            val entryA = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "A",
                lazy = true
            )
            val entryB = RegistryEntry(
                implClass = AnotherServiceImpl::class.java,
                priority = 0,
                name = "B",
                lazy = true
            )

            val instanceA = entryA.getInstance()
            val instanceB = entryB.getInstance()

            assertThat(instanceA).isNotSameAs(instanceB)
            assertThat(instanceA).isInstanceOf(TestServiceImpl::class.java)
            assertThat(instanceB).isInstanceOf(AnotherServiceImpl::class.java)
        }

        @Test
        @DisplayName("entries with different priorities sort correctly")
        fun `entries sort by priority`() {
            val high = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 100,
                name = "high",
                lazy = true
            )
            val low = RegistryEntry(
                implClass = AnotherServiceImpl::class.java,
                priority = 10,
                name = "low",
                lazy = true
            )

            val sorted = listOf(low, high).sortedWith(
                compareByDescending<RegistryEntry<*>> { it.priority }
                    .thenBy { it.implClass.name }
            )
            assertThat(sorted).containsExactly(high, low)
        }

        @Test
        @DisplayName("entries with same priority sort by class name")
        fun `entries with same priority sort by class name`() {
            val entryA = RegistryEntry(
                implClass = TestServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = true
            )
            val entryB = RegistryEntry(
                implClass = AnotherServiceImpl::class.java,
                priority = 0,
                name = "",
                lazy = true
            )

            val sorted = listOf(entryB, entryA).sortedWith(
                compareByDescending<RegistryEntry<*>> { it.priority }
                    .thenBy { it.implClass.name }
            )
            // AnotherServiceImpl alphabetically comes first
            assertThat(sorted).containsExactly(entryB, entryA)
        }
    }
}
