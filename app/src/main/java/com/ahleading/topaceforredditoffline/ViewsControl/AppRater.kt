package com.ahleading.topaceforredditoffline.ViewsControl

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// https://stackoverflow.com/questions/47617259/when-to-show-rate-my-app-dialog
object AppRater {
    private val APP_TITLE = "Topace"
    private val APP_PNAME = "com.ahleading.topaceforredditoffline"
    private val LAUNCH_COUNT_STR = "launch_count"
    private val PERIODIC_TIMES_TO_SHOW_DIALOG = 4


    fun app_launched(mContext: Context) {
        val prefs = mContext.getSharedPreferences("apprater", 0)
        val editor = prefs.edit()
        val launchCount = prefs.getInt(LAUNCH_COUNT_STR, 0) + 1
        editor.putInt(LAUNCH_COUNT_STR, launchCount)
        editor.apply()

        if (prefs.getInt(LAUNCH_COUNT_STR, 0) % PERIODIC_TIMES_TO_SHOW_DIALOG == 0 &&
                !prefs.getBoolean("AlreadyRated", false)) {
            showRateDialog(mContext)
        }
    }

    fun showRateDialog(mContext: Context) {
        val dialog = Dialog(mContext)
        dialog.setTitle("Rate $APP_TITLE")
        val ll = LinearLayout(mContext)
        ll.orientation = LinearLayout.VERTICAL
        val tv = TextView(mContext)
        tv.text = "If you enjoy using $APP_TITLE, please take a moment to rate it. Thanks for your support!"
        tv.width = 750
        tv.textSize = 18F
        tv.setPadding(24, 24, 24, 48)
        ll.addView(tv)
        val b1 = Button(mContext)
        b1.text = "Rate now"
        b1.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val prefs = mContext.getSharedPreferences("apprater", 0)
                val editor = prefs.edit()
                editor.putBoolean("AlreadyRated", true)
                editor.apply()
                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$APP_PNAME")))
                dialog.dismiss()
            }
        })
        ll.addView(b1)
        val b2 = Button(mContext)
        b2.text = "Remind me later"
        b2.setOnClickListener { dialog.dismiss() }
        ll.addView(b2)
//        val b3 = Button(mContext)
//        b3.text = "No, thanks"
//        b3.setOnClickListener {
//            if (editor != null) {
//                editor.putBoolean("dontshowagain", true)
//                editor.commit()
//            }
//            dialog.dismiss()
//        }
//        ll.addView(b3)
        dialog.setContentView(ll)
        dialog.show()
    }
}