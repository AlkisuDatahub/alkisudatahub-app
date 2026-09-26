package com.alkisudatahub.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    // Only allow these hosts to load inside the app; anything else opens in the
    // device browser (e.g. external payment gateway redirects, WhatsApp links).
    private val allowedHosts = listOf(
        "alkisudatahub.com.ng",
        "www.alkisudatahub.com.ng",
        "api.alkisudatahub.com.ng"
    )

    private val homeUrl = "https://alkisudatahub.com.ng"

    // --- File upload (BVN modification docs, NIN enrollment photos, etc.) ---
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val results: Array<Uri>? = when {
            result.resultCode != RESULT_OK || data == null -> null
            data.clipData != null -> {
                val count = data.clipData!!.itemCount
                Array(count) { i -> data.clipData!!.getItemAt(i).uri }
            }
            data.data != null -> arrayOf(data.data!!)
            else -> null
        }
        fileUploadCallback?.onReceiveValue(results)
        fileUploadCallback = null
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op: WebView will just fail gracefully if denied */ }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        requestNeededPermissions()
        setupWebView()

        swipeRefresh.setOnRefreshListener { webView.reload() }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(homeUrl)
        }
    }

    private fun requestNeededPermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (android.os.Build.VERSION.SDK_INT <= 32) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val notGranted = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setSupportZoom(false)
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true

        // Keep users logged in and let cookies persist across sessions
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                val host = url.host ?: ""
                val scheme = url.scheme ?: ""

                // Keep in-app for our own domain
                if (allowedHosts.any { host.equals(it, ignoreCase = true) }) {
                    return false
                }

                // tel:, mailto:, whatsapp:// etc. -> hand off to the system
                if (scheme != "http" && scheme != "https") {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                        true
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "No app found to handle this link", Toast.LENGTH_SHORT).show()
                        true
                    }
                }

                // Any other external http(s) domain (e.g. payment gateway) -> system browser
                startActivity(Intent(Intent.ACTION_VIEW, url))
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    Toast.makeText(
                        this@MainActivity,
                        "Connection problem. Pull down to retry.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            // Handles <input type="file"> for document/photo uploads
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = fileChooserParams.createIntent()
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                try {
                    fileChooserLauncher.launch(intent)
                } catch (e: Exception) {
                    fileUploadCallback = null
                    return false
                }
                return true
            }
        }

        // Proper download handling: sends PDFs/slips/receipts to the system
        // Download Manager (with notification) instead of failing silently,
        // which is the WebView PDF issue seen with the WebIntoApp build.
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                val cookie = CookieManager.getInstance().getCookie(url)
                request.addRequestHeader("cookie", cookie)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file...")
                val fileName = URLUtilGuessFileName(url, contentDisposition, mimeType)
                request.setTitle(fileName)
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "Downloading $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Download", "Failed to download", e)
                Toast.makeText(this, "Download failed. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun URLUtilGuessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }
}
