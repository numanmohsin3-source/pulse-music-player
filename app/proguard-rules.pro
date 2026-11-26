-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'p'
-flattenpackagehierarchy 'p'

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.pulsemusic.player.data.** { *; }
