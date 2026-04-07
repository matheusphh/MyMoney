package com.mts.mymoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mts.mymoney.data.AccountEntity
import com.mts.mymoney.data.TransactionEntity
import com.mts.mymoney.ui.components.TransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit
) {
    var selectedFilterAccount by remember { mutableStateOf<AccountEntity?>(null) }

    val filteredTransactions = if (selectedFilterAccount == null) {
        transactions
    } else {
        transactions.filter { it.accountId == selectedFilterAccount!!.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Movimentações", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text("Filtrar por conta:", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterAccount == null,
                        onClick = { selectedFilterAccount = null },
                        label = { Text("Todas") },
                        leadingIcon = { if (selectedFilterAccount == null) Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
                    )
                }
                items(accounts) { account ->
                    FilterChip(
                        selected = selectedFilterAccount?.id == account.id,
                        onClick = { selectedFilterAccount = account },
                        label = { Text(account.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Nenhuma movimentação encontrada.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredTransactions) { transaction ->
                        val accName = accounts.find { it.id == transaction.accountId }?.name ?: "Desconhecida"
                        TransactionItem(transaction, accName)
                    }
                }
            }
        }
    }
}
