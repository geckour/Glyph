# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android Studio.app/sdk/tools/proguard/proguard-android.txt
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
-dontwarn kotlin.**

-keep class com.facebook.stetho.** { *; }
-dontwarn com.facebook.stetho.**

-dontwarn android.databinding.**

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keepclassmembers class ** {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

-keep public class kotlin.reflect.jvm.internal.impl.builtins.* { public *; }
-keep class jp.org.example.geckour.glyph.ui.adapter.MoshiAdapter
-keep class jp.org.example.geckour.glyph.model.** { *; }
-dontwarn jp.org.example.geckour.glyph.ui.**