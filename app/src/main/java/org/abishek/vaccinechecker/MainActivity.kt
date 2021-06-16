package org.abishek.vaccinechecker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.judemanutd.autostarter.AutoStartPermissionHelper
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
                val importance = NotificationManager.IMPORTANCE_MIN
                val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, "Main", importance).apply {
                    description = "All notifications and alerts except the vaccine found notification"
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

    fun startChecking(identifier: String, category_18: Boolean, category_45: Boolean, search_with: Int, dose: Int){
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
        /* Cancel the current alarm manager pending intent so that the new one can be set */
        if(currentPendingIntentMode == Constants.ConstantSharedPreferences.mode_set) {
            cancelAllChecking()
        }
        sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.mode,
            Constants.ConstantSharedPreferences.mode_set)
        sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.search_with, search_with)
        val category: String?
        if(category_45) {
            category = "45"
        } else {
            category = "18"
        }
        sharedPreferencesEditor.putString(Constants.ConstantSharedPreferences.category, category)
        sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.dose_mode, dose)
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
        /* Is autostarup permission needed? */
        if(AutoStartPermissionHelper.getInstance().isAutoStartPermissionAvailable(this)) {
            if(
                sharedPreferences.getInt(
                    Constants.ConstantSharedPreferences.autostart,
                    Constants.ConstantSharedPreferences.autostart_disabled) !=
                        Constants.ConstantSharedPreferences.autostart_enabled
            ) {
                enableAutoStart()
            }
        }
        /* I hate the doze mode */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val powerManager: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if(!powerManager.isIgnoringBatteryOptimizations(packageName)){
                MaterialAlertDialogBuilder(this)
                    .setTitle("Permission required")
                    .setMessage("In order for the app to run in background, please allow Vaccine Finder to run unrestrictedly in the background.")
                    .setPositiveButton("OK") { dialog, which ->
                        val powerIntent = Intent().apply {
                            setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(powerIntent)
                    }
                    .show()
            }
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
                // Checks if the device is on a metered network
                if (isActiveNetworkMetered) {
                    // Checks user’s Data Saver settings.
                    when (restrictBackgroundStatus) {
                        RESTRICT_BACKGROUND_STATUS_ENABLED -> {
                            showBackgroundDataDialogue()
                        }
                        RESTRICT_BACKGROUND_STATUS_DISABLED -> {
                            showBackgroundDataDialogue()
                        }
                    }
                }
            }
        }
        /* Transition to current status fragment*/
        transitionToCurrentStatus()
    }

    fun showBackgroundDataDialogue() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission required")
            .setMessage("In order for the app to run in background, please allow Vaccine Finder to connect to the COWIN API in the background")
            .setPositiveButton("OK") { dialog, which ->
                val dataIntent = Intent().apply {
                    setAction(android.provider.Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS)
                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setData(Uri.parse("package:" + packageName))
                }
                startActivity(dataIntent)
            }
            .show()
    }

    fun enableAutoStart() {
        /* We have autostart settings but it is not enabled. let's show a dialog */
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission required")
            .setMessage("In order for the app to run in background, please give Vaccine Finder auto start permission in the screen that appears after pressing ok.")
            .setPositiveButton("OK") { dialog, which ->
                if(AutoStartPermissionHelper.getInstance().getAutoStartPermission(this)) {
                    sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.autostart, Constants.ConstantSharedPreferences.autostart_enabled)
                    sharedPreferencesEditor.apply()
                }

            }
            .show()
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
        val return_string: String
        if(sharedPreferences.getInt(Constants.ConstantSharedPreferences.search_with,
                Constants.SearchWith.pincode ) == Constants.SearchWith.pincode) {
            return_string = "We are actively searching for dose " +
                    sharedPreferences.getInt(Constants.ConstantSharedPreferences.dose_mode, Constants.ConstantSharedPreferences.dose1).toString() +
                    " vaccines based on the pincode " +
                    sharedPreferences.getString(Constants.ConstantSharedPreferences.pincode, "0")
        } else {
            return_string = "We are actively searching for dose " +
                    sharedPreferences.getInt(Constants.ConstantSharedPreferences.dose_mode, Constants.ConstantSharedPreferences.dose1).toString() +
                    "vaccines based on the district " +
                    Constants.district_names.get(
                        Constants.district_ids.indexOf(
                        sharedPreferences.getString(
                            Constants.ConstantSharedPreferences.district_id, "0"
                        ))
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