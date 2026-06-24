package com.spi.framework.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safety stress tests for SpiLoader.
 * SpiLoader 的线程安全压力测试。
 */
class SpiLoaderConcurrencyTest {

    // ── Test service interfaces and implementations ──

    interface ConcBasicService {
        fun execute(): String
    }

    class ConcBasicServiceImpl : ConcBasicService {
        override fun execute() = "ok"
    }

    interface ConcNamedService {
        fun identity(): String
    }

    class ConcNamedServiceImpl : ConcNamedService {
        override fun identity() = "impl-${hashCode()}"
    }

    interface ConcCountingService {
        fun getCount(): Int
    }

    class ConcCountingServiceImpl : ConcCountingService {
        companion object {
            val instantiationCounter = AtomicInteger(0)
        }

        init {
            instantiationCounter.incrementAndGet()
        }

        override fun getCount(): Int = instantiationCounter.get()
    }

    interface ConcDeadlockService {
        fun payload(): String
    }

    class ConcDeadlockServiceImpl : ConcDeadlockService {
        override fun payload() = "alive"
    }

    @AfterEach
    fun tearDown() {
        SpiLoader.reset()
        ConcCountingServiceImpl.instantiationCounter.set(0)
    }

    // 并发初始化是线程安全的
    @Test
    fun `concurrent initialization is thread-safe`() {
        SpiLoader.reset()

        val threadCount = 10
        val startLatch = CountDownLatch(1)
        val finishLatch = CountDownLatch(threadCount)
        val errors = AtomicInteger(0)

        repeat(threadCount) {
            Thread {
                startLatch.await()
                try {
                    SpiLoader.initialize()
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    finishLatch.countDown()
                }
            }.start()
        }

        startLatch.countDown()
        finishLatch.await()

        assertThat(errors.get())
            .`as`("no exceptions during concurrent initialize()")
            .isZero()
    }

    // 并发加载调用是线程安全的
    @Test
    fun `concurrent load calls are thread-safe`() {
        val registry = SpiLoader.getRegistry()
        registry.register(
            ConcBasicService::class.java,
            ConcBasicServiceImpl::class.java,
            lazy = false
        )

        val threadCount = 20
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        repeat(threadCount) {
            executor.submit {
                startLatch.await()
                try {
                    val service = SpiLoader.load(ConcBasicService::class.java)
                    assertThat(service.execute()).isEqualTo("ok")
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        assertThat(successCount.get()).isEqualTo(threadCount)
        assertThat(failureCount.get()).isZero()
    }

    // 并发注册和加载不会损坏注册表
    @Test
    fun `concurrent register and load does not corrupt registry`() {
        val registry = SpiLoader.getRegistry()
        val implCount = 10
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(implCount)

        repeat(implCount) { index ->
            val name = "conc-impl-$index"
            executor.submit {
                startLatch.await()
                registry.register(
                    ConcNamedService::class.java,
                    ConcNamedServiceImpl::class.java,
                    name = name,
                    lazy = false
                )
            }
        }

        startLatch.countDown()
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        val all = SpiLoader.loadAll(ConcNamedService::class.java)
        assertThat(all).hasSize(implCount)
    }

    // 延迟初始化在高并发下是线程安全的
    @Test
    fun `lazy initialization is thread-safe for high contention`() {
        val registry = SpiLoader.getRegistry()
        registry.register(
            ConcCountingService::class.java,
            ConcCountingServiceImpl::class.java,
            lazy = true
        )

        val threadCount = 50
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)

        repeat(threadCount) {
            executor.submit {
                startLatch.await()
                SpiLoader.load(ConcCountingService::class.java)
            }
        }

        startLatch.countDown()
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        assertThat(ConcCountingServiceImpl.instantiationCounter.get())
            .`as`("lazy service should be instantiated exactly once")
            .isEqualTo(1)
    }

    // 重置期间加载不会发生死锁
    @Test
    fun `reset during load does not deadlock`() {
        SpiLoader.reset()
        val registry = SpiLoader.getRegistry()
        registry.register(
            ConcDeadlockService::class.java,
            ConcDeadlockServiceImpl::class.java,
            lazy = false
        )

        val iterationCount = 50
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val loadCount = AtomicInteger(0)
        val resetCount = AtomicInteger(0)

        executor.submit {
            startLatch.await()
            repeat(iterationCount) {
                try {
                    SpiLoader.load(ConcDeadlockService::class.java)
                } catch (_: ServiceNotFoundException) {
                    // expected after reset clears the registry
                }
                loadCount.incrementAndGet()
            }
        }

        executor.submit {
            startLatch.await()
            repeat(iterationCount) {
                SpiLoader.reset()
                resetCount.incrementAndGet()
            }
        }

        startLatch.countDown()
        executor.shutdown()
        val completed = executor.awaitTermination(10, TimeUnit.SECONDS)

        assertThat(completed)
            .`as`("both threads completed without deadlock")
            .isTrue
        assertThat(loadCount.get() + resetCount.get())
            .`as`("all iterations completed")
            .isEqualTo(iterationCount * 2)
    }
}
