# SuperShade ProGuard rules

# Koin — keep generated metadata
-keepnames class com.supershade.** { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# HiddenApiBypass
-keep class org.lsposed.hiddenapibypass.** { *; }

# Sentry — keep stack traces readable
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class io.sentry.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}

# Compose runtime
-keep class androidx.compose.runtime.** { *; }

# Application entry point
-keep class com.supershade.SuperShadeApplication { *; }
-keep class com.supershade.MainActivity { *; }
-keep class com.supershade.service.** { *; }

# Keep all Koin module declarations
-keep class com.supershade.di.** { *; }
