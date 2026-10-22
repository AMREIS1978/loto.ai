package pt.lotosms.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Records the actual result returned by the Android SMS transport. */
class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val drawId = intent.getStringExtra("drawId") ?: return
        val game = intent.getStringExtra("game") ?: return
        val phone = intent.getStringExtra("phone") ?: return
        val message = intent.getStringExtra("message") ?: return
        val status = if (resultCode == Activity.RESULT_OK) "SENT_BY_MODEM" else "MODEM_ERROR_${resultCode}"
        AppStore(context).recordDetailedSend(drawId, game, phone, message, status)
    }
}
