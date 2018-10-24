package com.ahleading.topaceforredditoffline.Model

import com.google.android.gms.ads.AdRequest

class Constants {

    companion object {
        val publisherIDAdMob = "pub-3374269774143426"
        val VIEW_ADS = "view_ads"
        val NO_ADS_PERIOD = "no_ads_period"
        val HAS_PURCHASED_ID1 = "eshtra"
        val PREFS_BOOL_NAME = "MyPrefsFile"
        val CLICK_INVALID = "clickInvalid"
        val PERIODIC_FIRST_LAUNCH = "first_launch_periodic"
        val MIN_ODD_FOR_INTER_AD_POST_CONTENT = 1
        val MAX_ODD_FOR_INTER_AD_POST_CONTENT = 20000000
        val MIN_ODD_FOR_INTER_AD_ADD_SUBREDDIT = 1
        val MAX_ODD_FOR_INTER_AD_ADD_SUBREDDIT = 2000000
        var adRequest: AdRequest? = null
        var USER_IS_IN_EUROPE_OR_UNKNOWN = false
        val ADMOB_APP_ID = "ca-app-pub-3374269774143426~3098149286"
        val licenseKeyGooglePlayPurchases = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQE" +
                "Ao+YkxP8lY3/PEgvnwgVdmhZtoj7f9rkrtLl9UaP5efA4tVVCBxngzuy2e/A5f4/KGsfgGZt2" +
                "y7HGke5AEwgiKbaBaxmKy2XKkNMnvdGrarRk2wGpkpCBLPtBow+RqKGcyczl0hIUMbUsJPXy" +
                "OXWpW+MR1ZgrOIZbSJVxyVqhIQDgTNxV+0c+wiaYE/dVuwMqguyPv7FNR+rTEUCjFzIGQ2Zgg" +
                "MHCLaPPdnYq0JtQNSDX+" +
                "kp3vL+muYrfqZm3nhpnODwWhuC7o/92N6AB0X+GHzm5PdQM24bmYwpjLrqQNfKYfxlYPO92qlg" +
                "D/5lAiw42lmECMdIbF1HJx/gDLsrhmQIDAQAB"
        //        val productID1 = "topace_coffee_1"
        val productID1 = "android.test.purchased"
    }
}