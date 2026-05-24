# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.podtekst.decoder.**$$serializer { *; }
-keepclassmembers class com.podtekst.decoder.** {
    *** Companion;
}
-keepclasseswithmembers class com.podtekst.decoder.** {
    kotlinx.serialization.KSerializer serializer(...);
}
