package com.example.dailydrift.data.repo

import android.content.Context
import com.example.dailydrift.data.model.WaterEntry
import com.example.dailydrift.data.pref.Prefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.ZoneId

class HydrationRepo(context: Context) {
    private val prefs = Prefs(context)
    private val gson = Gson()

    private val KEY_ENTRIES = "water_entries"

    fun add(amountMl: Int) {
        val list = getAll().toMutableList()
        list.add(WaterEntry(amountMl = amountMl))
        save(list)
    }

    fun delete(id: String) {
        val list = getAll().toMutableList()
        list.removeAll { it.id == id }
        save(list)
    }

    fun getToday(): List<WaterEntry> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return getAll().filter { it.createdAt in start until end }
            .sortedByDescending { it.createdAt }
    }

    fun totalToday(): Int = getToday().sumOf { it.amountMl }

    private fun getAll(): List<WaterEntry> {
        val json = prefs.getString(KEY_ENTRIES, "[]")
        val type = object : TypeToken<List<WaterEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun save(list: List<WaterEntry>) {
        prefs.saveString(KEY_ENTRIES, gson.toJson(list))
    }
}
