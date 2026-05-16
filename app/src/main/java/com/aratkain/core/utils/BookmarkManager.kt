package com.aratkain.core.utils

import android.content.Context
import com.aratkain.core.model.EstablishmentResponse
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists bookmarked [EstablishmentResponse] objects in SharedPreferences as a JSON array.
 * All operations are synchronous and cheap — call off the main thread if you hold many entries.
 */
class BookmarkManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("aratkain_bookmarks", Context.MODE_PRIVATE)

    private val KEY = "bookmarked_places"

    // ── Public API ────────────────────────────────────────────

    fun getAll(): List<EstablishmentResponse> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it).toPlace() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isBookmarked(id: String): Boolean = getAll().any { it.id.toString() == id }

    /**
     * Toggles bookmark state.
     * @return `true` if the place is NOW bookmarked, `false` if it was removed.
     */
    fun toggle(place: EstablishmentResponse): Boolean {
        val current = getAll().toMutableList()
        val removed = current.removeAll { it.id == place.id }
        if (!removed) current.add(place)
        save(current)
        return !removed
    }

    fun remove(id: String) {
        val updated = getAll().filter { it.id.toString() != id }
        save(updated)
    }

    // ── Serialisation helpers ─────────────────────────────────

    private fun save(places: List<EstablishmentResponse>) {
        val arr = JSONArray().apply { places.forEach { put(it.toJson()) } }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun EstablishmentResponse.toJson(): JSONObject = JSONObject().apply {
        put("id",        id)               // FIX 1: store as Long directly, no toString()
        put("name",      name)
        put("type",      type     ?: "")
        put("address",   address  ?: "")
        put("latitude",  latitude)
        put("longitude", longitude)
        put("rating",    rating   ?: -1.0)
    }

    private fun JSONObject.toPlace(): EstablishmentResponse = EstablishmentResponse(
        id          = getLong("id"),       // FIX 1: read back as Long
        name        = getString("name"),
        type        = getString("type").ifEmpty { null },
        address     = getString("address").ifEmpty { null },
        latitude    = getDouble("latitude"),
        longitude   = getDouble("longitude"),
        rating      = getDouble("rating").let { if (it == -1.0) null else it }, // FIX 2: already Double, drop .toFloat()
        city        = null,                // FIX 3: supply missing fields
        reviewCount = null,
        isOpen      = null,
        distanceKm  = null
    )
}