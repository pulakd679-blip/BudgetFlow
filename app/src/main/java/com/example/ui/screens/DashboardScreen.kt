package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyBalanceSummary
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CloudBackupManagerDialog
import com.example.ui.components.GoogleConnectPromptDialog
import com.example.ui.components.HeroBalanceCard
import com.example.ui.components.TransactionItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.ScreenDestination
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val monthIncome by viewModel.currentMonthIncome.collectAsStateWithLifecycle()
    val monthExpense by viewModel.currentMonthExpense.collectAsStateWithLifecycle()
    val todayIncome by viewModel.todayIncome.collectAsStateWithLifecycle()
    val todayExpense by viewModel.todayExpense.collectAsStateWithLifecycle()
    val dailySummaries by viewModel.dailySummariesForMonth.collectAsStateWithLifecycle()
    val selectedDateString by viewModel.selectedDateString.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val lastBackupTimeMillis by viewModel.lastBackupTimeMillis.collectAsStateWithLifecycle()
    val showGoogleConnectPrompt by viewModel.showGoogleConnectPrompt.collectAsStateWithLifecycle()

    var showCloudDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var showAllTransactions by remember { mutableStateOf(false) }

    val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val currentMonthLabel = "${monthNames.getOrElse(selectedMonth) { "" }} $selectedYear"

    // Find the summary corresponding to the selected day
    val activeDaySummary = dailySummaries.firstOrNull { it.dateKey == selectedDateString }
        ?: dailySummaries.lastOrNull()

    val dayScrollState = rememberScrollState()

    // Auto-scroll to current day in the month strip
    LaunchedEffect(dailySummaries, selectedDateString) {
        val selectedIdx = dailySummaries.indexOfFirst { it.dateKey == selectedDateString }
        if (selectedIdx >= 0) {
            val targetOffset = (selectedIdx * 64 - 120).coerceAtLeast(0)
            dayScrollState.animateScrollTo(targetOffset)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Title & Tagline
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BudgetFlow",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Daily Cashflow & Expense Tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cloud Sync Badge / Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentUser != null) IncomeGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCloudDialog = true }
                                .testTag("cloud_backup_badge_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                    contentDescription = "Cloud Backup",
                                    tint = if (currentUser != null) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentUser != null) "Auto Sync" else "Backup",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (currentUser != null) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Quick Jump to Today
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSelectedDate(System.currentTimeMillis())
                                }
                                .testTag("jump_to_today_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Hero Balance Card
            item {
                HeroBalanceCard(
                    totalBalance = totalBalance,
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    todayIncome = todayIncome,
                    todayExpense = todayExpense,
                    onAddExpenseClick = { viewModel.openAddTransaction(TransactionType.EXPENSE) },
                    onAddIncomeClick = { viewModel.openAddTransaction(TransactionType.INCOME) },
                    onScanReceiptClick = { viewModel.navigateTo(ScreenDestination.RECEIPT_SCANNER) },
                    onOpenCalculatorClick = { viewModel.navigateTo(ScreenDestination.CALCULATORS) }
                )
            }

            // Month Navigation Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.changeMonth(-1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = currentMonthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { viewModel.changeMonth(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Day-to-Day Balance Slider / Calendar Strip
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(dayScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dailySummaries.forEach { summary ->
                            val isSelected = summary.dateKey == selectedDateString
                            val hasActivity = summary.transactions.isNotEmpty()

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .width(58.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.setSelectedDate(summary.dateMillis)
                                    }
                                    .testTag("day_chip_${summary.dayOfMonth}"),
                                shadowElevation = if (isSelected) 3.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = summary.dayOfWeek.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = summary.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Indicator dot (Green if net positive, red if net negative, gray if none)
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = when {
                                                    !hasActivity -> Color.Transparent
                                                    summary.netDayChange > 0 -> if (isSelected) Color.White else IncomeGreen
                                                    summary.netDayChange < 0 -> if (isSelected) Color(0xFFFCA5A5) else ExpenseRed
                                                    else -> MaterialTheme.colorScheme.outline
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Flow Card (Income, Expense, Net, and Ending Cumulative Balance)
            item {
                if (activeDaySummary != null) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeDaySummary.fullDateString,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "Closing: \$${String.format(Locale.US, "%,.2f", activeDaySummary.cumulativeBalance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Day's Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "+\$${String.format(Locale.US, "%.2f", activeDaySummary.incomeTotal)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }

                                Column {
                                    Text("Day's Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "-\$${String.format(Locale.US, "%.2f", activeDaySummary.expenseTotal)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val net = activeDaySummary.netDayChange
                                    Text(
                                        "${if (net >= 0) "+" else ""}\$${String.format(Locale.US, "%.2f", net)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (net >= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transactions Header & Mode Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showAllTransactions) "All Transactions" else "Selected Day Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { showAllTransactions = !showAllTransactions }) {
                        Text(if (showAllTransactions) "Show Day" else "View All")
                    }
                }
            }

            // Search & Filter (if viewing all)
            if (showAllTransactions) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search transactions, merchants, notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(null to "All", TransactionType.EXPENSE to "Expenses", TransactionType.INCOME to "Income").forEach { (type, label) ->
                            val isSelected = filterType == type
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { filterType = type }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Filtered Transactions List
            val displayedTransactions = if (showAllTransactions) {
                allTransactions.filter { tx ->
                    (filterType == null || tx.type == filterType) &&
                    (searchQuery.isBlank() || tx.title.contains(searchQuery, ignoreCase = true) ||
                     tx.category.contains(searchQuery, ignoreCase = true) ||
                     (tx.note?.contains(searchQuery, ignoreCase = true) == true))
                }
            } else {
                activeDaySummary?.transactions ?: emptyList()
            }

            if (displayedTransactions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (showAllTransactions) "No transactions match your search" else "No transactions on this day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the + button below to log an income, expense, or scan a receipt.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(displayedTransactions, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        onClick = { viewModel.openEditTransaction(tx) },
                        onEdit = { viewModel.openEditTransaction(tx) },
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
            }
        }

        // Floating Action Button to Add
        FloatingActionButton(
            onClick = { viewModel.openAddTransaction(TransactionType.EXPENSE) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .testTag("fab_add_transaction")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
        }

        // First Launch Google Account Connect Dialog
        GoogleConnectPromptDialog(
            isOpen = showGoogleConnectPrompt,
            onDismiss = { viewModel.dismissGooglePrompt() },
            onSignInClicked = { activity ->
                viewModel.signInWithGoogle(
                    activity = activity,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Connected Google Account! Background backup enabled.")
                        }
                    },
                    onError = { err ->
                        scope.launch {
                            snackbarHostState.showSnackbar(err)
                        }
                    }
                )
            }
        )

        // Cloud Backup & Account Management Dialog
        CloudBackupManagerDialog(
            isOpen = showCloudDialog,
            currentUser = currentUser,
            backupStatus = backupStatus,
            lastBackupTimeMillis = lastBackupTimeMillis,
            onDismiss = { showCloudDialog = false },
            onSignIn = { activity ->
                viewModel.signInWithGoogle(
                    activity = activity,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Connected! Background backup enabled.")
                        }
                    },
                    onError = { err ->
                        scope.launch {
                            snackbarHostState.showSnackbar(err)
                        }
                    }
                )
            },
            onSignOut = {
                viewModel.signOutGoogle()
                scope.launch {
                    snackbarHostState.showSnackbar("Signed out of Google Account.")
                }
            },
            onManualBackup = {
                viewModel.triggerManualBackup { success, message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            },
            onManualRestore = {
                viewModel.triggerRestoreFromCloud { success, message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp)
        )
    }
}
