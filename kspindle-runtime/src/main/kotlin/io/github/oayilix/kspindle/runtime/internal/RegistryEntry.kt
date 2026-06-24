package io.github.oayilix.kspindle.runtime.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Internal registry entry that holds a single service implementation's metadata
 * and manages lazy/eager instance creation.
 * 内部注册表条目，保存单个服务实现的元数据并管理延迟/即时实例创建。
 *
 * @param T the service interface type. 服务接口类型。
 * @param factory Optional factory lambda for custom instantiation logic.
 *                When non-null, this is used instead of reflection-based no-arg construction.
 *                可选的工厂 lambda，用于自定义实例化逻辑。
 *                当非 null 时，将使用它而不是基于反射的无参构造。
 */
internal class RegistryEntry<T : Any>(
    val implClass: Class<out T>,
    val priority: Int,
    val name: String,
    lazy: Boolean,
    private val factory: (() -> T)? = null
) {
    @Volatile
    private var _instance: T? = null

    private val instanceLock = Any()

    // Lazy holder for lazy initialization mode.
    // 用于延迟初始化模式的懒加载持有者。
    private val lazyHolder: Lazy<T> = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createInstance()
    }

    val isLazy: Boolean = lazy

    /**
     * Check whether this entry's service instance has been created.
     * 检查此条目的服务实例是否已创建。
     */
    fun isInitialized(): Boolean {
        return if (isLazy) {
            lazyHolder.isInitialized()
        } else {
            _instance != null
        }
    }

    /**
     * Human-readable debug representation showing priority, class name, name,
     * instantiation mode, and whether the instance has been created.
     * 人类可读的调试表示，显示优先级、类名、名称、实例化模式以及实例是否已创建。
     */
    override fun toString(): String {
        val initStatus = if (isInitialized()) "INITIALIZED" else "NOT initialized"
        val factoryInfo = if (factory != null) ", hasFactory" else ""
        val nameInfo = if (name.isNotEmpty()) "name=\"$name\"" else "unnamed"
        return "[$priority] ${implClass.name} ($nameInfo, lazy=$isLazy$factoryInfo, $initStatus)"
    }

    /**
     * Returns the service instance, creating it if necessary.
     * Thread-safe for both lazy and eager modes.
     * 返回服务实例，必要时创建它。对延迟和即时模式都是线程安全的。
     */
    fun getInstance(): T {
        return if (isLazy) {
            lazyHolder.value
        } else {
            if (_instance == null) {
                synchronized(instanceLock) {
                    if (_instance == null) {
                        _instance = createInstance()
                    }
                }
            }
            _instance!!
        }
    }

    /**
     * Eagerly initialize this entry. No-op if already initialized.
     * 立即初始化此条目。如果已初始化则不执行任何操作。
     */
    fun initializeEagerly() {
        getInstance()
    }

    /**
     * Create a new instance of the implementation class.
     * Uses the custom factory lambda if provided, otherwise falls back
     * to reflection-based no-arg construction.
     * 创建实现类的新实例。
     * 如果提供了自定义工厂 lambda 则使用它，否则回退到基于反射的无参构造。
     */
    private fun createInstance(): T {
        if (factory != null) {
            return factory.invoke()
        }
        val constructor = implClass.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance()
    }
}
