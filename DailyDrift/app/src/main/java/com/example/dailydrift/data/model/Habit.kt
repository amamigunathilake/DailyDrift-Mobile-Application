package com.example.dailydrift.data.model

data class Habit(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val targetCount: Int = 1,
    val completedCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)