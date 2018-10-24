package com.ahleading.topaceforredditoffline.Activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import com.ahleading.topaceforredditoffline.Adapters.PostsRVAdapter
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.R
import kotlinx.android.synthetic.main.activity_saved_posts.*

class SavedPostsActivity : AppCompatActivity() {

    private var postsAdapter: PostsRVAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private lateinit var postsController: PostsController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_posts)
        setSupportActionBar(toolbar_saved)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initializeView()
    }

    private fun initializeView() {
        postsController = PostsController(this)
        val savedPosts = postsController.sqlHelper.getSavedPosts()
        postsAdapter = PostsRVAdapter(savedPosts, this, R.layout.post_item,
                postsController, false)
        layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        saved_posts_rv?.isNestedScrollingEnabled = false
        saved_posts_rv?.layoutManager = layoutManager
        saved_posts_rv?.adapter = postsAdapter
        notifyAdapter()
        if (postsAdapter?.itemCount == 0) {
            no_posts_yet_saved_id.visibility = View.VISIBLE
        }
    }

    fun notifyAdapter() {
        runOnUiThread {
            postsAdapter?.notifyDataSetChanged()
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
}
