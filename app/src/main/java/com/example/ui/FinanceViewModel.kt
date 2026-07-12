package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.BudgetEntity
import com.example.data.database.RecurringTransactionEntity
import com.example.data.database.TransactionEntity
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    // Initialize with sample data if first run & run scheduled sync
    init {
        viewModelScope.launch {
            repository.prePopulateIfEmpty()
            repository.syncRecurringTransactions()
        }
    }

    // Raw sources from repository
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurringTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)

    // Reactive filtered transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        searchQuery,
        selectedCategoryFilter
    ) { txList, query, filter ->
        txList.filter { tx ->
            val matchesQuery = tx.title.contains(query, ignoreCase = true) ||
                    (tx.description?.contains(query, ignoreCase = true) ?: false) ||
                    tx.category.contains(query, ignoreCase = true)
            val matchesCategory = filter == null || tx.category.equals(filter, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Computed financial metrics
    val balanceMetrics: StateFlow<BalanceMetrics> = combine(transactions, budgets) { txList, budgetList ->
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (tx in txList) {
            if (tx.type == "INCOME") {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
            }
        }

        // Period-specific spending (Weekly vs Monthly)
        val categoryWeeklySpending = mutableMapOf<String, Double>()
        val categoryMonthlySpending = mutableMapOf<String, Double>()
        var totalWeeklyExpense = 0.0
        var totalMonthlyExpense = 0.0

        val now = System.currentTimeMillis()
        for (tx in txList) {
            if (tx.type == "EXPENSE") {
                if (isTimestampInCurrentWeek(tx.timestamp, now)) {
                    categoryWeeklySpending[tx.category] = (categoryWeeklySpending[tx.category] ?: 0.0) + tx.amount
                    totalWeeklyExpense += tx.amount
                }
                if (isTimestampInCurrentMonth(tx.timestamp, now)) {
                    categoryMonthlySpending[tx.category] = (categoryMonthlySpending[tx.category] ?: 0.0) + tx.amount
                    totalMonthlyExpense += tx.amount
                }
            }
        }

        val budgetStatusList = mutableListOf<BudgetStatus>()
        
        // Populate budget progress
        for (budget in budgetList) {
            val spent = if (budget.period == "WEEKLY") {
                if (budget.category == "Total") totalWeeklyExpense else (categoryWeeklySpending[budget.category] ?: 0.0)
            } else {
                if (budget.category == "Total") totalMonthlyExpense else (categoryMonthlySpending[budget.category] ?: 0.0)
            }

            budgetStatusList.add(
                BudgetStatus(
                    category = budget.category,
                    limit = budget.limitAmount,
                    spent = spent,
                    percentage = if (budget.limitAmount > 0) (spent / budget.limitAmount).coerceIn(0.0, 1.1) else 0.0,
                    period = budget.period
                )
            )
        }

        // All-time categories spending
        val categorySpendings = mutableMapOf<String, Double>()
        for (tx in txList) {
            if (tx.type == "EXPENSE") {
                categorySpendings[tx.category] = (categorySpendings[tx.category] ?: 0.0) + tx.amount
            }
        }

        BalanceMetrics(
            totalBalance = totalIncome - totalExpense,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            categorySpendings = categorySpendings,
            budgetsStatus = budgetStatusList.sortedByDescending { it.percentage }
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BalanceMetrics()
    )

    // Reactive Reports analytics state
    val reportsMetrics: StateFlow<ReportsMetrics> = transactions
        .combine(MutableStateFlow(System.currentTimeMillis())) { txList, now ->
            // Weekly, Monthly, and Yearly analytics data
            val weeklyExpenses = mutableMapOf<String, Double>()
            val monthlyExpenses = mutableMapOf<String, Double>()
            val yearlyExpenses = mutableMapOf<String, Double>()

            var weeklyIncomeSum = 0.0
            var weeklyExpenseSum = 0.0
            var monthlyIncomeSum = 0.0
            var monthlyExpenseSum = 0.0
            var yearlyIncomeSum = 0.0
            var yearlyExpenseSum = 0.0

            for (tx in txList) {
                val isWeekly = isTimestampInCurrentWeek(tx.timestamp, now)
                val isMonthly = isTimestampInCurrentMonth(tx.timestamp, now)
                val isYearly = isTimestampInCurrentYear(tx.timestamp, now)

                if (tx.type == "INCOME") {
                    if (isWeekly) weeklyIncomeSum += tx.amount
                    if (isMonthly) monthlyIncomeSum += tx.amount
                    if (isYearly) yearlyIncomeSum += tx.amount
                } else {
                    if (isWeekly) {
                        weeklyExpenseSum += tx.amount
                        weeklyExpenses[tx.category] = (weeklyExpenses[tx.category] ?: 0.0) + tx.amount
                    }
                    if (isMonthly) {
                        monthlyExpenseSum += tx.amount
                        monthlyExpenses[tx.category] = (monthlyExpenses[tx.category] ?: 0.0) + tx.amount
                    }
                    if (isYearly) {
                        yearlyExpenseSum += tx.amount
                        yearlyExpenses[tx.category] = (yearlyExpenses[tx.category] ?: 0.0) + tx.amount
                    }
                }
            }

            ReportsMetrics(
                weekly = PeriodSummary(
                    totalIncome = weeklyIncomeSum,
                    totalExpense = weeklyExpenseSum,
                    netSavings = weeklyIncomeSum - weeklyExpenseSum,
                    categoryBreakdown = weeklyExpenses
                ),
                monthly = PeriodSummary(
                    totalIncome = monthlyIncomeSum,
                    totalExpense = monthlyExpenseSum,
                    netSavings = monthlyIncomeSum - monthlyExpenseSum,
                    categoryBreakdown = monthlyExpenses
                ),
                yearly = PeriodSummary(
                    totalIncome = yearlyIncomeSum,
                    totalExpense = yearlyExpenseSum,
                    netSavings = yearlyIncomeSum - yearlyExpenseSum,
                    categoryBreakdown = yearlyExpenses
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReportsMetrics()
        )

    // Helper functions for date filtering
    private fun isTimestampInCurrentMonth(timestamp: Long, now: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val nowMonth = cal.get(Calendar.MONTH)
        val nowYear = cal.get(Calendar.YEAR)
        cal.timeInMillis = timestamp
        return cal.get(Calendar.MONTH) == nowMonth && cal.get(Calendar.YEAR) == nowYear
    }

    private fun isTimestampInCurrentWeek(timestamp: Long, now: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val nowWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val nowYear = cal.get(Calendar.YEAR)
        cal.timeInMillis = timestamp
        return cal.get(Calendar.WEEK_OF_YEAR) == nowWeek && cal.get(Calendar.YEAR) == nowYear
    }

    private fun isTimestampInCurrentYear(timestamp: Long, now: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val nowYear = cal.get(Calendar.YEAR)
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) == nowYear
    }

    // Operations
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        description: String? = null,
        paymentMethod: String? = "Cash",
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = timestamp,
                    description = description,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun saveBudget(category: String, limitAmount: Double, period: String = "MONTHLY") {
        viewModelScope.launch {
            repository.insertBudget(
                BudgetEntity(
                    category = category,
                    limitAmount = limitAmount,
                    period = period
                )
            )
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    fun addRecurringTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        frequency: String,
        startDate: Long = System.currentTimeMillis(),
        description: String? = null,
        paymentMethod: String? = "Cash"
    ) {
        viewModelScope.launch {
            repository.insertRecurringTransaction(
                RecurringTransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    frequency = frequency,
                    startDate = startDate,
                    lastTriggered = startDate, // initially equal to start date
                    description = description,
                    paymentMethod = paymentMethod
                )
            )
            // Immediately sync once added
            repository.syncRecurringTransactions()
        }
    }

    fun deleteRecurringTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id)
        }
    }
}

// Helper State Models
data class BalanceMetrics(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categorySpendings: Map<String, Double> = emptyMap(),
    val budgetsStatus: List<BudgetStatus> = emptyList()
)

data class BudgetStatus(
    val category: String,
    val limit: Double,
    val spent: Double,
    val percentage: Double,
    val period: String = "MONTHLY"
)

data class ReportsMetrics(
    val weekly: PeriodSummary = PeriodSummary(),
    val monthly: PeriodSummary = PeriodSummary(),
    val yearly: PeriodSummary = PeriodSummary()
)

data class PeriodSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap()
)

class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
