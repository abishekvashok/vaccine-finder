package org.abishek.vaccinechecker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import org.abishek.vaccinechecker.fragments.CurrentStatusFragment
import org.abishek.vaccinechecker.fragments.StartCheckingFragment
import org.abishek.vaccinechecker.fragments.onBoardingFragment
import org.abishek.vaccinechecker.services.broadcastIntent


class MainActivity : AppCompatActivity() {
    private var alarmMgr: AlarmManager? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences(Constants.ConstantSharedPreferences.name, MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()
        alarmMgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val broadcastIntent = Intent(this, broadcastIntent::class.java)
        pendingIntent =
            PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if(sharedPreferences.getInt(Constants.ConstantSharedPreferences.mode, Constants.ConstantSharedPreferences.mode_set)
            == Constants.ConstantSharedPreferences.mode_set_found
        ) {
            alarmMgr?.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 10000,
                60000,
                pendingIntent
            )
            val pendingIntentNotif: PendingIntent = PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java), 0)
            val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle("Actively searching for vaccines")
                .setContentText("Will notify you when we find an open slot")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendingIntentNotif)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, "Main", importance).apply {
                    description = "All notifications and alerts"
                }
                // Register the channel with the system
                val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
            with(NotificationManagerCompat.from(this)) {
                notify(Constants.NOTIFICATION_ID, builder.build())
            }
            sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.mode, Constants.ConstantSharedPreferences.mode_set)
            sharedPreferencesEditor.commit()
        }
        navigateAsSharedPreference()
    }

    fun transitionToStartChecking(){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.framelayout_fragment_holder, StartCheckingFragment())
        transaction.commit()
    }

    fun transitionToCurrentStatus() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.framelayout_fragment_holder, CurrentStatusFragment())
        transaction.commit()
    }

    fun cancelAllChecking() {
        alarmMgr?.cancel(pendingIntent)
        sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.mode,
            Constants.ConstantSharedPreferences.mode_unset)
        sharedPreferencesEditor.commit()
        NotificationManagerCompat.from(this).cancel(Constants.NOTIFICATION_ID)
    }

    fun startChecking(identifier: String, category_18: Boolean, category_45: Boolean, search_with: Int){
        val currentPendingIntentMode = sharedPreferences.getInt(
            Constants.ConstantSharedPreferences.mode, Constants.ConstantSharedPreferences.mode_unset)
        if(search_with == Constants.SearchWith.pincode) {
            sharedPreferencesEditor.putString(
                Constants.ConstantSharedPreferences.pincode,
                identifier
            )
        } else {
            sharedPreferencesEditor.putString(
                Constants.ConstantSharedPreferences.district_id,
                identifier
            )
        }
        if(currentPendingIntentMode == Constants.ConstantSharedPreferences.mode_set) {
            cancelAllChecking()
        }
        sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.mode,
            Constants.ConstantSharedPreferences.mode_set)
        sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.search_with, search_with)
        val category: String?
        if(category_18) {
            category = "18"
        } else {
            category = "45"
        }
        sharedPreferencesEditor.putString(Constants.ConstantSharedPreferences.category, category)
        sharedPreferencesEditor.commit()
        val broadcastIntent = Intent(this, broadcastIntent::class.java)
        pendingIntent =
            PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        alarmMgr?.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 10000,
            60000,
            pendingIntent
        )
        /* Build a notification */
        val pendingIntentNotif: PendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), 0)
        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Actively searching for vaccines")
            .setContentText("Will notify you when we find an open slot")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntentNotif)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, "Main", importance).apply {
                description = "All notifications and alerts"
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        with(NotificationManagerCompat.from(this)) {
            notify(Constants.NOTIFICATION_ID, builder.build())
        }
        transitionToCurrentStatus()
    }

    fun navigateAsSharedPreference() {
        val mode = sharedPreferences.getInt(Constants.ConstantSharedPreferences.mode,
            Constants.ConstantSharedPreferences.mode_unset)
        if(mode == Constants.ConstantSharedPreferences.mode_found) {
            val intentFound: Intent = Intent(this, VaccineFoundActivity::class.java).apply {
                putExtra("place", sharedPreferences.getString(Constants.ConstantSharedPreferences.center_name, ""))
                putExtra("address", sharedPreferences.getString(Constants.ConstantSharedPreferences.center_address, ""))
                putExtra("capacity", sharedPreferences.getInt(Constants.ConstantSharedPreferences.capacity, 0))
            }
            startActivity(intentFound)
            finish()
        }
        val fragment: Fragment?
        if(mode == Constants.ConstantSharedPreferences.mode_unset) { /* Not checking for vaccines */
            fragment = onBoardingFragment()
        } else { /* Actively searching for vaccines */
            fragment = CurrentStatusFragment()
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.framelayout_fragment_holder, fragment)
        transaction.commit()
    }

    fun transitionToSettings(){
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun getStatusText(): String{
        val return_string: String?
        if(sharedPreferences.getInt(Constants.ConstantSharedPreferences.search_with,
                Constants.SearchWith.pincode ) == Constants.SearchWith.pincode) {
            return_string = "We are actively searching for vaccines based on the pincode " +
                    sharedPreferences.getString(Constants.ConstantSharedPreferences.pincode, "0")
        } else {
            return_string = "We are actively searching for vaccines based on the district " +
                    Constants.district_names.get(
                        sharedPreferences.getString(
                            Constants.ConstantSharedPreferences.district_id, "0"
                        )!!.toInt()
                    )
        }
        return return_string
    }

    fun shareApp() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Tired searching for vaccines? Here's an app that can search vaccines for you. Download it now from https://abishekvashok.github.io/vaccine-finder/")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }
}