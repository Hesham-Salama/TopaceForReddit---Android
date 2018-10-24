package com.ahleading.topaceforredditoffline.Model

class ConstructRedditURL {

    companion object {

        private val firstPartRedditURL = "https://oauth.reddit.com/r/"
        //        private val secondPartRedditURL_TOP = "/top/?sort=top&limit=25&t="
//        private val secondPartRedditURL_TOP_100 = "/top/?sort=top&limit=100&t="
        private val TIME_AMP = "&t="
        private val ARTICLE_AMP = "&article="
        val secondPartRedditURL_TOP = "/top/?sort=top&limit=25"
        val secondPartRedditURL_TOP_100 = "/top/?sort=top&limit=100"
        val secondPartRedditURL_NEW = "/new/?limit=25"
        private val secondPartRedditURL_COMMENT = "/comments/article?&showmore=true"
        val secondPartDay = "&t=day"
        val secondPartWeek = "&t=week"
        val secondPartYear = "&t=year"
        val secondPartMonth = "&t=month"
        val secondPartAll = "&t=all"
        private val aboutSecondPart = "about"
        val limit25 = "?limit=25"
        val limit10 = "?limit=10"
        private val RAW_JSON: String = "raw_json=1"
        private val AFTER_ID = "&after=t3_"
        val newestPostURL = "all/new/?limit=1"

        fun constructURL(subredditAndParams: String): String = firstPartRedditURL + subredditAndParams

        fun addComments(link: String, id: String) = link + secondPartRedditURL_COMMENT + ARTICLE_AMP + id

        fun addAbout(link: String) = "$link/$aboutSecondPart"

        fun addAmpersandOrQuestionMark(link: String): String {
            return if (link[link.length - 1].equals("/")) {
                "$link?"
            } else {
                "$link&"
            }
        }

        fun addRawJsonCompatibility(link: String): String {
            return addAmpersandOrQuestionMark(link) + RAW_JSON
        }

        fun addAfterID(link: String, id: String) = addAmpersandOrQuestionMark(link) + AFTER_ID + id
    }
}