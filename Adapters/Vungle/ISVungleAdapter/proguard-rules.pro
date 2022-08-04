# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/galvan/Downloads/adt-bundle-mac-x86_64-20140702/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# Vungle
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.ads.identifier.**

# Moat SDK
-keep class com.moat.** { *; }
-dontwarn com.moat.**

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keepattributes *Annotation*

##Retrofit
# Retrofit does reflection on generic parameters and InnerClass is required to use Signature.
-keepattributes Signature, InnerClasses

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.-KotlinExtensions

##Okio+OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn com.vungle.warren.downloader.DownloadRequestMediator$Status
-dontwarn com.vungle.warren.error.VungleError$ErrorCode

#In case keep annotations don't work uncomment this
#-keep class com.vungle.warren.Vungle { *; }
#-keepclassmembers enum com.vungle.warren.** { *; }