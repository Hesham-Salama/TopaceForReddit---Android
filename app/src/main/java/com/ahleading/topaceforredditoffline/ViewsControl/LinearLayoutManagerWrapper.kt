package com.ahleading.topaceforredditoffline.ViewsControl

import android.content.Context
import android.support.v7.widget.LinearLayoutManager


// Solves RecyclerView inconsistency
// https://stackoverflow.com/a/40177879/
class LinearLayoutManagerWrapper : LinearLayoutManager {

    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout) {}

    /**
    "Predictive item animations" are automatically created animations that show where items came from,
    and where they are going to, as items are added, removed, or moved within a layout.

    A LayoutManager wishing to support predictive item animations must override this method to return true
    (the default implementation returns false) and must obey certain behavioral contracts outlined
    in onLayoutChildren(Recycler, State).

    Whether item animations actually occur in a RecyclerView is actually determined by both the return value from this
    method and the ItemAnimator set on the RecyclerView itself. If the RecyclerView has a non-null ItemAnimator
    but this method returns false, then only "simple item animations" will be enabled in the RecyclerView,
    in which views whose position are changing are simply faded in/out. If the RecyclerView has a non-null
    ItemAnimator and this method returns true, then predictive item animations will be enabled in the RecyclerView.
     */
    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }
}