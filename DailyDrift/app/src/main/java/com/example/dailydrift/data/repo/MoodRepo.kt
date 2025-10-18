package com.example.dailydrift.data.repo

import android.content.Context
import com.example.dailydrift.data.model.MoodEntry
import com.example.dailydrift.data.pref.Keys
import com.example.dailydrift.data.pref.Prefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.ZoneId

class MoodRepo(private val context: Context) {
    private val prefs = Prefs(context)
    private val gson = Gson()

    fun getMoodEntries(): List<MoodEntry> {
        val entriesJson = prefs.getString(Keys.MOOD_ENTRIES_KEY, "[]")
        val type = object : TypeToken<List<MoodEntry>>() {}.type
        return gson.fromJson(entriesJson, type) ?: emptyList()
    }

    fun addMoodEntry(entry: MoodEntry) {
        val entries = getMoodEntries().toMutableList()
        val newEntry = entry.copy(
            id = System.currentTimeMillis().toString(),
            createdAt = entry.createdAt
        )
        android.util.Log.d("MoodRepo", "Saving mood timestamp=${newEntry.createdAt}")
        entries.add(newEntry)
        saveMoodEntries(entries)
    }

    fun deleteMoodEntry(entryId: String) {
        val entries = getMoodEntries().toMutableList()
        entries.removeAll { it.id == entryId }
        saveMoodEntries(entries)
    }

    private fun saveMoodEntries(entries: List<MoodEntry>) {
        val entriesJson = gson.toJson(entries)
        prefs.saveString(Keys.MOOD_ENTRIES_KEY, entriesJson)
    }

    fun getMoodEntriesForWeek(): List<MoodEntry> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone)
            .minusDays(6)
            .atStartOfDay(zone)
            .toInstant().toEpochMilli()
        val end = LocalDate.now(zone)
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant().toEpochMilli()
        return getMoodEntries().filter { it.createdAt in start until end }
    }

    fun getMoodEntriesForToday(): List<MoodEntry> {
        val zone = ZoneId.systemDefault()
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowStart = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return getMoodEntries().filter { it.createdAt in todayStart until tomorrowStart }
    }
}
