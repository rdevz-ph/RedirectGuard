# Code & Resource Shrinking Rules for Redirect Guard

# Repackage all non-kept classes into root package to shrink DEX string pool
-repackageclasses ''
-allowaccessmodification

# Preserve line numbers and source file for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve standard annotations and generic signatures for Compose and Coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Shizuku API & Provider (Binder interfaces, AIDL, and reflection)
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }
-dontwarn dev.rikka.shizuku.**

# Android Components & Background Services
-keep class com.rdevzph.redirectguard.RedirectGuardApplication { *; }
-keep class com.rdevzph.redirectguard.MainActivity { *; }
-keep class com.rdevzph.redirectguard.ui.prompt.RedirectConfirmationActivity { *; }
-keep class com.rdevzph.redirectguard.service.** { *; }
-keep class com.rdevzph.redirectguard.shizuku.** { *; }
-keep class com.rdevzph.redirectguard.notification.** { *; }

# Models & Preferences
-keep class com.rdevzph.redirectguard.model.** { *; }
-keep class com.rdevzph.redirectguard.data.** { *; }

# AndroidX Lifecycle & ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Compose
-dontwarn androidx.compose.**