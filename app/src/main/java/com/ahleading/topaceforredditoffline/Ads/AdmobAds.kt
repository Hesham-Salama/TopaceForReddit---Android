package com.ahleading.topaceforredditoffline.Ads

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.Constants
import com.ahleading.topaceforredditoffline.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.InterstitialAd
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.ArrayList


class AdmobAds(private val context: Context) {
    private var mInterstitialAd: InterstitialAd? = null
    private var mInterstitialAdLoaded = false
    private val CLICKED_AD_LIMIT = 4
    private val CLICKED_AD_LIMIT_PERIOD: Long = 3 * 60 * 60 * 1000
    private val NO_ADS_DELAY: Long = 8 * 60 * 60 * 1000
    private val postController = PostsController(context)

    fun loadInterstitialAdWithOdds(minOdd: Int, maxOdd: Int) {
        val rand = Random().nextInt(maxOdd - minOdd + 1) + minOdd
        val myOdd: Int = (minOdd + maxOdd / 2)
        Log.i("Odd", rand.toString() + " " + myOdd.toString())
        if (rand == myOdd) {
            mInterstitialAd = InterstitialAd(context)
            mInterstitialAd?.adUnitId = context.getString(R.string.test_interstitial_admob_id)
            mInterstitialAd?.loadAd(Constants.adRequest)
            mInterstitialAd?.adListener = object : AdListener() {

                override fun onAdLoaded() {
                    mInterstitialAdLoaded = true
                }

                override fun onAdLeftApplication() {
                    super.onAdLeftApplication()
//                    Log.i("Ad clicked", "Ad clicked")
                    Thread(Runnable {
                        checkFraudlentActivity()
                    }).start()
                }
            }
        }
    }

    private fun isInterstitalAdLoaded(): Boolean {
        return this.mInterstitialAdLoaded
    }

    fun showInterstitalAd(minOdd: Int = 0, maxOdd: Int = 0) {
        if (isInterstitalAdLoaded()) {
            mInterstitialAd?.show()
            mInterstitialAdLoaded = false
            mInterstitialAd = null
        }
        if (mInterstitialAd == null && minOdd != 0
                && maxOdd != 0 && ManageAds(context).getAdStatus()) {
            loadInterstitialAdWithOdds(minOdd, maxOdd)
        }
        Thread(Runnable {
            checkIfNoAdsAndBan()
        }).start()
    }

    fun checkIfNoAdsAndBan() {
        if (!ManageAds(context).getAdStatus()) {
            val timeBanFinishes = ManageAds(context).getNoAdsPeriod()
            val post = postController.getPosts(Constants.newestPostURL)
            if (post.size == 1) {
                val pst = post[0]
                val timeNow = pst!!.mCreatedUTC * 1000
                if (timeBanFinishes < timeNow) {
                    ManageAds(context).setAdStatus(true)
                    ManageAds(context).setNoAdsPeriod(0L)
                }
            }
        }
    }

    fun checkFraudlentActivity() {
        val post = postController.getPosts(Constants.newestPostURL)
        if (post.size == 1) {
            val pst = post[0]
            val timeNow = pst!!.mCreatedUTC * 1000
            var arr = getArrayList(Constants.CLICK_INVALID)
            if (arr == null) {
                arr = ArrayList()
            }
            arr.add(timeNow.toString())
            if (arr.size == CLICKED_AD_LIMIT) {
                val differenceBetween1stAndLastClick =
                        arr[CLICKED_AD_LIMIT - 1].toLong() - arr[0].toLong()
                if (differenceBetween1stAndLastClick <= CLICKED_AD_LIMIT_PERIOD) {
                    ManageAds(context).setAdStatus(false)
                    ManageAds(context).setNoAdsPeriod(timeNow + NO_ADS_DELAY)
                }
                arr.removeAt(0)
            }
            saveArrayList(arr, Constants.CLICK_INVALID)
        }
    }

    fun saveArrayList(list: ArrayList<String>, key: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(list)
        editor.putString(key, json)
        editor.apply()
    }

    private fun getArrayList(key: String): ArrayList<String>? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val gson = Gson()
        val json = prefs.getString(key, null)
        val type = object : TypeToken<ArrayList<String>>() {
        }.type
        return gson.fromJson(json, type)
    }
}