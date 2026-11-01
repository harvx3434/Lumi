package com.lumi.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        if (i.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_RINGING) return
        val num = i.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        LumiService.inst?.onIncomingCall(PhoneActions.nameOf(ctx, num))
    }
}
