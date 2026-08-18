# FrostByte Launcher ProGuard rules.
# Add project-specific keep rules here as native/reflection-based libraries are introduced.

# Keep Room entities/DAOs (annotation-driven, but keep names for schema stability)
-keep class com.frostbyte.launcher.core.storage.** { *; }

# Keep Retrofit/Gson model classes (field names matter for JSON mapping)
-keep class com.frostbyte.launcher.core.network.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
