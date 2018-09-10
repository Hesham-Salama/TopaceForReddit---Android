package com.ahleading.topaceforredditoffline.Model

import com.google.android.gms.ads.AdRequest

class Constants {

    companion object {
        val firstPartRedditURL = "https://oauth.reddit.com/r/"
        val secondPartRedditURL_TOP = "/top/?sort=top&limit=25&t="
        val secondPartRedditURL_TOP_100 = "/top/?sort=top&limit=100&t="
        val secondPartRedditURL_NEW = "/new/"
        val secondPartRedditURL_COMMENT = "/comments/article?&showmore=true&article="
        val secondPartDay = "day"
        val secondPartWeek = "week"
        val secondPartYear = "year"
        val secondPartMonth = "month"
        val secondPartAll = "all"
        val aboutSecondPart = "about"
        val limit25 = "?limit=25"
        val limit10 = "?limit=10"
        val publisherIDAdMob = "pub-3374269774143426"
        val newestPostURL = "all/new/?limit=1"
        val VIEW_ADS = "view_ads"
        val AFTER_ID = "&after=t3_"
        val NO_ADS_PERIOD = "no_ads_period"
        val PREFS_BOOL_NAME = "MyPrefsFile"
        val CLICK_INVALID = "clickInvalid"
        val PERIODIC_FIRST_LAUNCH = "first_launch_periodic"
        val MIN_ODD_FOR_INTER_AD_POST_CONTENT = 1
        val MAX_ODD_FOR_INTER_AD_POST_CONTENT = 1003
        val MIN_ODD_FOR_INTER_AD_ADD_SUBREDDIT = 1
        val MAX_ODD_FOR_INTER_AD_ADD_SUBREDDIT = 1003
        var adRequest: AdRequest? = null
        var USER_IS_IN_EUROPE_OR_UNKNOWN = false
        val ADMOB_APP_ID = "ca-app-pub-3374269774143426~3098149286"
    }
}