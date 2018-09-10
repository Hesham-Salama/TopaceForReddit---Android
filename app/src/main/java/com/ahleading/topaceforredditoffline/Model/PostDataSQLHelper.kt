package com.ahleading.topaceforredditoffline.Model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File

class PostDataSQLHelper(context: Context) : SQLiteOpenHelper(context, DATABASENAME, null, DATABASEVERSION) {

    private val TABLE_NAME = "Posts"
    private val TABLE_NAME2 = "Active_posts"

    private val TITLE = "title"
    private val SUBREDDIT_NAME = "subreddit_name"
    private val SCORE = "score"
    private val THUMBNAIL_LINK = "thumbnail_link"
    private val THUMBNAIL_STORAGE = "thumbnail_storage"
    private val CREATED_UTC = "created_utc"
    private val AUTHOR = "author"
    private val NUMBER_OF_COMMENTS = "number_of_comments"
    private val WEBSITE_URL = "url"
    private val PERMALINK = "permalink"
    private val TEXT_HTML = "text_html"
    private val IMAGE_SRC_URL = "image_src_url"
    private val IMAGE_SRC_STORAGE = "image_src_storage"
    private val DOMAIN = "domain"
    private val COMMENTOR = "commentor"
    private val COMMENT_TEXT = "comment_text"
    private val POINTS = "points"
    private val SCORE_HIDDEN = "score_hidden"
    private val TABLE_NAME3 = "comment_table"
    private val TABLE_NAME4 = "posts_notification_table"

    private var db: SQLiteDatabase = this.writableDatabase
    // http://groups.google.com/group/android-developers/msg/74ee967b2fcff770
    // sqlitedatabase should be initialized only once and you may not close it if you want.

    companion object {
        private val DATABASENAME = "Database1"
        private val DATABASEVERSION = 1
    }

    override fun close() {
        db.close()
    }

    fun reopen() {
        close()
        db = this.getWritableDatabase()
    }

    override fun onCreate(p0: SQLiteDatabase?) {
        val query = "CREATE TABLE IF NOT EXISTS $TABLE_NAME ($PERMALINK TEXT PRIMARY KEY, $TITLE TEXT, " +
                "$SUBREDDIT_NAME TEXT, $SCORE INTEGER, $THUMBNAIL_LINK TEXT, $CREATED_UTC INTEGER, $AUTHOR TEXT," +
                "$NUMBER_OF_COMMENTS INTEGER, $WEBSITE_URL TEXT, $TEXT_HTML TEXT, $THUMBNAIL_STORAGE TEXT, $IMAGE_SRC_URL TEXT, " +
                "$IMAGE_SRC_STORAGE TEXT, $DOMAIN TEXT )"

        val query2 = "CREATE TABLE IF NOT EXISTS $TABLE_NAME2 ($SUBREDDIT_NAME TEXT PRIMARY KEY)"
        val query3 = "CREATE TABLE IF NOT EXISTS $TABLE_NAME3 ($PERMALINK TEXT PRIMARY KEY, $COMMENTOR TEXT, $COMMENT_TEXT TEXT, $POINTS INTEGER," +
                " $SCORE_HIDDEN INTEGER, $CREATED_UTC INTEGER)"
        val query4 = "CREATE TABLE IF NOT EXISTS $TABLE_NAME4 ($PERMALINK TEXT PRIMARY KEY)"

        p0?.execSQL(query)
        p0?.execSQL(query2)
        p0?.execSQL(query3)
        p0?.execSQL(query4)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        p0?.execSQL("DROP TABLE IF EXIST $TABLE_NAME")
        p0?.execSQL("DROP TABLE IF EXIST $TABLE_NAME2")
        onCreate(p0)
    }

