package com.alifmed.app

import android.webkit.JavascriptInterface

/**
 * The site is a client-routed SPA, so most "page" navigation inside it
 * never triggers a normal WebView page load (no onPageStarted/onPageFinished).
 * This little bridge gets injected into every page; the injected JS hooks
 * history.pushState/replaceState and the popstate event, and calls back
 * into this class whenever the in-app route changes — so the native layer
 * can play a short transition animation even for in-app navigation.
 */
class RouteBridge(private val onRouteChanged: () -> Unit) {
    @JavascriptInterface
    fun onRouteChanged() {
        onRouteChanged.invoke()
    }
}

const val ROUTE_WATCHER_JS = """
(function() {
  if (window.__alifRouteWatcherInstalled) return;
  window.__alifRouteWatcherInstalled = true;

  function notify() {
    if (window.AndroidRouteBridge) {
      window.AndroidRouteBridge.onRouteChanged();
    }
  }

  var pushState = history.pushState;
  history.pushState = function() {
    var result = pushState.apply(history, arguments);
    notify();
    return result;
  };

  var replaceState = history.replaceState;
  history.replaceState = function() {
    var result = replaceState.apply(history, arguments);
    notify();
    return result;
  };

  window.addEventListener('popstate', notify);
})();
"""
