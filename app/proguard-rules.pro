# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Tink optional deps (KeysDownloader uses Google API client + Joda, not included)
-dontwarn com.google.api.client.**
-dontwarn com.google.crypto.tink.util.KeysDownloader
-dontwarn org.joda.time.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