    fun populatePostsSQL(posts: ArrayList<PostData?>) {

        try {
            db.beginTransaction()
            for (i in 0 until posts.size) {
                val values = ContentValues()
                values.put(PERMALINK, posts[i]?.mPermalink)
                values.put(TITLE, posts[i]?.mTitle)
                val subreddit = posts[i]?.mSubreddit?.substring(0, 1)?.toUpperCase() +
                        posts[i]?.mSubreddit?.substring(1)?.toLowerCase()
                values.put(SUBREDDIT_NAME, subreddit)
                values.put(SCORE, posts[i]?.mScore)
                values.put(THUMBNAIL_LINK, posts[i]?.mThumbnailLink)
                values.put(CREATED_UTC, posts[i]?.mCreatedUTC)
                values.put(AUTHOR, posts[i]?.mAuthor)
                values.put(NUMBER_OF_COMMENTS, posts[i]?.mNoOfComments)
                values.put(WEBSITE_URL, posts[i]?.mUrl)
                values.put(TEXT_HTML, posts[i]?.mSelfText_HTML)
                values.put(THUMBNAIL_STORAGE, posts[i]?.mThumbnailStorage)
                values.put(IMAGE_SRC_URL, posts[i]?.image_src_url)
                values.put(IMAGE_SRC_STORAGE, posts[i]?.mImageSrcStorage)
                values.put(DOMAIN, posts[i]?.mDomain)
                db.insert(TABLE_NAME, null, values)
            }
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            db.endTransaction()
            // db.close()
        }
    }

