package com.limelight.shared.ui.components

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.ConsoleMessage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun PlatformWebView(
    url: String,
    modifier: Modifier,
    onBackAvailable: (Boolean) -> Unit,
    backTrigger: Boolean,
    onBackHandled: () -> Unit
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(backTrigger) {
        if (backTrigger) {
            val webView = webViewRef.value
            if (webView != null && webView.canGoBack()) {
                webView.goBack()
            }
            onBackHandled()
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                val currentWebView = this
                webViewRef.value = currentWebView

                // Configure Cookie Manager explicitly for session token storage
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        setAcceptThirdPartyCookies(currentWebView, true)
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    javaScriptCanOpenWindowsAutomatically = true
                }

                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        onBackAvailable(view?.canGoBack() == true)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage != null) {
                            android.util.Log.d(
                                "ImmichWebView",
                                "[JS ${consoleMessage.messageLevel()}]: ${consoleMessage.message()} (line ${consoleMessage.lineNumber()} in ${consoleMessage.sourceId()})"
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                loadUrl(url)
            }
        },
        modifier = modifier,
        update = { webView ->
            if (webView.url != url && !url.isNullOrEmpty()) {
                webView.loadUrl(url)
            }
        }
    )
}
