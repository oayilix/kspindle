# Sample app-specific shrinking rules.
#
# In the default project-dependency mode, sample/build.gradle.kts adds KSPindle
# rules from ../kspindle-runtime/consumer-rules.pro. In published-artifact mode,
# the sample relies on the embedded META-INF/proguard rules from the runtime JAR.
# Both paths exercise the same runtime-discovery requirements as consumers.