    fun getPostsSQL(subreddit: String): ArrayList<PostData?> {
        //To be used To get archived sub posts
        val list = ArrayList<PostData?>()
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NAME " +
                "WHERE $SUBREDDIT_NAME = \"$subreddit\"", null)
        try {
            db.beginTransaction()
            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val title = cursor.getString(cursor.getColumnIndex(TITLE))
                    val subredditName = cursor.getString(cursor.getColumnIndex(SUBREDDIT_NAME))
                    val score = cursor.getInt(cursor.getColumnIndex(SCORE))
                    val thumbnailLink = cursor.getString(cursor.getColumnIndex(THUMBNAIL_LINK))
                    val createdUTC = cursor.getLong(cursor.getColumnIndex(CREATED_UTC))
                    val author = cursor.getString(cursor.getColumnIndex(AUTHOR))
                    val numComments = cursor.getInt(cursor.getColumnIndex(NUMBER_OF_COMMENTS))
                    val url = cursor.getString(cursor.getColumnIndex(WEBSITE_URL))
                    val permalink = cursor.getString(cursor.getColumnIndex(PERMALINK))
                    val textHTML = cursor.getString(cursor.getColumnIndex(TEXT_HTML))
                    val thumb = cursor.getString(cursor.getColumnIndex(THUMBNAIL_STORAGE))
                    val imageSrcURL = cursor.getString(cursor.getColumnIndex(IMAGE_SRC_URL))
                    val imageSrcStorage = cursor.getString(cursor.getColumnIndex(IMAGE_SRC_STORAGE))
                    val domain = cursor.getString(cursor.getColumnIndex(DOMAIN))
                    val user = PostData(title, subredditName, score, thumbnailLink, createdUTC, author, numComments
                            , url, permalink, textHTML, thumb, imageSrcURL, imageSrcStorage, domain)
                    list.add(user)
                } while (cursor.moveToNext())
            }
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return list
    }

    fun addToActiveSubsTable(subreddit: String) {
        try {
            db.beginTransaction()
            val values = ContentValues()
//                val temp = subreddit.substring(0, 1).toUpperCase() + subreddit.substring(1).toLowerCase()
            values.put(SUBREDDIT_NAME, subreddit)
            db.insert(TABLE_NAME2, null, values)
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            db.endTransaction()
            // db.close()

        }
    }

    fun getActiveSubsTable(): ArrayList<String> {
        val arrList = ArrayList<String>()
        val cursor: Cursor = db.rawQuery("SELECT $SUBREDDIT_NAME FROM $TABLE_NAME2", null)
        try {
            db.beginTransaction()
            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val subreddit = cursor.getString(cursor.getColumnIndex(SUBREDDIT_NAME))
                    arrList.add(subreddit)
                } while (cursor.moveToNext())
            }
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return arrList
    }

    fun getArchivedSubsTable(): ArrayList<String> {
        //to be used when populating nav view
        val arrList = ArrayList<String>()
        val cursor: Cursor = db.rawQuery("SELECT DISTINCT $SUBREDDIT_NAME FROM $TABLE_NAME", null)
        try {
            db.beginTransaction()
            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val subreddit = cursor.getString(cursor.getColumnIndex(SUBREDDIT_NAME))
                    arrList.add(subreddit)
                } while (cursor.moveToNext())
            }
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return arrList
    }

    fun checkIfPermalinkExistsInArchTable(permalink: String): Boolean {
        var check: Boolean = false
        val cursor: Cursor = db.rawQuery("SELECT $PERMALINK FROM $TABLE_NAME WHERE $PERMALINK = \"$permalink\"", null)
        try {
            db.beginTransaction()
            check = cursor.count > 0
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()

            db.endTransaction()
            // db.close()
        }
        return check
    }

    fun getThumbnailPathFromPermaLink(permalink: String): String {
        var check: String = ""
        val cursor: Cursor = db.rawQuery("SELECT $THUMBNAIL_STORAGE FROM $TABLE_NAME WHERE $PERMALINK = \"$permalink\"", null)
        try {
            db.beginTransaction()
            if (cursor.count > 0) {
                cursor.moveToFirst()
                check = cursor.getString(cursor.getColumnIndex(THUMBNAIL_STORAGE))
            }
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return check
    }


    fun attachThumbToPermaLink(thumbnailStorage: String, permalink: String) {
        try {
            db.beginTransaction()
            val cv = ContentValues()
            cv.put(THUMBNAIL_STORAGE, thumbnailStorage)
            db.update(TABLE_NAME, cv, "$PERMALINK = \"$permalink\"", null)
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            db.endTransaction()
            // db.close()
        }
    }

    fun getImagePostPathFromPermaLink(permalink: String): String {
        var check = ""
        val cursor: Cursor = db.rawQuery("SELECT $IMAGE_SRC_STORAGE FROM $TABLE_NAME WHERE $PERMALINK = \"$permalink\"", null)
        try {
            db.beginTransaction()
            if (cursor.count > 0) {
                cursor.moveToFirst()
                check = cursor.getString(cursor.getColumnIndex(IMAGE_SRC_STORAGE))
            }
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return check
    }

    fun attachPostImageToPermaLink(imageSrcStorage: String, permalink: String) {
        try {
            db.beginTransaction()
            val cv = ContentValues()
            cv.put(IMAGE_SRC_STORAGE, imageSrcStorage)
            db.update(TABLE_NAME, cv, "$PERMALINK = \"$permalink\"", null)
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            db.endTransaction()
            // db.close()
        }
    }

    fun deleteActiveSubreddit(subreddit: String) {
        val whereClause = "$SUBREDDIT_NAME=?"
        val whereArgs = arrayOf(subreddit)
        db.delete(TABLE_NAME2, whereClause, whereArgs)
        // db.close()
    }

    fun deleteArchivedSubreddit(subreddit: String) {
        val listThumbs = ArrayList<String?>()
        val listImages = ArrayList<String?>()
        val listPermalinks = ArrayList<String>()
        val cursor: Cursor = db.rawQuery("SELECT $THUMBNAIL_STORAGE, $IMAGE_SRC_STORAGE, $PERMALINK FROM $TABLE_NAME " +
                "WHERE $SUBREDDIT_NAME = \"$subreddit\"", null)
        try {
            db.beginTransaction()
            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val thumb = cursor.getString(cursor.getColumnIndex(THUMBNAIL_STORAGE))
                    listThumbs.add(thumb)
                    val image = cursor.getString(cursor.getColumnIndex(IMAGE_SRC_STORAGE))
                    listImages.add(image)
                    val permalink = cursor.getString(cursor.getColumnIndex(PERMALINK))
                    listPermalinks.add(permalink)
                } while (cursor.moveToNext())
            }
            db.setTransactionSuccessful()

            for (fileDir in listThumbs) {
                if (fileDir != null) {
                    val file: File? = File(fileDir)
                    if (file != null && file.exists()) {
                        file.delete()
                    }
                }
            }
            for (fileDir in listImages) {
                if (fileDir != null) {
                    val file: File? = File(fileDir)
                    if (file != null && file.exists()) {
                        file.delete()
                    }
                }
            }
            //add delete comments
            var whereClause = "$PERMALINK like ?"
            for (item in listPermalinks) {
                val permlk = arrayOf("$item%")
                db.delete(TABLE_NAME3, whereClause, permlk)
            }

            //delete subreddit from table
            whereClause = "$SUBREDDIT_NAME=?"
            val whereArgs = arrayOf(subreddit)
            db.delete(TABLE_NAME, whereClause, whereArgs)
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
    }

    fun checkIfAsubredditExistsInArchivedTable(subreddit: String): Boolean {
        var check: Boolean = false
        val cursor: Cursor = db.rawQuery("SELECT $SUBREDDIT_NAME FROM $TABLE_NAME WHERE $SUBREDDIT_NAME = \"$subreddit\"" +
                "COLLATE NOCASE",
                null)
        try {
            db.beginTransaction()
            check = cursor.count > 0
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return check
    }

    fun checkIfAsubredditExistsInActiveTable(subreddit: String): Boolean {
        var check: Boolean = false
        val cursor: Cursor = db.rawQuery("SELECT $SUBREDDIT_NAME FROM $TABLE_NAME2 WHERE $SUBREDDIT_NAME = \"$subreddit\"" +
                "COLLATE NOCASE",
                null)
        try {
            db.beginTransaction()
            check = cursor.count > 0
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return check
    }

    fun populateComments(comments: ArrayList<CommentData>) {
        try {
            db.beginTransaction()
            for (comment in comments) {
                val values = ContentValues()
                values.put(PERMALINK, comment.mPermalLink)
                values.put(COMMENTOR, comment.mUser)
                values.put(COMMENT_TEXT, comment.mComment_HTML)
                values.put(POINTS, comment.mScore)
                val scoreHidden = if (comment.isScoreHidden) 1 else 0
                values.put(SCORE_HIDDEN, scoreHidden)
                values.put(CREATED_UTC, comment.mCreatedUTC)
                db.insert(TABLE_NAME3, null, values)
            }
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            db.endTransaction()
            // db.close()
        }
    }

    fun getComments(permalink: String): ArrayList<CommentData> {
        val commentsArray = ArrayList<CommentData>()
        val query = "SELECT * FROM $TABLE_NAME3 WHERE $PERMALINK LIKE \"$permalink%\""
        val cursor: Cursor = db.rawQuery(query, null)
        try {
            db.beginTransaction()
            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val commentor = cursor.getString(cursor.getColumnIndex(COMMENTOR))
                    val score = cursor.getInt(cursor.getColumnIndex(POINTS))
                    val commentText = cursor.getString(cursor.getColumnIndex(COMMENT_TEXT))
                    val perm = cursor.getString(cursor.getColumnIndex(PERMALINK))
                    val isScoreHidden = cursor.getInt(cursor.getColumnIndex(SCORE_HIDDEN)) == 1
                    val createdUTC = cursor.getLong(cursor.getColumnIndex(CREATED_UTC))
                    val comment = CommentData(commentor, commentText, score, perm, createdUTC, isScoreHidden)
                    commentsArray.add(comment)
                } while (cursor.moveToNext())
            }
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return commentsArray
    }

    fun checkIfItWasAsANotification(permalink: String): Boolean {
        var check: Boolean = false
        val cursor: Cursor = db.rawQuery("SELECT $PERMALINK FROM $TABLE_NAME4 WHERE $PERMALINK = \"$permalink\"",
                null)
        try {
            db.beginTransaction()
            check = cursor.count > 0
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            cursor.close()
            db.endTransaction()
            // db.close()
        }
        return check
    }

    fun addToNotificationPostsTable(permalink: String) {
        try {
            db.beginTransaction()
            val values = ContentValues()
            values.put(PERMALINK, permalink)
            db.insert(TABLE_NAME4, null, values)
            db.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.e("SQLEXCEPTION", e.toString())
        } finally {
            db.endTransaction()
            // db.close()
        }
    }
}