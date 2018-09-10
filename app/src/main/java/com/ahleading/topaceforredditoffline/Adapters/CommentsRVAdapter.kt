package com.ahleading.topaceforredditoffline.Adapters

import android.content.Context
import android.os.Build
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.CommentData
import com.ahleading.topaceforredditoffline.R
import kotlinx.android.synthetic.main.comment_item.view.*
import org.sufficientlysecure.htmltextview.ClickableTableSpan
import org.sufficientlysecure.htmltextview.DrawTableLinkSpan
import org.sufficientlysecure.htmltextview.HtmlResImageGetter

class CommentsRVAdapter(private val arrayList: ArrayList<CommentData>,
                        private val context: Context,
                        private val layout: Int)
    : RecyclerView.Adapter<CommentsRVAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(arrayList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(item: CommentData) {
            itemView.username_of_comment_id.text = item.mUser
            val scorePoints: String = if (item.isScoreHidden) {
                context.getString(R.string.score_hidden_string)
            } else {
                itemView.resources.getQuantityString(R.plurals.point_plural, item.mScore, item.mScore)
            }
//            val scorePoints = item.mScore.toString() + " Points"
            itemView.comment_time_id.text = PostsController.extractTimeOfNow(item.mCreatedUTC)
            itemView.comment_score_id.text = scorePoints
            setPostText(item.mComment_HTML)
        }

        internal inner class ClickableTableSpanImpl : ClickableTableSpan() {
            override fun newInstance(): ClickableTableSpan {
                return ClickableTableSpanImpl()
            }

            override fun onClick(widget: View) {
                val alert = AlertDialog.Builder(context)
                val wv = WebView(context)
                wv.loadData(getTableHtml(), "text/html", "UTF-8")
                alert.setView(wv)
                alert.show()
            }
        }

        private fun setPostText(commentHTML: String) {
            var html: String? = commentHTML
            html = if (html != null) {

                if (Build.VERSION.SDK_INT >= 24) {
                    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
                } else {
                    Html.fromHtml(html).toString()
                }
            } else {
                ""
            }

            itemView.comment_content_id.setClickableTableSpan(ClickableTableSpanImpl())
            val drawTableLinkSpan = DrawTableLinkSpan()
            drawTableLinkSpan.tableLinkText = context.getString(R.string.table_exists)
            itemView.comment_content_id.setDrawTableLinkSpan(drawTableLinkSpan)
            itemView.comment_content_id.setHtml(html, HtmlResImageGetter(itemView.comment_content_id))
        }
    }
}