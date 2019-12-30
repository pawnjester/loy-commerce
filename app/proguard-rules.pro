# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/ordgen/Android/Sdk/tools/proguard/proguard-android.txt
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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses

# joda time
-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# Jackson 2.x
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}

-keep class android.support.v7.widget.SearchView { *; }

-dontwarn org.apache.**

-dontwarn com.beloo.widget.chipslayoutmanager.Orientation
-dontwarn com.roughike.bottombar.**

-keep class co.loystar.loystarbusiness.models.databinders.** {
    public <methods>;
    protected <methods>;
}

-keepclassmembers class * {
     @com.fasterxml.jackson.annotation.JsonCreator *;
     @com.fasterxml.jackson.annotation.JsonProperty *;
}

 ## UXCAM
 -keep class com.uxcam.** { *; }
 -dontwarn com.uxcam.**

 ## Smooch
-dontwarn okio.**
-keep class com.google.gson.** { *; }
-keepclassmembers enum * { *; }
-keepclassmembers enum io.smooch.core.model.** { *; }