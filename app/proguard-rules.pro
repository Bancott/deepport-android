# 保持默认；后续接入 Room / Retrofit / WebView 时在此补充规则
-keepattributes *Annotation*, InnerClasses, Signature

# WebView 的 JS 桥：@JavascriptInterface 方法名不能被混淆
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class io.github.bancott.deepport.ui.chat.web.** { *; }

# Room 生成的实现类
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn javax.annotation.**
