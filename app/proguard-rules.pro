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

# WorkManager instantiates its Room database by reflection (WorkDatabase_Impl.<init>()).
# R8 from AGP 9.5 stopped keeping that constructor via the library's consumer rules,
# so the release build crashed in androidx.startup before any app code ran (1.7.1).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
