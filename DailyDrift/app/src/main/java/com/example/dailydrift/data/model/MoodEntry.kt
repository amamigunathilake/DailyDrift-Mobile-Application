package com.example.dailydrift.data.model

data class MoodEntry(
    val id: String = "",
    val moodEmoji: String,
    val moodName: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
