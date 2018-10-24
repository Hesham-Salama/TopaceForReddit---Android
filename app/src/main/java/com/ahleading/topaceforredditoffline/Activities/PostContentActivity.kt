package com.ahleading.topaceforredditoffline.Activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import com.ahleading.topaceforredditoffline.Adapters.CommentsRVAdapter
import com.ahleading.topaceforredditoffline.Ads.AdmobAds
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.CommentData
import com.ahleading.topaceforredditoffline.Model.Constants
import com.ahleading.topaceforredditoffline.Model.PostData
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
    private val IMAGE_SRC_STORAGE = "image_src_storage"
    private val THUMBNAIL_STORAGE = "Thumb_storage"
    private val THUMB_LINK = "thumb_link"
    private val TIME_OF_POST = "time_of_post"
    private val ALREADY_SAVED = "already_saved"
    private val NUMBER_OF_COMMENTS = "number_of_comments"
    private val URL_STR = "url"
    private lateinit var postsController: PostsController
    private var commentsRVAdapter: CommentsRVAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private val PERMISSION_REQUEST_CODE = 1
    private var menu: Menu? = null
    private lateinit var comments: ArrayList<CommentData>


    private var postsImageLoadingCompleted = false
    private var postsCommentLoadingCompleted = false


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.nav, menu)
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu
        setupMenuItem()
        return true
    }

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
        setSupportActionBar(toolbar_saved)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        postsController = PostsController(this)
        setClickableIfItIsLink()
        setPostContent()
    }

    private fun setupMenuItem() {
        val toBeSaved = intent.getBooleanExtra(ALREADY_SAVED, true)
        if (!toBeSaved) {
            disableAllItems()
        } else {
            enableSaveItem()
        }
    }

    private fun enableSaveItem() {
        menu?.findItem(R.id.sort_hot)?.isVisible = false
        menu?.findItem(R.id.sort_new)?.isVisible = false
        menu?.findItem(R.id.sort_top_all)?.isVisible = false
        menu?.findItem(R.id.sort_top_day)?.isVisible = false
        menu?.findItem(R.id.sort_top_month)?.isVisible = false
        menu?.findItem(R.id.sort_top_week)?.isVisible = false
        menu?.findItem(R.id.sort_top_year)?.isVisible = false
        menu?.findItem(R.id.delete_subreddit)?.isVisible = false
        menu?.findItem(R.id.save_post)?.isVisible = true
        menu?.findItem(R.id.delete_backup)?.isVisible = false
    }

    private fun disableAllItems() {
        menu?.findItem(R.id.sort_hot)?.isVisible = false
        menu?.findItem(R.id.sort_new)?.isVisible = false
        menu?.findItem(R.id.sort_top_all)?.isVisible = false
        menu?.findItem(R.id.sort_top_day)?.isVisible = false
        menu?.findItem(R.id.sort_top_month)?.isVisible = false
        menu?.findItem(R.id.sort_top_week)?.isVisible = false
        menu?.findItem(R.id.sort_top_year)?.isVisible = false
        menu?.findItem(R.id.delete_subreddit)?.isVisible = false
        menu?.findItem(R.id.save_post)?.isVisible = false
        menu?.findItem(R.id.delete_backup)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var thumbStorage = ""
        var imageStorage = ""
        when (item?.itemId) {
            R.id.save_post -> {
                // check if the post was already saved
                if (postsImageLoadingCompleted && postsCommentLoadingCompleted) {
                    val thumbLink = intent.getStringExtra(THUMB_LINK)
                    if (thumbLink.trim() != "" && thumbLink != "self" && thumbLink != "default") {
                        val bitmap: Bitmap? = (thumbnail_in_post_id.drawable as BitmapDrawable).bitmap
                        thumbStorage = saveImageToInternalStorage(bitmap)
                    }
                    if (post_image_id.drawable != null) {
                        val bitmap: Bitmap? = (post_image_id.drawable as BitmapDrawable).bitmap
                        imageStorage = saveImageToInternalStorage(bitmap)
                    }
                    if (!postsController.sqlHelper.checkIfPermalinkExistsInSavedPostsTable(intent.getStringExtra(PERMALINK))) {
                        postsController.sqlHelper.populateSavedPost(PostData(intent.getStringExtra(TITLE),
                                intent.getStringExtra(SUBREDDIT),
                                Integer.parseInt(intent.getStringExtra(SCORE)),
                                intent.getStringExtra(THUMB_LINK),
                                intent.getLongExtra(TIME_OF_POST, 0),
                                intent.getStringExtra(AUTHOR),
                                intent.getIntExtra(NUMBER_OF_COMMENTS, 0),
                                intent.getStringExtra(URL_STR),
                                intent.getStringExtra(PERMALINK),
                                intent.getStringExtra(SELF_TEXT_VAL),
                                thumbStorage,
                                intent.getStringExtra(IMAGE_LINK),
                                imageStorage,
                                intent.getStringExtra(DOMAIN)
                        ))
                        postsController.sqlHelper.populateComments(comments)
                        Toast.makeText(this, "Post saved successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: Post is already saved.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error: Post is loading...", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap?): String {
        if (bitmap != null) {
            val generatedString = postsController.genRand(10)
            val completePath = postsController.saveToInternalStorage(bitmap, generatedString, ".jpg")
            return completePath
        }
        return ""
    }

    private fun setClickableIfItIsLink() {
        val url = intent.getStringExtra(URL_STR)
        if (url.trim() != "" && !url.contains("www.reddit.com/r/")) {
            post_header_card_view.setOnClickListener {
                //                val i = Intent(Intent.ACTION_VIEW)
//                i.data = Uri.parse(url)
//                startActivity(i)
                val intent = Intent(this, CustomWebView::class.java)
                intent.putExtra("url", url)
                startActivity(intent)
            }
        }
    }

    fun setPostContent() {
        setOtherFields()
        setThumb()
        setPostText()
        Thread(Runnable {
            setCommentsInRV()
        }).start()
    }

    fun setCommentsInRV() {
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
            postsCommentLoadingCompleted = true
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

        if (intent.getStringExtra(THUMBNAIL_STORAGE).trim() != "") {
            val file = File(intent.getStringExtra(THUMBNAIL_STORAGE))
            if (file.exists()) {
                Picasso.get().load(file).into(thumbnail_in_post_id)
            }
        } else if (bitmap != null) {
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
            } else if (intent.getStringExtra(IMAGE_SRC_STORAGE).trim() != "") {
                val file = File(intent.getStringExtra(IMAGE_SRC_STORAGE))
                if (file.exists()) {
                    Picasso.get().load(file).transform(ImageTransformation.getTransformation(post_image_id))
                            .into(post_image_id)
                }
            } else {
                Picasso.get().load(imageSrc)
                        .transform(ImageTransformation.getTransformation(post_image_id))
                        .placeholder(R.drawable.progress_animation)
                        .into(post_image_id)
            }
            post_image_id.setOnClickListener(View.OnClickListener {
                if (isStoragePermissionGranted()) {
                    saveImage()
                }
            })
        }
        postsImageLoadingCompleted = true
    }


    private fun saveImage() {
        runOnUiThread {
            val message = "Do you want to save this picture?"
            alert(message) {
                title = "Saving Picture"
                yesButton {
                    try {
                        val root = Environment.getExternalStorageDirectory().toString()
                        val myDir = File(root + "/Topace/Pictures")
                        myDir.mkdirs()
                        val time = intent.getLongExtra(TIME_OF_POST, 1L).toString()
                        val subreddit = intent.getStringExtra(SUBREDDIT)
                        val imageName = subreddit + "_" + time + ".jpg"
                        val file = File(myDir, imageName)
                        if (file.exists()) file.delete()
                        val out = FileOutputStream(file)
                        val bitmap = ImageTransformation.getBitmap()
                        if (bitmap != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                            out.flush()
                            out.close()
                        } else throw Exception("No image found, couldn't save")
                        Toast.makeText(applicationContext, "Image ($imageName) is saved in Topace/Pictures/ directory",
                                Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.i("PostContentActivity:: ", e.message)
                        Toast.makeText(applicationContext, "Image saving failed :/", Toast.LENGTH_LONG).show()
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

    override fun onDestroy() {
        AdmobAds(applicationContext).showInterstitalAd(
                Constants.MIN_ODD_FOR_INTER_AD_POST_CONTENT, Constants.MAX_ODD_FOR_INTER_AD_POST_CONTENT)
        super.onDestroy()
    }
}