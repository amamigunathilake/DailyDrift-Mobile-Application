package com.example.dailydrift.data.repo

import android.content.Context
import com.example.dailydrift.data.model.Habit
import com.example.dailydrift.data.pref.Keys
import com.example.dailydrift.data.pref.Prefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.example.dailydrift.uii.widget.HabitWidgetProvider

class HabitRepo(private val context: Context) {
    private val prefs = Prefs(context)
    private val gson = Gson()

    fun getHabits(): List<Habit> {
        val habitsJson = prefs.getString(Keys.HABITS_KEY, "[]")
        val type = object : TypeToken<List<Habit>>() {}.type
        return gson.fromJson(habitsJson, type) ?: emptyList()
    }

    fun addHabit(habit: Habit) {
        val habits = getHabits().toMutableList()
        val newHabit = habit.copy(id = System.currentTimeMillis().toString())
        habits.add(newHabit)
        saveHabits(habits)
    }

    fun updateHabit(updatedHabit: Habit) {
        val habits = getHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == updatedHabit.id }
        if (index != -1) {
            habits[index] = updatedHabit
            saveHabits(habits)
        }
    }

    fun deleteHabit(habitId: String) {
        val habits = getHabits().toMutableList()
        habits.removeAll { it.id == habitId }
        saveHabits(habits)
    }

    fun restoreHabit(habit: Habit, index: Int?) {
        val habits = getHabits().toMutableList()
        if (index != null && index >= 0 && index <= habits.size) {
            habits.add(index, habit)
        } else {
            habits.add(habit)
        }
        saveHabits(habits)
    }

    private fun saveHabits(habits: List<Habit>) {
        val habitsJson = gson.toJson(habits)
        prefs.saveString(Keys.HABITS_KEY, habitsJson)
        notifyWidgetsSafe()
    }

    fun getTodayProgress(): Pair<Int, Int> {
        val habits = getHabits()
        val total = habits.size
        val completed = habits.count { it.isCompleted }
        return Pair(completed, total)
    }

    fun getLastResetDate(): String {
        return prefs.getString("last_reset_date", "")
    }

    fun setLastResetDate(date: String) {
        prefs.saveString("last_reset_date", date)
    }

    fun getCurrentDateString(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        // Use system default timezone to ensure correct date
        return LocalDate.now(ZoneId.systemDefault()).format(formatter)
    }

    fun isNewDay(): Boolean {
        val lastResetDate = getLastResetDate()
        val currentDate = getCurrentDateString()
        return lastResetDate != currentDate
    }

    fun resetHabitsForNewDay() {
        val habits = getHabits().map { h ->
            h.copy(completedCount = 0, isCompleted = false)
        }
        saveHabits(habits) // will notify widget
        setLastResetDate(getCurrentDateString())
        notifyWidgetsSafe()
    }

    fun checkAndResetIfNewDay() {
        if (isNewDay()) {
            resetHabitsForNewDay()
        }
    }


    private fun notifyWidgetsSafe() {
        try {
            HabitWidgetProvider.updateAllWidgets(context)
        } catch (_: Throwable) {

        }
    }
}
