# KSPindle

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue)](https://kotlinlang.org)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-purple)](https://github.com/google/ksp)
[![License](https://img.shields.io/badge/License-Apache%202.0-green)](LICENSE)

# KSPindle

A compile-time, annotation-driven Service Provider Interface (SPI) framework for Kotlin and Android. Eliminates manual service registration -- annotate your implementations, let the KSP processor generate the wiring, and load services at runtime without classpath scanning.

一个基于编译时、注解驱动的服务提供者接口（SPI）框架，适用于 Kotlin 和 Android。消除手动服务注册 —— 只需注解你的实现类，让 KSP 处理器生成连接代码，即可在运行时避免 classpath 扫描。

---

## Quick Start

## 快速开始

### 1. Annotate your interface and implementations

### 1. 注解你的接口和实现

```kotlin
// Define a service interface
// 定义服务接口
@ServiceProviderInterface(name = "Payment Gateway")
interface PaymentGateway {
    fun charge(amount: Double): String
}

// Implement it -- the KSP processor handles discovery automatically
// 实现它 —— KSP 处理器会自动处理发现机制
@ServiceProvider(service = PaymentGateway::class, priority = 10, name = "stripe")
class StripeGateway : PaymentGateway {
    override fun charge(amount: Double) = "Charged $$amount via Stripe"
}

@ServiceProvider(service = PaymentGateway::class, priority = 5, name = "paypal")
class PayPalGateway : PaymentGateway {
    override fun charge(amount: Double) = "Charged $$amount via PayPal"
}
```

### 2. Apply the KSP processor

### 2. 应用 KSP 处理器

```kotlin
// build.gradle.kts (module-level / 模块级别)
plugins {
    id("org.jetbrains.kotlin.plugin.ksp")
}

dependencies {
    implementation("io.github.oayilix:kspindle-runtime:1.0.0")
    ksp("io.github.oayilix:kspindle-compiler:1.0.0")
}
```

### 3. Load services at runtime

### 3. 在运行时加载服务

```kotlin
Kspindle.initialize()

// Highest-priority implementation
// 获取最高优先级的实现
val gateway: PaymentGateway = Kspindle.load(PaymentGateway::class.java)
println(gateway.charge(49.99))

// All implementations, sorted by priority
// 获取所有实现，按优先级排序
val all: List<PaymentGateway> = Kspindle.loadAll(PaymentGateway::class.java)

// Named lookup
// 按名称查找
val paypal = Kspindle.loadByName(PaymentGateway::class.java, "paypal")
```

---

## Features

## 特性

- **Annotation-driven** -- `@ServiceProvider` marks implementations; `@ServiceProviderInterface` documents interfaces
- **注解驱动** —— `@ServiceProvider` 标记实现类；`@ServiceProviderInterface` 文档化接口
- **Compile-time code generation** -- KSP processor generates `ServiceIndexProvider` implementations; no runtime scanning
- **编译时代码生成** —— KSP 处理器生成 `ServiceIndexProvider` 实现；无需运行时扫描
- **Priority ordering** -- `priority` attribute controls load order; higher values take precedence
- **优先级排序** —— `priority` 属性控制加载顺序；数值越高优先级越高
- **Named services** -- retrieve specific implementations by name with `loadByName()`
- **命名服务** —— 通过 `loadByName()` 按名称获取特定实现
- **Lazy by default** -- implementations are instantiated on first access; opt into eager creation
- **默认懒加载** —— 实现类在首次访问时实例化；可选择预创建
- **Thread-safe** -- all `Kspindle` and `ServiceRegistry` methods are safe for concurrent access
- **线程安全** —— 所有 `Kspindle` 和 `ServiceRegistry` 方法均支持并发访问
- **Lightweight** -- no dependencies beyond Kotlin stdlib and the KSP API; single `ServiceLoader` discovery call
- **轻量级** —— 除 Kotlin 标准库和 KSP API 外无其他依赖；仅需一次 `ServiceLoader` 发现调用
- **Repeatable annotations** -- a single class can implement multiple service interfaces
- **可重复注解** —— 一个类可以实现多个服务接口

---

## Module Overview

## 模块概览

| Module / 模块 | Artifact / 工件 | Description / 说明 |
|---|---|---|
| `kspindle-annotations` | `kspindle-annotations` | Contains `@ServiceProvider` and `@ServiceProviderInterface` annotations. Zero dependencies. / 包含 `@ServiceProvider` 和 `@ServiceProviderInterface` 注解。零依赖。 |
| `kspindle-compiler` | `kspindle-compiler` | KSP symbol processor that discovers annotated classes and generates `ServiceIndexProvider` code. Required as a `ksp(...)` dependency. / KSP 符号处理器，发现注解类并生成 `ServiceIndexProvider` 代码。需要作为 `ksp(...)` 依赖添加。 |
| `kspindle-runtime` | `kspindle-runtime` | Runtime library with `Kspindle`, `ServiceRegistry`, and supporting types. Depends on `kspindle-annotations` (transitive via `api`). / 运行时库，包含 `Kspindle`、`ServiceRegistry` 及支持类型。依赖于 `kspindle-annotations`（通过 `api` 传递）。 |

---

## Architecture Decoupling Demonstration

## 架构解耦演示

KSPindle enforces strict decoupling between modules. Business modules depend only on the API (`sample-api`), never on the implementation (`sample-impl`). The actual implementation class is discovered at runtime via the SPI mechanism.

KSPindle 在模块之间强制执行严格的解耦。业务模块仅依赖于 API（`sample-api`），从不依赖于具体实现（`sample-impl`）。实际的实现类在运行时通过 SPI 机制发现。

```
┌─────────────────────────────────────────────────────────────────┐
│                      Application / 应用                         │
│                                                                 │
│   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐          │
│   │ Business     │   │ Business    │   │ Business    │          │
│   │ Module A     │   │ Module B    │   │ Module C    │          │
│   │ (业务模块A)  │   │ (业务模块B)  │   │ (业务模块C)  │          │
│   └──────┬──────┘   └──────┬──────┘   └──────┬──────┘          │
│          │                 │                 │                  │
│          └─────────┬───────┴───────┬─────────┘                  │
│                    │               │                            │
│                    ▼               ▼                            │
│          ┌──────────────────────────────────┐                   │
│          │        sample-api                │                   │
│          │        (Service Interface        │                   │
│          │        服务接口定义)              │                   │
│          └──────────────────────────────────┘                   │
│                    │                                            │
│                    │   depends only on api / 仅依赖 api         │
│                    ▼                                            │
│          ┌──────────────────────────────────┐                   │
│          │        sample-impl               │                   │
│          │        (Concrete Implementation  │                   │
│          │        具体实现)                  │                   │
│          └──────────────────────────────────┘                   │
│                                                                 │
│   SPI discovers implementations at runtime / SPI 在运行时发现实现 │
└─────────────────────────────────────────────────────────────────┘
```

### How decoupling works / 解耦原理

1. **Business modules** import only the `sample-api` artifact, which contains the service interface definition.
2. **业务模块** 仅导入 `sample-api` 工件，其中包含服务接口定义。
3. **The `sample-impl` module** provides the actual implementation, annotated with `@ServiceProvider`.
4. **`sample-impl` 模块** 提供实际的实现类，并使用 `@ServiceProvider` 注解标记。
5. **At compile time**, the KSP processor in `sample-impl` generates a `ServiceIndexProvider` that maps the interface to the concrete class.
6. **在编译时**，`sample-impl` 中的 KSP 处理器生成 `ServiceIndexProvider`，将接口映射到具体类。
7. **At runtime**, `Kspindle` discovers the generated index via `java.util.ServiceLoader` and instantiates the implementation on demand.
8. **在运行时**，`Kspindle` 通过 `java.util.ServiceLoader` 发现生成的索引，并按需实例化实现类。

This means you can swap out `sample-impl` for a different implementation (e.g., a mock for testing, a platform-specific variant) without changing a single line of business code. The business module never has a compile-time dependency on the concrete implementation class.

这意味着你可以将 `sample-impl` 替换为其他实现（例如测试用的模拟实现、特定平台的变体），而无需修改任何业务代码。业务模块在编译时从不依赖于具体实现类。

---

## Dependency Coordinates

## 依赖坐标

Add the following to your module-level `build.gradle.kts`:

将以下内容添加到模块级别的 `build.gradle.kts` 中：

```kotlin
plugins {
    // Required: KSP plugin
    // 必需：KSP 插件
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

dependencies {
    // Runtime library (includes kspindle-annotations transitively)
    // 运行时库（传递包含 kspindle-annotations）
    implementation("io.github.oayilix:kspindle-runtime:1.0.0")

    // KSP processor (compile-time only)
    // KSP 处理器（仅在编译时使用）
    ksp("io.github.oayilix:kspindle-compiler:1.0.0")
}
```

**For local development** (multi-module project):

**本地开发**（多模块项目）：

```kotlin
dependencies {
    implementation(project(":kspindle-runtime"))
    ksp(project(":kspindle-compiler"))
}
```

---

## Minimum Requirements

## 最低要求

| Requirement / 要求 | Minimum Version / 最低版本 |
|---|---|
| Kotlin | 2.0.0+ |
| KSP | 2.0.21-1.0.28+ (matching your Kotlin version / 需匹配你的 Kotlin 版本) |
| Android Gradle Plugin | 8.x+ (if used on Android / 如果在 Android 上使用) |
| Java/JVM Toolchain | 17+ |

---

## Full Documentation

## 完整文档

See the [User Guide](docs/USER_GUIDE.md) for detailed setup instructions, annotation reference, API documentation, troubleshooting, and advanced topics.

请参阅[用户指南](docs/USER_GUIDE.md)获取详细的设置说明、注解参考、API 文档、故障排除以及高级主题。
