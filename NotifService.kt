package com.lumi.assistant

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifService : NotificationListenerService() {
    companion object {
        val pending = LinkedHashMap<String, Notification.Action>()
        var onMessage: ((String, String) -> Unit)? = null

        fun reply(ctx: Context, who: String, text: String): Boolean {
            val act = pending.entries.lastOrNull { it.key.contains(who, true) }?.value ?: return false
            val ri = act.remoteInputs?.firstOrNull() ?: return false
            val i = Intent(); val b = Bundle().apply { putCharSequence(ri.resultKey, text) }
            RemoteInput.addResultsToIntent(act.remoteInputs, i, b)
            act.actionIntent.send(ctx, 0, i)
            return true
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification
        val e = n.extras
        val title = e.getString(Notification.EXTRA_TITLE) ?: return
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val act = n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true } ?: return
        pending[title] = act
        onMessage?.invoke(title, text)
    }
}
