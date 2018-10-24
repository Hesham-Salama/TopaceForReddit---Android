package com.ahleading.topaceforredditoffline.Activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.Toast
import com.ahleading.topaceforredditoffline.Ads.AdmobAds
import com.ahleading.topaceforredditoffline.Ads.ManageAds
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.Constants
import com.ahleading.topaceforredditoffline.Model.ConstructRedditURL
import com.ahleading.topaceforredditoffline.Model.SingletonServiceManager
import com.ahleading.topaceforredditoffline.R
import com.ahleading.topaceforredditoffline.ViewsControl.WindowControl
import com.anjlab.android.iab.v3.BillingProcessor
import kotlinx.android.synthetic.main.activity_add_subreddit.*
import org.jetbrains.anko.alert
import org.json.JSONObject


class AddSubredditActivity : AppCompatActivity() {

    private var postController: PostsController? = null
    private val DATA_STR = "data"
    private val OVER_18 = "over18"
    private var admobInstance: AdmobAds? = null
    private lateinit var relativeLayout: RelativeLayout

    companion object {
        lateinit var billingProcessor: BillingProcessor
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_subreddit)
        setSupportActionBar(toolbar_add_subreddit)
        postController = PostsController(this)
        admobInstance = AdmobAds(this)
        admobInstance!!.showInterstitalAd(Constants.MIN_ODD_FOR_INTER_AD_ADD_SUBREDDIT,
                Constants.MAX_ODD_FOR_INTER_AD_ADD_SUBREDDIT)

        relativeLayout = findViewById(R.id.add_subreddit_relative_layout)

        var selectedCheckBox: String? = getString(R.string.don_t_save_posts)
        RGroup.check(R.id.dont_save_posts)

        var selectedCheckBox2: String? = getString(R.string._100_posts)
        RGroup2.check(R.id.save_100_posts_rb)
        disableAllRGroup2Buttons()

        RGroup.setOnCheckedChangeListener { _, checkedId ->
            val rb: RadioButton = findViewById(checkedId)
            selectedCheckBox = rb.text.toString()
            if (selectedCheckBox != getString(R.string.don_t_save_posts)) {
                enableAllRGroup2Buttons()
            } else {
                disableAllRGroup2Buttons()
            }
        }

        RGroup2.setOnCheckedChangeListener { _, checkedId ->
            val rb: RadioButton = findViewById(checkedId)
            selectedCheckBox2 = rb.text.toString()
        }

