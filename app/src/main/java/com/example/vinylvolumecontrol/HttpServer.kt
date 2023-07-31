package com.example.vinylvolumecontrol

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.media.AudioManager
import android.os.Bundle
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Matcher
import java.util.regex.Pattern


class HttpServer(audio: AudioManager?, ctx: Context?) : Thread() {
    private var audioManager: AudioManager? = null
    private var context: Context? = null

    init {
        try {
            audioManager = audio
            context = ctx
        } catch (er: Exception) {
            er.printStackTrace()
        }
    }

    private fun isPrivateAddress(ip: String?): Boolean {
        if (ip != null && !ip.isEmpty()) {
            val pattern: Pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.\\d+\\.\\d+")
            val matcher: Matcher = pattern.matcher(ip)
            if (matcher.find()) {
                if (matcher.groupCount() > 1) {
                    val firstMemberString = matcher.group(1)
                    val secondMemberString = matcher.group(2)
                    if (firstMemberString != null && secondMemberString != null) {
                        val firstMember = firstMemberString.toInt()
                        val secondMember = secondMemberString.toInt()
                        // As pointed out to me by Joe Harrison, I have to check for all three valid private ip ranges, as detailed here https://en.wikipedia.org/wiki/Private_network#Private_IPv4_addresses
                        return firstMember == 10 || firstMember == 172 && 16 <= secondMember && secondMember <= 31 || firstMember == 192 && secondMember == 168 // 192.168.0.0 – 192.168.255.255
                    }
                }
            }
        }
        return false
    }

    private fun notify_observers() {
//        val urlUpdatedIntent = Intent("com.tanaka42.webremotevolumecontrol.urlupdated")
        val urlUpdatedIntent = Intent("com.example.vinylvolumecontrol.urlupdated")
        val extras = Bundle()
        extras.putString("url", "http://" + server_ip + ":" + server_port)
        extras.putString("ip", server_ip)
        extras.putInt("port", server_port)
        extras.putBoolean("is_a_private_ip", is_a_private_ip_address)
        urlUpdatedIntent.putExtras(extras)
        context!!.sendBroadcast(urlUpdatedIntent)
    }

    override fun run() {
        //System.out.println("Starting server ...");

        // Determine local network IP address
        try {
            val socket = DatagramSocket()
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
            server_ip = socket.localAddress.hostAddress
            socket.disconnect()
        } catch (ignored: SocketException) {

        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }

        // Check if determined IP address is a private one (a local network one, not an internet one).
        // As pointed out to me by Joe Harrison, 192.168.*.* is not the only private range, 10.*.*.* and 172.16.0.0 - 172.31.255.255 are private too.
        is_a_private_ip_address = isPrivateAddress(server_ip)

        // update Activity
        notify_observers()

        // Start accepting request and responding
        if (server_ip != null && is_a_private_ip_address) {
            try {
                val addr = InetAddress.getByName(server_ip)
                serverSocket = ServerSocket(server_port, 100, addr)
                serverSocket!!.soTimeout = 5000
                isStarted = true
                //System.out.println("Server started : listening.");
                while (isStarted) {
                    try {
                        val newSocket = serverSocket!!.accept()
                        val newClient: Thread = ClientThread(newSocket)
                        newClient.start()
                    } catch (ignored: SocketTimeoutException) {
                    } catch (ignored: IOException) {
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isStarted = false
        context!!.stopService(Intent(context, ForegroundService::class.java))
        // update Activity
        notify_observers()
        //System.out.println("Server stopped");
    }

    inner class ClientThread(protected var socket: Socket) : Thread() {
        private var content_type = ""
        private var status_code: String? = null
        override fun run() {
            try {
                var `in`: DataInputStream? = null
                var out: DataOutputStream? = null
                if (socket.isConnected) {
                    try {
                        `in` = DataInputStream(socket.getInputStream())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    try {
                        out = DataOutputStream(socket.getOutputStream())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                val data = ByteArray(1500)
                if (`in` != null) {
                    while (`in`.read(data) != -1) {
                        val recData = String(data).trim { it <= ' ' }
                        val header =
                            recData.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        val h1 = header[0].split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        var requestedFile = ""
                        if (h1.size > 1) {
                            val requestLocation = h1[1]
                            status_code = "200"
                            when (requestLocation) {
                                "/volume-up" -> {

                                    audioManager!!.adjustVolume(
                                        AudioManager.ADJUST_RAISE,
                                        AudioManager.FLAG_PLAY_SOUND
                                    )
                                }

                                "/volume-down" ->
                                {
                                    audioManager!!.adjustVolume(
                                        AudioManager.ADJUST_LOWER,
                                        AudioManager.FLAG_PLAY_SOUND
                                    )
                                }

                                "/volume-up.png", "/volume-down.png" -> {
                                    requestedFile = requestLocation.substring(1)
                                    content_type = "image/png"
                                }

                                "/" -> {
                                    requestedFile = "webremotevolumecontrol_spa.html"
                                    content_type = "text/html"
                                }

                                else -> {
                                    status_code = "404"
                                }
                            }
                        } else {
                            status_code = "404"
                        }
                        var buffer = ByteArray(0)
                        if (!requestedFile.isEmpty()) {
                            val fileStream =
                                context!!.assets.open(requestedFile, AssetManager.ACCESS_BUFFER)
                            val size = fileStream.available()
                            buffer = ByteArray(size)
                            val readResult = fileStream.read(buffer)
                        }
                        writeResponse(
                            out, buffer.size.toString() + "", buffer,
                            status_code!!, content_type
                        )
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    protected fun printHeader(pw: PrintWriter, key: String?, value: String?) {
        pw.append(key).append(": ").append(value).append("\r\n")
    }

    private fun writeResponse(
        output: DataOutputStream?,
        size: String,
        data: ByteArray,
        status_code: String,
        content_type: String,
    ) {
        try {
            val gmtFrmt = SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US)
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"))
            val pw = PrintWriter(BufferedWriter(OutputStreamWriter(output)), false)
            pw.append("HTTP/1.1 ").append(status_code).append(" \r\n")
            if (!content_type.isEmpty()) {
                printHeader(pw, "Content-Type", content_type)
            }
            printHeader(pw, "Date", gmtFrmt.format(Date()))
            printHeader(pw, "Connection", "close")
            printHeader(pw, "Content-Length", size)
            printHeader(pw, "Server", server_ip)
            pw.append("\r\n")
            pw.flush()
            when (content_type) {
                "text/html" -> pw.append(String(data))
                "image/png" -> {
                    output!!.write(data)
                    output.flush()
                }
            }
            pw.flush()
            //pw.close();
        } catch (er: Exception) {
            er.printStackTrace()
        }
    }

    companion object {
        private var serverSocket: ServerSocket? = null
        private var server_ip: String? = null
        private const val server_port = 9000
        private var is_a_private_ip_address = false
        var isStarted = false
            private set

        fun stopServer() {
            if (isStarted) {
                try {
                    //System.out.println("Stopping server ...");
                    isStarted = false
                    serverSocket!!.close()
                } catch (er: IOException) {
                    er.printStackTrace()
                }
            }
        }
    }
}
