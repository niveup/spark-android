# Phase 8 R8 rules.
# Keep annotations and generic signatures used by Retrofit and
# kotlinx.serialization. Without Signature, Retrofit cannot resolve generic
# response types after shrinking.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, EnclosingMethod

# kotlinx.serialization generates a serializer class plus a serializer()
# bridge for every @Serializable class. The runtime looks these up
# reflectively, so they must survive R8.
-keep,includedescriptorclasses class com.spark.app.**$$serializer { *; }
-keepclassmembers class com.spark.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.spark.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit calls the annotated methods through a dynamic proxy. Keep the
# interface (and its annotations) intact.
-keep interface com.spark.app.data.remote.SparkApi { *; }

# Retrofit/OkHttp generic types and coroutine continuations.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
