package pt.lotosms.app

import android.content.Context

object Automation {
    fun onNewDraw(context: Context, payload: DrawPayload) {
        val store = AppStore(context)
        if (!store.isAutomaticEnabled()) return
        if (store.hasProcessed(payload.drawId)) return

        val gameSelected = store.selectedGames().any { selected ->
            normalizeGame(selected) == normalizeGame(payload.game) ||
                normalizeGame(selected) == normalizeGame(payload.title)
        }
        if (!gameSelected) return

        val linkedGroupIds = store.groupIdsForGame(payload.game)
        if (linkedGroupIds.isEmpty()) return

        val groups = store.loadGroups().filter { group ->
            group.enabled && linkedGroupIds.contains(group.id)
        }

        val recipients = groups
            .flatMap { it.numbers }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { normalizePhone(it) }

        if (recipients.isEmpty()) return

        val result = SmsEngine.sendMany(context, recipients, payload.message)

        if (result.success > 0) {
            store.markProcessed(payload.drawId)
        }

        store.addHistory(
            HistoryEntry(
                timestamp = System.currentTimeMillis(),
                title = "${payload.title} — envio automático",
                recipients = recipients.size,
                success = result.success,
                failed = result.failed
            )
        )
    }

    private fun normalizePhone(value: String): String = value.filter { it.isDigit() || it == '+' }

    private fun normalizeGame(value: String): String = value
        .lowercase()
        .replace("õ", "o")
        .replace("ó", "o")
        .replace("é", "e")
        .replace(" ", "")
        .replace("-", "")
}
