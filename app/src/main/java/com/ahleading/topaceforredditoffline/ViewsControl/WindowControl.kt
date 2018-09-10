package com.ahleading.topaceforredditoffline.ViewsControl

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager

class WindowControl {
    companion object {
        fun disableAndDimWindow(activity: Activity) {
            activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            val root = activity.window.decorView.rootView as ViewGroup
            applyDim(root, 0.15f)
        }

        fun enableAndBrightWindow(activity: Activity) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            val root = activity.window.decorView.rootView as ViewGroup
            clearDim(root)
        }

        private fun applyDim(parent: ViewGroup, dimAmount: Float) {
            val dim = ColorDrawable(Color.BLACK)
            dim.setBounds(0, 0, parent.width, parent.height)
            dim.alpha = (255 * dimAmount).toInt()
            val overlay = parent.overlay
            overlay.add(dim)
        }

        private fun clearDim(parent: ViewGroup) {
            val overlay = parent.overlay
            overlay.clear()
        }
    }
}