# Pocketmind ProGuard rules

# Keep kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.pocketmind.**$$serializer { *; }
-keepclassmembers class com.pocketmind.** { *** Companion; }
-keepclasseswithmembers class com.pocketmind.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Keep llama.cpp JNI bridge
-keep class com.pocketmind.engine.LlamaCppEngine { *; }
-keep class com.pocketmind.engine.LlamaCppEngine$* { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Tink (via security-crypto) references error-prone annotations that are
# compile-only; SLF4J ships optional bindings. Safe to ignore at runtime.
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
