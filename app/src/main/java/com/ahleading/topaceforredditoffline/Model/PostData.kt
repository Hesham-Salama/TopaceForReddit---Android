package com.ahleading.topaceforredditoffline.Model

//Included: title,subreddit,score,thumbnail,createdUTC,author,noOfComments,url,isover18

class PostData(val mTitle: String, val mSubreddit: String, val mScore: Int, val mThumbnailLink: String,
               val mCreatedUTC: Long, val mAuthor: String, val mNoOfComments: Int,
               val mUrl: String, val mPermalink: String, val mSelfText_HTML: String,
               val mThumbnailStorage: String?, val image_src_url: String?, val mImageSrcStorage: String?
               , val mDomain: String)