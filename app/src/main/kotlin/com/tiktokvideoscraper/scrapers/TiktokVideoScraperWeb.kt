package com.tiktokvideoscraper.scrapers

import okhttp3.*
import okhttp3.JavaNetCookieJar
import java.io.File
import java.io.IOException
import com.tiktokvideoscraper.jsonutils.parseJson
import com.tiktokvideoscraper.jsonutils.JsonWrapper
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.regex.Pattern

class TikTokVideoScraperWeb {
    private var client: OkHttpClient
    private val headers: Headers

    init {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
        val cookieJar = JavaNetCookieJar(cookieManager)

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()

        headers = Headers.Builder()
            .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36")
            .add("Referer", "https://www.tiktok.com/")
            .build()
    }

    fun setProxy(protocol: String, ip: String, port: Int) {
        val proxyType: Proxy.Type = when (protocol.lowercase()) {
            "http", "https" -> Proxy.Type.HTTP
            "socks4", "socks5" -> Proxy.Type.SOCKS
            else -> throw IllegalArgumentException("Unsupported proxy protocol: $protocol")
        }

        val proxyAddress = InetSocketAddress.createUnresolved(ip, port)
        val proxy = Proxy(proxyType, proxyAddress)

        // Configurar el cliente con el nuevo proxy
        client = client.newBuilder()
            .proxy(proxy)
            .build()

        println("Proxy set to $protocol://$ip:$port")
    }


    fun getVideoDataByVideoUrl(tiktokUrl: String): Pair<String, String> {
        val request = Request.Builder()
            .url(tiktokUrl)
            .headers(headers)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val htmlTiktokWebVideo = response.body?.string() ?: throw IOException("Empty response body")

            val pattern = Pattern.compile(
                "<script\\s+[^>]*id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"[^>]*>\\s*(.*?)\\s*</script>",
                Pattern.DOTALL
            )
            val matcher = pattern.matcher(htmlTiktokWebVideo)

            if (!matcher.find()) throw IOException("__UNIVERSAL_DATA_FOR_REHYDRATION__ error")

            val textVideoData = matcher.group(1)?.trim() ?: throw IOException("No match found")

            // Usando JsonWrapper para analizar el JSON
            val jsonVideoData = parseJson(textVideoData)

            val tiktokThumb = jsonVideoData["__DEFAULT_SCOPE__"]["webapp.video-detail"]["itemInfo"]["itemStruct"]["video"]["dynamicCover"].asString()
            val tiktokVideoUrl = jsonVideoData["__DEFAULT_SCOPE__"]["webapp.video-detail"]["itemInfo"]["itemStruct"]["video"]["playAddr"].asString()

            println("$tiktokVideoUrl, $tiktokThumb")
            return Pair(tiktokVideoUrl, tiktokThumb)
        }
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

    fun getVideoIdByUrl(videoUrl: String): String {
        val urlWithoutQuery = videoUrl.split("?")[0]
        return if (urlWithoutQuery.endsWith("/")) {
            urlWithoutQuery.dropLast(1).split("/").last()
        } else {
            urlWithoutQuery.split("/").last()
        }
    }
}

fun main() {
    // set ur tt video url
    val tiktokUrl = ""

    val tiktokVideo = TikTokVideoScraperWeb()

    // Set proxy if needed - ((str)protocol, (str)ip, (int)port)
    //tiktokVideo.setProxy("https", "157.230.250.185", 2144)

    val videoId = tiktokVideo.getVideoIdByUrl(tiktokUrl)

    val (tiktokVideoUrl, videoThumbnail) = tiktokVideo.getVideoDataByVideoUrl(tiktokUrl)

    val videoSize = tiktokVideo.getVideoFilesize(tiktokVideoUrl)
    println("filesize: ~$videoSize bytes")

    val downloadedVideoList = tiktokVideo.download(tiktokVideoUrl, videoId)
    println("Downloaded files: $downloadedVideoList")
}
