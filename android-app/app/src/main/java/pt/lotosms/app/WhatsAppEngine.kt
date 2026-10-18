package pt.lotosms.app

import android.content.Context
import android.content.Intent

object WhatsAppEngine {
    fun shareMessage(context: Context, message: String): Boolean {
        if (message.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallback, "Enviar por WhatsApp"))
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
