# Sweet Lab ERP ProGuard Rules

# Keep JNA classes for UniFFI bindings
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# Keep UniFFI generated bindings
-keep class org.sweetlab.bindings.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# JNA references java.awt classes not available on Android
-dontwarn java.awt.**
