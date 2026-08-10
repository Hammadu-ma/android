package com.alifmed.app

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val APP_URL = "https://alifmeta.vercel.app"
private val BrandBackground = androidx.compose.ui.graphics.Color(0xFFF4F7F5)
private val BrandDark = androidx.compose.ui.graphics.Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isOnline by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableStateOf(0f) }
    var isTransitioning by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        observeConnectivity(context).collect { online -> isOnline = online }
    }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.parseColor("#F4F7F5"))

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                loadWithOverviewMode = true
                useWideViewPort = true
                mediaPlaybackRequiresUserGesture = false
                setSupportZoom(false)
                userAgentString = "$userAgentString AlifMedApp/1.0"
            }

            // If the site registers a service worker (typical for a modern
            // Vite/React PWA), this lets that service worker actually
            // intercept requests inside the WebView — meaning real offline
            // caching driven by the site itself, not a guess on our end.
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
                val swController = ServiceWorkerControllerCompat.getInstance()
                swController.setServiceWorkerClient(object : ServiceWorkerClientCompat() {
                    override fun shouldInterceptRequest(request: WebResourceRequest) = null
                })
            }

            addJavascriptInterface(
                RouteBridge {
                    // Called from JS whenever the SPA's client-side router
                    // changes route (pushState/replaceState/popstate).
                    scope.launch {
                        isTransitioning = true
                        delay(220)
                        isTransitioning = false
                    }
                },
                "AndroidRouteBridge"
            )

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    loadProgress = newProgress / 100f
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    isLoading = true
                    hasError = false
                    isTransitioning = true
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    isLoading = false
                    isTransitioning = false
                    view.evaluateJavascript(ROUTE_WATCHER_JS, null)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        isLoading = false
                        hasError = true
                    }
                }
            }

            loadUrl(APP_URL)
        }
    }

    BackHandler(enabled = true) {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BrandBackground)) {
        when {
            !isOnline -> StatusScreen(
                icon = Icons.Filled.WifiOff,
                title = "No internet connection",
                subtitle = "Check your connection and try again.",
                onRetry = { webView.reload() }
            )
            hasError -> StatusScreen(
                icon = Icons.Filled.Refresh,
                title = "Something went wrong",
                subtitle = "We couldn't load Alif Med. Please try again.",
                onRetry = {
                    hasError = false
                    isLoading = true
                    webView.reload()
                }
            )
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            isRefreshing = true
                            webView.reload()
                            delay(600)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Thin progress bar during real page loads (not SPA route changes).
        AnimatedVisibility(
            visible = isLoading && !hasError && isOnline,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { loadProgress },
                modifier = Modifier.fillMaxWidth(),
                color = BrandDark,
                trackColor = BrandDark.copy(alpha = 0.08f)
            )
        }

        // Quick, modern crossfade any time navigation happens — full page
        // load OR an in-app SPA route change caught by RouteBridge.
        AnimatedVisibility(
            visible = isTransitioning && !hasError && isOnline,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(220)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(BrandBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandDark, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun StatusScreen(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(BrandBackground), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandDark.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = BrandDark,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandDark.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
