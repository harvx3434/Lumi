package com.lumi.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(48, 48, 48, 48) }
        box.addView(TextView(this).apply { text = "💗 Lumi"; textSize = 32f; gravity = Gravity.CENTER })
        fun btn(t: String, f: () -> Unit) = box.addView(Button(this).apply { text = t; setOnClickListener { f() } })
        btn("1. Permissions do") {
            requestPermissions(arrayOf(
                Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        btn("2. Notification access do (WhatsApp/SMS reply ke liye)") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        btn("3. Lumi ko jagao") { startForegroundService(Intent(this, LumiService::class.java)) }
        btn("Lumi ko sulao") { stopService(Intent(this, LumiService::class.java)) }
        setContentView(box)
    }
}
