package pt.lotosms.app

import android.content.Context

object Automation {
    fun onNewDraw(context: Context, payload: DrawPayload) {
        val store = AppStore(context)
        if (!store.isAutomaticEnabled()) return
        if (store.hasProcessed(payload.drawId)) return

        val selected = store.selectedGroupIds()
        val groups = store.loadGroups().filter { it.enabled && selected.contains(it.id) }
        val recipients = groups.flatMap { it.numbers }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (recipients.isEmpty()) return

        val result = SmsEngine.sendMany(context, recipients, payload.message)
        if (result.success > 0) store.markProcessed(payload.drawId)
        store.addHistory(
            HistoryEntry(
                timestamp = System.currentTimeMillis(),
                title = payload.title,
                recipients = recipients.size,
                success = result.success,
                failed = result.failed
            )
        )
    }
}
