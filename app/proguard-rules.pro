# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keep class com.epic.aiexpensevoice.data.remote.dto.** { *; }
-keep class com.epic.aiexpensevoice.domain.model.** { *; }
-keep class com.epic.aiexpensevoice.data.local.db.** { *; }
-keep class com.epic.aiexpensevoice.data.local.chat.** { *; }

# Keep OkHttp & Retrofit annotations
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }

# Keep Gson annotations
-keep class com.google.gson.** { *; }
