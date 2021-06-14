package org.abishek.vaccinechecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import org.abishek.vaccinechecker.services.broadcastIntent


class VaccineFoundActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vaccine_found)
        val center_name = intent.getStringExtra("place")
        val center_address = intent.getStringExtra("address")
        val capacity = intent.getIntExtra("capacity", 0).toString()
        val dateFound = intent.getStringExtra("date")
        var final_text = "We found "+capacity+" vaccine(s) that are available at "+dateFound
        if(center_name?.isNotEmpty() == true) {
            final_text = final_text + " at " + center_name + ", " + center_address
        }
        findViewById<TextView>(R.id.textview_vaccine_found).text = final_text

        val button_book_now = findViewById<Button>(R.id.button_book_vaccine)
        button_book_now.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://selfregistration.cowin.gov.in"))
            startActivity(browserIntent)
        }

        /* cancel scheduled tasks and set shared preference */
        val sharedPreferencesEditor = getSharedPreferences(Constants.ConstantSharedPreferences.name, MODE_PRIVATE).edit()
        val alarmMgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val broadcastIntent = Intent(this, broadcastIntent::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmMgr?.cancel(pendingIntent)
        sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.mode,
            Constants.ConstantSharedPreferences.mode_found)
        sharedPreferencesEditor.putString(Constants.ConstantSharedPreferences.center_name, center_name)
        sharedPreferencesEditor.putString(Constants.ConstantSharedPreferences.center_address, center_address)
        sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.capacity, capacity.toInt())

        sharedPreferencesEditor.commit()
        NotificationManagerCompat.from(this).cancel(Constants.NOTIFICATION_ID)

        val button_cancel_vaccine_found = findViewById<Button>(R.id.button_cancel_vaccine_found)
        button_cancel_vaccine_found.setOnClickListener {
            sharedPreferencesEditor.putInt(Constants.ConstantSharedPreferences.mode, Constants.ConstantSharedPreferences.mode_set_found)
            sharedPreferencesEditor.commit()
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
        }
    }
}