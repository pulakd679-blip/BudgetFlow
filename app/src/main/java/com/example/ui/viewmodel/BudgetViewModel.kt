package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.BackupStatus
import com.example.data.firebase.FirestoreBackupManager
import com.example.data.firebase.GoogleAuthManager
import com.example.data.model.CategoryBudget
import com.example.data.model.CategoryRegistry
import com.example.data.model.DailyBalanceSummary
import com.example.data.model.ReceiptLineItem
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.BudgetRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ScreenDestination {
    DASHBOARD,
    TRENDS,
    RECEIPT_SCANNER,
    CALCULATORS
}

data class MonthlyBarData(
    val monthName: String,
    val year: Int,
    val monthIndex: Int,
    val income: Double,
    val expense: Double
)

data class CategorySpendData(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class BudgetStatus(
    val category: String,
    val spent: Double,
    val limit: Double,
    val percentage: Float,
    val isOverBudget: Boolean
)

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = GoogleAuthManager(application)
    private val backupManager = FirestoreBackupManager(application)
    private val repository: BudgetRepository

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val backupStatus: StateFlow<BackupStatus> = backupManager.backupStatus
    val lastBackupTimeMillis: StateFlow<Long?> = backupManager.lastBackupTimeMillis

    private val _showGoogleConnectPrompt = MutableStateFlow(false)
    val showGoogleConnectPrompt: StateFlow<Boolean> = _showGoogleConnectPrompt.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = BudgetRepository(db.transactionDao(), db.budgetDao(), backupManager)
        viewModelScope.launch {
            repository.initializeCleanDatabase()

            // Try silent auto-sign in
            authManager.attemptAutoSignIn(
                scope = viewModelScope,
                onAuthSuccess = {
                    _showGoogleConnectPrompt.value = false
                    // If local is clean, auto-restore user's transactions from cloud
                    viewModelScope.launch {
                        backupManager.restoreFromCloud().onSuccess { (txs, budgets) ->
                            if (txs.isNotEmpty()) {
                                repository.restoreAllData(txs, budgets)
                            }
                        }
                    }
                },
                onUnauthenticated = {
                    // Show prompt on startup to connect Google account
                    _showGoogleConnectPrompt.value = true
                }
            )
        }
    }

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<CategoryBudget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen navigation
    private val _currentScreen = MutableStateFlow(ScreenDestination.DASHBOARD)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    // Selected Month & Year (0-indexed month)
    private val initialCal = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(initialCal.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(initialCal.get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    // Selected Date for day-by-day inspection (format: yyyy-MM-dd)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val _selectedDateString = MutableStateFlow(dateFormat.format(initialCal.time))
    val selectedDateString: StateFlow<String> = _selectedDateString.asStateFlow()

    fun setSelectedDate(dateMillis: Long) {
        _selectedDateString.value = dateFormat.format(dateMillis)
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH)
    }

    fun changeMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value)
            add(Calendar.MONTH, delta)
        }
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH)
    }

    // Day-by-Day Balance Summaries for selected month
    val dailySummariesForMonth: StateFlow<List<DailyBalanceSummary>> = combine(
        allTransactions,
        selectedYear,
        selectedMonth
    ) { transactions, year, month ->
        computeDailySummaries(transactions, year, month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall metrics (Dynamic, starting at 0.00)
    val totalBalance: StateFlow<Double> = allTransactions.combine(MutableStateFlow(0)) { txs, _ ->
        var balance = 0.0
        for (tx in txs) {
            when (tx.type) {
                TransactionType.INCOME -> balance += tx.amount
                TransactionType.EXPENSE -> balance -= tx.amount
                TransactionType.TRANSFER -> {}
            }
        }
        balance
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthIncome: StateFlow<Double> = combine(allTransactions, selectedYear, selectedMonth) { txs, y, m ->
        txs.filter { isSameMonth(it.dateMillis, y, m) && it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthExpense: StateFlow<Double> = combine(allTransactions, selectedYear, selectedMonth) { txs, y, m ->
        txs.filter { isSameMonth(it.dateMillis, y, m) && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayIncome: StateFlow<Double> = allTransactions.combine(_selectedDateString) { txs, dateStr ->
        txs.filter { dateFormat.format(it.dateMillis) == dateStr && it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayExpense: StateFlow<Double> = allTransactions.combine(_selectedDateString) { txs, dateStr ->
        txs.filter { dateFormat.format(it.dateMillis) == dateStr && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayTransactions: StateFlow<List<TransactionEntity>> = allTransactions.combine(_selectedDateString) { txs, dateStr ->
        txs.filter { dateFormat.format(it.dateMillis) == dateStr }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Trends & Visualizations
    val monthlyTrendBars: StateFlow<List<MonthlyBarData>> = allTransactions.combine(selectedYear) { txs, year ->
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val result = mutableListOf<MonthlyBarData>()
        for (m in 0..11) {
            val inc = txs.filter { isSameMonth(it.dateMillis, year, m) && it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = txs.filter { isSameMonth(it.dateMillis, year, m) && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            result.add(MonthlyBarData(monthNames[m], year, m, inc, exp))
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryBreakdown: StateFlow<List<CategorySpendData>> = combine(allTransactions, selectedYear, selectedMonth) { txs, y, m ->
        val monthExpenses = txs.filter { isSameMonth(it.dateMillis, y, m) && it.type == TransactionType.EXPENSE }
        val totalExp = monthExpenses.sumOf { it.amount }
        if (totalExp <= 0.0) {
            emptyList()
        } else {
            monthExpenses.groupBy { it.category }
                .map { (cat, list) ->
                    val catTotal = list.sumOf { it.amount }
                    val pct = (catTotal / totalExp).toFloat()
                    CategorySpendData(cat, catTotal, pct, list.size)
                }
                .sortedByDescending { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetStatuses: StateFlow<List<BudgetStatus>> = combine(allTransactions, allBudgets, selectedYear, selectedMonth) { txs, budgets, y, m ->
        val expensesByCategory = txs
            .filter { isSameMonth(it.dateMillis, y, m) && it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        budgets.map { b ->
            val spent = expensesByCategory[b.category] ?: 0.0
            val pct = if (b.monthlyLimit > 0) (spent / b.monthlyLimit).toFloat() else 0f
            BudgetStatus(
                category = b.category,
                spent = spent,
                limit = b.monthlyLimit,
                percentage = pct,
                isOverBudget = spent > b.monthlyLimit
            )
        }.sortedByDescending { it.percentage }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transaction Management Actions
    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        dateMillis: Long = System.currentTimeMillis(),
        note: String? = null,
        paymentMethod: String = "Cash",
        receiptUri: String? = null,
        receiptNote: String? = null,
        receiptItemsJson: String? = null
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title.ifBlank { if (type == TransactionType.INCOME) "Income" else "Expense" },
                    amount = amount,
                    type = type,
                    category = category,
                    dateMillis = dateMillis,
                    note = note,
                    paymentMethod = paymentMethod,
                    receiptUri = receiptUri,
                    receiptNote = receiptNote,
                    receiptItemsJson = receiptItemsJson
                )
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun saveBudget(category: String, limit: Double) {
        viewModelScope.launch {
            repository.saveBudget(CategoryBudget(category, limit))
        }
    }

    // Google Sign-In & Cloud Backup Management
    fun dismissGooglePrompt() {
        _showGoogleConnectPrompt.value = false
    }

    fun showGoogleConnect() {
        _showGoogleConnectPrompt.value = true
    }

    fun signInWithGoogle(
        activity: Activity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        authManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onAuthSuccess = {
                _showGoogleConnectPrompt.value = false
                // On successful sign-in, trigger initial sync of all current data
                viewModelScope.launch {
                    backupManager.performFullBackup(allTransactions.value, allBudgets.value)
                }
                onSuccess()
            },
            onAuthError = onError,
            onAuthCancelled = {}
        )
    }

    fun signOutGoogle() {
        authManager.signOut(viewModelScope)
    }

    fun triggerManualBackup(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            backupManager.performFullBackup(allTransactions.value, allBudgets.value)
                .onSuccess { count ->
                    onResult(true, "Successfully backed up $count records to cloud!")
                }
                .onFailure { error ->
                    onResult(false, error.localizedMessage ?: "Backup failed")
                }
        }
    }

    fun triggerRestoreFromCloud(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            backupManager.restoreFromCloud()
                .onSuccess { (txs, budgets) ->
                    if (txs.isNotEmpty() || budgets.isNotEmpty()) {
                        repository.restoreAllData(txs, budgets)
                        onResult(true, "Restored ${txs.size} transactions and ${budgets.size} budgets from cloud!")
                    } else {
                        onResult(true, "No remote records found in your cloud database.")
                    }
                }
                .onFailure { error ->
                    onResult(false, error.localizedMessage ?: "Restore failed")
                }
        }
    }

    // Modal Sheet State for Add/Edit Transaction
    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    val editingTransaction: StateFlow<TransactionEntity?> = _editingTransaction.asStateFlow()

    private val _prefilledType = MutableStateFlow(TransactionType.EXPENSE)
    val prefilledType: StateFlow<TransactionType> = _prefilledType.asStateFlow()

    fun openAddTransaction(type: TransactionType = TransactionType.EXPENSE) {
        _editingTransaction.value = null
        _prefilledType.value = type
        _isAddEditOpen.value = true
    }

    fun openEditTransaction(transaction: TransactionEntity) {
        _editingTransaction.value = transaction
        _prefilledType.value = transaction.type
        _isAddEditOpen.value = true
    }

    fun closeAddEditTransaction() {
        _isAddEditOpen.value = false
        _editingTransaction.value = null
    }

    // Receipt Scanner State
    private val _receiptImageUri = MutableStateFlow<Uri?>(null)
    val receiptImageUri: StateFlow<Uri?> = _receiptImageUri.asStateFlow()

    private val _receiptMerchant = MutableStateFlow("Store / Market")
    val receiptMerchant: StateFlow<String> = _receiptMerchant.asStateFlow()

    private val _receiptCategory = MutableStateFlow("Groceries")
    val receiptCategory: StateFlow<String> = _receiptCategory.asStateFlow()

    private val _receiptDateMillis = MutableStateFlow(System.currentTimeMillis())
    val receiptDateMillis: StateFlow<Long> = _receiptDateMillis.asStateFlow()

    private val _receiptItems = MutableStateFlow<List<ReceiptLineItem>>(emptyList())
    val receiptItems: StateFlow<List<ReceiptLineItem>> = _receiptItems.asStateFlow()

    private val _receiptTaxPercent = MutableStateFlow(8.0)
    val receiptTaxPercent: StateFlow<Double> = _receiptTaxPercent.asStateFlow()

    private val _receiptTipAmount = MutableStateFlow(0.0)
    val receiptTipAmount: StateFlow<Double> = _receiptTipAmount.asStateFlow()

    fun setReceiptImageUri(uri: Uri?) {
        _receiptImageUri.value = uri
    }

    fun updateReceiptMerchant(name: String) {
        _receiptMerchant.value = name
    }

    fun updateReceiptCategory(cat: String) {
        _receiptCategory.value = cat
    }

    fun updateReceiptTaxPercent(tax: Double) {
        _receiptTaxPercent.value = tax
    }

    fun updateReceiptTipAmount(tip: Double) {
        _receiptTipAmount.value = tip
    }

    fun addReceiptItem(name: String, price: Double, qty: Int = 1) {
        val current = _receiptItems.value.toMutableList()
        current.add(ReceiptLineItem(name = name.ifBlank { "Item ${current.size + 1}" }, price = price, quantity = qty))
        _receiptItems.value = current
    }

    fun removeReceiptItem(id: String) {
        _receiptItems.value = _receiptItems.value.filterNot { it.id == id }
    }

    fun updateReceiptItem(id: String, name: String, price: Double, qty: Int) {
        _receiptItems.value = _receiptItems.value.map {
            if (it.id == id) it.copy(name = name, price = price, quantity = qty) else it
        }
    }

    fun parseReceiptRawText(text: String) {
        val lines = text.split("\n").filter { it.isNotBlank() }
        val newItems = mutableListOf<ReceiptLineItem>()
        val priceRegex = """(?:\$|€|£)?\s*([0-9]+(?:[\.,][0-9]{2})?)""".toRegex()

        for (line in lines) {
            val match = priceRegex.findAll(line).lastOrNull()
            if (match != null) {
                val priceVal = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: continue
                if (priceVal > 0.0) {
                    val rawName = line.substring(0, match.range.first).trim().trim(':', '-', '$')
                    val name = if (rawName.isBlank()) "Item ${newItems.size + 1}" else rawName
                    newItems.add(ReceiptLineItem(name = name, price = priceVal, quantity = 1))
                }
            }
        }
        if (newItems.isNotEmpty()) {
            _receiptItems.value = newItems
        }
    }

    fun saveScannedReceiptAsExpense(onSuccess: () -> Unit) {
        val items = _receiptItems.value
        val subtotal = items.sumOf { it.total }
        val tax = subtotal * (_receiptTaxPercent.value / 100.0)
        val grandTotal = subtotal + tax + _receiptTipAmount.value

        if (grandTotal <= 0.0) return

        val itemsSummary = items.joinToString(", ") { "${it.name} (${it.quantity}x \$${String.format(Locale.US, "%.2f", it.price)})" }
        val receiptNote = "Scanned Receipt - Subtotal: \$${String.format(Locale.US, "%.2f", subtotal)} | Tax: \$${String.format(Locale.US, "%.2f", tax)} | Tip: \$${String.format(Locale.US, "%.2f", _receiptTipAmount.value)}\nItems: $itemsSummary"

        addTransaction(
            title = _receiptMerchant.value.ifBlank { "Receipt Expense" },
            amount = grandTotal,
            type = TransactionType.EXPENSE,
            category = _receiptCategory.value,
            dateMillis = _receiptDateMillis.value,
            note = receiptNote,
            paymentMethod = "Debit Card",
            receiptUri = _receiptImageUri.value?.toString(),
            receiptNote = receiptNote
        )
        onSuccess()
    }

    private fun isSameMonth(millis: Long, year: Int, month: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }

    private fun computeDailySummaries(transactions: List<TransactionEntity>, year: Int, month: Int): List<DailyBalanceSummary> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val transactionsInMonth = transactions.filter { isSameMonth(it.dateMillis, year, month) }
            .groupBy { keyFormat.format(it.dateMillis) }

        val startOfMonthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonthMillis = startOfMonthCal.timeInMillis

        var runningBalance = transactions.filter { it.dateMillis < startOfMonthMillis }
            .fold(0.0) { acc, tx ->
                when (tx.type) {
                    TransactionType.INCOME -> acc + tx.amount
                    TransactionType.EXPENSE -> acc - tx.amount
                    TransactionType.TRANSFER -> acc
                }
            }

        val list = mutableListOf<DailyBalanceSummary>()
        for (day in 1..daysInMonth) {
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 12)
            }
            val key = keyFormat.format(dayCal.time)
            val dayTxs = transactionsInMonth[key] ?: emptyList()
            val dayInc = dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val dayExp = dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val netChange = dayInc - dayExp
            runningBalance += netChange

            list.add(
                DailyBalanceSummary(
                    dateKey = key,
                    dayOfMonth = day,
                    dayOfWeek = dayOfWeekFormat.format(dayCal.time),
                    fullDateString = fullDateFormat.format(dayCal.time),
                    dateMillis = dayCal.timeInMillis,
                    incomeTotal = dayInc,
                    expenseTotal = dayExp,
                    netDayChange = netChange,
                    cumulativeBalance = runningBalance,
                    transactions = dayTxs.sortedByDescending { it.dateMillis }
                )
            )
        }
        return list
    }
}
