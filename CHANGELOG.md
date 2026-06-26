# Changelog / 变更日志

All notable changes to KSPindle. / KSPindle 的所有重要变更。

---

## [0.5.0] — Unreleased / 未发布

### Added / 新增
- **Core SPI runtime** (`kspindle-runtime`): `Kspindle`, `ServiceRegistry`, `RegistryEntry`, `ProviderRegistration`, `ServiceNotFoundException`.
- **Annotations** (`kspindle-annotations`): `@ServiceProvider`, `@ServiceProviderInterface`, `ServiceFactory`.
- **KSP compiler** (`kspindle-compiler`): `ServiceProviderProcessor` with validation, code generation, and `META-INF/services` discovery.
- **Kotlin reified API**: `Kspindle.load<T>()`, `loadAll<T>()`, `loadByName<T>(name)`, `isRegistered<T>()`.
- **Factory pattern**: `ServiceFactory<T>` interface for DI-friendly instantiation.
- **Compile-time validation**: KSP checks for no-arg constructor when factory is not specified.
- **Debug tooling**: `Kspindle.dump()` and `ServiceRegistry.dump()` for registry inspection.
- **ProGuard/R8 rules**: `consumer-rules.pro` shipped in `kspindle-runtime` under `META-INF/proguard/`.
- **Thread safety**: `ConcurrentHashMap` + `CopyOnWriteArrayList` + double-checked locking + `@Volatile`.
- **Multi-module sample**: `sample-api` / `sample-impl` / `sample` demonstrating SPI decoupling.
- **Bilingual support**: All KDoc, comments, and documentation in Chinese + English. / 中英双语支持。
- **Test suite**: 106 unit tests including concurrency stress tests and edge cases.

### Module Structure / 模块结构

```
io.github.oayilix:kspindle-annotations — annotations
io.github.oayilix:kspindle-compiler    — KSP processor
io.github.oayilix:kspindle-runtime     — runtime
```

### Requirements / 环境要求

| Requirement / 要求 | Version / 版本 |
|-------------------|---------------|
| Kotlin            | 2.0+          |
| KSP               | 2.0.21-1.0.28+|
| JVM Target        | 17            |
| Gradle            | 8.x+          |

---

## [0.4.0] — 2026-06-11

### Added / 新增
- Debug `dump()` method on `Kspindle` and `ServiceRegistry`.
- `RegistryEntry.toString()` with initialization status, factory presence, and priority.
- Sample `MainActivity` logs registry dump via `Log.d("SPI", Kspindle.dump())`.

---

## [0.3.0] — 2026-06-11

### Added / 新增
- `ServiceFactory<T>` interface and `factory` parameter on `@ServiceProvider`.
- KSP compile-time no-arg constructor validation with clear error messages.
- `consumer-rules.pro` shipped with `kspindle-runtime` for R8/ProGuard safety.
- `docs/proguard.md` — setup guide for ProGuard/R8.

---

## [0.2.0] — 2026-06-11

### Added / 新增
- Kotlin reified inline API: `load<T>()`, `loadAll<T>()`, `loadByName<T>(name)`, `isRegistered<T>()`.
- KSP duplicate (service, name) pair detection with warnings.
- KSP processing summary log grouped by service interface.
- Concurrency stress tests (50-thread lazy singleton, 20-thread parallel load, etc.).
- Edge case tests (extreme values, empty name, non-existent service, etc.).
- Bilingual KDoc and comments across all source files. / 全部源文件的中英双语 KDoc 和注释。
- Bilingual README and User Guide. / 中英双语 README 和用户指南。
- Multi-module sample: `sample-api` / `sample-impl` / `sample` decoupling demo.

### Changed / 变更
- Test suite: 88 → 103 tests.

---

[0.5.0]: https://github.com/oayilix/kspindle/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/oayilix/kspindle/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/oayilix/kspindle/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/oayilix/kspindle/compare/v0.1.0...v0.2.0
