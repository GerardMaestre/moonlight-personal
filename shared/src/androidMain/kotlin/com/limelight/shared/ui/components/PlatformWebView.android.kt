package com.limelight.shared.ui.components

import android.webkit.CookieManager
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.ConsoleMessage
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import android.webkit.SslErrorHandler
import android.net.http.SslError
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Volatile
private var latestToken: String? = null

@Composable
actual fun PlatformWebView(
    url: String,
    modifier: Modifier,
    onBackAvailable: (Boolean) -> Unit,
    backTrigger: Boolean,
    onBackHandled: () -> Unit,
    onTokenAcquired: (String) -> Unit
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val lastLoadedUrl = remember { mutableStateOf("") }

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
                // Enable WebView remote debugging for easier JS/console inspection
                try {
                    WebView.setWebContentsDebuggingEnabled(true)
                    android.util.Log.d("ImmichWebView", "WebView debugging enabled")
                } catch (e: Exception) {
                    android.util.Log.w("ImmichWebView", "Failed to enable WebView debugging: ${e.message}")
                }

                // Configure Cookie Manager explicitly for session token storage
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        setAcceptThirdPartyCookies(currentWebView, true)
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    // Allow multiple windows (OAuth popups rely on this)
                    setSupportMultipleWindows(true)
                    // Use a modern mobile user-agent to avoid some embedded-browser blocks
                    userAgentString = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36"
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
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        try {
                            if (request != null) {
                                val urlStr = request.url.toString()
                                android.util.Log.d("ImmichWebView", "Request: ${request.method} $urlStr")
                                
                                // 1. Extract Bearer Token from headers if present
                                val authHeader = request.requestHeaders["Authorization"] ?: request.requestHeaders["authorization"]
                                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                    val token = authHeader.substringAfter("Bearer ").trim()
                                    if (token.isNotBlank() && token != latestToken) {
                                        android.util.Log.d("ImmichWebView", "Extracted Bearer token from request headers: $token")
                                        latestToken = token
                                    }
                                }

                                // 2. Manually intercept and authenticate asset requests
                                if (urlStr.contains("/api/assets/") && request.method == "GET") {
                                    val token = latestToken
                                    if (token != null) {
                                        try {
                                            val connection = URL(urlStr).openConnection() as HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.connectTimeout = 10000
                                            connection.readTimeout = 10000
                                            
                                            // Copy original request headers
                                            request.requestHeaders.forEach { (key, value) ->
                                                connection.setRequestProperty(key, value)
                                            }
                                            
                                            // Inject authentication
                                            connection.setRequestProperty("Authorization", "Bearer $token")
                                            connection.setRequestProperty("Cookie", "immich_access_token=$token")
                                            
                                            val responseCode = connection.responseCode
                                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                                val contentType = connection.contentType ?: "image/jpeg"
                                                val contentEncoding = connection.contentEncoding ?: "UTF-8"
                                                
                                                val responseHeaders = mutableMapOf<String, String>()
                                                connection.headerFields.forEach { (key, value) ->
                                                    if (key != null && value.isNotEmpty()) {
                                                        responseHeaders[key] = value.joinToString(", ")
                                                    }
                                                }
                                                
                                                android.util.Log.d("ImmichWebView", "Manually authenticated asset load: $urlStr -> 200 OK")
                                                return WebResourceResponse(
                                                    contentType,
                                                    contentEncoding,
                                                    responseCode,
                                                    "OK",
                                                    responseHeaders,
                                                    connection.inputStream
                                                )
                                            } else {
                                                android.util.Log.w("ImmichWebView", "Manually authenticated asset failed: $urlStr -> Code $responseCode")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.w("ImmichWebView", "Failed manual asset fetch: ${e.message}")
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ImmichWebView", "Request logging/intercept failed: ${e.message}")
                        }
                        return null
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        onBackAvailable(view?.canGoBack() == true)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url
                        val urlStr = uri?.toString()
                        if (view != null && urlStr != null) {
                            android.util.Log.d("ImmichWebView", "shouldOverrideUrlLoading -> $urlStr")
                            return false
                        }
                        return false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (view != null && url != null) {
                            android.util.Log.d("ImmichWebView", "shouldOverrideUrlLoading (deprecated) -> $url")
                            return false
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        android.util.Log.d("ImmichWebView", "onPageStarted: $url")
                        try {
                            val cookies = if (url != null) CookieManager.getInstance().getCookie(url) else null
                            android.util.Log.d("ImmichWebView", "Cookies on start for $url: $cookies")
                        } catch (e: Exception) {
                            android.util.Log.w("ImmichWebView", "Failed reading cookies on start: ${e.message}")
                        }
                        
                        // Inject early for initial execution
                        try {
                            view?.evaluateJavascript(immichInstrumentationAndPolyfillJs, null)
                        } catch (e: Exception) {
                            android.util.Log.w("ImmichWebView", "JS injection onPageStarted failed: ${e.message}")
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Update back availability and flush cookies to persistent storage
                        onBackAvailable(view?.canGoBack() == true)
                        try {
                            val cookies = if (url != null) CookieManager.getInstance().getCookie(url) else null
                            android.util.Log.d("ImmichWebView", "onPageFinished: $url; cookies=$cookies")
                            
                            // Extract immich_access_token if present and call the callback
                            if (cookies != null) {
                                val pairs = cookies.split(";")
                                for (pair in pairs) {
                                    val trimmed = pair.trim()
                                    if (trimmed.startsWith("immich_access_token=")) {
                                        val token = trimmed.substringAfter("immich_access_token=")
                                        if (token.isNotBlank()) {
                                            android.util.Log.d("ImmichWebView", "Extracted immich_access_token from cookies: $token")
                                            latestToken = token
                                            onTokenAcquired(token)
                                            break
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ImmichWebView", "Failed reading/parsing cookies: ${e.message}")
                        }
                        
                        // Re-inject on page finish to ensure absolute coverage
                        try {
                            view?.evaluateJavascript(immichInstrumentationAndPolyfillJs, null)
                        } catch (e: Exception) {
                            android.util.Log.w("ImmichWebView", "JS injection onPageFinished failed: ${e.message}")
                        }
                        
                        try {
                            CookieManager.getInstance().flush()
                        } catch (e: Exception) {
                            android.util.Log.w("ImmichWebView", "Cookie flush failed: ${e.message}")
                        }
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        if (request != null && error != null) {
                            android.util.Log.e("ImmichWebView", "Error loading: ${request.url} -> ${error.description} (code ${error.errorCode})")
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                        android.util.Log.e("ImmichWebView", "SSL Error loading: ${error?.url} -> ${error?.primaryError}")
                        handler?.proceed()
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

                    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                        // Handle popup windows (commonly used by OAuth flows)
                        android.util.Log.d("ImmichWebView", "onCreateWindow triggered; isUserGesture=$isUserGesture")
                        val context = view?.context ?: return false
                        val newWebView = WebView(context)
                        newWebView.settings.javaScriptEnabled = true
                        newWebView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                // Redirect popup navigation back to the parent WebView
                                if (url != null && view != null) {
                                    android.util.Log.d("ImmichWebView", "Popup attempted to open: $url")
                                    try {
                                        this@apply.loadUrl(url)
                                    } catch (e: Exception) {
                                        android.util.Log.w("ImmichWebView", "Failed to load popup URL: ${e.message}")
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        transport?.webView = newWebView
                        resultMsg?.sendToTarget()
                        android.util.Log.d("ImmichWebView", "Popup routed into main WebView")
                        return true
                    }
                }

                lastLoadedUrl.value = url
                loadUrl(url)
            }
        },
        modifier = modifier,
        update = { webView ->
            if (url != lastLoadedUrl.value && !url.isNullOrEmpty()) {
                lastLoadedUrl.value = url
                webView.loadUrl(url)
            }
        }
    )
}

private val immichInstrumentationAndPolyfillJs = """
    (function(){
        // 1. Fetch & XHR Logger Hooks
        if (!window.__immich_xhr_hooked) {
            window.__immich_xhr_hooked = true;
            const _fetch = window.fetch;
            window.fetch = function(input, init){
                try{
                    const method = (init && init.method) || 'GET';
                    console.log('[ImmichXHR] fetch', method, input, init && init.body);
                } catch(e){}
                return _fetch.apply(this, arguments).then(function(res){
                    try{ console.log('[ImmichXHR] fetch:response', res.url, res.status); }catch(e){}
                    return res;
                });
            };
            const XHR = window.XMLHttpRequest;
            function HookedXHR(){
                const xhr = new XHR();
                const origOpen = xhr.open;
                xhr.open = function(method, url){
                    this._immich_method = method;
                    this._immich_url = url;
                    return origOpen.apply(this, arguments);
                };
                xhr.addEventListener('load', function(){
                    try{ console.log('[ImmichXHR] xhr', this._immich_method, this._immich_url, this.status); }catch(e){}
                });
                return xhr;
            }
            try{ window.XMLHttpRequest = HookedXHR; }catch(e){}
        }

        // 2. Web Crypto API crypto.subtle.digest (SHA-256) Polyfill for HTTP contexts
        if (!window.crypto) {
            window.crypto = {};
        }
        if (!window.crypto.subtle) {
            console.log('[ImmichWebView] Web Crypto API (crypto.subtle) was undefined. Injecting SHA-256 polyfill...');
            
            function sha256_pure_js(buffer) {
                var uint8 = new Uint8Array(buffer);
                var words = [];
                for (var i = 0; i < uint8.length; i++) {
                    words[i >> 2] |= uint8[i] << (24 - (i % 4) * 8);
                }
                var h0 = 0x6a09e667, h1 = 0xbb67ae85, h2 = 0x3c6ef372, h3 = 0xa54ff53a;
                var h4 = 0x5be0cd19, h5 = 0x510e527f, h6 = 0x9b05688c, h7 = 0x1f83d9ab;
                var k = [
                    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
                    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
                    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
                    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
                    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
                    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
                    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
                    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
                ];
                var byteLength = uint8.length;
                var bitLength = byteLength * 8;
                words[byteLength >> 2] |= 0x80 << (24 - (byteLength % 4) * 8);
                var wordLength = ((byteLength + 8) >> 6) * 16 + 14;
                words[wordLength] = (bitLength / 0x100000000) | 0;
                words[wordLength + 1] = bitLength | 0;
                for (var i = 0; i < wordLength + 2; i++) {
                    if (words[i] === undefined) { words[i] = 0; }
                }
                for (var i = 0; i < words.length; i += 16) {
                    var w = new Array(64);
                    for (var j = 0; j < 16; j++) { w[j] = words[i + j] | 0; }
                    for (var j = 16; j < 64; j++) {
                        var s0 = ((w[j - 15] >>> 7) | (w[j - 15] << 25)) ^ ((w[j - 15] >>> 18) | (w[j - 15] << 14)) ^ (w[j - 15] >>> 3);
                        var s1 = ((w[j - 2] >>> 17) | (w[j - 2] << 15)) ^ ((w[j - 2] >>> 19) | (w[j - 2] << 13)) ^ (w[j - 2] >>> 10);
                        w[j] = (w[j - 16] + s0 + w[j - 7] + s1) | 0;
                    }
                    var a = h0, b = h1, c = h2, d = h3, e = h4, f = h5, g = h6, h = h7;
                    for (var j = 0; j < 64; j++) {
                        var S1 = ((e >>> 6) | (e << 26)) ^ ((e >>> 11) | (e << 21)) ^ ((e >>> 25) | (e << 7));
                        var ch = (e & f) ^ ((~e) & g);
                        var temp1 = (h + S1 + ch + k[j] + w[j]) | 0;
                        var S0 = ((a >>> 2) | (a << 30)) ^ ((a >>> 13) | (a << 19)) ^ ((a >>> 22) | (a << 10));
                        var maj = (a & b) ^ (a & c) ^ (b & c);
                        var temp2 = (S0 + maj) | 0;
                        h = g; g = f; f = e; e = (d + temp1) | 0; d = c; c = b; b = a; a = (temp1 + temp2) | 0;
                    }
                    h0 = (h0 + a) | 0; h1 = (h1 + b) | 0; h2 = (h2 + c) | 0; h3 = (h3 + d) | 0;
                    h4 = (h4 + e) | 0; h5 = (h5 + f) | 0; h6 = (h6 + g) | 0; h7 = (h7 + h) | 0;
                }
                var res = new Uint8Array(32);
                var hash = [h0, h1, h2, h3, h4, h5, h6, h7];
                for (var i = 0; i < 8; i++) {
                    res[i * 4] = (hash[i] >>> 24) & 0xff;
                    res[i * 4 + 1] = (hash[i] >>> 16) & 0xff;
                    res[i * 4 + 2] = (hash[i] >>> 8) & 0xff;
                    res[i * 4 + 3] = hash[i] & 0xff;
                }
                return res.buffer;
            }

            window.crypto.subtle = {
                digest: function(algorithm, data) {
                    return new Promise(function(resolve, reject) {
                        try {
                            var algName = '';
                            if (typeof algorithm === 'string') {
                                algName = algorithm.toUpperCase();
                            } else if (algorithm && typeof algorithm === 'object' && typeof algorithm.name === 'string') {
                                algName = algorithm.name.toUpperCase();
                            }
                            if (algName === 'SHA-256') {
                                var hashBuffer = sha256_pure_js(data);
                                resolve(hashBuffer);
                            } else {
                                reject(new Error('Algorithm not supported by polyfill: ' + algName));
                            }
                        } catch (e) {
                            reject(e);
                        }
                    });
                }
            };
        }

        // 3. Virtual Scroll Trigger: Dispatch resize events to force SvelteKit/Immich to measure and render items
        (function() {
            function triggerResize() {
                try {
                    console.log('[ImmichWebView] Triggering window resize event to force scroller layout...');
                    window.dispatchEvent(new Event('resize'));
                    document.body.dispatchEvent(new Event('resize'));
                } catch(e) {}
            }
            triggerResize();
            setTimeout(triggerResize, 300);
            setTimeout(triggerResize, 800);
            setTimeout(triggerResize, 1500);
            setTimeout(triggerResize, 3000);
            setTimeout(triggerResize, 5000);
        })();
    })();
""".trimIndent()
