package pt.lotosms.app

import android.content.Context
import android.content.Intent
import android.net.Uri

object SmsEngine {
    fun sendMany(context: Context, recipients: List<String>, message: String): SendResult {
        val uniqueRecipients = recipients.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (uniqueRecipients.isEmpty()) return SendResult(0, 0)

        return try {
            val address = uniqueRecipients.joinToString(";")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$address")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SendResult(uniqueRecipients.size, 0)
        } catch (_: Exception) {
            SendResult(0, uniqueRecipients.size)
        }
    }
}
