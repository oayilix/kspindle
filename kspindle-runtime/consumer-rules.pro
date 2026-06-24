# KSPindle - Consumer ProGuard/R8 Rules
# KSPindle - 消费者混淆规则
# Include these rules in Android apps that enable R8/ProGuard minification.
# 开启 R8/ProGuard 混淆的 Android 应用需要引入这些规则。

# Keep ServiceIndexProvider interface and all implementations (KSP-generated classes)
# 保留 ServiceIndexProvider 接口及其所有实现（KSP 生成的类）
-keep interface io.github.oayilix.kspindle.runtime.ServiceIndexProvider { *; }
-keep class * implements io.github.oayilix.kspindle.runtime.ServiceIndexProvider {
    <init>();
    void initialize(io.github.oayilix.kspindle.runtime.ServiceRegistry);
}

# Keep no-arg constructors of classes annotated with @ServiceProvider
# Reflection uses them to instantiate service implementations
# 保留 @ServiceProvider 注解类的无参构造函数（反射实例化需要）
-keepclassmembers @io.github.oayilix.kspindle.annotations.ServiceProvider class * {
    <init>();
}

# Keep Kspindle and ServiceRegistry public API
# 保留 Kspindle 和 ServiceRegistry 的公开 API
-keep class io.github.oayilix.kspindle.runtime.Kspindle { public *; }
-keep class io.github.oayilix.kspindle.runtime.ServiceRegistry { public *; }
-keep class io.github.oayilix.kspindle.runtime.ServiceNotFoundException { *; }

# Keep ProviderRegistration data class (used by generated code)
# 保留 ProviderRegistration 数据类（生成的代码会使用）
-keep class io.github.oayilix.kspindle.runtime.ProviderRegistration { *; }
