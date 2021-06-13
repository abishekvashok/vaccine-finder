package org.abishek.vaccinechecker.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService

class broadcastIntent: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        JobIntentService.enqueueWork(context!!, jobIntentService::class.java, 1, Intent())
    }
}