# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
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

# We use Reflection on the builder to avoid creating too many objects
 -keep class net.objecthunter.exp4j.ExpressionBuilder**
 -keepclassmembers class net.objecthunter.exp4j.ExpressionBuilder** {
    *;
 }
# Option screens
 -keep class com.frostbyte.launcher.prefs.screens** {*;}

# Gson deserializes model classes via reflection at runtime - R8 doesn't see
# this usage and will strip no-args constructors / restructure fields, causing
# "Abstract class can't be instantiated" crashes on minified builds.
# Keep fields and no-args constructors intact for the packages that actually
# hold Gson-deserialized model classes, without keeping the whole app.
-keepclassmembers class com.frostbyte.launcher.modloaders.** {
    <init>();
    <fields>;
}
-keep,allowobfuscation class com.frostbyte.launcher.modloaders.** { *; }

-keepclassmembers class com.frostbyte.launcher.authenticator.** {
    <init>();
    <fields>;
}
-keep,allowobfuscation class com.frostbyte.launcher.authenticator.** { *; }

-keepclassmembers class com.frostbyte.launcher.instances.** {
    <init>();
    <fields>;
}
-keep,allowobfuscation class com.frostbyte.launcher.instances.** { *; }

# Gson's own TypeToken/reflection internals need generic signature info preserved
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken


