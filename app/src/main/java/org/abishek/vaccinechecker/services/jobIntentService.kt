package org.abishek.vaccinechecker.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.RequestQueue
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.abishek.vaccinechecker.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class jobIntentService: JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        val alarmMgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val broadcastIntent = Intent(this, broadcastIntent::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val sharedPreferences = getSharedPreferences(Constants.ConstantSharedPreferences.name, MODE_PRIVATE)
        val min_age = sharedPreferences.getString(Constants.ConstantSharedPreferences.category, "45")!!.toInt()
        val dose = sharedPreferences.getInt(Constants.ConstantSharedPreferences.dose_mode, Constants.ConstantSharedPreferences.dose1)
        val price_free = sharedPreferences.getBoolean(Constants.ConstantSharedPreferences.price_free, true)
        val price_paid = sharedPreferences.getBoolean(Constants.ConstantSharedPreferences.price_paid, true)
        if(!isNetworkAvailable()) {
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                this, 0, Intent(this, NoNetworkActivity::class.java), 0)
            val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle("Can't search for vaccines")
                .setContentText("Please turn on internet connection to search for vaccines.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
            with(NotificationManagerCompat.from(this)) {
                notify(Constants.NOTIFICATION_ID, builder.build())
            }
            alarmMgr.cancel(pi) // Cancel all next alarms
            return
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), 0)
        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Actively searching for vaccines")
            .setContentText("Will notify you when we find an open slot")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
        with(NotificationManagerCompat.from(this)) {
            notify(Constants.NOTIFICATION_ID, builder.build())
        }
        val gc = GregorianCalendar()
        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy")
        var vaccineFound = false
        var checkDates = 0
        while((checkDates <= 7) && (vaccineFound == false)) {
            val date = dateFormat.format(gc.getTime())
            val mode = sharedPreferences.getInt(
                Constants.ConstantSharedPreferences.search_with,
                Constants.SearchWith.pincode
            )
            val url: String?
            val request: JsonObjectRequest?
            if (mode == Constants.SearchWith.district) {
                url =
                    "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?date=" + date + "&district_id=" + sharedPreferences.getString(
                        Constants.ConstantSharedPreferences.district_id,
                        "000"
                    )
                request = JsonObjectRequest(url, null,
                    {
                        val centers: JSONArray = it.get("centers") as JSONArray
                        var i = 0
                        while (i < centers.length()) {
                            val center_name = (centers.get(i) as JSONObject).get("name").toString()
                            val center_address =
                                (centers.get(i) as JSONObject).get("address").toString()
                            val sessions =
                                (centers.get(i) as JSONObject).get("sessions") as JSONArray
                            val type =
                                (centers.get(i) as JSONObject).get("fee_type") as String
                            var j = 0
                            if(type == "Paid") {
                                if(!price_paid) {
                                    continue
                                }
                            } else if(type == "Free") {
                                if(!price_free) {
                                    continue
                                }
                            }
                            while (j < sessions.length()) {
                                val session = (sessions.get(j) as JSONObject)
                                if (session.get("min_age_limit").toString().toInt() <= min_age) {
                                    val capacity: Int
                                    if(dose == Constants.ConstantSharedPreferences.dose1) {
                                        capacity =
                                            session.get("available_capacity_dose1").toString().toInt()
                                    } else {
                                        capacity =
                                            session.get("available_capacity_dose2").toString().toInt()
                                    }
                                    if (capacity > 0) {
                                        createVaccineFoundNotification(
                                            center_name,
                                            center_address,
                                            capacity,
                                            date.toString(),
                                            dose
                                        )
                                        vaccineFound = true
                                    }
                                }
                                j++
                            }
                            i++;
                        }
                    },
                    {}
                )
            } else {
                url =
                    "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/findByPin?date=" + date + "&pincode=" + sharedPreferences.getString(
                        Constants.ConstantSharedPreferences.pincode,
                        "000000"
                    )
                request = JsonObjectRequest(url, null,
                    {
                        val sessions = it.get("sessions") as JSONArray
                        var j = 0
                        while (j < sessions.length()) {
                            val session = (sessions.get(j) as JSONObject)
                            val type = session.get("fee_type") as String
                            if(type == "Free") {
                                if(!price_free) {
                                    continue
                                }
                            } else if (type == "Paid") {
                                if(!price_paid) {
                                    continue
                                }
                            }
                            if (session.get("min_age_limit").toString().toInt() <= min_age) {
                                val capacity: Int
                                if(dose == Constants.ConstantSharedPreferences.dose1) {
                                    capacity =
                                        session.get("available_capacity_dose1").toString().toInt()
                                } else {
                                    capacity =
                                        session.get("available_capacity_dose2").toString().toInt()
                                }
                                if (capacity > 0) {
                                    createVaccineFoundNotification("", "", capacity, date.toString(), dose)
                                    vaccineFound = true
                                }
                            }
                            j++
                        }
                    },
                    {}
                )
            }
            val requestQueue: RequestQueue = Volley.newRequestQueue(applicationContext)
            requestQueue.add(request)
            checkDates += 1
            gc.add(Calendar.DATE, 1)
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
    fun createVaccineFoundNotification(place: String, address: String, capacity: Int, dateFound: String, dose: Int) {
        val targetIntent = Intent(this, VaccineFoundActivity::class.java).apply {
            putExtra("capacity", capacity)
            putExtra("place", place)
            putExtra("address", address)
            putExtra("date", dateFound)
            putExtra("dose", dose)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, targetIntent, 0)
        /* For custom sound */
        val soundUri =  RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_V_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Vaccine Found!")
            .setContentText("We found a vaccine for you")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setOngoing(false)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_V_ID, "Vaccine found", importance).apply {
                description = "Vaccine Found Notifications"
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            // Registering the channel with the system doesn't change anything.
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        with(NotificationManagerCompat.from(this)) {
            notify(Constants.NOTIFICATION_V_ID, builder.build())
        }
    }
}