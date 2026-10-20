package pt.lotosms.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("lotosms", Context.MODE_PRIVATE)

    fun saveGroups(groups: List<SmsGroup>) {
        val arr = JSONArray()
        groups.forEach { group ->
            arr.put(JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
                put("enabled", group.enabled)
                put("numbers", JSONArray(group.numbers))
            })
        }
        prefs.edit().putString("groups", arr.toString()).apply()
    }

    fun loadGroups(): List<SmsGroup> {
        val raw = prefs.getString("groups", null) ?: return listOf(
            SmsGroup("familia", "Família", listOf(), true),
            SmsGroup("amigos", "Amigos", listOf(), true)
        )
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val nums = o.getJSONArray("numbers")
            SmsGroup(
                id = o.getString("id"),
                name = o.getString("name"),
                numbers = (0 until nums.length()).map { nums.getString(it) },
                enabled = o.optBoolean("enabled", true)
            )
        }
    }

    fun isAutomaticEnabled() = prefs.getBoolean("auto_enabled", true)
    fun setAutomaticEnabled(value: Boolean) = prefs.edit().putBoolean("auto_enabled", value).apply()

    // Mantido para compatibilidade com versões anteriores.
    fun selectedGroupIds(): Set<String> = prefs.getStringSet("selected_groups", emptySet()) ?: emptySet()
    fun setSelectedGroupIds(ids: Set<String>) = prefs.edit().putStringSet("selected_groups", ids).apply()

    fun selectedGames(): Set<String> = prefs.getStringSet(
        "selected_games",
        setOf("Euromilhões", "Totoloto", "EuroDreams")
    ) ?: setOf("Euromilhões", "Totoloto", "EuroDreams")

    fun setSelectedGames(games: Set<String>) = prefs.edit().putStringSet("selected_games", games).apply()

    fun groupIdsForGame(game: String): Set<String> {
        val key = "game_groups_${gameKey(game)}"
        if (prefs.contains(key)) {
            return prefs.getStringSet(key, emptySet()) ?: emptySet()
        }

        // Migração suave: numa instalação existente, usa a seleção global anterior.
        val legacy = selectedGroupIds()
        if (legacy.isNotEmpty()) return legacy

        // Numa instalação nova, associa por defeito todos os grupos ativos.
        return loadGroups().filter { it.enabled }.map { it.id }.toSet()
    }

    fun setGroupIdsForGame(game: String, ids: Set<String>) {
        prefs.edit().putStringSet("game_groups_${gameKey(game)}", ids).apply()
    }

    fun wasQueuedForRecipient(drawId: String, phone: String): Boolean =
        prefs.getBoolean("queued_${drawId}_${phone.filter { it.isDigit() }}", false)

    fun markQueuedForRecipient(drawId: String, phone: String) {
        prefs.edit().putBoolean("queued_${drawId}_${phone.filter { it.isDigit() }}", true).apply()
    }

    fun recordDetailedSend(drawId: String, game: String, phone: String, message: String, status: String) {
        val arr = JSONArray(prefs.getString("send_details", "[]") ?: "[]")
        arr.put(JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("drawId", drawId)
            put("game", game)
            put("phone", phone)
            put("message", message)
            put("status", status)
        })
        prefs.edit().putString("send_details", arr.toString()).apply()
    }

    fun detailedSendHistory(): String = prefs.getString("send_details", "[]") ?: "[]"

    fun hasProcessed(drawId: String): Boolean = prefs.getBoolean("draw_$drawId", false)
    fun markProcessed(drawId: String) = prefs.edit().putBoolean("draw_$drawId", true).apply()

    fun addHistory(entry: HistoryEntry) {
        val existing = prefs.getString("history", "[]") ?: "[]"
        val arr = JSONArray(existing)
        arr.put(JSONObject().apply {
            put("timestamp", entry.timestamp)
            put("title", entry.title)
            put("recipients", entry.recipients)
            put("success", entry.success)
            put("failed", entry.failed)
        })
        prefs.edit().putString("history", arr.toString()).apply()
    }

    private fun gameKey(value: String): String = value
        .lowercase()
        .replace("õ", "o")
        .replace("ó", "o")
        .replace("é", "e")
        .replace(" ", "")
        .replace("-", "")
}
