package com.ahleading.topaceforredditoffline.PostNotificationSchedule

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object ScheduleJobUtility {
    fun scheduleJob(context: Context) {
        val jobInfo: JobInfo
        val jobID = 123
        val networkType = JobInfo.NETWORK_TYPE_ANY
        val serviceComponent = ComponentName(context, PostNotificationJobService::class.java)

        jobInfo = JobInfo.Builder(jobID, serviceComponent)
                .setPeriodic(TimeUnit.HOURS.toMillis(5))
                .setRequiredNetworkType(networkType)
                .setPersisted(true)
                .build()

        val jobScheduler = context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val test = jobScheduler.schedule(jobInfo)
        if (test <= 0) {
            Log.i("JobSchedule", "COULDNOT_SCHEDULE")
        } else {
            val sdf = SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.US)
            val resultDate = Date(Calendar.getInstance().timeInMillis + TimeUnit.MINUTES.toMillis(15))
            Log.i("JobSchedule", "Scheduled at: " + sdf.format(resultDate).toString())
        }
    }
}