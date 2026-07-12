package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.TransactionEntity
import com.example.data.database.RecurringTransactionEntity
import com.example.data.database.BudgetEntity
import com.example.ui.BudgetStatus
import com.example.ui.FinanceViewModel
import com.example.ui.PeriodSummary
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val balanceMetrics by viewModel.balanceMetrics.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val recurringTransactions by viewModel.recurringTransactions.collectAsStateWithLifecycle()
    val reportsMetrics by viewModel.reportsMetrics.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Transactions, 1: Budgets, 2: Reports, 3: Recurring

    val currencyFormat = remember { DecimalFormat("$#,##0.00") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeTab == 3) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddRecurringDialog = true },
                        icon = { Icon(Icons.Default.Autorenew, contentDescription = null) },
                        text = { Text("New Schedule") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.navigationBarsPadding()
                    )
                } else {
                    FloatingActionButton(
                        onClick = { showAddTransactionDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("add_transaction_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            // App Bar Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Coinzy Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Coinzy",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = { showSetBudgetDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Budgets",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Bento Grid Layout (Total Balance Card, plus Savings & Spendings Cards side-by-side)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Total Balance Bento Card (Span 2)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("balance_card"),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BentoLavender
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BentoDeepViolet.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = BentoDeepViolet,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(BentoDeepViolet)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = "Total Balance",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = BentoDeepViolet.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currencyFormat.format(balanceMetrics.totalBalance),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = BentoDeepViolet
                        )
                    }
                }

                // Row 2: Two columns side-by-side (Savings/Income & Spendings Bento Cards)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Bento Card: Income / Savings
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BentoBlue
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Income",
                                    tint = BentoDeepNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    text = "INCOME",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = BentoDeepNavy.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = currencyFormat.format(balanceMetrics.totalIncome),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = BentoDeepNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Right Bento Card: Spent
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BentoPink
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = "Spent",
                                    tint = BentoDeepRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    text = "SPENT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = BentoDeepRose.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = currencyFormat.format(balanceMetrics.totalExpense),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = BentoDeepRose,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp)
            ) {
                TabItemButton(
                    selected = activeTab == 0,
                    text = "History",
                    icon = Icons.Outlined.ReceiptLong,
                    selectedIcon = Icons.Default.ReceiptLong,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_transactions"),
                    onClick = { activeTab = 0 }
                )
                TabItemButton(
                    selected = activeTab == 1,
                    text = "Budgets",
                    icon = Icons.Outlined.AccountBalance,
                    selectedIcon = Icons.Default.AccountBalance,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_budgets"),
                    onClick = { activeTab = 1 }
                )
                TabItemButton(
                    selected = activeTab == 2,
                    text = "Reports",
                    icon = Icons.Outlined.PieChart,
                    selectedIcon = Icons.Default.PieChart,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_reports"),
                    onClick = { activeTab = 2 }
                )
                TabItemButton(
                    selected = activeTab == 3,
                    text = "Recurring",
                    icon = Icons.Outlined.Autorenew,
                    selectedIcon = Icons.Default.Autorenew,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_recurring"),
                    onClick = { activeTab = 3 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Render active tab body
            when (activeTab) {
                0 -> {
                    TransactionsTab(
                        transactions = transactions,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategoryFilter,
                        onSearchChange = { viewModel.searchQuery.value = it },
                        onCategoryChange = { viewModel.selectedCategoryFilter.value = it },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        currencyFormat = currencyFormat
                    )
                }
                1 -> {
                    BudgetsTab(
                        budgetsStatus = balanceMetrics.budgetsStatus,
                        onAdjustBudget = { showSetBudgetDialog = true },
                        currencyFormat = currencyFormat
                    )
                }
                2 -> {
                    ReportsTab(
                        reportsMetrics = reportsMetrics,
                        currencyFormat = currencyFormat
                    )
                }
                3 -> {
                    RecurringTab(
                        recurringTransactions = recurringTransactions,
                        onDelete = { viewModel.deleteRecurringTransaction(it) },
                        currencyFormat = currencyFormat
                    )
                }
            }
        }

        // Add Transaction Dialog
        if (showAddTransactionDialog) {
            AddTransactionDialog(
                onDismiss = { showAddTransactionDialog = false },
                onConfirm = { title, amt, type, cat, desc, method ->
                    viewModel.addTransaction(title, amt, type, cat, desc, method)
                    showAddTransactionDialog = false
                }
            )
        }

        // Add Recurring Transaction Dialog
        if (showAddRecurringDialog) {
            AddRecurringTransactionDialog(
                onDismiss = { showAddRecurringDialog = false },
                onConfirm = { title, amt, type, cat, freq, desc, method ->
                    viewModel.addRecurringTransaction(title, amt, type, cat, freq, System.currentTimeMillis(), desc, method)
                    showAddRecurringDialog = false
                }
            )
        }

        // Set Budget Dialog
        if (showSetBudgetDialog) {
            SetBudgetDialog(
                existingBudgets = budgets,
                onDismiss = { showSetBudgetDialog = false },
                onSave = { category, limit, period ->
                    viewModel.saveBudget(category, limit, period)
                },
                onDelete = { category ->
                    viewModel.deleteBudget(category)
                }
            )
        }
    }
}

@Composable
fun TabItemButton(
    selected: Boolean,
    text: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(durationMillis = 200)
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        animationSpec = tween(durationMillis = 200)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// TRANSACTIONS TAB
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsTab(
    transactions: List<TransactionEntity>,
    searchQuery: String,
    selectedCategory: String?,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onDeleteTransaction: (Int) -> Unit,
    currencyFormat: DecimalFormat
) {
    val categories = listOf("All", "Food", "Shopping", "Entertainment", "Utilities", "Transport", "Health", "Salary", "Freelance", "Investment", "Gift", "Other")
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .testTag("search_input"),
            placeholder = { Text("Search description or title...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        // Horizontal Category Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { categoryName ->
                val isSelected = (categoryName == "All" && selectedCategory == null) || (categoryName == selectedCategory)
                val chipBgColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
                val chipTextColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                val chipBorderColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBgColor)
                        .border(1.dp, chipBorderColor, RoundedCornerShape(12.dp))
                        .clickable {
                            focusManager.clearFocus()
                            if (categoryName == "All") {
                                onCategoryChange(null)
                            } else {
                                onCategoryChange(categoryName)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("category_filter_chip_$categoryName")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = getCategoryEmoji(categoryName) + " " + categoryName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = chipTextColor
                        )
                    }
                }
            }
        }

        // Transactions list
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Transactions Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try searching for something else or add a new transaction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = transactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItemRow(
                        transaction = transaction,
                        currencyFormat = currencyFormat,
                        onDelete = { onDeleteTransaction(transaction.id) },
                        modifier = Modifier.testTag("transaction_item_${transaction.id}")
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Avoid covering by FAB
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    currencyFormat: DecimalFormat,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(transaction.timestamp) {
        val date = Date(transaction.timestamp)
        val format = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        format.format(date)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmoji(transaction.category),
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$dateStr • ${transaction.paymentMethod ?: "Cash"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (transaction.type == "INCOME") "+" + currencyFormat.format(transaction.amount) else "-" + currencyFormat.format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (transaction.type == "INCOME") Color(0xFF10B981) else Color(0xFFF43F5E)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// BUDGETS TAB WITH LIVE ALERTS
// ==========================================

@Composable
fun BudgetsTab(
    budgetsStatus: List<BudgetStatus>,
    onAdjustBudget: () -> Unit,
    currencyFormat: DecimalFormat
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-importance local budget alerts
        val warningAlerts = budgetsStatus.filter { it.percentage >= 0.8 }
        if (warningAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF7ED) // Light warning orange bg
                ),
                border = BorderStroke(1.dp, Color(0xFFFFEDD5))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Budget Bulletins",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFC2410C)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        warningAlerts.take(3).forEach { alert ->
                            val alertMsg = if (alert.spent > alert.limit) {
                                "⚠️ Exceeded ${alert.category} budget limit by ${currencyFormat.format(alert.spent - alert.limit)}!"
                            } else {
                                "⚠️ Approaching limit! ${alert.category} budget is ${((alert.percentage) * 100).toInt()}% spent."
                            }
                            Text(
                                text = alertMsg,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF9A3412)
                            )
                        }
                    }
                }
            }
        }

        // Overall Budget Indicator (Canvas Donut Progress Arc Chart)
        val overallBudget = budgetsStatus.find { it.category == "Total" }

        if (overallBudget != null) {
            val progress = overallBudget.percentage
            val animatedProgress by animateFloatAsState(
                targetValue = progress.toFloat(),
                animationSpec = tween(1000)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Canvas Drawing (Donut Chart)
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        val progressColor = when {
                            progress >= 1.0 -> Color(0xFFF43F5E) // Red/danger
                            progress >= 0.8 -> Color(0xFFEA580C) // Warning
                            else -> MaterialTheme.colorScheme.primary // Green/normal
                        }

                        Canvas(modifier = Modifier.size(80.dp)) {
                            // Track Arc
                            drawArc(
                                color = trackColor,
                                startAngle = -220f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Progress Arc
                            drawArc(
                                color = progressColor,
                                startAngle = -220f,
                                sweepAngle = (260f * animatedProgress),
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Right: Text Stats
                    Column {
                        Text(
                            text = if (overallBudget.period == "WEEKLY") "Weekly Overall Cap" else "Monthly Overall Cap",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Limit: ${currencyFormat.format(overallBudget.limit)} (${overallBudget.period.lowercase()})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Spent: ${currencyFormat.format(overallBudget.spent)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (overallBudget.spent > overallBudget.limit) Color(0xFFF43F5E) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = if (overallBudget.spent > overallBudget.limit) "⚠️ Limit Exceeded!" else "👍 Remaining is safe",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (overallBudget.spent > overallBudget.limit) Color(0xFFF43F5E) else Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Limits & Progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onAdjustBudget) {
                Text(
                    text = "Configure Limits",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Category Budgets Grid/List
        val listWithoutTotal = budgetsStatus.filter { it.category != "Total" }
        if (listWithoutTotal.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Budgets Set",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Configure weekly or monthly caps for your spending categories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listWithoutTotal) { item ->
                    CategoryBudgetCard(
                        status = item,
                        currencyFormat = currencyFormat
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryBudgetCard(
    status: BudgetStatus,
    currencyFormat: DecimalFormat
) {
    val overBudget = status.spent > status.limit
    val fillWidth = status.percentage.toFloat()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = getCategoryEmoji(status.category), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = status.category,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = status.period.lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Text(
                    text = "${currencyFormat.format(status.spent)} of ${currencyFormat.format(status.limit)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (overBudget) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Percentage Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val progressColor = if (overBudget) Color(0xFFF43F5E) else MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = fillWidth)
                        .clip(CircleShape)
                        .background(progressColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (overBudget) {
                        "Exceeded by ${currencyFormat.format(status.spent - status.limit)}"
                    } else {
                        "Remaining: ${currencyFormat.format(status.limit - status.spent)}"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (overBudget) Color(0xFFF43F5E) else Color(0xFF10B981)
                )
            }
        }
    }
}

// ==========================================
// VISUAL REPORTS TAB (PIE/BAR GRAPHS)
// ==========================================

@Composable
fun ReportsTab(
    reportsMetrics: com.example.ui.ReportsMetrics,
    currencyFormat: DecimalFormat
) {
    var periodTab by remember { mutableStateOf(1) } // 0: Weekly, 1: Monthly, 2: Yearly
    val activeSummary = when (periodTab) {
        0 -> reportsMetrics.weekly
        1 -> reportsMetrics.monthly
        else -> reportsMetrics.yearly
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(3.dp)
        ) {
            listOf("Weekly", "Monthly", "Yearly").forEachIndexed { index, label ->
                val selected = periodTab == index
                val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                val textCol by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .clickable { periodTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = textCol
                    )
                }
            }
        }

        // Summary Statistics Bento Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income & Expenses
            Card(
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Period Flow",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(currencyFormat.format(activeSummary.totalIncome), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = Color(0xFF10B981))
                    }
                    Column {
                        Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(currencyFormat.format(activeSummary.totalExpense), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = Color(0xFFF43F5E))
                    }
                }
            }

            // Net Savings Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeSummary.netSavings >= 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                ),
                border = BorderStroke(1.dp, if (activeSummary.netSavings >= 0) Color(0xFFD1FAE5) else Color(0xFFFEE2E2))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Net Savings",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (activeSummary.netSavings >= 0) Color(0xFF065F46) else Color(0xFF991B1B)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Column {
                        Text(
                            text = currencyFormat.format(activeSummary.netSavings),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = if (activeSummary.netSavings >= 0) Color(0xFF047857) else Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeSummary.netSavings >= 0) "Growth Zone" else "Deficit Alert",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeSummary.netSavings >= 0) Color(0xFF059669) else Color(0xFFEF4444)
                        )
                    }
                }
            }
        }

        // Pie Chart Visual Representation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Category Distributions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(20.dp))
                DoughnutChart(
                    data = activeSummary.categoryBreakdown,
                    currencyFormat = currencyFormat,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bar Chart Comparison Visual
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Ranking Category Spending",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                BarChart(
                    data = activeSummary.categoryBreakdown,
                    currencyFormat = currencyFormat,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun DoughnutChart(
    data: Map<String, Double>,
    currencyFormat: DecimalFormat,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total == 0.0) {
        Box(
            modifier = modifier
                .height(140.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No spendings recorded for this frame.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        return
    }

    // Modern Bento palette
    val sliceColors = listOf(
        Color(0xFF6750A4), // Purple
        Color(0xFF3B82F6), // Blue
        Color(0xFFEF4444), // Red
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Green
        Color(0xFFEC4899), // Pink
        Color(0xFF8B5CF6), // Indigo
        Color(0xFF14B8A6), // Teal
        Color(0xFFF97316), // Orange
        Color(0xFF6B7280)  // Gray
    )

    val entries = data.entries.toList().sortedByDescending { it.value }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Doughnut Canvas Chart
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = 0f
                entries.forEachIndexed { idx, entry ->
                    val sweepAngle = (entry.value / total * 360f).toFloat()
                    val color = sliceColors[idx % sliceColors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currencyFormat.format(total),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Legend Grid
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            entries.take(4).forEachIndexed { index, entry ->
                val color = sliceColors[index % sliceColors.size]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = entry.key,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${(entry.value / total * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (entries.size > 4) {
                val othersSum = entries.drop(4).sumOf { it.value }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                    Text(
                        text = "Others",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(othersSum / total * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun BarChart(
    data: Map<String, Double>,
    currencyFormat: DecimalFormat,
    modifier: Modifier = Modifier
) {
    val maxVal = data.values.maxOrNull() ?: 1.0
    val entries = data.entries.toList().sortedByDescending { it.value }.take(4)

    if (entries.isEmpty()) {
        Box(
            modifier = modifier
                .height(100.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No entries to prioritize.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        entries.forEach { entry ->
            val ratio = (entry.value / maxVal).toFloat()
            val animatedRatio by animateFloatAsState(targetValue = ratio, animationSpec = tween(800))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(getCategoryEmoji(entry.key), fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.key,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = currencyFormat.format(entry.value),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedRatio)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.primary
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

// ==========================================
// RECURRING TRANSACTIONS TAB UI
// ==========================================

@Composable
fun RecurringTab(
    recurringTransactions: List<RecurringTransactionEntity>,
    onDelete: (Int) -> Unit,
    currencyFormat: DecimalFormat
) {
    val forecastFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        // Bento Card explaining automatic scheduler sync
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoBlue.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, BentoBlue)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Automatic Sync",
                    tint = BentoDeepNavy,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Automated Engine Active",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BentoDeepNavy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Schedules trigger automatically as time progresses. Any backlogged payments will catch up on startup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoDeepNavy.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (recurringTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Recurring Templates Set",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Create templates like Rent or Subscriptions to automate calculations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = recurringTransactions,
                    key = { it.id }
                ) { rec ->
                    val nextExpectedDate = remember(rec.lastTriggered, rec.frequency) {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = if (rec.lastTriggered == 0L) rec.startDate else rec.lastTriggered
                        }
                        when (rec.frequency.uppercase()) {
                            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
                            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        calendar.timeInMillis
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = getCategoryEmoji(rec.category), fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = rec.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Next: " + forecastFormat.format(Date(nextExpectedDate)),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = rec.frequency.lowercase() + " • " + (rec.paymentMethod ?: "Cash"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (rec.type == "INCOME") "+" + currencyFormat.format(rec.amount) else "-" + currencyFormat.format(rec.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (rec.type == "INCOME") Color(0xFF10B981) else Color(0xFFF43F5E)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDelete(rec.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Cancel Schedule",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

// ==========================================
// ADD MANUALLY TRANSACTION DIALOG
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, type: String, category: String, description: String?, method: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME
    var category by remember { mutableStateOf("Food") }
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Card") }

    val expenseCategories = listOf("Food", "Shopping", "Entertainment", "Utilities", "Transport", "Health", "Other")
    val incomeCategories = listOf("Salary", "Freelance", "Investment", "Gift", "Other")
    val paymentMethods = remember { listOf("Card", "Cash", "Bank Transfer") }

    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .systemBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New Transaction",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add a transaction manually to your history ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Type Toggle (Expense vs Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "EXPENSE") Color(0xFFF43F5E).copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                type = "EXPENSE"
                                category = expenseCategories.first()
                            }
                            .padding(12.dp)
                            .testTag("add_dialog_expense_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💸 Expense",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "EXPENSE") Color(0xFFF43F5E) else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "INCOME") Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                type = "INCOME"
                                category = incomeCategories.first()
                            }
                            .padding(12.dp)
                            .testTag("add_dialog_income_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💰 Income",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "INCOME") Color(0xFF10B981) else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                            amountStr = it
                            amountError = false
                        }
                    },
                    label = { Text("Amount ($)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_dialog_amount_input"),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    isError = amountError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Label Description") },
                    placeholder = { Text("e.g. Weekly Groceries, Gas Station") },
                    modifier = Modifier.fillMaxWidth().testTag("add_dialog_title_input"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    isError = titleError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category selection list
                Text(
                    text = "Select Category Scope",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val targetCategories = if (type == "EXPENSE") expenseCategories else incomeCategories
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targetCategories) { catName ->
                        val isSelected = catName == category
                        val chipBgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        val chipContentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBgColor)
                                .clickable { category = catName }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = getCategoryEmoji(catName) + " " + catName,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = chipContentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Selector
                Text(
                    text = "Transaction Payout Method",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        val isSelected = method == paymentMethod
                        val btnBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        val btnTextColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        val border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(btnBg)
                                .then(if (border != null) Modifier.border(border, RoundedCornerShape(12.dp)) else Modifier)
                                .clickable { paymentMethod = method }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = btnTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val finalAmt = amountStr.toDoubleOrNull()
                            var valid = true
                            if (finalAmt == null || finalAmt <= 0) {
                                amountError = true
                                valid = false
                            }
                            if (title.isBlank()) {
                                titleError = true
                                valid = false
                            }

                            if (valid && finalAmt != null) {
                                onConfirm(
                                    title.trim(),
                                    finalAmt,
                                    type,
                                    category,
                                    if (description.isBlank()) null else description.trim(),
                                    paymentMethod
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("add_dialog_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ==========================================
// NEW RECURRING TRANSACTION DIALOG
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, type: String, category: String, frequency: String, description: String?, method: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME
    var category by remember { mutableStateOf("Entertainment") }
    var frequency by remember { mutableStateOf("MONTHLY") } // DAILY, WEEKLY, MONTHLY, YEARLY
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Card") }

    val expenseCategories = listOf("Food", "Shopping", "Entertainment", "Utilities", "Transport", "Health", "Other")
    val incomeCategories = listOf("Salary", "Freelance", "Investment", "Gift", "Other")
    val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
    val paymentMethods = remember { listOf("Card", "Cash", "Bank Transfer") }

    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .systemBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recurring Schedule",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure template to trigger transactions on elapsed intervals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Type Toggle Custom Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "EXPENSE") Color(0xFFF43F5E).copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                type = "EXPENSE"
                                category = expenseCategories.first()
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💸 Expense",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "EXPENSE") Color(0xFFF43F5E) else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "INCOME") Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                type = "INCOME"
                                category = incomeCategories.first()
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💰 Income",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "INCOME") Color(0xFF10B981) else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Textfield
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                            amountStr = it
                            amountError = false
                        }
                    },
                    label = { Text("Value ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    isError = amountError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title Textfield
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Schedule Label") },
                    placeholder = { Text("e.g. Netflix Subscription, Landlord Rent") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    isError = titleError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Frequency Chip Selector
                Text(
                    text = "Interval Freq",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    frequencies.forEach { freq ->
                        val isSelected = freq == frequency
                        val btnBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        val btnTextColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        val border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(btnBg)
                                .then(if (border != null) Modifier.border(border, RoundedCornerShape(10.dp)) else Modifier)
                                .clickable { frequency = freq }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = freq.lowercase(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = btnTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categories chip selector
                Text(
                    text = "Assign Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val targetCategories = if (type == "EXPENSE") expenseCategories else incomeCategories
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targetCategories) { catName ->
                        val isSelected = catName == category
                        val chipBgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        val chipContentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBgColor)
                                .clickable { category = catName }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                  text = getCategoryEmoji(catName) + " " + catName,
                                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                  color = chipContentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Selector
                Text(
                    text = "Payout Method",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        val isSelected = method == paymentMethod
                        val btnBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        val btnTextColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        val border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(btnBg)
                                .then(if (border != null) Modifier.border(border, RoundedCornerShape(12.dp)) else Modifier)
                                .clickable { paymentMethod = method }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = btnTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val finalAmt = amountStr.toDoubleOrNull()
                            var valid = true
                            if (finalAmt == null || finalAmt <= 0) {
                                amountError = true
                                valid = false
                            }
                            if (title.isBlank()) {
                                titleError = true
                                valid = false
                            }

                            if (valid && finalAmt != null) {
                                onConfirm(
                                    title.trim(),
                                    finalAmt,
                                    type,
                                    category,
                                    frequency,
                                    if (description.isBlank()) null else description.trim(),
                                    paymentMethod
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ==========================================
// SET BUDGET DIALOG WITH DURATION TOGGLES
// ==========================================

@Composable
fun SetBudgetDialog(
    existingBudgets: List<BudgetEntity>,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double, period: String) -> Unit,
    onDelete: (category: String) -> Unit
) {
    val categories = listOf("Total", "Food", "Shopping", "Entertainment", "Utilities", "Transport", "Health", "Other")
    var selectedCategory by remember { mutableStateOf("Total") }
    var limitAmountStr by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("MONTHLY") } // WEEKLY or MONTHLY
    var isInputError by remember { mutableStateOf(false) }

    val budgetsIndexed = remember(existingBudgets) {
        existingBudgets.associateBy { it.category }
    }

    // Populate the fields whenever category changes
    LaunchedEffect(selectedCategory) {
        val curBudget = budgetsIndexed[selectedCategory]
        limitAmountStr = if (curBudget != null && curBudget.limitAmount > 0) curBudget.limitAmount.toString() else ""
        selectedPeriod = curBudget?.period ?: "MONTHLY"
        isInputError = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Limit Controls",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure custom weekly or monthly caps to track and prevent overspendings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Selector for category
                Text(
                    text = "Scope Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        val chipText = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (cat == "Total") "📊 Total Cap" else getCategoryEmoji(cat) + " " + cat,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = chipText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Period Choice (Weekly vs Monthly)
                Text(
                    text = "Budgeting Period",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedPeriod == "WEEKLY") MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedPeriod = "WEEKLY" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗓️ Weekly",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedPeriod == "WEEKLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedPeriod == "MONTHLY") MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedPeriod = "MONTHLY" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📅 Monthly",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedPeriod == "MONTHLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Input Limit
                OutlinedTextField(
                    value = limitAmountStr,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                            limitAmountStr = it
                            isInputError = false
                        }
                    },
                    label = { Text("Budget Cap ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isInputError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current limit indicator
                val activeBudget = budgetsIndexed[selectedCategory]
                if (activeBudget != null && activeBudget.limitAmount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current: $${DecimalFormat("#,##0.00").format(activeBudget.limitAmount)} (${activeBudget.period.lowercase()})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                onDelete(selectedCategory)
                                limitAmountStr = ""
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete limit",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            val limitVal = limitAmountStr.toDoubleOrNull()
                            if (limitVal == null || limitVal <= 0) {
                                isInputError = true
                            } else {
                                onSave(selectedCategory, limitVal, selectedPeriod)
                                limitAmountStr = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

// Map categories to emojis nicely
fun getCategoryEmoji(category: String): String {
    return when (category) {
        "Food" -> "🍔"
        "Shopping" -> "🛍️"
        "Entertainment" -> "🎬"
        "Utilities" -> "⚡"
        "Transport" -> "🚗"
        "Health" -> "🏥"
        "Salary" -> "💼"
        "Freelance" -> "💻"
        "Investment" -> "📈"
        "Gift" -> "🎁"
        "Other" -> "🏷️"
        "Total" -> "📊"
        else -> "🏷️"
    }
}
