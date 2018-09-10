package com.ahleading.topaceforredditoffline.SavePostsService

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.SingletonServiceManager
import com.ahleading.topaceforredditoffline.R
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

class SavePostsOfflineService : Service() {
    lateinit var notificationManager: NotificationManager
    lateinit var postsController: PostsController

    override fun onCreate() {
        super.onCreate()
        postsController = PostsController(this)
        notificationManager = this.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val subreddit = intent.getStringExtra("subreddit")
        DownloadExtraStuffForPosts(subreddit, postsController, notificationManager, this)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        SingletonServiceManager.isMyServiceRunning = true
        SingletonServiceManager.currentSubredditDownloaded = subreddit
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    class DownloadExtraStuffForPosts(private val subreddit: String, private val postsController: PostsController,
                                     private val notificationManager: NotificationManager, service: Service?)
        : AsyncTask<Void?, Int, Void?>() {

        private var contextRef: WeakReference<Service>? = null
        lateinit var mBuilder: NotificationCompat.Builder
        private var NOTIFICATION_ID = "download_notification"
        private var NOTIFICATION_TAG = 414

        init {
            this.contextRef = WeakReference(service!!)
        }

        override fun onPreExecute() {
            //Setup For Notification
            val cntxt = contextRef?.get()!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mChannel = NotificationChannel(NOTIFICATION_ID,
                        "Downloading Assets Channel", NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(mChannel)
            }
            mBuilder = NotificationCompat.Builder(cntxt, NOTIFICATION_ID)
            mBuilder.setContentTitle("Saving posts from /r/$subreddit")
                    .setColor(ContextCompat.getColor(cntxt, R.color.colorPrimary))
                    .setContentText("Downloading Posts... 0%")
                    .setSmallIcon(R.drawable.ic_stat_title)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                mBuilder.priority = NotificationCompat.PRIORITY_HIGH
            }
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                val cntxt = contextRef?.get()!!
                cntxt.startForeground(NOTIFICATION_TAG, mBuilder.build())
                val posts = postsController.sqlHelper.getPostsSQL(subreddit)
                for ((index, post) in posts.withIndex()) {
                    Log.i("Downloading Assets", "Downloading $index ${post!!.mPermalink}")
                    if (post.mThumbnailLink != "" && post.mThumbnailLink.length > 7) {
                        downloadAndAttachThumb_Image(post.mThumbnailLink, post.mPermalink, 1)
                    }
                    if (post.image_src_url != null && post.image_src_url.trim() != "") {
                        downloadAndAttachThumb_Image(post.image_src_url, post.mPermalink, 2)
                    }
                    downloadAndAttachComments(post.mPermalink)
                    val progress = (index + 1) * 100 / posts.size
                    publishProgress(progress)
                }
            } catch (e: Exception) {
                Log.i("LoadExtraStuff", e.toString())
            }
            return null
        }

        override fun onProgressUpdate(vararg values: Int?) {
            val progress = values[0]!!
            super.onProgressUpdate(progress)
            mBuilder.setContentText("Downloading... $progress%")
                    .setProgress(100, progress, false)
            notificationManager.notify(NOTIFICATION_TAG, mBuilder.build())
        }

        override fun onPostExecute(result: Void?) {
            val cntxt = contextRef?.get()!!
            notificationManager.cancel(NOTIFICATION_TAG)
            cntxt.stopSelf()
        }

        fun downloadAndAttachThumb_Image(thumbnailLink: String, permalink: String, ThumbOrImage: Int) {
            val bitmap = downloadBitmap(thumbnailLink)
            if (bitmap != null) {
                val randomName = postsController.genRand(10)
                val completePath = postsController.saveToInternalStorage(bitmap, randomName, ".jpg")
                if (ThumbOrImage == 1) {
                    postsController.sqlHelper.attachThumbToPermaLink(completePath, permalink)
                } else {
                    postsController.sqlHelper.attachPostImageToPermaLink(completePath, permalink)
                }
            }
        }


        fun downloadAndAttachComments(permalink: String) {
            val postID = postsController.getIDFromPermalink(permalink)
            val commentsArray = postsController.getComments(subreddit, postID)
            postsController.sqlHelper.populateComments(commentsArray)
        }

        private fun downloadBitmap(url: String): Bitmap? {
            var urlConnection: HttpURLConnection? = null
            var bitmap: Bitmap? = null
            try {
                val uri = URL(url)
                urlConnection = uri.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 15 * 1000
                val inputStream = urlConnection.inputStream
                if (inputStream != null) {
                    bitmap = BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Log.d("URLCONNECTIONERROR", e.toString())
                // When an exception is thrown, a message to close reponse body appeaars:
                // apparently it is a bug: https://github.com/bumptech/glide/issues/2812
                Log.w("ImageDownloader", "Error downloading image from " + url)
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
            } finally {
                urlConnection?.disconnect()
            }
            return bitmap
        }
    }

    override fun onDestroy() {
        SingletonServiceManager.isMyServiceRunning = false
        super.onDestroy()
    }
}
