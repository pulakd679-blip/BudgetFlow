package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReceiptScanScreen
import com.example.ui.screens.TrendsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BudgetFlowApp()
            }
        }
    }
}

@Composable
fun BudgetFlowApp(viewModel: BudgetViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val prefilledType by viewModel.prefilledType.collectAsStateWithLifecycle()

    // Handle system back navigation to return to Dashboard
    if (currentScreen != ScreenDestination.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(ScreenDestination.DASHBOARD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Dashboard Item
                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.DASHBOARD,
                    onClick = { viewModel.navigateTo(ScreenDestination.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenDestination.DASHBOARD) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = {
                        Text(
                            text = "Dashboard",
                            fontWeight = if (currentScreen == ScreenDestination.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_dashboard")
                )

                // Trends Item
                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.TRENDS,
                    onClick = { viewModel.navigateTo(ScreenDestination.TRENDS) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenDestination.TRENDS) Icons.Filled.ShowChart else Icons.Outlined.ShowChart,
                            contentDescription = "Trends"
                        )
                    },
                    label = {
                        Text(
                            text = "Trends",
                            fontWeight = if (currentScreen == ScreenDestination.TRENDS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_trends")
                )

                // Receipt Scanner Item
                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.RECEIPT_SCANNER,
                    onClick = { viewModel.navigateTo(ScreenDestination.RECEIPT_SCANNER) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenDestination.RECEIPT_SCANNER) Icons.Filled.DocumentScanner else Icons.Outlined.DocumentScanner,
                            contentDescription = "Scanner"
                        )
                    },
                    label = {
                        Text(
                            text = "Receipts",
                            fontWeight = if (currentScreen == ScreenDestination.RECEIPT_SCANNER) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_scanner")
                )

                // Calculators Item
                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.CALCULATORS,
                    onClick = { viewModel.navigateTo(ScreenDestination.CALCULATORS) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenDestination.CALCULATORS) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                            contentDescription = "Calculators"
                        )
                    },
                    label = {
                        Text(
                            text = "Calculator",
                            fontWeight = if (currentScreen == ScreenDestination.CALCULATORS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_calculator")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    ScreenDestination.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    ScreenDestination.TRENDS -> TrendsScreen(viewModel = viewModel)
                    ScreenDestination.RECEIPT_SCANNER -> ReceiptScanScreen(viewModel = viewModel)
                    ScreenDestination.CALCULATORS -> CalculatorScreen(viewModel = viewModel)
                }
            }

            // Modal Sheet for Add/Edit Transaction
            AddEditTransactionSheet(
                isOpen = isAddEditOpen,
                editingTransaction = editingTransaction,
                defaultType = prefilledType,
                onDismiss = { viewModel.closeAddEditTransaction() },
                onSave = { title, amount, type, category, dateMillis, note, paymentMethod, receiptUri ->
                    if (editingTransaction != null) {
                        viewModel.updateTransaction(
                            editingTransaction!!.copy(
                                title = title,
                                amount = amount,
                                type = type,
                                category = category,
                                dateMillis = dateMillis,
                                note = note,
                                paymentMethod = paymentMethod,
                                receiptUri = receiptUri
                            )
                        )
                    } else {
                        viewModel.addTransaction(
                            title = title,
                            amount = amount,
                            type = type,
                            category = category,
                            dateMillis = dateMillis,
                            note = note,
                            paymentMethod = paymentMethod,
                            receiptUri = receiptUri
                        )
                    }
                },
                onDelete = { tx ->
                    viewModel.deleteTransaction(tx)
                }
            )
        }
    }
}
