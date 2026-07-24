# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# HyprMX specific rules
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep class com.hyprmx.** {*;}
-keep class okhttp3.hyprmx.** { *; }
-keep interface okhttp3.hyprmx.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn okhttp3.hyprmx.**
-dontwarn okio.hyprmx.**
-dontwarn com.google.android.gms.ads.identifier.**
-dontwarn kotlin.**
-dontwarn com.hyprmx.**

-dontnote okhttp3.hyprmx.**
-dontnote okio.hyprmx.**
-dontnote com.google.android.gms.ads.identifier.**
-dontnote kotlin.**
-dontnote com.hyprmx.**
