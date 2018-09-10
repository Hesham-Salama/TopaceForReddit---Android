package com.ahleading.topaceforredditoffline.Adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ahleading.topaceforredditoffline.Activities.PostContentActivity
import com.ahleading.topaceforredditoffline.Ads.AdmobAds
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.Constants
import com.ahleading.topaceforredditoffline.Model.PostData
import com.ahleading.topaceforredditoffline.R
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.post_item.view.*
import java.io.File
import java.lang.Exception
import java.util.*

class PostsRVAdapter(private val arrayList: ArrayList<PostData?>,
                     private val context: Context,
                     private val layout: Int, private val postsController: PostsController)
    : RecyclerView.Adapter<PostsRVAdapter.ViewHolder>() {


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
    private val IMAGE_SRC_STORAGE = "image_src_storage"
    private val TIME_OF_POST = "time_of_post"
    private val URL_STR = "url"
    private val admob_instance = AdmobAds(context)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View?
        view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= 0 && position < arrayList.size) {
            val post = arrayList[position]
            holder.bindItems(post!!)
        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun setThumbnail(item: PostData) {

            if (item.mThumbnailLink == "self") {
                Picasso.get().load(R.drawable.text).into(itemView.thumbnail_id)
            } else if (item.mThumbnailLink.trim() == "" || item.mThumbnailLink == "default") {
                Picasso.get().load(R.drawable.no_thumbnail).into(itemView.thumbnail_id)
            } else if (postsController.sqlHelper.checkIfPermalinkExistsInArchTable(item.mPermalink)) {
                if (postsController.sqlHelper.getThumbnailPathFromPermaLink(item.mPermalink) != "") {
                    val file = File(postsController.sqlHelper.getThumbnailPathFromPermaLink(item.mPermalink))
                    if (file.exists()) {
                        Picasso.get().load(file).fit().centerCrop().into(itemView.thumbnail_id)
                    }
                } else {
                    Picasso.get().load(item.mThumbnailLink).into(object : Target {
                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        }

                        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                            val generatedString = postsController.genRand(10)
                            val completePath = postsController.saveToInternalStorage(bitmap!!, generatedString, ".jpg")
                            postsController.sqlHelper.attachThumbToPermaLink(completePath, item.mPermalink)
                            Picasso.get().load(File(completePath)).fit().centerCrop().into(itemView.thumbnail_id)
                        }

                        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        }

                    })
                }
            } else {
                Picasso.get().load(item.mThumbnailLink)
                        .into(itemView.thumbnail_id)
            }
        }

        fun bindItems(item: PostData) {

            itemView.score.text = item.mScore.toString()
            val subredditName = "/r/" + item.mSubreddit
            itemView.subreddit_id.text = subredditName
            itemView.title_id.text = item.mTitle
            var byUserNumberOfComments = "by /u/" + item.mAuthor + " - "
            byUserNumberOfComments += itemView.resources.getQuantityString(R.plurals.comment_plural,
                    item.mNoOfComments, item.mNoOfComments)
            itemView.byuser_comments_id.text = byUserNumberOfComments
            val submittedAt = "Submitted " + PostsController.extractTimeOfNow(item.mCreatedUTC)
            itemView.submitted_at_id.text = submittedAt
            setThumbnail(item)
            itemView.setOnClickListener(View.OnClickListener {
                val intent = Intent(context, PostContentActivity::class.java)
                intent.putExtra(SELF_TEXT_VAL, item.mSelfText_HTML)
                intent.putExtra(PERMALINK, item.mPermalink)
                intent.putExtra(TITLE, item.mTitle)
                intent.putExtra(SCORE, item.mScore.toString())
                intent.putExtra(AUTHOR, item.mAuthor)
                intent.putExtra(DOMAIN, item.mDomain)
                intent.putExtra(SUBREDDIT, item.mSubreddit)
                intent.putExtra(IMAGE_LINK, item.image_src_url)
                intent.putExtra(IMAGE_SRC_STORAGE, postsController.sqlHelper.getImagePostPathFromPermaLink(item.mPermalink))
                intent.putExtra(THUMB_LINK, item.mThumbnailLink)
                intent.putExtra(URL_STR, item.mUrl)
                intent.putExtra(TIME_OF_POST, item.mCreatedUTC)

                itemView.thumbnail_id.buildDrawingCache()
                val bitmap: Bitmap? = itemView.thumbnail_id.drawingCache
                val extras = Bundle()
                extras.putParcelable(THUMB, bitmap)
                intent.putExtras(extras)
                context.startActivity(intent)
                admob_instance.showInterstitalAd(Constants.MIN_ODD_FOR_INTER_AD_POST_CONTENT,
                        Constants.MAX_ODD_FOR_INTER_AD_POST_CONTENT)
            })
        }


    }
}