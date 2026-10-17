# AetherVault Omni Edition - Aggressive R8 / ProGuard Obfuscation & Security Rules

-repackageclasses com.example.secure.vault
-allowaccessmodification

# Optimization & Shrinking
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Remove all Logcat calls and debugging info in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Attributes removal
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-renamesourcefileattribute AetherVaultSecureCore

# Keep Room Database Entities & DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration { *; }

# Keep Kotlin Serialization & Retrofit / Ktor models
-keepattributes kotlinx.serialization.*
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}

# Keep JNI Native Methods & Security Checkpoints
-keepclassmembers class * {
    native <methods>;
}
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
