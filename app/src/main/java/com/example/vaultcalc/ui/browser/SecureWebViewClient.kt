package com.example.vaultcalc.ui.browser

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.vaultcalc.data.browser.AdBlockManager
import java.io.ByteArrayInputStream

class SecureWebViewClient(
    private val adBlockManager: AdBlockManager,
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String, String?) -> Unit,
    private val onError: (String) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return true

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false // Let WebView load it
        }

        // Block all other schemes (file, intent, tel, mailto, etc) for absolute security
        return true
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        val currentUrl = view?.url ?: ""

        if (adBlockManager.isAd(url, currentUrl)) {
            val emptyStream = ByteArrayInputStream(ByteArray(0))
            return WebResourceResponse("text/plain", "UTF-8", emptyStream)
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) {
            onPageStarted(url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            onPageFinished(url, view?.title)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val msg = error?.description?.toString() ?: "Unknown Error"
            Log.e("SecureWebViewClient", "Error: $msg")
            onError(msg)
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame == true) {
             val code = errorResponse?.statusCode ?: 0
             val reason = errorResponse?.reasonPhrase ?: "HTTP Error"
             Log.e("SecureWebViewClient", "HTTP Error $code: $reason")
             if (code >= 400) {
                 onError("HTTP Error $code")
             }
        }
    }
}
