package com.lumi.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.SmsManager

object PhoneActions {
    fun number(ctx: Context, name: String): String? {
        if (name.any { it.isDigit() } && name.count { it.isDigit() } >= 5) return name.filter { it.isDigit() || it == '+' }
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$name%"), null
        )?.use { if (it.moveToFirst()) return it.getString(0) }
        return null
    }

    fun nameOf(ctx: Context, num: String?): String {
        if (num.isNullOrEmpty()) return "koi unknown number"
        val u = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(num))
        ctx.contentResolver.query(u, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) return it.getString(0) }
        return num
    }

    fun call(ctx: Context, name: String): Boolean {
        val n = number(ctx, name) ?: return false
        ctx.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$n")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun sms(ctx: Context, name: String, text: String): Boolean {
        val n = number(ctx, name) ?: return false
        val sm = ctx.getSystemService(SmsManager::class.java)
        sm.sendMultipartTextMessage(n, null, sm.divideMessage(text), null, null)
        return true
    }

    fun answer(ctx: Context) = ctx.getSystemService(TelecomManager::class.java).acceptRingingCall()
    @Suppress("DEPRECATION")
    fun reject(ctx: Context): Boolean = ctx.getSystemService(TelecomManager::class.java).endCall()
}
