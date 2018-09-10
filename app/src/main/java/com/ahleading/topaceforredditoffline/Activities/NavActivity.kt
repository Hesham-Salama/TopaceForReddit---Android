package com.ahleading.topaceforredditoffline.Activities

import android.app.Activity
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.ahleading.topaceforredditoffline.Adapters.PostsRVAdapter
import com.ahleading.topaceforredditoffline.Controllers.PostsController
import com.ahleading.topaceforredditoffline.Model.Constants
import com.ahleading.topaceforredditoffline.Model.SingletonServiceManager
import com.ahleading.topaceforredditoffline.PostNotificationSchedule.ScheduleJobUtility
import com.ahleading.topaceforredditoffline.R
import com.ahleading.topaceforredditoffline.ViewsControl.AppRater
import com.ahleading.topaceforredditoffline.ViewsControl.WindowControl
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_nav.*
import kotlinx.android.synthetic.main.app_bar_nav.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton


class NavActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val GROUP_ID1 = 12
    private val GROUP_ID2 = 15
    private val REQUEST_CODE = 12345
    private val RESULT_CONSENT_REVOKE = 12
    private val BACK_BUTTON_TO_BE_PRESSED_INTERVAL = 2000
    private var mBackPressedTime: Long = 0L
    private lateinit var postsController: PostsController
    private var postsAdapter: PostsRVAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    var menu: Menu? = null
    var isPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        postsController = PostsController(this)
        initializeView()
        initializeAdsMode()
        populateNavView()
        checkJobsScheduling()
        Thread(Runnable {
            updateRedditTimeLine(1)
            runOnUiThread {
                AppRater.app_launched(this)
            }
        }).start()
    }

    private fun initializeAdsMode() {
        MobileAds.initialize(this, Constants.ADMOB_APP_ID)
    }

    private fun checkJobsScheduling() {
        val jobScheduler = this.getSystemService(
                Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (jobScheduler.allPendingJobs.size == 0) {
            val editor = getSharedPreferences(Constants.PREFS_BOOL_NAME, Context.MODE_PRIVATE).edit()
            editor.putBoolean(Constants.PERIODIC_FIRST_LAUNCH, false)
            editor.apply()
            ScheduleJobUtility.scheduleJob(this)

        }
    }

    private fun initializeView() {
        //setup Navigation and toolbar
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        postsAdapter = PostsRVAdapter(postsController.postsArrayList, this, R.layout.post_item,
                postsController)
        layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        recycler_view?.isNestedScrollingEnabled = false
        recycler_view?.layoutManager = layoutManager
        recycler_view?.adapter = postsAdapter
        recycler_view?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && dy != 0) {
                    if (postsController.getIsActive() && !isPending) {
                        getAdditionalPosts()
                    }
                }
            }
        })
    }

    fun getAdditionalPosts() {
        loading_more_progress_bar.visibility = View.VISIBLE
        Thread(Runnable {
            isPending = true
            val subreddit = postsController.getCurrentSubreddit()
            val lastID = "t3_" + postsController.getLastPostID()
            val lastIDParameter = "after=" + lastID
            var params = postsController.getExtras()
            if (params.contains("after=t3_")) {
                params = params.substring(0, params.length - 16)
            }
            params = if (params.contains("?")) {
                "$params&$lastIDParameter"
            } else {
                "$params?$lastIDParameter"
            }
            val oldSize = postsAdapter!!.itemCount
            updateRedditTimeLine(2,
                    subreddit, params, true)
            runOnUiThread {
                try {
                    val itemsCount = postsController.getNumberOfPostsInThisPatch()
                    postsAdapter?.notifyItemRangeInserted(oldSize, itemsCount)
                } catch (e: IndexOutOfBoundsException) {
                    Log.i("Oops", "ExcceptionOutOfbound")
                }
                loading_more_progress_bar.visibility = View.GONE
            }
            isPending = false
        }).start()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            if (mBackPressedTime + BACK_BUTTON_TO_BE_PRESSED_INTERVAL > System.currentTimeMillis()) {
                super.onBackPressed()
                return
            } else {
                Toast.makeText(this, getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
            }
            mBackPressedTime = System.currentTimeMillis()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu
        menuInflater.inflate(R.menu.nav, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val subAndStatus = postsController.getCurrentSubreddit()
        val subredditMode = if (subAndStatus.contains("+")) 1 else 2
        when (item.itemId) {
            R.id.sort_new -> {
                Thread(Runnable {
                    updateRedditTimeLine(subredditMode, subAndStatus, Constants.secondPartRedditURL_NEW)
                }).start()
                return true
            }
            R.id.sort_hot -> {
                Thread(Runnable {
                    updateRedditTimeLine(subredditMode, subAndStatus)
                }).start()
                return true
            }
            R.id.sort_top_day -> {
                Thread(Runnable {
                    updateRedditTimeLine(subredditMode, subAndStatus, Constants.secondPartRedditURL_TOP
                            + Constants.secondPartDay)
                }).start()
                return true
            }
            R.id.sort_top_week -> {
                Thread(Runnable {
                    updateRedditTimeLine(subredditMode, subAndStatus, Constants.secondPartRedditURL_TOP
                            + Constants.secondPartWeek)
                }).start()
                return true
            }
            R.id.sort_top_month -> {
                Thread(Runnable {
                    updateRedditTimeLine(subredditMode, subAndStatus, Constants.secondPartRedditURL_TOP
                            + Constants.secondPartMonth)
                }).start()
                return true
            }
            R.id.sort_top_year -> {
                Thread(Runnable {
                    updateRedditTimeLine(subredditMode, subAndStatus, Constants.secondPartRedditURL_TOP
                            + Constants.secondPartYear)
                }).start()
                return true
            }
            R.id.sort_top_all -> {
                Thread(Runnable {
                    updateRedditTimeLine(subredditMode, subAndStatus, Constants.secondPartRedditURL_TOP
                            + Constants.secondPartAll)
                }).start()
                return true
            }
            R.id.delete_subreddit -> {
                val msg = getString(R.string.sure_remove_string) +
                        postsController.getCurrentSubreddit() + " subreddit?"
                alert(msg) {
                    title = getString(R.string.removing_subreddit_string)
                    yesButton {
                        Thread(Runnable {
                            if (!SingletonServiceManager.isMyServiceRunning) {
                                postsController.deleteActiveSubredditFromSQL()
                                updateRedditTimeLine(1)
                                runOnUiThread {
                                    refreshGroupInNavView(GROUP_ID1)
                                    Toast.makeText(this@NavActivity, getString(R.string.subreddit_removed_string),
                                            Toast.LENGTH_LONG).show()
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this@NavActivity,
                                            getString(R.string.wait_error_string), Toast.LENGTH_LONG).show()
                                }
                            }
                        }).start()
                    }
                    noButton { }
                }.show()
                return true
            }
            R.id.delete_backup -> {
                if (SingletonServiceManager.isMyServiceRunning &&
                        SingletonServiceManager.currentSubredditDownloaded == postsController.getCurrentSubreddit()) {
                    Toast.makeText(this@NavActivity, getString(R.string.backup_error_string),
                            Toast.LENGTH_LONG).show()
                } else {
                    val msg = getString(R.string.sure_remove_string) +
                            postsController.getCurrentSubreddit() + getString(R.string.subreddit_backup)
                    alert(msg) {
                        title = getString(R.string.removing_backup_subreddit_string)
                        yesButton {
                            Thread(Runnable {
                                postsController.deleteArchivedSubredditFromSQL()
                                updateRedditTimeLine(1)
                                runOnUiThread {
                                    refreshGroupInNavView(GROUP_ID2)
                                    Toast.makeText(this@NavActivity, getString(R.string.backup_subreddit_deleted),
                                            Toast.LENGTH_LONG).show()
                                }
                            }).start()
                        }
                        noButton { }
                    }.show()
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.add_a_subreddit -> {
                val i = Intent(this@NavActivity, AddSubredditActivity::class.java)
                startActivityForResult(i, REQUEST_CODE)
            }
            R.id.about_id -> {
                val i = Intent(this, AboutActivity::class.java)
                startActivityForResult(i, REQUEST_CODE)
            }
            else -> {
                when (item.itemId) {
                    R.id.all_subreddits -> {
                        Thread(Runnable {
                            updateRedditTimeLine(1)
                        }).start()
                    }
                    R.id.popular_subreddits -> {
                        Thread(Runnable {
                            updateRedditTimeLine(2, "popular")
                        }).start()
                    }
                    else -> {
                        when (item.groupId) {
                            GROUP_ID1 -> {
                                Thread(Runnable {
                                    updateRedditTimeLine(2, item.title.toString())
                                }).start()
                            }
                            GROUP_ID2 -> {
                                Thread(Runnable {
                                    updateRedditTimeLine(3, item.title.toString())
                                }).start()
                            }
                        }
                    }
                }
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Thread(Runnable {
            runOnUiThread {
                WindowControl.disableAndDimWindow(this@NavActivity)
                progress_layout.visibility = View.VISIBLE
            }
            if (requestCode == REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    val subreddit = data?.extras?.getString("subreddit")
                    if (subreddit != null && !postsController.sqlHelper.checkIfAsubredditExistsInActiveTable(subreddit)) {
                        postsController.sqlHelper.addToActiveSubsTable(subreddit)
                        runOnUiThread {
                            val activeSubs = postsController.sqlHelper.getActiveSubsTable()
                            nav_view.getMenu().findItem(R.id.all_subreddits).isVisible = activeSubs.size > 1
                            val myMoveGroupItem = nav_view.menu.findItem(R.id.active_subreddits_id)
                            val subMenu = myMoveGroupItem.subMenu
                            subMenu.add(GROUP_ID1, Menu.NONE, Menu.NONE, subreddit)
                        }
                    }
                    val saveOption = data?.extras?.getString("checkboxString")
                    val numberOfSavedPosts = data?.extras?.getString("checkboxString2")
                    if (saveOption != null && numberOfSavedPosts != null && !saveOption.contains("Don\'t")
                            && !postsController.sqlHelper.checkIfAsubredditExistsInArchivedTable(subreddit!!)) {
                        runOnUiThread {
                            val myMoveGroupItem = nav_view.menu.findItem(R.id.arc_subreddits_id)
                            val subMenu = myMoveGroupItem.subMenu
                            subMenu.add(GROUP_ID2, Menu.NONE, Menu.NONE, subreddit)
                        }
                        postsController.archivePosts(subreddit, saveOption, numberOfSavedPosts)
                        updateRedditTimeLine(3, subreddit)
                    } else {
                        if (subreddit != null) {
                            updateRedditTimeLine(2, subreddit)
                        }
                    }
                } else if (resultCode == RESULT_CONSENT_REVOKE) {
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()
                }
            }
            runOnUiThread {
                WindowControl.enableAndBrightWindow(this@NavActivity)
                progress_layout.visibility = View.INVISIBLE
            }
        }).start()
    }

    fun notifyAdapter() {
        runOnUiThread {
            postsAdapter?.notifyDataSetChanged()
        }
    }

    private fun menuItemsActiveMode(enableDeleteSubreddit: Boolean) {
        runOnUiThread {
            menu?.findItem(R.id.sort_hot)?.isVisible = true
            menu?.findItem(R.id.sort_new)?.isVisible = true
            menu?.findItem(R.id.sort_top_all)?.isVisible = true
            menu?.findItem(R.id.sort_top_day)?.isVisible = true
            menu?.findItem(R.id.sort_top_month)?.isVisible = true
            menu?.findItem(R.id.sort_top_week)?.isVisible = true
            menu?.findItem(R.id.sort_top_year)?.isVisible = true
            menu?.findItem(R.id.delete_backup)?.isVisible = false
            menu?.findItem(R.id.delete_subreddit)?.isVisible = enableDeleteSubreddit
        }
    }

    private fun menuItemsArchivedMode() {
        runOnUiThread {
            menu?.findItem(R.id.sort_hot)?.isVisible = false
            menu?.findItem(R.id.sort_new)?.isVisible = false
            menu?.findItem(R.id.sort_top_all)?.isVisible = false
            menu?.findItem(R.id.sort_top_day)?.isVisible = false
            menu?.findItem(R.id.sort_top_month)?.isVisible = false
            menu?.findItem(R.id.sort_top_week)?.isVisible = false
            menu?.findItem(R.id.sort_top_year)?.isVisible = false
            menu?.findItem(R.id.delete_subreddit)?.isVisible = false
            menu?.findItem(R.id.delete_backup)?.isVisible = true
        }
    }

    private fun disableAllMenuItems() {
        runOnUiThread {
            menu?.findItem(R.id.sort_hot)?.isVisible = false
            menu?.findItem(R.id.sort_new)?.isVisible = false
            menu?.findItem(R.id.sort_top_all)?.isVisible = false
            menu?.findItem(R.id.sort_top_day)?.isVisible = false
            menu?.findItem(R.id.sort_top_month)?.isVisible = false
            menu?.findItem(R.id.sort_top_week)?.isVisible = false
            menu?.findItem(R.id.sort_top_year)?.isVisible = false
            menu?.findItem(R.id.delete_subreddit)?.isVisible = false
            menu?.findItem(R.id.delete_backup)?.isVisible = false
        }
    }

    fun populateNavView() {
        var activeSubGroup = nav_view.menu.findItem(R.id.active_subreddits_id)
        if (activeSubGroup != null) {
            val submenu = activeSubGroup.subMenu
            val activeSubs = postsController.sqlHelper.getActiveSubsTable()
            nav_view.getMenu().findItem(R.id.all_subreddits).isVisible = activeSubs.size > 1
            for (item in activeSubs) {
                submenu.add(GROUP_ID1, Menu.NONE, Menu.NONE, item)
            }
        }
        activeSubGroup = nav_view.menu.findItem(R.id.arc_subreddits_id)
        if (activeSubGroup != null) {
            val submenu = activeSubGroup.subMenu
            val archSubs = postsController.sqlHelper.getArchivedSubsTable()
            for (item in archSubs) {
                submenu.add(GROUP_ID2, Menu.NONE, Menu.NONE, item)
            }
        }
    }

    private fun refreshGroupInNavView(groupID: Int) {
        if (groupID == GROUP_ID1) {
            val myMoveGroupItem = nav_view.getMenu().findItem(R.id.active_subreddits_id)
            val subMenu = myMoveGroupItem.getSubMenu()
            subMenu.removeGroup(GROUP_ID1)
            val activeSubs = postsController.sqlHelper.getActiveSubsTable()
            nav_view.getMenu().findItem(R.id.all_subreddits).isVisible = activeSubs.size > 1
            for (item in activeSubs) {
                subMenu.add(GROUP_ID1, Menu.NONE, Menu.NONE, item)
            }
        } else {
            val myMoveGroupItem = nav_view.getMenu().findItem(R.id.arc_subreddits_id)
            val subMenu = myMoveGroupItem.getSubMenu()
            subMenu.removeGroup(GROUP_ID2)
            val archSubs = postsController.sqlHelper.getArchivedSubsTable()
            for (item in archSubs) {
                subMenu.add(GROUP_ID2, Menu.NONE, Menu.NONE, item)
            }
        }
    }

    private fun enableMenuItems(mode: Int) {
        runOnUiThread {
            when (mode) {
                1 -> menuItemsActiveMode(false)
                2 -> menuItemsActiveMode(true)
                3 -> menuItemsArchivedMode()
            }
        }
    }

    private fun updateRedditTimeLine(subredditMode: Int, subreddit: String = "",
                                     parameters: String = Constants.limit25, append: Boolean = false) {
        // 1 - All, 2 - active subreddit, 3 - archived subreddit
        runOnUiThread {
            disableAllMenuItems()
            if (!append) {
                no_posts_yet_id.visibility = View.GONE
                progress_layout.visibility = View.VISIBLE
            }
            if (subreddit.trim() == "" || subreddit.contains("+")) {
                setTitle("Topace For Reddit")
            } else {
                setTitle("/r/$subreddit")
            }
        }
        try {
            if (!parameters.contains("after=t3_")) {
                runOnUiThread {
                    recycler_view.visibility = View.INVISIBLE
                    recycler_view.scrollToPosition(0)
                }
            }
            when (subredditMode) {
                1 -> {
                    postsController.updateRVWithActiveSubreddits(parameters, append)
                }
                2 -> {
                    postsController.updateRVWithActiveSubreddit(subreddit, parameters, append)
                }
                3 -> {
                    postsController.updateRVWithArchivedSubreddit(subreddit)
                }
            }
            notifyAdapter()
            runOnUiThread {
                recycler_view.visibility = View.VISIBLE
                progress_layout.visibility = View.GONE
            }
        } catch (e: Exception) {
            runOnUiThread {
                progress_layout.visibility = View.GONE
                loading_more_progress_bar.visibility = View.GONE
                if (e.message?.contains("connection")!!
                        || e.message?.contains("posts")!!
                        || e.message?.contains("Unknown")!!)
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                else Log.i("NavActivity", e.message)
            }
        }
        // An exception to /r/popular
        val mode = if (subreddit.toLowerCase() == "popular") 1 else subredditMode
        enableMenuItems(mode)
        runOnUiThread {
            if (postsAdapter?.itemCount == 0) {
                no_posts_yet_id.visibility = View.VISIBLE
            }
        }
    }
}