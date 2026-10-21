# Rules for the release build (minification is disabled by default).
# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.sahandsoft.englishlearner.**$$serializer { *; }
-keepclassmembers class com.sahandsoft.englishlearner.** { *** Companion; }
-keepclasseswithmembers class com.sahandsoft.englishlearner.** { kotlinx.serialization.KSerializer serializer(...); }
