package com.ahleading.topaceforredditoffline.Model

import com.google.android.gms.ads.AdRequest

class Constants {

    companion object {
        val publisherIDAdMob = "pub-3374269774143426"
        val VIEW_ADS = "view_ads"
        val NO_ADS_PERIOD = "no_ads_period"
        val PREFS_BOOL_NAME = "MyPrefsFile"
        val CLICK_INVALID = "clickInvalid"
        val PERIODIC_FIRST_LAUNCH = "first_launch_periodic"
        val MIN_ODD_FOR_INTER_AD_POST_CONTENT = 1
        val MAX_ODD_FOR_INTER_AD_POST_CONTENT = 2
        val MIN_ODD_FOR_INTER_AD_ADD_SUBREDDIT = 1
        val MAX_ODD_FOR_INTER_AD_ADD_SUBREDDIT = 1
        var adRequest: AdRequest? = null
        var USER_IS_IN_EUROPE_OR_UNKNOWN = false
        val ADMOB_APP_ID = "ca-app-pub-3374269774143426~3098149286"
    }
}