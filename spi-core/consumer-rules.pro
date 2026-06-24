# SPI Framework - Consumer ProGuard/R8 Rules
# SPI 框架 - 消费者混淆规则
# Include these rules in Android apps that enable R8/ProGuard minification.
# 开启 R8/ProGuard 混淆的 Android 应用需要引入这些规则。

# Keep ServiceIndexProvider interface and all implementations (KSP-generated classes)
# 保留 ServiceIndexProvider 接口及其所有实现（KSP 生成的类）
-keep interface com.spi.framework.core.ServiceIndexProvider { *; }
-keep class * implements com.spi.framework.core.ServiceIndexProvider {
    <init>();
    void initialize(com.spi.framework.core.ServiceRegistry);
}

# Keep no-arg constructors of classes annotated with @ServiceProvider
# Reflection uses them to instantiate service implementations
# 保留 @ServiceProvider 注解类的无参构造函数（反射实例化需要）
-keepclassmembers @com.spi.framework.annotations.ServiceProvider class * {
    <init>();
}

# Keep SpiLoader and ServiceRegistry public API
# 保留 SpiLoader 和 ServiceRegistry 的公开 API
-keep class com.spi.framework.core.SpiLoader { public *; }
-keep class com.spi.framework.core.ServiceRegistry { public *; }
-keep class com.spi.framework.core.ServiceNotFoundException { *; }

# Keep ProviderRegistration data class (used by generated code)
# 保留 ProviderRegistration 数据类（生成的代码会使用）
-keep class com.spi.framework.core.ProviderRegistration { *; }
