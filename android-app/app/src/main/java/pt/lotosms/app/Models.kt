package pt.lotosms.app

data class SmsGroup(val id: String, val name: String, val numbers: List<String>, val enabled: Boolean)
data class HistoryEntry(val timestamp: Long, val title: String, val recipients: Int, val success: Int, val failed: Int)
data class DrawPayload(val drawId: String, val game: String, val title: String, val message: String)
data class SendResult(val success: Int, val failed: Int)
