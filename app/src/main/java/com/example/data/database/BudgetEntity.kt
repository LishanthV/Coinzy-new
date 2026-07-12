package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String, // e.g. "Total", "Food", "Shopping", "Entertainment", "Utilities", "Transport"
    val limitAmount: Double,
    val period: String = "MONTHLY" // "WEEKLY" or "MONTHLY"
)

