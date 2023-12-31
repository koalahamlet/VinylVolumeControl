package com.example.vinylvolumecontrol


import android.content.Context
import android.media.AudioManager
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException


class AppServer(audio: AudioManager?, ctx: Context?) : NanoHTTPD(9000) {

    private var audioManager: AudioManager? = null
    private var context: Context? = null

    init {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            audioManager = audio
            context = ctx
        } catch (er: Exception) {
            er.printStackTrace()
        }
    }

    override fun serve(session: IHTTPSession): Response {

        val method = session.method
        val uri = session.uri

        if (method == Method.POST) {
            if (uri == "/volume-up") {
                Timber.d("duck: handling volume up")
                // handle volume up action here
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "VOLUME UP")
            } else if (uri == "/volume-down") {
                Timber.d("duck: handling volume down")
                // handle volume down action here
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "VOLUME DOWN")
            }
        } else if (method == Method.GET) {
            // handle GET request, probably you will return your web page here
                var msg: String? = ""
                try {
                    val inputStream: InputStream? =
                        MyApplication.appContext?.assets?.open("webremotevolumecontrol_spa.html")
                    Timber.d("duck: was it null ${MyApplication.appContext}")
                    Timber.d("duck: was it null ${MyApplication.appContext?.assets}")
                    Timber.d("duck: was it null $inputStream")
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        msg += line
                    }
                    reader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                return newFixedLengthResponse(msg)
        }

        return newFixedLengthResponse(
            Response.Status.NOT_FOUND, MIME_PLAINTEXT,
            "Requested URI not found: $uri"
        )
    }

    fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e("Local IP Address", "Can't get Local IP Address", ex)
        }
        return null
    }
}
