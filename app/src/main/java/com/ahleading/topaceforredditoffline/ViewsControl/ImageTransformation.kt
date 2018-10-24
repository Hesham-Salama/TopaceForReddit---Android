package com.ahleading.topaceforredditoffline.ViewsControl

import android.graphics.Bitmap
import android.widget.ImageView
import com.squareup.picasso.Transformation

// https://stackoverflow.com/a/34261586/7727544
object ImageTransformation {

    var savedBitmap: Bitmap? = null

    fun getBitmap(): Bitmap? {
        return savedBitmap
    }

    fun getTransformation(imageView: ImageView): Transformation {
        return object : Transformation {

            override fun transform(source: Bitmap): Bitmap {
                val targetWidth = imageView.width

                val aspectRatio = source.height.toDouble() / source.width.toDouble()
                val targetHeight = (targetWidth * aspectRatio).toInt()
                val result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false)
                if (result != source) {
                    // Same bitmap is returned if sizes are the same
                    source.recycle()
                }
                savedBitmap = result
                return result
            }

            override fun key(): String {
                return "transformation" + " desiredWidth"
            }
        }
    }
}