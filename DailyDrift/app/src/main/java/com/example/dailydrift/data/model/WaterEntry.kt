package com.example.dailydrift.data.model

data class WaterEntry(
    val id: String = System.currentTimeMillis().toString(),
    val amountMl: Int,
    val createdAt: Long = System.currentTimeMillis()
)
