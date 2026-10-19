package pt.lotosms.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object SmsEngine {
    private const val MAX_RECIPIENTS_PER_BATCH = 100

    fun canSend(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun sendMany(context: Context, recipients: List<String>, message: String): SendResult {
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
                if (parts.size > 1) {
                    manager.sendMultipartTextMessage(phone, null, parts, null, null)
                } else {
                    manager.sendTextMessage(phone, null, message, null, null)
                }
                success++
            } catch (_: Exception) {
                failed++
            }
        }

        return SendResult(success, failed)
    }
}
