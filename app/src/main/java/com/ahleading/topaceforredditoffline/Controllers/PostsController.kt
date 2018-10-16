package com.ahleading.topaceforredditoffline.Controllers

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.ahleading.topaceforredditoffline.Model.CommentData
import com.ahleading.topaceforredditoffline.Model.ConstructRedditURL
import com.ahleading.topaceforredditoffline.Model.PostData
import com.ahleading.topaceforredditoffline.Model.PostDataSQLHelper
import com.ahleading.topaceforredditoffline.R
import com.ahleading.topaceforredditoffline.SavePostsService.SavePostsOfflineService
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class PostsController(val context: Context) {

    var postsArrayList: ArrayList<PostData?>
    var downloadController: DownloadController
    var sqlHelper: PostDataSQLHelper

    private lateinit var lastPostID: String
    private var currentSubreddit: String = "popular"
    private lateinit var extras: String
    private var IsActive: Boolean = false
    private var numberOfGottenPostsInThisPatch: Int = 0

    private val DATA_STR = "data"
    private val CHILDREN_STR = "children"
    private val TITLE_STR = "title"
    private val SUBREDDIT_STR = "subreddit"
    private val SCORE_STR = "score"
    private val THUMB_STR = "thumbnail"
    private val CREATED_UTC_STR = "created_utc"
    private val AUTHOR_STR = "author"
    private val NO_OF_COMMENTS_STR = "num_comments"
    private val URL_STR = "url"
    private val OVER18_STR = "over_18"
    private val PERMALINK_STR = "permalink"
    private val SELF_TEXT_HTML_STR = "selftext_html"
    private val PREVIEW_STR = "preview"
    private val IMAGES_STR = "images"
    private val SOURCE_STR = "source"
    private val BODY_HTML_STR = "body_html"
    private val DOMAIN = "domain"
    private val SCORE_HIDDEN = "score_hidden"
    private val DISTINGUISHED = "distinguished"
    private val IS_SUBMITTER = "is_submitter"

    init {
        postsArrayList = ArrayList()
        downloadController = DownloadController()
        sqlHelper = PostDataSQLHelper(context)
    }

    fun getLastPostID(): String {
        return this.lastPostID
    }

    fun getCurrentSubreddit(): String {
        return this.currentSubreddit
    }

    fun getNumberOfPostsInThisPatch(): Int {
        return this.numberOfGottenPostsInThisPatch
    }

    fun getExtras(): String {
        return this.extras
    }

    fun getIsActive(): Boolean {
        return this.IsActive
    }

    fun getActiveSubs(): String {
        var subreddits = ""
        val listSubs = sqlHelper.getActiveSubsTable()
        if (listSubs.size > 0) {
            for (item in listSubs) {
                subreddits += "$item+"
            }
            subreddits = subreddits.substring(0, subreddits.length - 1)
        } else {
            subreddits = "popular"
        }
        return subreddits
    }

    fun modifyParamsIfNeeded(params: String): String {
        return if (params[0] != '/') {
            "/$params"
        } else params
    }

    fun setSubredditAndItsProperities(subreddit: String, extras: String, isActive: Boolean) {
        this.currentSubreddit = subreddit
        this.extras = extras
        this.IsActive = isActive
    }

    fun setPostsSizeAndItsLastID(lastID: String, postsSizeInThisPatch: Int) {
        this.lastPostID = lastID
        this.numberOfGottenPostsInThisPatch = postsSizeInThisPatch
    }

//    fun updateRVWithActiveSubreddits(params: String, append: Boolean) {
//        if (!append) {
//            postsArrayList.clear()
//        }
//        val subs = getActiveSubs()
//        setSubredditAndItsProperities(subs, params, true)
//        val finalParams = modifyParamsIfNeeded(params)
//        val arrayList = getPosts(subs + finalParams)
//        val lastID = getIDFromPermalink(arrayList[arrayList.size - 1]!!.mPermalink)
//        setPostsSizeAndItsLastID(lastID, arrayList.size)
//        postsArrayList.addAll(arrayList)
//    }

    fun updateRVWithActiveSubreddit(params: String, append: Boolean, subreddit: String = "") {
        if (!append) {
            postsArrayList.clear()
        }
        var subredditName = if (subreddit.equals("")) {
            getActiveSubs()
        } else {
            subreddit
        }
        setSubredditAndItsProperities(subredditName, params, true)
        val prms = modifyParamsIfNeeded(params)
        val url = ConstructRedditURL.constructURL(subredditName + prms)
        val arrayList = getPosts(url)
        val lastID = getIDFromPermalink(arrayList[arrayList.size - 1]!!.mPermalink)
        setPostsSizeAndItsLastID(lastID, arrayList.size)
        postsArrayList.addAll(arrayList)
    }

    fun updateRVWithArchivedSubreddit(subreddit: String) {
        postsArrayList.clear()
        setSubredditAndItsProperities(subreddit, "", false)
        val arrayList = sqlHelper.getPostsSQL(subreddit)
        setPostsSizeAndItsLastID("", arrayList.size)
        postsArrayList.addAll(arrayList)
    }

    fun getPosts(url: String): ArrayList<PostData?> {
        val jsonCompURL = ConstructRedditURL.addRawJsonCompatibility(url)
        Log.i("PostsController", jsonCompURL)
        val postsJson = downloadController.getJSONReddit(jsonCompURL)
        return extractPostInfo(postsJson!!)
    }

    fun archivePosts(subreddit: String, saveOptionStr: String, numberOfSavedPostsStr: String) {
        val subredditAndParams: String = when {
            saveOptionStr.contains("week") -> subreddit + ConstructRedditURL.secondPartRedditURL_TOP_100 + ConstructRedditURL.secondPartWeek
            saveOptionStr.contains("month") -> subreddit + ConstructRedditURL.secondPartRedditURL_TOP_100 + ConstructRedditURL.secondPartMonth
            saveOptionStr.contains("year") -> subreddit + ConstructRedditURL.secondPartRedditURL_TOP_100 + ConstructRedditURL.secondPartYear
            else -> subreddit + ConstructRedditURL.secondPartRedditURL_TOP_100 + ConstructRedditURL.secondPartAll
        }
        val i = when (numberOfSavedPostsStr) {
            context.getString(R.string._100_posts) -> 1
            context.getString(R.string._200_posts) -> 2
            context.getString(R.string._300_posts) -> 3
            else -> 5
        }
        val arrayListOfPosts: ArrayList<PostData?> = ArrayList()
        var params = subredditAndParams
        for (index in 0 until i) {
            if (index in 1 until i) {
                val lastIndex = arrayListOfPosts.size - 1
                val postData = arrayListOfPosts[lastIndex]
                val lastID = getIDFromPermalink(postData?.mPermalink!!)
                params = ConstructRedditURL.addAfterID(subredditAndParams, lastID)
            }
            val url = ConstructRedditURL.constructURL(params)
            arrayListOfPosts.addAll(getPosts(url))
            if (arrayListOfPosts.size == 0) {
                return
            }
        }
        sqlHelper.populatePostsSQL(arrayListOfPosts)
        val intent = Intent(context, SavePostsOfflineService::class.java)
        intent.putExtra("subreddit", subreddit)
        context.startService(intent)
    }

    fun deleteActiveSubredditFromSQL() {
        sqlHelper.deleteActiveSubreddit(getCurrentSubreddit())
    }

    fun deleteArchivedSubredditFromSQL() {
        sqlHelper.deleteArchivedSubreddit(getCurrentSubreddit())
    }

    fun getIDFromPermalink(urlString: String): String {
        val strArr = urlString.split("/")
        return strArr[4]
    }

    fun genRand(length: Int): String {
        val rand = Random()
        val possibleLetters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val sb = StringBuilder(length)
        for (i in 0 until length)
            sb.append(possibleLetters[rand.nextInt(possibleLetters.length)])
        return sb.toString()
    }

    fun saveToInternalStorage(bitmapImage: Bitmap, name: String, extension: String): String {
        val cw = ContextWrapper(context)
        // path to /data/data/yourapp/app_data/images
        val directory = cw.getDir("Images", Context.MODE_PRIVATE)
        // Create images
        val mypath = File(directory, name + extension)

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(mypath)
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return mypath.absolutePath
    }

    fun getComments(subreddit: String, IDPost: String): ArrayList<CommentData> {
        val commentsArrayList: ArrayList<CommentData>
//        val urlStr = Constants.firstPartRedditURL + subreddit + Constants.secondPartRedditURL_COMMENT + IDPost
        var url = ConstructRedditURL.constructURL(subreddit)
        url = ConstructRedditURL.addComments(url, IDPost)
        val commentsJson = downloadController.getJSONReddit(url)
        commentsArrayList = extractCommentsInfo(commentsJson!!)
        return commentsArrayList
    }

    fun extractCommentsInfo(jsonString: String): ArrayList<CommentData> {
        val commentDataObjectsList = arrayListOf<CommentData>()
        val response = JSONArray(jsonString)
        val responseIndex1 = response.getJSONObject(1)
        val data = responseIndex1.getJSONObject(DATA_STR)
        val children = data.getJSONArray(CHILDREN_STR)
        for (i in 0 until children.length()) {
            val data2 = children.getJSONObject(i).getJSONObject(DATA_STR)
            if (data2.has("count"))
                break
            val permalink = data2.getString(PERMALINK_STR)
            val commentHTML = data2.getString(BODY_HTML_STR)
            val author = data2.getString(AUTHOR_STR)
            val score = data2.getInt(SCORE_STR)
            val createdUTC = data2.getLong(CREATED_UTC_STR)
            val isScoreHidden = data2.getBoolean(SCORE_HIDDEN)
            var distinguishedStatus: String? = data2.getString(DISTINGUISHED)
            if (distinguishedStatus == null) {
                distinguishedStatus = "null"
            }
            val isSubmitter = data2.getBoolean(IS_SUBMITTER)
            val commentDataObj = CommentData(author, commentHTML, score, permalink, createdUTC, isScoreHidden, distinguishedStatus, isSubmitter)
            commentDataObjectsList.add(commentDataObj)
        }
        return commentDataObjectsList
    }

    //Included: title,subreddit,score,thumbnail,createdUTC,author,noOfComments,url
    fun extractPostInfo(jsonString: String): ArrayList<PostData?> {
        val postDataObjectsList = arrayListOf<PostData?>()
        val response = JSONObject(jsonString)
        val data = response.getJSONObject(DATA_STR)
        val children = data.getJSONArray(CHILDREN_STR)
        for (i in 0 until children.length()) {
            val topic = children.getJSONObject(i).getJSONObject(DATA_STR)
            if (topic.getString(OVER18_STR) == "true") {
                continue
            }
            var imageLink: String = ""
            if (topic.has(PREVIEW_STR)) {
                val preview = topic.getJSONObject(PREVIEW_STR)
                val imagesArray = preview.getJSONArray(IMAGES_STR)
                val src = imagesArray.getJSONObject(0).getJSONObject(SOURCE_STR)
                imageLink = src.getString(URL_STR)
            }
            val title = topic.getString(TITLE_STR)
            val selfText = topic.getString(SELF_TEXT_HTML_STR)
            val postDataObj = PostData(title,
                    topic.getString(SUBREDDIT_STR),
                    topic.getInt(SCORE_STR),
                    topic.getString(THUMB_STR),
                    topic.getLong(CREATED_UTC_STR),
                    topic.getString(AUTHOR_STR),
                    topic.getInt(NO_OF_COMMENTS_STR),
                    topic.getString(URL_STR),
                    topic.getString(PERMALINK_STR),
                    selfText,
                    "",
                    imageLink,
                    "",
                    topic.getString(DOMAIN)
            )
            postDataObjectsList.add(postDataObj)
        }
        return postDataObjectsList
    }

//    fun getDecodedStringFromHTML(sentence: String): String {
//        return if (Build.VERSION.SDK_INT >= 24) {
//            Html.fromHtml(sentence, Html.FROM_HTML_MODE_LEGACY).toString()
//        } else {
//            Html.fromHtml(sentence).toString()
//        }
//    }

    companion object {
        fun extractTimeOfNow(delta: Long): String? {
            val difference: Long
            val mDate = System.currentTimeMillis()
            val newDelta = delta * 1000
            if (mDate > newDelta) {
                difference = mDate - newDelta
                val seconds = difference / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val days = hours / 24
                val months = days / 31
                val years = days / 365

                return when {
                    seconds < 0 -> "incorrect_phone_date"
                    seconds < 60 -> "just now"
                    seconds < 120 -> "a minute ago"
                    seconds < 60 * 60 -> minutes.toString() + " minutes ago"
                    seconds < 2 * 60 * 60 -> "an hour ago"
                    seconds < 24 * 60 * 60 -> hours.toString() + " hours ago"
                    seconds < 48 * 60 * 60 -> "1 day ago"
                    seconds < 30 * 24 * 60 * 60 -> days.toString() + " days ago"
                    seconds < 60 * 24 * 60 * 60 -> "1 month ago"
                    seconds < 365 * 24 * 60 * 60 -> months.toString() + " months ago"
                    else -> if (years <= 1) "1 year ago" else years.toString() + " years ago"
                }
            }
            return null
        }
    }
}