package com.ahleading.topaceforredditoffline.Activities

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.ahleading.topaceforredditoffline.R
import kotlinx.android.synthetic.main.activity_custom_web_view.*


class CustomWebView : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_web_view)
        val url = intent.getStringExtra("url")
        webView_id.settings.javaScriptEnabled = true
        webView_id.loadUrl(url)

        setSupportActionBar(toolbar_web_view)

        toolbar_web_view.setOnLongClickListener {
            val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            var selectedURL =
                    if (toolbar_web_view.title != null && URLUtil.isValidUrl(toolbar_web_view.title.toString())) {
                        toolbar_web_view.title
                    } else {
                        toolbar_web_view.subtitle
                    }
            val clip = android.content.ClipData.newPlainText("Copied Text", selectedURL)
            clipboard.primaryClip = clip
            Toast.makeText(this, "URL is copied to clipboard", Toast.LENGTH_SHORT).show()
            false
        }

        webView_id.webViewClient = object : WebViewClient() {
//            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
//                view?.loadUrl(url)
//                return true
//            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                supportActionBar?.title = view?.url
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view?.url != view?.title && view?.title?.trim() != "") {
                    supportActionBar?.subtitle = view?.url
                    supportActionBar?.title = view?.title
                }
            }
        }
    }
}
