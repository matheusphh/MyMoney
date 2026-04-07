package com.mts.mymoney

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.provider.Settings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mts.mymoney.ui.components.NewAccountDialog
import com.mts.mymoney.ui.screens.DashboardScreen
import com.mts.mymoney.ui.screens.TransactionHistoryScreen
import com.mts.mymoney.viewmodel.FinanceViewModel

const val SCREEN_DASHBOARD = "dashboard"
const val SCREEN_HISTORY = "history"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    val accounts by viewModel.accountsFlow.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle()

    var currentScreen by rememberSaveable { mutableStateOf(SCREEN_DASHBOARD) }
    var showNewAccountDialog by remember { mutableStateOf(false) }

    if (accounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    BackHandler(enabled = currentScreen == SCREEN_HISTORY) {
        currentScreen = SCREEN_DASHBOARD
    }

    when (currentScreen) {
        SCREEN_DASHBOARD -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("\uD83D\uDCB5 Minhas finanças", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        actions = {
                            val context = LocalContext.current
                            IconButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null
                                )
                            }
                            IconButton(onClick = { showNewAccountDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Nova Conta", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                if (accounts.isNotEmpty()) {
                    DashboardScreen(
                        modifier = Modifier.padding(paddingValues),
                        accounts = accounts,
                        transactions = transactions,
                        onAddTransaction = { accountId, desc, amount, isIncome ->
                            viewModel.addTransaction(accountId, desc, amount, isIncome)
                        },
                        onDeleteAccount = { viewModel.deleteAccount(it) },
                        onNavigateToHistory = { currentScreen = SCREEN_HISTORY }
                    )
                }
            }
        }
        SCREEN_HISTORY -> {
            TransactionHistoryScreen(
                accounts = accounts,
                transactions = transactions,
                onBack = { currentScreen = SCREEN_DASHBOARD }
            )
        }
    }

    if (showNewAccountDialog) {
        NewAccountDialog(
            onDismiss = { showNewAccountDialog = false },
            onConfirm = { name, pkg, balanceStr ->
                val parsedBalance = balanceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                viewModel.addAccount(name, pkg, parsedBalance)
                showNewAccountDialog = false
            }
        )
    }
}

