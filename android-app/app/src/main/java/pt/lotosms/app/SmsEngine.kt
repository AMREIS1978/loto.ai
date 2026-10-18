package pt.lotosms.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object SmsEngine {
    fun canSend(context: Context): Boolean {
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        return defaultPackage == context.packageName && permission
    }

    fun sendMany(context: Context, recipients: List<String>, message: String): SendResult {
        if (!canSend(context)) return SendResult(0, recipients.size)
        val manager = context.getSystemService(SmsManager::class.java)
        var ok = 0
        var failed = 0
        recipients.distinct().forEach { phone ->
            try {
                val parts = manager.divideMessage(message)
                if (parts.size > 1) manager.sendMultipartTextMessage(phone, null, parts, null, null)
                else manager.sendTextMessage(phone, null, message, null, null)
                ok++
            } catch (_: Exception) {
                failed++
            }
        }
        return SendResult(ok, failed)
    }
}
