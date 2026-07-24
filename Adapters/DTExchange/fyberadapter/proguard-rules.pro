# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

##---------------Begin: proguard configuration for Inneractive SDK  ----------

-keep class com.fyber.inneractive.sdk.** {*;}
-dontwarn com.fyber.inneractive.sdk.**
-keepclassmembers class com.fyber.inneractive.sdk.** {*;}
-keepclassmembers class com.fyber.inneractive.sdk.rtb.nativead.** {*;}

##---------------End: proguard configuration for Inneractive SDK  ----------


##---------------Begin: proguard configuration for Google play services  ----------

-dontwarn com.google.android.gms.**

# for Google play services – general – based on Google's documentation
-keep class * extends java.util.ListResourceBundle
{ protected Object[][] getContents(); }
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable
{ public static final *** NULL; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class *
{ @com.google.android.gms.common.annotation.KeepName *; }
-keepnames class * implements android.os.Parcelable
{ public static final ** CREATOR; }

# for google play services, to support Inneractive’s reflection
-keep class com.google.android.gms.common.GooglePlayServicesUtil
{ int isGooglePlayServicesAvailable (android.content.Context); }

-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient
{ static com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info
{ *; }
-keep class com.google.android.gms.common.GoogleApiAvailability
{ static com.google.android.gms.common.GoogleApiAvailability getInstance(); int isGooglePlayServicesAvailable(android.content.Context);}

##---------------End: proguard configuration for Google play services  ----------


##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

-keep class com.google.gson.Gson { *; }
-keep class com.google.gson.GsonBuilder { *; }
-keep class com.google.gson.FieldNamingStrategy { *; }

##---------------End: proguard configuration for Gson  ----------

##---------------Start: inneractive log removal -----------------


-assumenosideeffects class com.fyber.inneractive.sdk.util.IAlog {
    public static void v(...);
    public static void i(...);
    # Keep warnning
    # public static void w(...);
    public static void d(...);
    # Keep errors
    # public static void e(...);
}
