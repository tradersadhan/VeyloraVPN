package com.veylora.vpn

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ServerConfig(
    val id: String,
    val name: String,
    val country: String,
    val config: String,
    val imported: Boolean = false
)

/**
 * Servers come from two places:
 *  1. app/src/main/assets/servers.json  (your own servers, shipped with the app)
 *  2. WireGuard .conf files the user imports inside the app
 */
class ServerRepository(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("veylora", Context.MODE_PRIVATE)

    fun load(): List<ServerConfig> {
        val list = mutableListOf<ServerConfig>()
        runCatching {
            val text = ctx.assets.open("servers.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += ServerConfig(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    country = o.optString("country"),
                    config = o.getString("config")
                )
            }
        }
        runCatching {
            val arr = JSONArray(prefs.getString("imported", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += ServerConfig(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    country = "Imported",
                    config = o.getString("config"),
                    imported = true
                )
            }
        }
        return list
    }

    fun addImported(name: String, config: String) {
        val arr = JSONArray(prefs.getString("imported", "[]"))
        arr.put(JSONObject().put("id", "imp-" + UUID.randomUUID()).put("name", name).put("config", config))
        prefs.edit().putString("imported", arr.toString()).apply()
    }

    fun removeImported(id: String) {
        val old = JSONArray(prefs.getString("imported", "[]"))
        val new = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.getJSONObject(i)
            if (o.getString("id") != id) new.put(o)
        }
        prefs.edit().putString("imported", new.toString()).apply()
    }

    fun selectedId(): String? = prefs.getString("selected", null)
    fun setSelected(id: String) = prefs.edit().putString("selected", id).apply()
}
