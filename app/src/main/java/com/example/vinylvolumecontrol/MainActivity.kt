package com.example.vinylvolumecontrol

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.vinylvolumecontrol.ui.theme.VinylVolumeControlTheme
import timber.log.Timber
import java.io.IOException


class MainActivity : ComponentActivity() {

    lateinit var blah: Context

    private var server: AppServer? = null

    private var mServerURL = ""
    private var mServerIp = ""
    private var mServerPort = 0
    private var myNum = 0
    private var mServerIpIsPrivate = true

    private val urlUpdatedReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())

        blah = this.application.applicationContext

        getReadyToReceiveURLforUI();

        setContent {
            VinylVolumeControlTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting("Android")

                        Button(

                            onClick = {

                                if (HttpServer.isStarted) {
                                    Timber.d("stopping service")
                                    stopRemoteControlService();
                                } else {
                                    Timber.d("starting service")
                                    startRemoteControlService();
                                }

                            }) {
                            Text(text = "start/stop http server")

                        }
                        Button(
                            onClick = {
                                finish()
                            }) {
                            Text(text = "finish")
                        }

                        Button(
                            onClick = {
                                myNum += 1
                            }) {
                            Text(text = myNum.toString())
                        }
                    }

                }
            }
        }


        updateActivity();

        startRemoteControlService();

    }


    private fun startRemoteControlService() {
        if (Build.VERSION.SDK_INT >= 26) {
            Timber.d("duck: build 26")
//            ContextCompat.startForegroundService(blah, Intent(this, ForegroundService::class.java))
            try {
                Timber.d("duck: trying to get audio manager")
                val audioManager : AudioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
                server = AppServer(audioManager, this)
                val ipAddre = server!!.getLocalIpAddress()
                Timber.d("duck: ipAdress was $ipAddre")
            } catch (ioe: IOException) {
                Timber.w("Httpd The server could not start.")
            }
        } else {
            Timber.d("build < 26")
            startService(Intent(this, ForegroundService::class.java))
        }
        updateActivity()
    }

    private fun stopRemoteControlService() {
        stopService(Intent(this, ForegroundService::class.java))
        updateActivity()
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun getReadyToReceiveURLforUI() {
        val urlUpdatedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                mServerURL = intent.getStringExtra("url") ?: "fakeServerUrl"
                mServerIp = intent.getStringExtra("ip") ?: "fakeServerIP"
                mServerPort = intent.getIntExtra("port", 0)
                mServerIpIsPrivate = intent.getBooleanExtra("is_a_private_ip", true)
                updateActivity()
            }
        }
        registerReceiver(
            urlUpdatedReceiver,
            IntentFilter("com.example.vinylvolumecontrol.urlupdated")
//            IntentFilter("com.tanaka42.webremotevolumecontrol.urlupdated")
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(urlUpdatedReceiver)
    }

    private fun updateActivity() {
        if (HttpServer.isStarted) {

            Timber.d("mServerUrl: $mServerURL")
            Timber.d("mServerIp: $mServerIp")
            Timber.d("mServerPort: $mServerPort")
    //        mEnableDisableButton.setText(R.string.disable_volume_remote_control)
    //        mHowToTextView.setText(R.string.how_to_enabled)
    //        mURLTextView.setText(mServerURL)
    //        mURLTextView.setVisibility(View.VISIBLE)
    //        mCloseHintTextView.setText(R.string.close_when_ready)
    //        mCloseHintTextView.setVisibility(View.VISIBLE)
        } else {
    //        mEnableDisableButton.setText(R.string.enable_volume_remote_control)
    //        if (mServerIpIsPrivate) {
    //            mHowToTextView.setText(R.string.how_to_disabled)
    //            mCloseHintTextView.setVisibility(View.INVISIBLE)
    //            mURLTextView.setVisibility(View.INVISIBLE)
    //        } else {
    //            mCloseHintTextView.setVisibility(View.INVISIBLE)
    //            mURLTextView.setText(R.string.verify_local_network_connection)
    //            mURLTextView.setVisibility(View.VISIBLE)
    //            mCloseHintTextView.setVisibility(View.VISIBLE)
    //            mCloseHintTextView.setText(R.string.about_private_limitation)
    //            mHowToTextView.setText(R.string.how_to_unable_to_find_local_address)
    //        }
        }
    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VinylVolumeControlTheme {
        Greeting("Android")
    }
}
