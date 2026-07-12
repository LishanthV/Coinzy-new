package com.example.data.repository

import com.example.data.database.BudgetEntity
import com.example.data.database.RecurringTransactionEntity
import com.example.data.database.TransactionDao
import com.example.data.database.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class FinanceRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = transactionDao.getAllBudgets()
    val allRecurringTransactions: Flow<List<RecurringTransactionEntity>> = transactionDao.getAllRecurringTransactions()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        transactionDao.insertBudget(budget)
    }

    suspend fun deleteBudget(category: String) {
        transactionDao.deleteBudgetByCategory(category)
    }

    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity) {
        transactionDao.insertRecurringTransaction(recurring)
    }

    suspend fun deleteRecurringTransaction(id: Int) {
        transactionDao.deleteRecurringTransactionById(id)
    }

    suspend fun syncRecurringTransactions() {
        val now = System.currentTimeMillis()
        val recurringList = transactionDao.getAllRecurringTransactions().first()
        
        for (rec in recurringList) {
            var lastTime = rec.lastTriggered
            if (lastTime == 0L) {
                lastTime = rec.startDate
            }
            
            var updatedLastTriggered = rec.lastTriggered
            var nextTrigger = calculateNextTrigger(lastTime, rec.frequency)
            
            var changed = false
            // Keep creating occurrences that should have happened up to now
            while (nextTrigger <= now) {
                val tx = TransactionEntity(
                    title = rec.title,
                    amount = rec.amount,
                    type = rec.type,
                    category = rec.category,
                    timestamp = nextTrigger,
                    description = rec.description ?: "Auto-generated recurring transaction (${rec.frequency.lowercase()})",
                    paymentMethod = rec.paymentMethod ?: "Bank Transfer"
                )
                transactionDao.insertTransaction(tx)
                
                updatedLastTriggered = nextTrigger
                nextTrigger = calculateNextTrigger(nextTrigger, rec.frequency)
                changed = true
            }
            
            if (changed) {
                transactionDao.insertRecurringTransaction(rec.copy(lastTriggered = updatedLastTriggered))
            }
        }
    }

    fun calculateNextTrigger(lastTime: Long, frequency: String): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = lastTime }
        when (frequency.uppercase()) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    suspend fun prePopulateIfEmpty() {
        val currentTransactions = allTransactions.first()
        if (currentTransactions.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L

            val initialTransactions = listOf(
                TransactionEntity(
                    title = "Monthly Salary",
                    amount = 4200.0,
                    type = "INCOME",
                    category = "Salary",
                    timestamp = now - 5 * dayMillis,
                    description = "Regular monthly paycheck",
                    paymentMethod = "Bank Transfer"
                ),
                TransactionEntity(
                    title = "Whole Foods Grocery",
                    amount = 142.50,
                    type = "EXPENSE",
                    category = "Food",
                    timestamp = now - 3 * dayMillis,
                    description = "Weekly grocery shopping",
                    paymentMethod = "Card"
                ),
                TransactionEntity(
                    title = "Electricity Bill",
                    amount = 95.0,
                    type = "EXPENSE",
                    category = "Utilities",
                    timestamp = now - 2 * dayMillis,
                    description = "Electric utility payment",
                    paymentMethod = "Bank Transfer"
                ),
                TransactionEntity(
                    title = "Cinema & Snacks",
                    amount = 45.0,
                    type = "EXPENSE",
                    category = "Entertainment",
                    timestamp = now - 1 * dayMillis,
                    description = "New movie release with popcorn",
                    paymentMethod = "Cash"
                ),
                TransactionEntity(
                    title = "Freelance Project Design",
                    amount = 850.0,
                    type = "INCOME",
                    category = "Freelance",
                    timestamp = now - 12 * 60 * 60 * 1000L,
                    description = "Logo and UI kit delivery",
                    paymentMethod = "Bank Transfer"
                ),
                TransactionEntity(
                    title = "Gas Station Refuel",
                    amount = 55.0,
                    type = "EXPENSE",
                    category = "Transport",
                    timestamp = now - 3 * 60 * 60 * 1000L,
                    description = "Car refuel",
                    paymentMethod = "Card"
                )
            )

            for (transaction in initialTransactions) {
                transactionDao.insertTransaction(transaction)
            }

            // Populating default budgets (using "MONTHLY" by default, some "WEEKLY")
            val initialBudgets = listOf(
                BudgetEntity("Total", 1500.0, "MONTHLY"),
                BudgetEntity("Food", 400.0, "MONTHLY"),
                BudgetEntity("Shopping", 100.0, "WEEKLY"),
                BudgetEntity("Entertainment", 200.0, "MONTHLY"),
                BudgetEntity("Utilities", 250.0, "MONTHLY"),
                BudgetEntity("Transport", 45.0, "WEEKLY")
            )
            for (budget in initialBudgets) {
                transactionDao.insertBudget(budget)
            }
        }

        // Prepopulate recurring schedules if they don't exist yet
        val currentRecurring = allRecurringTransactions.first()
        if (currentRecurring.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L
            
            val initialRecurring = listOf(
                RecurringTransactionEntity(
                    title = "Netflix Premium",
                    amount = 15.99,
                    type = "EXPENSE",
                    category = "Entertainment",
                    frequency = "MONTHLY",
                    startDate = now - 32 * dayMillis, // more than a month ago
                    lastTriggered = now - 32 * dayMillis, // initially started
                    description = "Monthly Netflix subscription",
                    paymentMethod = "Card"
                ),
                RecurringTransactionEntity(
                    title = "Freelance UI Retainer",
                    amount = 350.0,
                    type = "INCOME",
                    category = "Freelance",
                    frequency = "WEEKLY",
                    startDate = now - 15 * dayMillis, // more than 2 weeks ago
                    lastTriggered = now - 15 * dayMillis,
                    description = "Weekly freelance ui design package",
                    paymentMethod = "Bank Transfer"
                )
            )
            
            for (rec in initialRecurring) {
                transactionDao.insertRecurringTransaction(rec)
            }
        }
    }
}
