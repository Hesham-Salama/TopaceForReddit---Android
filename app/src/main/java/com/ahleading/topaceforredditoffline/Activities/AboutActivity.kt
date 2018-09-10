package com.ahleading.topaceforredditoffline.Activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.view.View
import com.ahleading.topaceforredditoffline.Model.Constants
import com.ahleading.topaceforredditoffline.R
import com.google.ads.consent.ConsentInformation
import com.marcoscg.licenser.Library
import com.marcoscg.licenser.License
import com.marcoscg.licenser.LicenserDialog
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import java.util.*


class AboutActivity : AppCompatActivity() {

    private val RESULT_CONSENT_REVOKE = 12
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        simulateDayNight(/* DAY */0)

        val aboutPage: AboutPage = AboutPage(this)
                .isRTL(false)
                .setDescription(getString(R.string.app_name))
                .setImage(R.drawable.final_topace_logo)
                .addItem(Element().setTitle(getString(R.string.version)))
                .addGroup("Connect with us")
                .addEmail(getString(R.string.contact_email))
                .addPlayStore(getString(R.string.package_store))
        if (Constants.USER_IS_IN_EUROPE_OR_UNKNOWN) {
            aboutPage.addItem(resetConsent())
        }
        aboutPage.addItem(getCopyRightsElement())
        setContentView(aboutPage.create())
    }

    fun resetConsent(): Element {
        val resetElement = Element()
        resetElement.title = "Revoke Consent (for EU citizens)"
        resetElement.onClickListener = View.OnClickListener {
            val msg = "Are you sure that you want to revoke the consent?"
            alert(msg) {
                title = "Revoking GDPR consent"
                yesButton {
                    val consentInformation = ConsentInformation.getInstance(this@AboutActivity)
                    consentInformation.reset()
                    val data = Intent()
                    setResult(RESULT_CONSENT_REVOKE, data)
                    finish()
                }
                noButton { }
            }.show()
        }
        return resetElement
    }

    fun getCopyRightsElement(): Element {
        val copyRightsElement = Element()
        val copyrights = String.format("Licenses", Calendar.getInstance().get(Calendar.YEAR))
        copyRightsElement.setTitle(copyrights)
        copyRightsElement.onClickListener = View.OnClickListener {
            LicenserDialog(this)
                    .setTitle("Licenses")
                    .setCustomNoticeTitle("Notices for files:")
                    .setLibrary(Library("jsoup",
                            "https://github.com/jhy/jsoup",
                            License.MIT))
                    .setLibrary(Library("Picasso",
                            "https://github.com/square/picasso",
                            License.APACHE))
                    .setLibrary(Library("Anko",
                            "https://github.com/Kotlin/anko",
                            License.APACHE))
                    .setLibrary(Library("HtmlTextView",
                            "https://github.com/PrivacyApps/html-textview",
                            License.APACHE))
                    .setLibrary(Library("Android About Page",
                            "https://github.com/medyo/android-about-page",
                            License.MIT))
                    .setLibrary(Library("Licenser",
                            "https://github.com/marcoscgdev/Licenser",
                            License.MIT))
                    .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    }
                    .show()
        }
        return copyRightsElement
    }

    fun simulateDayNight(currentSetting: Int) {
        val DAY = 0
        val NIGHT = 1
        val FOLLOW_SYSTEM = 3

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentSetting == DAY && currentNightMode != Configuration.UI_MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO)
        } else if (currentSetting == NIGHT && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES)
        } else if (currentSetting == FOLLOW_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
