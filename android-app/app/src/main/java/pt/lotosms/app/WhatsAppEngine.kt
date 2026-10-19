package pt.lotosms.app

import android.content.Context
import android.content.Intent

object WhatsAppEngine {
    private fun tryPackage(context: Context, message: String, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareMessage(context: Context, message: String): Boolean {
        if (message.isBlank()) return false

        if (tryPackage(context, message, "com.whatsapp")) return true
        if (tryPackage(context, message, "com.whatsapp.w4b")) return true

        return try {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallback, "Enviar mensagem"))
            true
        } catch (_: Exception) {
            false
        }
    }
}
