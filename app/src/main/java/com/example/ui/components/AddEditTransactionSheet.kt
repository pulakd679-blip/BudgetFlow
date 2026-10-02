package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.example.data.model.CategoryRegistry
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    isOpen: Boolean,
    editingTransaction: TransactionEntity?,
    defaultType: TransactionType,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        dateMillis: Long,
        note: String?,
        paymentMethod: String,
        receiptUri: String?
    ) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember(editingTransaction, defaultType) {
        mutableStateOf(editingTransaction?.type ?: defaultType)
    }
    var amountText by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }
    var title by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.title ?: "")
    }
    var category by remember(editingTransaction, type) {
        mutableStateOf(
            editingTransaction?.category ?: if (type == TransactionType.INCOME) "Salary" else "Groceries"
        )
    }
    var dateMillis by remember(editingTransaction) {
        mutableLongStateOf(editingTransaction?.dateMillis ?: System.currentTimeMillis())
    }
    var note by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }
    var paymentMethod by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.paymentMethod ?: "Credit Card")
    }
    var receiptUri by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.receiptUri)
    }

    var showKeypad by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    val availableCategories = if (type == TransactionType.INCOME) {
        CategoryRegistry.incomeCategories
    } else {
        CategoryRegistry.expenseCategories
    }

    val paymentMethods = listOf("Credit Card", "Debit Card", "Cash", "Bank Transfer", "Mobile Wallet")
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction == null) "New Transaction" else "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Income / Expense Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Expense Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            type = TransactionType.EXPENSE
                            if (CategoryRegistry.incomeCategories.any { it.name == category }) {
                                category = "Groceries"
                            }
                        }
                        .testTag("toggle_expense_type"),
                    color = if (type == TransactionType.EXPENSE) ExpenseRed else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                // Income Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            type = TransactionType.INCOME
                            if (CategoryRegistry.expenseCategories.any { it.name == category }) {
                                category = "Salary"
                            }
                        }
                        .testTag("toggle_income_type"),
                    color = if (type == TransactionType.INCOME) IncomeGreen else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = if (type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (amountText.isBlank()) "0.00" else amountText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (amountText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { showKeypad = !showKeypad },
                        modifier = Modifier
                            .background(
                                if (showKeypad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                CircleShape
                            )
                            .testTag("toggle_calc_keypad_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Toggle Calculator Keypad",
                            tint = if (showKeypad) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick Calc Keypad Collapsible
            AnimatedVisibility(visible = showKeypad) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    QuickCalcKeypad(
                        currentExpression = amountText,
                        onExpressionChanged = { amountText = it },
                        onCalculateDone = {
                            amountText = String.format(Locale.US, "%.2f", it)
                            showKeypad = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title / Description
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title or Merchant") },
                placeholder = { Text(if (type == TransactionType.INCOME) "e.g. Monthly Salary, Freelance project" else "e.g. Trader Joe's, Starbucks coffee") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { cat ->
                    val isSelected = category == cat.name
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) cat.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) cat.color else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { category = cat.name }
                            .testTag("category_chip_${cat.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Method
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = paymentMethod == method
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { paymentMethod = method }
                    ) {
                        Text(
                            text = method,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date & Receipt Photo Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Selector Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormat.format(dateMillis),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                // Receipt Photo Attach Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (receiptUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("attach_receipt_photo_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Attach Receipt",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (receiptUri != null) "Receipt Attached" else "Attach Receipt",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Receipt preview thumbnail if attached
            if (receiptUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    AsyncImage(
                        model = receiptUri,
                        contentDescription = "Receipt preview",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { receiptUri = null }) {
                        Text("Remove Photo", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notes
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Receipt details, tax notes, etc.") },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editingTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(editingTransaction)
                            onDismiss()
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .testTag("delete_tx_btn")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Button(
                    onClick = {
                        val parsedAmount = evaluateSimpleExpression(amountText) ?: 0.0
                        if (parsedAmount > 0.0) {
                            onSave(
                                title.ifBlank { if (type == TransactionType.INCOME) "Income" else "Expense" },
                                parsedAmount,
                                type,
                                category,
                                dateMillis,
                                note.ifBlank { null },
                                paymentMethod,
                                receiptUri
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_transaction_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (editingTransaction == null) "Save Transaction" else "Update Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            dateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
