package org.abishek.vaccinechecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.snackbar.Snackbar
import org.abishek.vaccinechecker.services.broadcastIntent

class NoNetworkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_network)
        val button_try_again = findViewById<Button>(R.id.button_try_again)
        button_try_again.setOnClickListener {
            if(isNetworkAvailable()) {
                /* Rebuild the notification */
                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java), 0)
                val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle("Actively searching for vaccines")
                    .setContentText("Will notify you when we find an open slot")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                with(NotificationManagerCompat.from(this)) {
                    notify(Constants.NOTIFICATION_ID, builder.build())
                }
                /* Rebuild the recurring search process*/
                val alarmMgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val broadcastIntent = Intent(this, broadcastIntent::class.java)
                val pi = PendingIntent.getBroadcast(
                    this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                alarmMgr.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 60000,
                    60000,
                    pi
                )
                finish()
            } else {
                Snackbar.make(findViewById(R.id.no_network_activity),
                    "No internet connection detected. Please try again", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }
}