package com.ahleading.topaceforredditoffline.PostNotificationSchedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.ahleading.topaceforredditoffline.Activities.SplashActivity
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.Constants
import com.ahleading.topaceforredditoffline.Model.PostData
import com.ahleading.topaceforredditoffline.R
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

class PostNotificationJobService : JobService() {
    private var isWorking = false
    private var jobCancelled = false
    private val SOON_VALUE = "soon_value"

    // Called by the Android system when it's time to run the job
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        Log.d("JobSchedule", "Job started!")
        isWorking = true
        startWorkOnNewThread(jobParameters)
        return isWorking
    }

    // Called if the job was cancelled before being finished
    override fun onStopJob(jobParameters: JobParameters): Boolean {
        Log.d("JobSchedule", "Job cancelled before being completed.")
        jobCancelled = true
        val needsReschedule = isWorking
        jobFinished(jobParameters, needsReschedule)
        return needsReschedule
    }

    private fun startWorkOnNewThread(jobParameters: JobParameters) {
        var flag = true
        if (!checkIfItIsSuitableTime()) {
            Log.d("JobSchedule", "Job's time is not suitable.")
            flag = false
        } else if (!checkIfFirstPeriodicIsBypassed()) {
            Log.d("JobSchedule", "First periodic problem...")
            flag = false
        } else if (itIsTooSoon()) {
            Log.d("JobSchedule", "This job is too soon.")
            flag = false
        }
        if (!flag) {
            isWorking = false
            val needsReschedule = false
            jobFinished(jobParameters, needsReschedule)
        } else {
            Thread(Runnable { pickPost(jobParameters) }).start()
        }
    }

    private fun itIsTooSoon(): Boolean {
        val prefs = getSharedPreferences(Constants.PREFS_BOOL_NAME, Context.MODE_PRIVATE)
        val savedPrefs = prefs.getLong(SOON_VALUE, 0L)
        if (savedPrefs != 0L) {
            val timeNowInMillis = Calendar.getInstance().timeInMillis
            return timeNowInMillis - savedPrefs < TimeUnit.HOURS.toMillis(1)
        }
        return false
    }

    private fun checkIfFirstPeriodicIsBypassed(): Boolean {
        val prefs = getSharedPreferences(Constants.PREFS_BOOL_NAME, Context.MODE_PRIVATE)
        val savedPrefs = prefs.getBoolean(Constants.PERIODIC_FIRST_LAUNCH, false)
        if (!savedPrefs) {
            val editor = prefs.edit()
            editor.putBoolean(Constants.PERIODIC_FIRST_LAUNCH, true)
            editor.apply()
        }
        return savedPrefs
    }

    private fun checkIfItIsSuitableTime(): Boolean {
        val cal = Calendar.getInstance()
        return !(cal.get(Calendar.HOUR_OF_DAY) >= 22 || cal.get(Calendar.HOUR_OF_DAY) in 0..10)
    }


    private fun setWorkTime() {
        val editor = getSharedPreferences(Constants.PREFS_BOOL_NAME, Context.MODE_PRIVATE).edit()
        editor.putLong(SOON_VALUE, Calendar.getInstance().timeInMillis)
        editor.apply()
    }

    private fun pickPost(jobParameters: JobParameters) {
        try {
            val postsController = PostsController(applicationContext)
            val arrPosts = postsController.getPosts(postsController.getActiveSubs()
                    + "/" + Constants.limit10)
            if (arrPosts.size > 0) {
                var count = 0
                var postdata: PostData
                do {
                    val rnd = Random()
                    val randomIndex = rnd.nextInt(9 - 0 + 1) + 0
                    postdata = arrPosts[randomIndex]!!
                    if (!postsController.sqlHelper.checkIfItWasAsANotification(postdata.mPermalink)) {
                        break
                    }
                    count++
                } while (count != 100)
                if (count != 100) {
                    showNotification("/r/${postdata.mSubreddit}", postdata.mTitle
                            , downloadBitmap(postdata.mThumbnailLink))
                }
                postsController.sqlHelper.addToNotificationPostsTable(postdata.mPermalink)
            }
            setWorkTime()
            Log.d("JobSchedule", "Job finished!")
            isWorking = false
            val needsReschedule = false
            jobFinished(jobParameters, needsReschedule)
        } catch (e: Exception) {
            Log.i("JobSchedule", "JobSchedule needs rescheduling, Exception" + e.toString())
            val needsReschedule = true
            jobFinished(jobParameters, needsReschedule)
        }
    }

    fun showNotification(title: String, body: String, bitmap: Bitmap?) {
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = 1
        val channelId = "post_notification"
        val channelName = "Showing trending posts"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(mChannel)
        }

        val mBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_title)
                .setContentTitle(title)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(body))
                .setContentText(body)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setAutoCancel(true)

        if (bitmap != null) {
            mBuilder.setLargeIcon(bitmap)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mBuilder.priority = NotificationCompat.PRIORITY_HIGH
        }

        val contentIntent = PendingIntent.getActivity(this, 0,
                Intent(this, SplashActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(contentIntent)

        notificationManager.notify(notificationId, mBuilder.build())
    }

    private fun downloadBitmap(url: String): Bitmap? {
        var urlConnection: HttpURLConnection? = null
        var bitmap: Bitmap? = null
        try {
            val uri = URL(url)
            urlConnection = uri.openConnection() as HttpURLConnection
            val inputStream = urlConnection.inputStream
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.d("URLCONNECTIONERROR", e.toString())
            Log.w("ImageDownloader", "Error downloading image from " + url)
            if (urlConnection != null) {
                urlConnection.disconnect()
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect()
            }
        }
        return bitmap
    }
}
