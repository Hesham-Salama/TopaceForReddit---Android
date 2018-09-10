package com.ahleading.topaceforredditoffline.Activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import com.ahleading.topaceforredditoffline.Adapters.CommentsRVAdapter
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.CommentData
import com.ahleading.topaceforredditoffline.R
import com.ahleading.topaceforredditoffline.ViewsControl.ImageTransformation
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_post_content.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import org.sufficientlysecure.htmltextview.ClickableTableSpan
import org.sufficientlysecure.htmltextview.DrawTableLinkSpan
import org.sufficientlysecure.htmltextview.HtmlResImageGetter
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class PostContentActivity : AppCompatActivity() {

    private val SELF_TEXT_VAL = "selftext_html"
    private val PERMALINK = "permalink"
    private val TITLE = "title"
    private val AUTHOR = "author"
    private val SCORE = "score"
    private val DOMAIN = "domain"
    private val THUMB = "thumb"
    private val IMAGE_LINK = "image_link"
    private val SUBREDDIT = "subreddit"
    private val THUMB_LINK = "thumb_link"
    private val TIME_OF_POST = "time_of_post"
    private val URL_STR = "url"
    private lateinit var postsController: PostsController
    private var commentsRVAdapter: CommentsRVAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private val PERMISSION_REQUEST_CODE = 1


    internal inner class ClickableTableSpanImpl : ClickableTableSpan() {
        override fun newInstance(): ClickableTableSpan {
            return ClickableTableSpanImpl()
        }

        override fun onClick(widget: View) {
            val alert = AlertDialog.Builder(this@PostContentActivity)
            val wv = WebView(this@PostContentActivity)
            wv.loadData(getTableHtml(), "text/html", "UTF-8")
            alert.setView(wv)
            alert.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_content)
        postsController = PostsController(this)
        setClickableIfItIsLink()
        setPostContent()
    }

    fun setClickableIfItIsLink() {
        val url = intent.getStringExtra(URL_STR)
        if (url.trim() != "" && !url.contains("www.reddit.com/r/")) {
            post_header_card_view.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        }
    }

    fun setPostContent() {
        setOtherFields()
        setThumb()
        setPostText()
//        setPostImage()
        Thread(Runnable {
            setCommentsInRV()
        }).start()
    }

    fun setCommentsInRV() {
        var comments: ArrayList<CommentData>
        val permalink = intent.getStringExtra(PERMALINK)
        runOnUiThread {
            progress_bar_comment_id.visibility = View.VISIBLE
        }
        comments = postsController.sqlHelper.getComments(permalink)
        try {
            if (comments.size == 0) {
                val subreddit = intent.getStringExtra(SUBREDDIT)
                val postID = postsController.getIDFromPermalink(permalink)
                comments = postsController.getComments(subreddit, postID)
            }
        } catch (e: Exception) {
            if (comments.size == 0) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.error_fetching_comments), Toast.LENGTH_SHORT).show()
                }
            }
            Log.i("SetCommentsInRV", e.toString())
        }
        runOnUiThread {
            val commentsSize = getString(R.string.number_of_root_comments) + " " + comments.size.toString()
            root_comments_number_id.text = commentsSize
            commentsRVAdapter = CommentsRVAdapter(comments, this, R.layout.comment_item)
            layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
            comments_recycler_view?.layoutManager = layoutManager
            comments_recycler_view?.adapter = commentsRVAdapter
            comments_recycler_view.isNestedScrollingEnabled = false
            progress_bar_comment_id.visibility = View.GONE
        }
    }

    fun setOtherFields() {
        score_post_content_id.text = intent.getStringExtra(SCORE)
        val subreddit = "/r/" + intent.getStringExtra(SUBREDDIT)
        subreddit_content_id.text = subreddit
        post_title_content_id.text = intent.getStringExtra(TITLE)
        val domain = "(" + intent.getStringExtra(DOMAIN) + ")"
        domain_time_id.text = domain

        val username = "by /u/" + intent.getStringExtra(AUTHOR)
        by_username_id.text = username

        val time: Long = intent.getLongExtra(TIME_OF_POST, 1L)
        val submittedAt = "Submitted " + PostsController.extractTimeOfNow(time)
        submitted_at_in_post_id.text = submittedAt
    }

    fun setThumb() {
        val extras = intent.extras
        val bitmap: Bitmap? = extras.getParcelable(THUMB)

        if (bitmap != null) {
            thumbnail_in_post_id.setImageBitmap(bitmap)
        } else {
            Picasso.get().load(intent.getStringExtra(THUMB_LINK))
                    .into(thumbnail_in_post_id)
        }
    }

    fun setPostText() {
        //1- set post content if any.
        var html = intent.getStringExtra(SELF_TEXT_VAL)
        if (html == null || html == "null") {
            html = ""
        }
        if (html.trim() != "") {
            post_content.setClickableTableSpan(ClickableTableSpanImpl())
            val drawTableLinkSpan = DrawTableLinkSpan()
            drawTableLinkSpan.tableLinkText = getString(R.string.table_exists)
            post_content.setDrawTableLinkSpan(drawTableLinkSpan)
            post_content.setHtml(html, HtmlResImageGetter(post_content))
        } else {
            post_content.visibility = View.GONE
        }
    }

    fun setPostImage() {
        val imageSrc: String? = intent.getStringExtra(IMAGE_LINK)
        val permalink = intent.getStringExtra(PERMALINK)
        if (imageSrc == null || imageSrc.trim() == "") {
            post_image_id.visibility = View.GONE
        } else {
            if (postsController.sqlHelper.checkIfPermalinkExistsInArchTable(permalink)) {
                val imageStorage = postsController.sqlHelper.getImagePostPathFromPermaLink(permalink)
                if (imageStorage.trim() != "") {
                    val file = File(imageStorage)
                    if (file.exists()) {
                        Picasso.get().load(file).transform(ImageTransformation.getTransformation(post_image_id))
                                .into(post_image_id)
                    }
                } else {
                    post_image_id.visibility = View.GONE
                }
            } else {

                Picasso.get().load(imageSrc)
                        .transform(ImageTransformation.getTransformation(post_image_id))
                        .into(post_image_id)
            }
            post_image_id.setOnClickListener(View.OnClickListener {
                if (isStoragePermissionGranted()) {
                    saveImage()
                }
            })
        }
    }

    private fun saveImage() {
        runOnUiThread {
            val message = "Do you want to save this picture?"
            alert(message) {
                title = "Saving Picture"
                yesButton {
                    post_image_id.isDrawingCacheEnabled = true
                    post_image_id.buildDrawingCache()
                    val bitmap = Bitmap.createBitmap(post_image_id.drawingCache)

                    val root = Environment.getExternalStorageDirectory().toString()
                    val myDir = File(root + "/Topace/Pictures")
                    myDir.mkdirs()
                    val time = intent.getLongExtra(TIME_OF_POST, 1L).toString()
                    val subreddit = intent.getStringExtra(SUBREDDIT)
                    val imageName = subreddit + "_" + time + ".jpg"
                    val file = File(myDir, imageName)
                    if (file.exists()) file.delete()
                    try {
                        val out = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        out.flush()
                        out.close()
//                        val pathURI = file.toURI().toString()
                        Toast.makeText(applicationContext, "Image ($imageName) is saved in Topace/Pictures/ directory",
                                Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                noButton { }
            }.show()
        }
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (checkExternalStoragePermission()) {
            true
        } else {
            requestExternalStoragePermission()
            false
        }
    }

    private fun checkExternalStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return (result == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestExternalStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        } else {
            Toast.makeText(this, getString(R.string.write_external_error_str), Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission is now granted.
                saveImage()
            } else {
                Toast.makeText(this, getString(R.string.write_external_error_str), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setPostImage()
        }
    }
}