# KSPindle User Guide

# KSPindle 用户指南

## Table of Contents

## 目录

1. [Setup Instructions](#setup-instructions)
2. [Annotations Reference](#annotations-reference)
3. [API Reference](#api-reference)
4. [Priority System](#priority-system)
5. [Named Services](#named-services)
6. [Lazy vs Eager Loading](#lazy-vs-eager-loading)
7. [Thread Safety](#thread-safety)
8. [Troubleshooting](#troubleshooting)

[设置说明](#setup-instructions-设置说明)
[注解参考](#annotations-reference-注解参考)
[API 参考](#api-reference-api-参考)
[优先级系统](#priority-system-优先级系统)
[命名服务](#named-services-命名服务)
[懒加载 vs 预加载](#lazy-vs-eager-loading-懒加载-vs-预加载)
[线程安全](#thread-safety-线程安全)
[故障排除](#troubleshooting-故障排除)

---

## Setup Instructions

## 设置说明

### Prerequisites

### 前置条件

- Kotlin 2.0 or later / Kotlin 2.0 或更高版本
- KSP plugin matching your Kotlin version / KSP 插件需与你的 Kotlin 版本匹配
- Java 17+ (JVM toolchain) / Java 17+（JVM 工具链）
- Android Gradle Plugin 8.x (for Android projects) / Android Gradle Plugin 8.x（适用于 Android 项目）

### Step 1: Add the KSP plugin

### 步骤 1：添加 KSP 插件

Apply the KSP plugin in your module-level `build.gradle.kts`:

在模块级别的 `build.gradle.kts` 中应用 KSP 插件：

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}
```

If you use a version catalog (`libs.versions.toml`):

如果你使用版本目录（`libs.versions.toml`）：

```toml
[versions]
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

```kotlin
plugins {
    alias(libs.plugins.ksp)
}
```

### Step 2: Add KSPindle dependencies

### 步骤 2：添加 KSPindle 依赖

```kotlin
dependencies {
    // Runtime library (includes kspindle-annotations transitively)
    // 运行时库（传递包含 kspindle-annotations）
    implementation("io.github.oayilix:kspindle-runtime:1.0.0")

    // Compile-time annotation processor
    // 编译时注解处理器
    ksp("io.github.oayilix:kspindle-compiler:1.0.0")
}
```

In a multi-module project where the framework modules are local subprojects:

在多模块项目中，如果框架模块是本地子项目：

```kotlin
dependencies {
    implementation(project(":kspindle-runtime"))
    ksp(project(":kspindle-compiler"))
}
```

The `ksp` configuration ensures the processor runs during compilation but is not included in the runtime classpath.

`ksp` 配置确保处理器在编译时运行，但不会包含在运行时类路径中。

### Step 3: Define a service interface

### 步骤 3：定义服务接口

```kotlin
// The @ServiceProviderInterface annotation is optional but recommended.
// It serves as documentation and enables extra KSP validation.
// @ServiceProviderInterface 注解是可选的，但推荐使用。
// 它用作文档，并启用额外的 KSP 验证。
@ServiceProviderInterface(name = "Payment Gateway")
interface PaymentGateway {
    fun charge(amount: Double): String
}
```

### Step 4: Create implementations

### 步骤 4：创建实现类

```kotlin
@ServiceProvider(service = PaymentGateway::class, priority = 10, name = "stripe")
class StripeGateway : PaymentGateway {
    override fun charge(amount: Double) = "Charged $$amount via Stripe"
}

@ServiceProvider(service = PaymentGateway::class, priority = 5, name = "paypal")
class PayPalGateway : PaymentGateway {
    override fun charge(amount: Double) = "Charged $$amount via PayPal"
}
```

### Step 5: Initialize and load

### 步骤 5：初始化并加载

```kotlin
// Call once at application startup
// 在应用启动时调用一次
Kspindle.initialize()

// Use services throughout your application
// 在整个应用中调用服务
val gateway: PaymentGateway = Kspindle.load(PaymentGateway::class.java)
```

---

## Annotations Reference

## 注解参考

### @ServiceProvider

Marks a class as a service provider implementation for a specific service interface. This is the primary annotation that drives the KSP code generation.

将类标记为特定服务接口的服务提供者实现。这是驱动 KSP 代码生成的主要注解。

**Target:** `AnnotationTarget.CLASS`
**目标：** `AnnotationTarget.CLASS`

**Retention:** `AnnotationRetention.BINARY`
**保留策略：** `AnnotationRetention.BINARY`

**Repeatable:** Yes (a single class can implement multiple service interfaces)
**可重复：** 是（一个类可以实现多个服务接口）

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class ServiceProvider(
    val service: KClass<*>,
    val priority: Int = 0,
    val name: String = "",
    val lazy: Boolean = true
)
```

#### Parameters

#### 参数

| Parameter / 参数 | Type / 类型 | Default / 默认值 | Description / 说明 |
|---|---|---|---|
| `service` | `KClass<*>` | (required / 必需) | The service interface this class implements. Must be an interface or abstract class. / 该类实现的服务接口。必须是接口或抽象类。 |
| `priority` | `Int` | `0` | Ordering priority. Higher values are returned first by `Kspindle.load()` and sorted first in `Kspindle.loadAll()`. / 排序优先级。数值越高的实现会被 `Kspindle.load()` 优先返回，并在 `Kspindle.loadAll()` 中优先排序。 |
| `name` | `String` | `""` | An optional logical name for this implementation, enabling retrieval by name via `Kspindle.loadByName()`. / 此实现的可选逻辑名称，可通过 `Kspindle.loadByName()` 按名称查询。 |
| `lazy` | `Boolean` | `true` | Whether the implementation should be instantiated lazily (on first access) or eagerly (during initialization). / 实现应该是懒加载（首次访问时）还是预加载（初始化期间）。 |

#### Examples

#### 示例

Simple registration:
简单注册：

```kotlin
@ServiceProvider(service = Logger::class)
class FileLogger : Logger { ... }
```

With priority and name:
带优先级和名称：

```kotlin
@ServiceProvider(service = Cache::class, priority = 100, name = "redis")
class RedisCache : Cache { ... }
```

Eager initialization:
预加载初始化：

```kotlin
@ServiceProvider(service = HealthCheck::class, lazy = false)
class DatabaseHealthCheck : HealthCheck { ... }
```

Multiple service interfaces (repeatable):
多个服务接口（可重复）：

```kotlin
@ServiceProvider(service = Serializer::class, name = "json")
@ServiceProvider(service = Deserializer::class, name = "json")
class JsonCodec : Serializer, Deserializer { ... }
```

#### Validation rules (enforced by KSP processor)

#### 验证规则（由 KSP 处理器强制执行）

1. The `service` type must be an interface or abstract class -- concrete classes and enums are rejected.
   `service` 类型必须是接口或抽象类 —— 具体类和枚举会被拒绝。
2. The annotated class must actually implement or extend the declared `service` type -- the processor checks supertype chains transitively.
   被注解的类必须实际实现或继承声明的 `service` 类型 —— 处理器会传递性地检查超类型链。
3. The `priority` value is coerced to `Int` range (`Int.MIN_VALUE` to `Int.MAX_VALUE - 1`).
   `priority` 值会被强制转换为 `Int` 范围（`Int.MIN_VALUE` 到 `Int.MAX_VALUE - 1`）。

### @ServiceProviderInterface

Marks an interface or abstract class as a service provider interface. This annotation is entirely optional -- any interface or abstract class can be used as a service type without it. It primarily serves as documentation and enables the KSP processor to perform additional validation.

将接口或抽象类标记为服务提供者接口。该注解完全是可选的 —— 不使用它时，任何接口或抽象类都可以作为服务类型。它主要用作文档，并使 KSP 处理器能够执行额外的验证。

**Target:** `AnnotationTarget.CLASS`
**目标：** `AnnotationTarget.CLASS`

**Retention:** `AnnotationRetention.SOURCE`
**保留策略：** `AnnotationRetention.SOURCE`

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ServiceProviderInterface(
    val name: String = ""
)
```

#### Parameters

#### 参数

| Parameter / 参数 | Type / 类型 | Default / 默认值 | Description / 说明 |
|---|---|---|---|
| `name` | `String` | `""` | A human-readable name for this service interface. Used for documentation and logging. / 该服务接口的可读名称。用于文档和日志记录。 |

#### Example

#### 示例

```kotlin
@ServiceProviderInterface(name = "Authentication Provider")
interface AuthProvider {
    fun authenticate(token: String): User
}
```

---

## API Reference

## API 参考

### Kspindle

The main entry point for the KSPindle. `Kspindle` is a singleton object that discovers `ServiceIndexProvider` implementations (generated by the KSP processor) using `java.util.ServiceLoader`, populates the internal `ServiceRegistry`, and provides typed access to registered service implementations.

KSPindle 的主要入口点。`Kspindle` 是一个单例对象，它通过 `java.util.ServiceLoader` 发现 `ServiceIndexProvider` 实现（由 KSP 处理器生成），填充内部的 `ServiceRegistry`，并提供对已注册服务实现的有类型访问。

---

#### `Kspindle.initialize()`

```kotlin
fun initialize(classLoader: ClassLoader = Thread.currentThread().contextClassLoader)
```

Initializes the service registry by discovering all `ServiceIndexProvider` implementations via `java.util.ServiceLoader`.

通过 `java.util.ServiceLoader` 发现所有 `ServiceIndexProvider` 实现来初始化服务注册表。

- **Thread-safe** -- uses double-checked locking.
- **线程安全** —— 使用双重检查锁定。
- **Idempotent** -- subsequent calls are no-ops. If you need to re-initialize with a new environment, call `reset()` first.
- **幂等** —— 后续调用是无操作的。如果需要在新环境中重新初始化，请先调用 `reset()`。
- Accepts an optional `ClassLoader` parameter for environments with custom class loading (e.g., OSGi, test fixtures).
- 接受可选的 `ClassLoader` 参数，用于自定义类加载的环境（例如 OSGi、测试夹具）。

**Example:**
**示例：**

```kotlin
// Default initialization (uses thread context class loader)
// 默认初始化（使用线程上下文类加载器）
Kspindle.initialize()

// With custom class loader
// 使用自定义类加载器
Kspindle.initialize(myCustomClassLoader)

// Idempotent -- safe to call multiple times
// 幂等 —— 可安全多次调用
Kspindle.initialize()
Kspindle.initialize() // no-op / 无操作
```

---

#### `Kspindle.load()`

```kotlin
fun <T : Any> load(serviceClass: Class<T>): T
```

Returns the highest-priority implementation of the specified service interface.

返回指定服务接口的最高优先级实现。

- Calls `initialize()` automatically if not yet initialized.
- 如果尚未初始化，会自动调用 `initialize()`。
- Throws `ServiceNotFoundException` if no implementation is registered.
- 如果没有注册实现，会抛出 `ServiceNotFoundException`。
- Priority tiebreaker: implementation class name (alphabetical ordering).
- 优先级平局处理：实现类名（字母顺序）。

**Example:**
**示例：**

```kotlin
interface GreetingService {
    fun greet(): String
}

@ServiceProvider(service = GreetingService::class, priority = 10, name = "english")
class EnglishGreeting : GreetingService {
    override fun greet() = "Hello"
}

@ServiceProvider(service = GreetingService::class, priority = 5, name = "spanish")
class SpanishGreeting : GreetingService {
    override fun greet() = "Hola"
}

// Returns EnglishGreeting (priority 10 > 5)
// 返回 EnglishGreeting（优先级 10 > 5）
val greeting: GreetingService = Kspindle.load(GreetingService::class.java)
println(greeting.greet()) // "Hello"
```

---

#### `Kspindle.loadAll()`

```kotlin
fun <T : Any> loadAll(serviceClass: Class<T>): List<T>
```

Returns all registered implementations of the specified service interface, ordered by priority descending (then by implementation class name for deterministic ordering).

返回指定服务接口的所有已注册实现，按优先级降序排列（优先级相同时按实现类名排序以确保确定性）。

- Calls `initialize()` automatically if not yet initialized.
- 如果尚未初始化，会自动调用 `initialize()`。
- Returns an empty list if no implementations are registered (never throws).
- 如果没有注册实现，返回空列表（从不抛出异常）。

**Example:**
**示例：**

```kotlin
val all: List<GreetingService> = Kspindle.loadAll(GreetingService::class.java)
all.forEach { println(it.greet()) }
// Output / 输出:
// Hello      (priority / 优先级 10)
// Hola       (priority / 优先级 5)
```

---

#### `Kspindle.loadByName()`

```kotlin
fun <T : Any> loadByName(serviceClass: Class<T>, name: String): T?
```

Returns the implementation with the specified logical `name`, or `null` if no matching implementation is found.

返回具有指定逻辑名称 `name` 的实现，如果未找到匹配的实现则返回 `null`。

- Calls `initialize()` automatically if not yet initialized.
- 如果尚未初始化，会自动调用 `initialize()`。
- Returns `null` (not throws) when the name is not found -- use the return value to handle missing services gracefully.
- 当未找到名称时返回 `null`（而不是抛出异常）—— 使用返回值优雅地处理缺失服务。
- If multiple implementations share the same name, the highest-priority match is returned.
- 如果多个实现共享同一名称，返回优先级最高的匹配项。

**Example:**
**示例：**

```kotlin
val spanish: GreetingService? = Kspindle.loadByName(GreetingService::class.java, "spanish")
if (spanish != null) {
    println(spanish.greet()) // "Hola"
}

val unknown: GreetingService? = Kspindle.loadByName(GreetingService::class.java, "french")
println(unknown) // null -- no error thrown / null —— 不会抛出错误
```

---

#### `Kspindle.isRegistered()`

```kotlin
fun <T : Any> isRegistered(serviceClass: Class<T>): Boolean
```

Checks whether at least one implementation is registered for the specified service interface.

检查指定服务接口是否至少有一个已注册的实现。

```kotlin
if (Kspindle.isRegistered(PaymentGateway::class.java)) {
    val gateway = Kspindle.load(PaymentGateway::class.java)
}
```

---

#### `Kspindle.getRegistry()`

```kotlin
fun getRegistry(): ServiceRegistry
```

Returns the underlying `ServiceRegistry` instance. The registry is initialized before being returned. Useful for advanced use cases like direct programmatic registration.

返回底层的 `ServiceRegistry` 实例。返回前会初始化注册表。适用于需要直接编程注册的高级用例。

```kotlin
val registry = Kspindle.getRegistry()
registry.register(
    serviceClass = Logger::class.java,
    implClass = FileLogger::class.java,
    priority = 0,
    name = "file"
)
```

---

#### `Kspindle.reset()`

```kotlin
fun reset()
```

Resets the SPI framework to its uninitialized state. Clears all registrations and resets the initialization flag. Primarily useful for testing -- call in `@BeforeEach`/`@AfterEach` to prevent test pollution.

将 KSPindle 重置为未初始化状态。清除所有注册并重置初始化标志。主要用于测试 —— 在 `@BeforeEach`/`@AfterEach` 中调用以防止测试污染。

```kotlin
class MyTest {
    @BeforeEach
    fun setUp() {
        Kspindle.reset()
    }

    @AfterEach
    fun tearDown() {
        Kspindle.reset()
    }
}
```

---

### ServiceRegistry

The thread-safe registry that stores and retrieves service provider instances. Normally you interact with it through `Kspindle`, but direct access is available for advanced scenarios via `Kspindle.getRegistry()`.

线程安全的注册表，用于存储和检索服务提供者实例。通常通过 `Kspindle` 与其交互，但在高级场景下可通过 `Kspindle.getRegistry()` 直接访问。

#### `register()`

```kotlin
fun <T : Any> register(
    serviceClass: Class<T>,
    implClass: Class<out T>,
    priority: Int = 0,
    name: String = "",
    lazy: Boolean = true
)

fun <T : Any> register(registration: ProviderRegistration<T>)
```

Registers a service implementation. Accepts either individual parameters or a `ProviderRegistration` data class.
注册服务实现。接受单独参数或 `ProviderRegistration` 数据类。

#### `getService()`

```kotlin
fun <T : Any> getService(serviceClass: Class<T>): T?
fun <T : Any> getService(serviceClass: Class<T>, name: String): T?
```

#### `getServices()`

```kotlin
fun <T : Any> getServices(serviceClass: Class<T>): List<T>
```

#### `hasService()`

```kotlin
fun <T : Any> hasService(serviceClass: Class<T>): Boolean
fun <T : Any> hasService(serviceClass: Class<T>, name: String): Boolean
```

#### `clear()`

```kotlin
fun clear()
```

#### Count methods

#### 计数方法

```kotlin
fun serviceCount(): Int      // number of distinct service interfaces / 不同服务接口的数量
fun implementationCount(): Int // total implementations across all services / 所有服务的总实现数
```

---

### ServiceNotFoundException

```kotlin
class ServiceNotFoundException(message: String) : RuntimeException(message)
```

Thrown by `Kspindle.load()` when no implementation is registered for the requested service interface. The message includes the service class name and guidance on what to check.

当请求的服务接口没有注册实现时，由 `Kspindle.load()` 抛出。消息中包含服务类名和检查建议。

---

### ServiceIndexProvider

```kotlin
interface ServiceIndexProvider {
    fun initialize(registry: ServiceRegistry)
}
```

Contract for generated code. The KSP processor generates one implementation of this interface per module that uses `@ServiceProvider` annotations. `Kspindle` discovers these implementations at runtime via `java.util.ServiceLoader` and calls `initialize()` to populate the registry. End users never implement this interface directly.

生成代码的契约。KSP 处理器为每个使用 `@ServiceProvider` 注解的模块生成该接口的一个实现。`Kspindle` 在运行时通过 `java.util.ServiceLoader` 发现这些实现，并调用 `initialize()` 填充注册表。最终用户不需要直接实现此接口。

---

### ProviderRegistration

```kotlin
data class ProviderRegistration<T : Any>(
    val serviceClass: Class<T>,
    val implClass: Class<out T>,
    val priority: Int = 0,
    val name: String = "",
    val lazy: Boolean = true
)
```

A data class that describes a single service provider registration. Used by generated `ServiceIndexProvider` implementations and available for direct programmatic registration.

描述单个服务提供者注册的数据类。由生成的 `ServiceIndexProvider` 实现使用，也可用于直接编程注册。

---

## Priority System

## 优先级系统

The priority system controls the order in which service implementations are returned.

优先级系统控制服务实现返回的顺序。

### Rules

### 规则

1. **Higher priority values are returned first.** `Kspindle.load()` returns the implementation with the highest priority. `Kspindle.loadAll()` sorts descending by priority.
   **数值越高的优先级越先返回。** `Kspindle.load()` 返回优先级最高的实现。`Kspindle.loadAll()` 按优先级降序排序。

2. **Tiebreaker is implementation class name.** When two implementations have the same priority, they are ordered alphabetically by their fully qualified class name. This ensures deterministic ordering even when priorities are identical.
   **平局处理依据是实现类名。** 当两个实现具有相同优先级时，它们按完全限定类名的字母顺序排序。这确保了即使在优先级相同时也能确定性地排序。

3. **Priority is an `Int`.** The KSP processor coerces values to the valid `Int` range. Use any value from `Int.MIN_VALUE` to `Int.MAX_VALUE - 1`.
   **优先级是 `Int` 类型。** KSP 处理器会将值强制转换为有效的 `Int` 范围。可使用 `Int.MIN_VALUE` 到 `Int.MAX_VALUE - 1` 之间的任意值。

### Example

### 示例

```kotlin
@ServiceProvider(service = Reporter::class, priority = 50)
class FastReporter : Reporter { ... }     // returned 2nd / 第二个返回

@ServiceProvider(service = Reporter::class, priority = 100)
class DetailedReporter : Reporter { ... } // returned 1st / 第一个返回

@ServiceProvider(service = Reporter::class, priority = 50)
class SummaryReporter : Reporter { ... }  // returned 3rd (alphabetical: F < S) / 第三个返回（字母顺序：F < S）
```

### Use cases

### 使用场景

- **Primary/fallback pattern**: Give your primary implementation priority `100` and fallback implementations priority `0`.
- **主备模式**：将主要实现设为优先级 `100`，备用实现设为优先级 `0`。
- **Feature flags**: Dynamically register higher-priority implementations under certain build flavors or runtime conditions.
- **特性开关**：在特定的构建变体或运行时条件下动态注册更高优先级的实现。
- **Override pattern**: In tests or specific deployments, register a mock implementation with priority higher than the real one.
- **覆盖模式**：在测试或特定部署中，注册一个优先级高于真实实现的模拟实现。

---

## Named Services

## 命名服务

The `name` parameter on `@ServiceProvider` gives each implementation a logical identifier that can be used for targeted retrieval via `Kspindle.loadByName()`.

`@ServiceProvider` 上的 `name` 参数为每个实现提供了一个逻辑标识符，可通过 `Kspindle.loadByName()` 进行定向检索。

### When to use named services

### 何时使用命名服务

- **Strategy pattern**: Select a specific algorithm or provider at runtime based on configuration or user preference.
- **策略模式**：根据配置或用户偏好在运行时选择特定的算法或提供者。
- **Multi-tenant services**: Different implementations for different regions, languages, or deployment environments.
- **多租户服务**：为不同区域、语言或部署环境提供不同的实现。
- **Pluggable architecture**: Let consumers choose which implementation to use without coupling to concrete class names.
- **可插拔架构**：让消费者选择使用哪个实现，而不与具体类名耦合。

### Behavior

### 行为

- The name is an arbitrary string -- use any naming convention that fits your domain.
- 名称是任意字符串 —— 可以使用适合你领域的任何命名约定。
- Names do not need to be unique across implementations of different service interfaces. However, within a single service interface, loading by name returns the first match (deterministic: priority-sorted order).
- 不同服务接口的实现之间，名称不必唯一。但在单个服务接口内，按名称加载返回第一个匹配项（确定性：按优先级排序）。
- Keep names reasonably short and descriptive. They are not validated by the KSP processor other than being a non-null String.
- 保持名称合理简短且具有描述性。KSP 处理器仅验证它是否为非 null 字符串。
- `Kspindle.loadByName()` returns `null` (not throws) when no matching name is found. Always check the result for `null`.
- `Kspindle.loadByName()` 在未找到匹配名称时返回 `null`（而不是抛出异常）。始终检查结果是否为 `null`。

### Example

### 示例

```kotlin
@ServiceProvider(service = Translator::class, name = "google", priority = 10)
class GoogleTranslator : Translator { ... }

@ServiceProvider(service = Translator::class, name = "deepl", priority = 5)
class DeepLTranslator : Translator { ... }

fun getTranslator(provider: String): Translator? =
    Kspindle.loadByName(Translator::class.java, provider)
```

---

## Lazy vs Eager Loading

## 懒加载 vs 预加载

The `lazy` parameter on `@ServiceProvider` controls when an implementation instance is created.

`@ServiceProvider` 上的 `lazy` 参数控制何时创建实现实例。

### Lazy loading (default, `lazy = true`)

### 懒加载（默认，`lazy = true`）

- Instance is created on the first call to `getInstance()` (the first time someone calls `Kspindle.load()`, `loadAll()`, or `loadByName()` on this service class).
- 实例在首次调用 `getInstance()` 时创建（即有人首次在此服务类上调用 `Kspindle.load()`、`loadAll()` 或 `loadByName()` 时）。
- Uses `kotlin.lazy(LazyThreadSafetyMode.SYNCHRONIZED)` -- thread-safe, instance created at most once.
- 使用 `kotlin.lazy(LazyThreadSafetyMode.SYNCHRONIZED)` —— 线程安全，实例最多创建一次。
- **Best for:** Services that are expensive to construct, may not be used at all, or depend on runtime state that is not available at initialization time.
- **最适合：** 构造开销大、可能不会被使用、或依赖于初始化时不可用的运行时状态的服务。

### Eager loading (`lazy = false`)

### 预加载（`lazy = false`）

- Instance is created during registration, before `Kspindle.initialize()` completes.
- 实例会在注册期间创建，也就是在 `Kspindle.initialize()` 完成之前创建。
- Uses double-checked locking for thread safety.
- 使用双重检查锁定确保线程安全。
- **Best for:** Lightweight services that are always needed, or services with side effects that must run during startup (e.g., registering listeners, starting background threads).
- **最适合：** 始终需要的轻量级服务，或必须在启动时运行副作用代码的服务（如注册监听器、启动后台线程）。

### Choosing the right mode

### 选择合适的模式

| Scenario / 场景 | Recommended / 建议 |
|---|---|
| Service may not be used on every screen / 服务可能并非在每个界面都会使用 | `lazy = true` (default / 默认) |
| Service does heavy I/O or network on construction / 服务在构造时执行重 I/O 或网络操作 | `lazy = true` |
| Service is lightweight and always needed / 服务轻量且始终需要 | either (negligible difference / 两者均可，差异可忽略) |
| Service must register callbacks during construction / 服务必须在构造时注册回调 | `lazy = false` |
| Service is used as a singleton throughout the app / 服务在整个应用中作为单例使用 | `lazy = true` (safe; registry holds the instance / 安全；注册表持有实例) |

### Example

### 示例

```kotlin
// Created on first access (default behavior)
// 首次访问时创建（默认行为）
@ServiceProvider(service = AnalyticsTracker::class)
class LazyTracker : AnalyticsTracker { ... }

// Created as soon as registry entries are populated during Kspindle.initialize()
// 在 Kspindle.initialize() 填充注册表条目时立即创建
@ServiceProvider(service = HealthCheck::class, lazy = false)
class StartupHealthCheck : HealthCheck {
    init {
        Runtime.getRuntime().addShutdownHook(Thread { ... })
    }
}
```

---

## Thread Safety

## 线程安全

KSPindle is designed for concurrent access from the ground up.

KSPindle 从一开始就设计为支持并发访问。

### Guarantees

### 保证

| Component / 组件 | Mechanism / 机制 | Safe from multiple threads? / 多线程安全？ |
|---|---|---|
| `Kspindle.initialize()` | Double-checked locking with `@Volatile` flag and `synchronized` block / 使用 `@Volatile` 标志和 `synchronized` 块的双重检查锁定 | Yes / 是 |
| `Kspindle.load()` / `loadAll()` / `loadByName()` | Delegates to `ServiceRegistry` (read-only after initialization) / 委托给 `ServiceRegistry`（初始化后只读） | Yes / 是 |
| `Kspindle.reset()` | `synchronized(initLock)` | Yes / 是 |
| `ServiceRegistry.register()` | `ConcurrentHashMap.compute()` for atomic update; `CopyOnWriteArrayList` for thread-safe list / 使用 `ConcurrentHashMap.compute()` 进行原子更新；`CopyOnWriteArrayList` 用于线程安全列表 | Yes / 是 |
| `ServiceRegistry.getService()` / `getServices()` | Read on `ConcurrentHashMap` + iteration over `CopyOnWriteArrayList` / 在 `ConcurrentHashMap` 上读取 + 在 `CopyOnWriteArrayList` 上迭代 | Yes / 是 |
| `RegistryEntry.getInstance()` (lazy / 懒加载) | `LazyThreadSafetyMode.SYNCHRONIZED` | Yes / 是 |
| `RegistryEntry.getInstance()` (eager / 预加载) | Double-checked locking with `@Volatile` field / 使用 `@Volatile` 字段的双重检查锁定 | Yes / 是 |

### Best practices

### 最佳实践

1. **Call `Kspindle.initialize()` once at application startup** from a safe initialization point (e.g., `Application.onCreate()` in Android, `main()` in a JVM application). After initialization, all read operations are lock-free on `ConcurrentHashMap`.
   **在应用启动时调用一次 `Kspindle.initialize()`**，从安全的初始化点调用（例如 Android 中的 `Application.onCreate()`，JVM 应用中的 `main()`）。初始化后，所有读取操作在 `ConcurrentHashMap` 上均为无锁操作。

2. **Do not call `reset()` in production code.** It is intended for testing isolation. Calling `reset()` while other threads are loading services can cause transient `ServiceNotFoundException` errors.
   **不要在生产代码中调用 `reset()`。** 它用于测试隔离。在其它线程加载服务时调用 `reset()` 可能导致瞬时的 `ServiceNotFoundException` 错误。

3. **`Kspindle` is safe to use as a singleton.** It is itself an `object` (Kotlin singleton), and the `ServiceRegistry` is a single instance held inside it.
   **`Kspindle` 可以作为单例安全使用。** 它本身是一个 `object`（Kotlin 单例），`ServiceRegistry` 是其内部持有的单个实例。

---

## Troubleshooting

## 故障排除

### `ServiceNotFoundException: No implementation registered for ...`

### `ServiceNotFoundException: 未注册 ... 的实现`

**Cause:** `Kspindle.load()` was called but no `@ServiceProvider`-annotated class was found for the requested service interface at compile time, or the KSP processor was not applied to the module containing the annotated classes.

**原因：** 调用了 `Kspindle.load()`，但在编译时未找到请求的服务接口的 `@ServiceProvider` 注解类，或者 KSP 处理器未应用于包含注解类的模块。

**Solutions:**
**解决方案：**

1. **Verify the annotation.** Ensure your implementation class has `@ServiceProvider(service = YourInterface::class)`.
   **验证注解。** 确保你的实现类有 `@ServiceProvider(service = YourInterface::class)`。
2. **Verify the KSP processor.** Check that `ksp("...kspindle-compiler:...")` is applied in the build.gradle.kts of the module where the `@ServiceProvider` annotations live (not just the consuming module).
   **验证 KSP 处理器。** 检查 `ksp("...kspindle-compiler:...")` 是否已添加到 `@ServiceProvider` 注解所在模块的 build.gradle.kts 中（而不仅仅是消费模块）。
3. **Run a clean build.** Stale generated files can cause issues: `./gradlew clean build`.
   **运行一次 clean 构建。** 过时的生成文件可能导致问题：`./gradlew clean build`。
4. **Check the generated code.** Look under `build/generated/ksp/` for a file named `ServiceIndex_*.kt`. If it does not exist, the KSP processor did not run. Verify the KSP plugin version matches your Kotlin version.
   **检查生成的代码。** 在 `build/generated/ksp/` 目录下查找名为 `ServiceIndex_*.kt` 的文件。如果文件不存在，说明 KSP 处理器没有运行。确认 KSP 插件版本与你的 Kotlin 版本匹配。

### Generated `ServiceIndex_*.kt` file is missing

### 生成的 `ServiceIndex_*.kt` 文件缺失

**Cause:** The KSP processor (`kspindle-compiler`) did not run on the module containing `@ServiceProvider` annotations.

**原因：** KSP 处理器（`kspindle-compiler`）未在包含 `@ServiceProvider` 注解的模块上运行。

**Solutions:**
**解决方案：**

1. Ensure the module applies the `com.google.devtools.ksp` plugin.
   确保模块应用了 `com.google.devtools.ksp` 插件。
2. Ensure `ksp(project(":kspindle-compiler"))` or `ksp("io.github.oayilix:kspindle-compiler:VERSION")` is in the module's `dependencies` block.
   确保模块的 `dependencies` 块中有 `ksp(project(":kspindle-compiler"))` 或 `ksp("io.github.oayilix:kspindle-compiler:VERSION")`。
3. Verify KSP version compatibility -- the KSP version must match the Kotlin version. For Kotlin `2.0.x`, use KSP `2.0.x-1.0.y` (e.g., `2.0.21-1.0.28`).
   验证 KSP 版本兼容性 —— KSP 版本必须与 Kotlin 版本匹配。对于 Kotlin `2.0.x`，使用 KSP `2.0.x-1.0.y`（例如 `2.0.21-1.0.28`）。

### Duplicate or conflicting registrations

### 重复或冲突的注册

**Cause:** Multiple modules (e.g., library + app) both contain `@ServiceProvider` implementations for the same service interface. When both modules have the KSP processor applied, each generates its own `ServiceIndexProvider`, and the service registry accumulates all implementations.

**原因：** 多个模块（例如库 + 应用）都包含同一服务接口的 `@ServiceProvider` 实现。当两个模块都应用了 KSP 处理器时，每个模块都会生成自己的 `ServiceIndexProvider`，服务注册表会累积所有实现。

**Resolution:** This is by design for unnamed providers -- the framework supports multi-module discovery and `loadAll()` returns all registered implementations. For non-empty names, a later registration with the same `(service, name)` replaces the earlier named registration. Use priority for ordering and names for explicit selection. If you are seeing unexpected duplicates, check that both modules are not applying the KSP processor unnecessarily. Typically, only the leaf modules (those containing `@ServiceProvider` classes) need `ksp(...)`.

**解决方法：** 对未命名 provider 来说这是设计使然 —— 框架支持多模块发现，`loadAll()` 会返回所有已注册实现。对于非空名称，后注册的相同 `(service, name)` 会替换更早的命名注册。使用优先级控制排序，使用名称进行明确选择。如果你看到意外的重复项，请检查两个模块是否都不必要地应用了 KSP 处理器。通常，只有叶模块（包含 `@ServiceProvider` 类的模块）需要 `ksp(...)`。

### `ClassNotFoundException` or `NoClassDefFoundError` at runtime

### 运行时出现 `ClassNotFoundException` 或 `NoClassDefFoundError`

**Cause:** An eagerly-loaded (`lazy = false`) service implementation class is not on the runtime classpath.

**原因：** 预加载（`lazy = false`）的服务实现类不在运行时类路径上。

**Solution:** Ensure the module containing the implementation is a dependency of the consuming module at runtime (`implementation` configuration, not just `ksp`).

**解决方法：** 确保包含实现的模块是消费模块在运行时的依赖项（`implementation` 配置，而不仅仅是 `ksp`）。

### `java.lang.NoSuchMethodException: ...<init>()`

**Cause:** The service implementation class does not have a public no-argument constructor. `RegistryEntry.createInstance()` uses `constructor.newInstance()` via reflection, which requires a public no-arg constructor.

**原因：** 服务实现类没有公共无参构造函数。`RegistryEntry.createInstance()` 通过反射使用 `constructor.newInstance()`，这需要一个公共无参构造函数。

**Solution:** Add a public no-argument constructor to your implementation class. If your class uses constructor injection, you need to either:
**解决方法：** 为你的实现类添加公共无参构造函数。如果你的类使用构造函数注入，你需要：

- Add a no-arg constructor, or
- 添加无参构造函数，或者
- Register the service programmatically via `Kspindle.getRegistry().register()` with a pre-built instance (future feature).
- 通过 `Kspindle.getRegistry().register()` 使用预构建实例以编程方式注册服务（未来特性）。

### KSP processor warnings: "Could not create META-INF/services file"

### KSP 处理器警告："无法创建 META-INF/services 文件"

**Cause:** The processor attempts to write the `ServiceLoader` descriptor file redundantly across incremental compilation rounds. This warning is benign -- the file is created on the first processing round and subsequent attempts are safely skipped.

**原因：** 处理器在增量编译轮次中尝试冗余写入 `ServiceLoader` 描述符文件。此警告是良性的 —— 文件在首次处理轮次中创建，后续尝试会被安全跳过。

**Action:** Ignore this warning. If it causes build failures, ensure `Dependencies(aggregating = true)` is used in the `createNewFileByPath` call (it already is in the default implementation).
**操作：** 忽略此警告。如果它导致构建失败，请确保在 `createNewFileByPath` 调用中使用 `Dependencies(aggregating = true)`（默认实现中已经如此）。

### Migrating from Java `ServiceLoader`

### 从 Java `ServiceLoader` 迁移

If you are switching from Java's `java.util.ServiceLoader`:

如果你正在从 Java 的 `java.util.ServiceLoader` 迁移：

| Java ServiceLoader | KSPindle |
|---|---|
| `META-INF/services/` files / 文件 | Generated automatically by KSP processor / 由 KSP 处理器自动生成 |
| `ServiceLoader.load(Class)` | `Kspindle.load(Class)` |
| `ServiceLoader.iterator()` | `Kspindle.loadAll(Class)` |
| No ordering / 无排序 | Priority-based ordering / 基于优先级的排序 |
| No named lookups / 无命名查询 | `Kspindle.loadByName()` |
| Instantiation via `ServiceLoader` / 通过 `ServiceLoader` 实例化 | Lazy or eager controlled by annotation / 通过注解控制懒加载或预加载 |
