# Keep the JS bridge class + its methods, since ProGuard would otherwise
# strip the @JavascriptInterface method the site's JS calls into.
-keepclassmembers class com.alifmed.app.RouteBridge {
    @android.webkit.JavascriptInterface <methods>;
}
