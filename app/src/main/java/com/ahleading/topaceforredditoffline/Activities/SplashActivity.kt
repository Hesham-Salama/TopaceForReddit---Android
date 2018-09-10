package com.ahleading.topaceforredditoffline.Activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ahleading.topaceforredditoffline.Model.Constants
import com.ahleading.topaceforredditoffline.R
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_splash.*
import java.net.URL


class SplashActivity : AppCompatActivity() {

    private var form: ConsentForm? = null
    lateinit var consentInformation: ConsentInformation
    private val PERMISSION_REQUEST_CODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        consentInformation = ConsentInformation.getInstance(this)
        beginSplashWork()
    }

    private fun beginSplashWork() {
        val thread = object : Thread() {
            override fun run() {
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                runOnUiThread {
                    textView3.visibility = View.VISIBLE
                    if (!checkExternalStoragePermission())
                        requestExternalStoragePermission()
                    else getConsent()
                }
            }
        }
        thread.start()
    }

    fun getConsent() {
        /*
        Requesting Consent from European Users
        https://developers.google.com/admob/android/eu-consent
        IMPORTANT: YOU MUST SPECIFY YOUR PUBLISHER_IDS
         */
        val publisherIds = arrayOf(Constants.publisherIDAdMob)
        consentInformation.requestConsentInfoUpdate(publisherIds, object : ConsentInfoUpdateListener {
            override fun onConsentInfoUpdated(consentStatus: ConsentStatus) {
                when (consentStatus) {
                    ConsentStatus.PERSONALIZED -> {
                        showPersonalizedAds()
                        Constants.USER_IS_IN_EUROPE_OR_UNKNOWN = true
                        startNavActivity()
                    }
                    ConsentStatus.NON_PERSONALIZED -> {
                        showNonPersonalizedAds()
                        Constants.USER_IS_IN_EUROPE_OR_UNKNOWN = true
                        startNavActivity()
                    }
                    ConsentStatus.UNKNOWN -> {
                        if (consentInformation.isRequestLocationInEeaOrUnknown) {
                            showConsentDialog()
                            Constants.USER_IS_IN_EUROPE_OR_UNKNOWN = true
                        } else {
                            showPersonalizedAds()
                            Constants.USER_IS_IN_EUROPE_OR_UNKNOWN = false
                            startNavActivity()
                        }
                    }
                }
            }

            override fun onFailedToUpdateConsentInfo(errorDescription: String) {
                showNonPersonalizedAds()
                startNavActivity()
            }
        })
    }

    private fun showPersonalizedAds() {
        val adRequest = AdRequest.Builder()
                .build()
        Constants.adRequest = adRequest
    }

    private fun showNonPersonalizedAds() {
        val extras = Bundle()
        extras.putString("npa", "1")
        val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()
        Constants.adRequest = adRequest
    }

    private fun showConsentDialog() {
        form = ConsentForm.Builder(this, URL(getString(R.string.privacy_policy_html)))
                .withListener(object : ConsentFormListener() {
                    override fun onConsentFormLoaded() {
                        // Consent form loaded successfully.
                        showForm()
                    }

                    override fun onConsentFormClosed(consentStatus: ConsentStatus?, userPrefersAdFree: Boolean?) {
                        when (consentStatus) {
                            ConsentStatus.UNKNOWN, ConsentStatus.NON_PERSONALIZED -> {
                                showNonPersonalizedAds()
                                startNavActivity()
                            }
                            ConsentStatus.PERSONALIZED -> {
                                showPersonalizedAds()
                                startNavActivity()
                            }
                        }
                    }

                    override fun onConsentFormError(errorDescription: String?) {
                        // Consent form error.
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build()
        form?.load()
    }

    private fun showForm() {
        if (form != null) {
            form?.show()
        }
    }

    private fun startNavActivity() {
        val intent = Intent(this, NavActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkExternalStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return (result == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestExternalStoragePermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            getConsent()
        }
    }
}
