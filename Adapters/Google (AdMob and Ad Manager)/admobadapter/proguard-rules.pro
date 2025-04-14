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
-keep public class com.google.android.gms.ads.internal.ClientApi {
  <init>();
}

# We keep all fields for every generated proto file as the runtime uses
# reflection over them that ProGuard cannot detect. Without this keep
# rule, fields may be removed that would cause runtime failures.
-keepclassmembers class * extends com.google.android.gms.internal.ads.zzecd {
  <fields>;
}

# Auto-generated proguard rule with obfuscated symbol
-dontwarn com.google.android.gms.internal.ads.zzaxq

