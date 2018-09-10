package com.ahleading.topaceforredditoffline.Ads

import android.content.Context
import com.ahleading.topaceforredditoffline.Model.Constants


class ManageAds(private val context: Context) {

    fun setAdStatus(flag: Boolean) {
        val prefs = this.context.getSharedPreferences(Constants.PREFS_BOOL_NAME, Context.MODE_PRIVATE).edit()
        prefs.putBoolean(Constants.VIEW_ADS, flag)
        prefs.apply()
    }

    fun getAdStatus(): Boolean {
        val prefs = this.context.getSharedPreferences(Constants.PREFS_BOOL_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.VIEW_ADS, true)
    }

    fun setNoAdsPeriod(period: Long) {
        val prefs = this.context.getSharedPreferences(Constants.PREFS_BOOL_NAME, Context.MODE_PRIVATE).edit()
        prefs.putLong(Constants.NO_ADS_PERIOD, period)
        prefs.apply()
    }

    fun getNoAdsPeriod(): Long {
        val prefs = this.context.getSharedPreferences(Constants.PREFS_BOOL_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(Constants.NO_ADS_PERIOD, 0L)
    }
}