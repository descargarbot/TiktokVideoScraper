package com.tiktokvideoscraper.main

import okhttp3.*
import okhttp3.JavaNetCookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.tiktokvideoscraper.jsonutils.parseJson
import java.io.File
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.regex.Pattern
import kotlin.random.Random

class TikTokVideoScraperMobile {
    private var client: OkHttpClient
    private val headers: Headers
    private val tiktokRegex = """https?://www\.tiktok\.com/(?:embed|@([\w\.-]+)?/video)/(\d+)""".toRegex()

    init {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
        val cookieJar = JavaNetCookieJar(cookieManager)

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()

        headers = Headers.Builder()
            .add("content-type", "application/x-www-form-urlencoded")
            .add("User-Agent", "com.zhiliaoapp.musically/2023501030 (Linux; U; Android 14; en_US; Pixel 8 Pro; Build/TP1A.220624.014;tt-ok/3.12.13.4-tiktok)")
            .add("x-argus", "")
            .build()
    }

    fun setProxy(protocol: String, ip: String, port: Int) {
        val proxyType = when (protocol.lowercase()) {
            "http", "https" -> Proxy.Type.HTTP
            "socks4", "socks5" -> Proxy.Type.SOCKS
            else -> throw IllegalArgumentException("Unsupported proxy protocol: $protocol")
        }

        val proxyAddress = InetSocketAddress.createUnresolved(ip, port)
        val proxy = Proxy(proxyType, proxyAddress)

        client = client.newBuilder()
            .proxy(proxy)
            .build()

        println("Proxy set to $protocol://$ip:$port")
    }

    fun getVideoIdByUrl(tiktokUrl: String): String {
        var finalUrl = tiktokUrl
        if ("vm." in tiktokUrl || "vt." in tiktokUrl || "/t/" in tiktokUrl) {
            val request = Request.Builder()
                .url(tiktokUrl)
                .headers(headers)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                finalUrl = response.request.url.toString()
            }
        }

        val matchResult = tiktokRegex.find(finalUrl)
            ?: throw IllegalArgumentException("Invalid TikTok URL")
        return matchResult.groupValues[2]
    }

    fun getVideoDataByVideoId(videoId: String): Pair<String, String> {

        val iidDidUrl = "https://cdn.jsdelivr.net/gh/descargarbot/tiktok-video-scraper-mobile@main/ids.json"
        val request = Request.Builder().url(iidDidUrl).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Error downloading ids.json")

        val idsJson = parseJson(response.body?.string() ?: throw IOException("Response body is null"))
        val iidDidList = mutableListOf<Map<String, String>>()

        for (i in 0 until idsJson.size) {
            val iid = idsJson[i]["iid"].asString()
            val deviceId = idsJson[i]["device_id"].asString()
            iidDidList.add(mapOf("iid" to iid, "device_id" to deviceId))
        }

        val tiktokVideoDataEndpoint = "https://api16-normal-c-useast1a.tiktokv.com/aweme/v1/multi/aweme/detail/"
        val params = mutableMapOf(
            "channel" to "googleplay",
            "aid" to "1233",
            "app_name" to "musical_ly",
            "version_code" to "350103",
            "version_name" to "35.1.3",
            "device_platform" to "android",
            "device_type" to "Pixel 8 Pro",
            "os_version" to "14",
            "aweme_ids" to "[${videoId}]"
        )

        var running = true
        var tryCount = 1

        while (running) {
            if (iidDidList.isNotEmpty()) {
                val tryIidDid = iidDidList.random()
                params["iid"] = tryIidDid["iid"]!!
                params["device_id"] = tryIidDid["device_id"]!!

                val formBodyBuilder = FormBody.Builder()
                params.forEach { (key, value) ->
                    formBodyBuilder.add(key, value)
                }
                val formBody = formBodyBuilder.build()

                val requestBuilder = Request.Builder()
                    .url(tiktokVideoDataEndpoint)
                    .headers(headers)
                    .post(formBody)
                    .build()

                try {
                    val videoResponse = client.newCall(requestBuilder).execute()
                    val jsonVideoData = parseJson(videoResponse.body?.string() ?: throw IOException("Response body is null"))

                    val videoUrl = jsonVideoData["aweme_details"][0]["video"]["bit_rate"][0]["play_addr"]["url_list"][0].asString()
                    val thumbnail = jsonVideoData["aweme_details"][0]["video"]["dynamic_cover"]["url_list"][0].asString()

                    return Pair(videoUrl, thumbnail)
                } catch (e: Exception) {
                    if (tryCount > 4) {
                        println("fail: ${params["iid"]}, ${params["device_id"]}")
                        iidDidList.remove(tryIidDid)
                        tryCount = 1
                    } else {
                        println("Error, retry: $tryCount")
                        tryCount += 1
                    }
                }
            } else {
                throw IOException("No valid pairs")
            }
        }
        throw IOException("Error getting video data")
    }


    fun download(tiktokVideoUrl: String, videoId: String): List<String> {
        val request = Request.Builder()
            .url(tiktokVideoUrl)
            .headers(headers)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val pathFilename = "$videoId.mp4"
            val file = File(pathFilename)
            file.outputStream().use { fileOutputStream ->
                response.body?.byteStream()?.copyTo(fileOutputStream)
            }

            return listOf(pathFilename)
        }
    }

    fun getVideoFilesize(videoUrl: String): String {
        val request = Request.Builder()
            .url(videoUrl)
            .headers(headers)
            .head()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            return response.header("content-length") ?: throw IOException("Content-Length not found")
        }
    }
}

fun main() {
    // set ur tt video url
    val tiktokUrl = ""

    val tiktokVideo = TikTokVideoScraperMobile()

    // Set proxy if needed
    // tiktokVideo.setProxy("socks5", "157.230.250.185", 2144)

    val videoId = tiktokVideo.getVideoIdByUrl(tiktokUrl)
    
    val (tiktokVideoUrl, videoThumbnail) = tiktokVideo.getVideoDataByVideoId(videoId)

    val videoSize = tiktokVideo.getVideoFilesize(tiktokVideoUrl)
    println("filesize: ~$videoSize bytes")

    val downloadedVideoList = tiktokVideo.download(tiktokVideoUrl, videoId)
    println("Downloaded files: $downloadedVideoList")
}
