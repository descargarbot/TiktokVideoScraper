package com.tiktokvideoscraper.main

import okhttp3.*
import java.io.IOException

import com.tiktokvideoscraper.scrapers.TikTokVideoScraperWeb
import com.tiktokvideoscraper.scrapers.TikTokVideoScraperMobile


fun runTikTokVideoScraperWeb(tiktokUrl: String) {

    val tiktokVideo = TikTokVideoScraperWeb()

    val videoId = tiktokVideo.getVideoIdByUrl(tiktokUrl)

    val (tiktokVideoUrl, videoThumbnail) = tiktokVideo.getVideoDataByVideoUrl(tiktokUrl)

    val downloadedVideoList = tiktokVideo.download(tiktokVideoUrl, videoId)

}

fun runTikTokVideoScraperMobile(tiktokUrl: String) {

    val tiktokVideo = TikTokVideoScraperMobile()

    val VideoId = tiktokVideo.getVideoIdByUrl(tiktokUrl)
    
    val (tiktokVideoUrls, tiktokVideoThumbnail) = tiktokVideo.getVideoDataByVideoId(VideoId)

    val downloadedVideoList = tiktokVideo.download(tiktokVideoUrls, VideoId)

}

fun main() {
    // set ur tt url
    var tiktokUrl = ""

    try{
        runTikTokVideoScraperWeb(tiktokUrl)
    }catch (e: Exception) {
        runTikTokVideoScraperMobile(tiktokUrl)
    }
    
}
