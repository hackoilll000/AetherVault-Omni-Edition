package com.example.features.workstation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyanNeon
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance Web & WebAssembly Workstation Runtime
 * Injects:
 * - Android DownloadListener for direct HTTP/HTTPS file downloads
 * - JavaScript Blob & WebAssembly interception bridge
 * - WebResourceRequest interceptor
 * Automatically routes downloaded/generated binary artifacts into native storage or Telegram pipeline.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WasmWebWorkstationView(
    url: String,
    modifier: Modifier = Modifier,
    onFileCaptured: (capturedFile: File, filename: String, mimeType: String) -> Unit = { _, _, _ -> },
    onTitleChanged: (String) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = false
                        allowContentAccess = false
                    }

                    // 1. Native JS Bridge for Blob & WASM Stream Interception
                    addJavascriptInterface(
                        WasmBlobNativeBridge(context) { capturedFile, filename, mime ->
                            onFileCaptured(capturedFile, filename, mime)
                        },
                        "AetherVaultNative"
                    )

                    // 2. Standard DownloadListener
                    setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                        Log.d("WasmWorkstation", "Download triggered via DownloadListener: $downloadUrl")
                        // Extract filename
                        val filename = URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                        // Trigger async fetch or notify bridge
                        val code = """
                            (function() {
                                fetch('$downloadUrl')
                                    .then(res => res.blob())
                                    .then(blob => {
                                        var reader = new FileReader();
                                        reader.onloadend = function() {
                                            var base64data = reader.result.split(',')[1];
                                            window.AetherVaultNative.onBlobCaptured(base64data, '$filename', '$mimetype');
                                        };
                                        reader.readAsDataURL(blob);
                                    })
                                    .catch(err => console.error('Fetch error:', err));
                            })();
                        """.trimIndent()
                        evaluateJavascript(code, null)
                    }

                    // 3. Custom WebViewClient with Blob Interception Script Injection
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            injectBlobInterceptionScript(view)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            // Can inspect or log WASM/binary resource loads
                            val reqUrl = request?.url?.toString() ?: ""
                            if (reqUrl.endsWith(".wasm") || reqUrl.contains("application/wasm")) {
                                Log.d("WasmWorkstation", "Intercepted WASM Binary Load: $reqUrl")
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { onTitleChanged(it) }
                        }
                    }

                    loadUrl(url)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("wasm_workstation_webview")
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(40.dp))
            }
        }
    }
}

/**
 * Injects a script that hooks window.URL.createObjectURL and <a> click downloads
 * to automatically relay Blob & WebAssembly outputs to AetherVaultNative bridge.
 */
private fun injectBlobInterceptionScript(view: WebView?) {
    val script = """
        (function() {
            if (window._aetherVaultHooked) return;
            window._aetherVaultHooked = true;

            const origCreateObjectURL = window.URL.createObjectURL;
            window.URL.createObjectURL = function(blob) {
                const url = origCreateObjectURL.call(window.URL, blob);
                if (blob instanceof Blob && blob.size > 0) {
                    const reader = new FileReader();
                    reader.onloadend = function() {
                        const base64 = reader.result.split(',')[1];
                        const name = "wasm_artifact_" + Date.now() + (blob.type ? "." + blob.type.split('/')[1] : ".bin");
                        window.AetherVaultNative.onBlobCaptured(base64, name, blob.type || 'application/octet-stream');
                    };
                    reader.readAsDataURL(blob);
                }
                return url;
            };

            // Intercept anchor downloads
            document.addEventListener('click', function(e) {
                let target = e.target;
                while (target && target.tagName !== 'A') {
                    target = target.parentElement;
                }
                if (target && target.tagName === 'A' && target.hasAttribute('download')) {
                    const href = target.getAttribute('href');
                    const filename = target.getAttribute('download') || 'download.bin';
                    if (href && href.startsWith('blob:')) {
                        fetch(href)
                            .then(r => r.blob())
                            .then(blob => {
                                const reader = new FileReader();
                                reader.onloadend = function() {
                                    const base64 = reader.result.split(',')[1];
                                    window.AetherVaultNative.onBlobCaptured(base64, filename, blob.type || 'application/octet-stream');
                                };
                                reader.readAsDataURL(blob);
                            })
                            .catch(err => console.error(err));
                    }
                }
            }, true);
        })();
    """.trimIndent()

    view?.evaluateJavascript(script, null)
}

/**
 * Native Android JavaScript Interface
 */
class WasmBlobNativeBridge(
    private val context: Context,
    private val onCaptured: (File, String, String) -> Unit
) {
    @JavascriptInterface
    fun onBlobCaptured(base64Data: String, filename: String, mimeType: String) {
        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val outputDir = File(context.cacheDir, "wasm_captured")
            if (!outputDir.exists()) outputDir.mkdirs()

            val sanitizedName = filename.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(outputDir, sanitizedName)

            FileOutputStream(targetFile).use { out ->
                out.write(bytes)
            }
            Log.i("WasmBlobBridge", "Successfully captured WASM blob: ${targetFile.absolutePath} (${bytes.size} bytes)")
            onCaptured(targetFile, sanitizedName, mimeType)
        } catch (e: Exception) {
            Log.e("WasmBlobBridge", "Failed to decode captured WASM blob", e)
        }
    }
}
