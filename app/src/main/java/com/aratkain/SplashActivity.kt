package com.aratkain

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.core.utils.SessionManager
import com.aratkain.dashboard.DashboardActivity
import com.aratkain.login.LoginActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    // Same backend the rest of the app talks to (see EstablishmentsApiClient).
    // The dedicated client (longer read timeout) exists because a cold-started
    // Render instance holds the ping connection for up to ~100 s before
    // responding; the response is ignored anyway.
    private val renderHealthUrl = "https://aratkain-backend.onrender.com/api/auth/health"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        wakeBackend()

        val session = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = if (session.isLoggedIn()) {
                Intent(this, DashboardActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 2000)
    }

    /**
     * Fire-and-forget ping: starts waking the sleeping free-tier backend while
     * the splash shows instead of letting the first dashboard API call wait
     * 1-2 minutes. Failures are deliberately ignored - this is a nudge, not a
     * health check. (A workflow in the backend repo keeps it warm on a timer.)
     */
    private fun wakeBackend() {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        client.newCall(Request.Builder().url(renderHealthUrl).build())
            .enqueue(object : Callback {

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                }

                override fun onFailure(call: Call, e: IOException) {
                    // ignored on purpose
                }
            })
    }
}
