package pt.lotosms.app

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object SmsEngine {
    private const val MAX_RECIPIENTS_PER_BATCH = 100

    fun canSend(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun sendMany(context: Context, recipients: List<String>, message: String, drawId: String? = null, game: String? = null): SendResult {
        val uniqueRecipients = recipients
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_RECIPIENTS_PER_BATCH)

        if (uniqueRecipients.isEmpty()) return SendResult(0, 0)
        if (!canSend(context)) return SendResult(0, uniqueRecipients.size)

        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        var success = 0
        var failed = 0

        uniqueRecipients.forEach { phone ->
            try {
                val parts = manager.divideMessage(message)
                val sentIntent = if (drawId != null && game != null) {
                    val callback = Intent(context, SmsStatusReceiver::class.java).apply {
                        putExtra("drawId", drawId)
                        putExtra("game", game)
                        putExtra("phone", phone)
                        putExtra("message", message)
                    }
                    PendingIntent.getBroadcast(context, (drawId + phone).hashCode(), callback,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                } else null
                if (parts.size > 1) {
                    manager.sendMultipartTextMessage(phone, null, parts, sentIntent?.let { arrayListOf<PendingIntent?>(it).apply { repeat(parts.size - 1) { add(null) } } }, null)
                } else {
                    manager.sendTextMessage(phone, null, message, sentIntent, null)
                }
                success++
            } catch (_: Exception) {
                failed++
            }
        }

        return SendResult(success, failed)
    }
}
