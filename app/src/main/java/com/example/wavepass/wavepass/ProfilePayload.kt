package com.example.wavepass.wavepass

import org.json.JSONArray
import org.json.JSONObject

// Simple, dependency-free JSON representation of a WaveProfile,
// used as the payload transmitted over GATT between devices.
data class ProfilePayload(
    val anonymousId: String,
    val displayAlias: String,
    val favoriteArtist: String,
    val favoriteSongTitles: List<String>
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("anonymousId", anonymousId)
        json.put("displayAlias", displayAlias)
        json.put("favoriteArtist", favoriteArtist)
        json.put("favoriteSongTitles", JSONArray(favoriteSongTitles))
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): ProfilePayload? {
            return try {
                val json = JSONObject(jsonString)
                val titlesArray = json.getJSONArray("favoriteSongTitles")
                val titles = (0 until titlesArray.length()).map { titlesArray.getString(it) }

                ProfilePayload(
                    anonymousId = json.getString("anonymousId"),
                    displayAlias = json.getString("displayAlias"),
                    favoriteArtist = json.getString("favoriteArtist"),
                    favoriteSongTitles = titles
                )
            } catch (e: Exception) {
                null // Malformed or unexpected payload from a nearby device — discard safely.
            }
        }
    }
}