        val saveSubredditButton: Button = findViewById(R.id.add_subreddit_button)
        saveSubredditButton.setOnClickListener { it ->
            WindowControl.disableAndDimWindow(this@AddSubredditActivity)
            progress_layout_add_subreddit.visibility = View.VISIBLE
            var subreddit: String = editText.text.toString()
            Thread(Runnable {
                val check = checksRequiredToAddSubreddit(subreddit, selectedCheckBox)
                runOnUiThread {
                    if (check != "OK") {
                        Toast.makeText(this, check, Toast.LENGTH_LONG).show()
                    } else {
                        if (!ManageAds.hasPurchased(applicationContext) && selectedCheckBox != getString(R.string.don_t_save_posts)
                                && selectedCheckBox2 != getString(R.string._100_posts)) {
                            val msg = "Unlock saving up to 500 posts instead of 100 only!\n" +
                                    "Please consider donating, supporting us to provide more updates and making new useful apps.\n\n" +
                                    "1- Remove all ads from the app (100% no ads)." +
                                    "\n2- Remove all limits on saved posts."
                            this@AddSubredditActivity.alert(msg) {
                                title = "Save up to 500 posts!"
                                positiveButton("Ok!") {
                                    val isOneTimePurchaseSupported = billingProcessor.isOneTimePurchaseSupported
                                    if (isOneTimePurchaseSupported) {
                                        // launch payment flow
                                        billingProcessor.purchase(this@AddSubredditActivity, Constants.productID1)
                                    } else {
                                        Toast.makeText(applicationContext,
                                                "In-App purchases are not available at the moment", Toast.LENGTH_LONG).show()
                                    }
                                }
                                negativeButton("No, thanks.") {
                                    progress_layout_add_subreddit.visibility = View.GONE
                                    WindowControl.enableAndBrightWindow(this@AddSubredditActivity)
                                }
                            }.show()
                        } else {
                            subreddit = subreddit.substring(0, 1).toUpperCase() + subreddit.substring(1).toLowerCase()
                            val data = Intent()
                            data.putExtra("subreddit", subreddit)
                            data.putExtra("checkboxString", selectedCheckBox)
                            data.putExtra("checkboxString2", selectedCheckBox2)
                            setResult(RESULT_OK, data)
                            finish()
                        }
                    }
                    progress_layout_add_subreddit.visibility = View.GONE
                    WindowControl.enableAndBrightWindow(this@AddSubredditActivity)
                }
            }).start()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun disableAllRGroup2Buttons() {
        for (i in 0 until RGroup2.childCount) {
            (RGroup2.getChildAt(i) as RadioButton).isEnabled = false
        }
    }

    fun enableAllRGroup2Buttons() {
        for (i in 0 until RGroup2.childCount) {
            (RGroup2.getChildAt(i) as RadioButton).isEnabled = true
        }
    }

    fun checksRequiredToAddSubreddit(subreddit: String, selectedCheckBox: String?): String {
        //check if there is internet connection
        //check if subreddit exists
        //check if it is NSFW
        //check if it is duplicate either in active or archived
        //check if it is downloading
        //check blank spaces
        //Check: banned
        //Check: want save subreddit but it's empty
        //check if it is alphanumeric
        val urlSubreddit = ConstructRedditURL.constructURL(subreddit)
        val urlAbout = ConstructRedditURL.addAbout(urlSubreddit)
        try {
            if (SingletonServiceManager.isMyServiceRunning && !selectedCheckBox?.contains("Don't save")!!) {
                return getString(R.string.wait_error_string)
            }
            if (subreddit.trim() == "" || subreddit.contains(" ") || subreddit.contains("\n") || subreddit.contains("\t")) {
                return getString(R.string.white_spaces_error)
            }
            if (!subreddit.matches("[A-Za-z0-9]+".toRegex())) {
                return getString(R.string.should_consist_alphanumeric_string)
            }
            if (selectedCheckBox?.contains("Don't save")!!) {
                if (postController?.sqlHelper?.checkIfAsubredditExistsInActiveTable(subreddit)!!) {
                    return getString(R.string.already_added_sub_string)
                } else if (subreddit.toLowerCase() == "popular" || subreddit.toLowerCase() == "all") {
                    return getString(R.string.popular_is_already_added_error)
                }
            } else {
                if (postController?.sqlHelper?.checkIfAsubredditExistsInArchivedTable(subreddit)!!) {
                    return getString(R.string.already_added_sub_arch_error_string)
                }
            }
            val jsonStr = postController?.downloadController?.getJSONReddit(urlAbout)
            if (jsonStr?.contains("ERROR_404")!!) {
                //Subreddit is banned
                return getString(R.string.subreddit_banned_nonexistent_error_string)
            }
            val aboutJson = JSONObject(jsonStr)
            val data = aboutJson.getJSONObject(DATA_STR)
            if (!data.has(OVER_18)) {
                return "Error: This subreddit \"$subreddit\" doesn't exist"
            }
            if (data.getBoolean(OVER_18)) {
                return getString(R.string.nsfw_subreddit_error_string)
            }
            return "OK"
        } catch (e: Exception) {
            return getString(R.string.no_internet_conn_error_string)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        admobInstance!!.showInterstitalAd(Constants.MIN_ODD_FOR_INTER_AD_ADD_SUBREDDIT,
                Constants.MAX_ODD_FOR_INTER_AD_ADD_SUBREDDIT)
        super.onDestroy()
    }
}


