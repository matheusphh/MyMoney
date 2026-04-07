package com.mts.mymoney.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mts.mymoney.ui.theme.ExpenseColor
import com.mts.mymoney.ui.theme.IncomeColor
import com.mts.mymoney.data.AccountEntity
import com.mts.mymoney.data.TransactionEntity

@Composable
fun DashboardHeader(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    viewFilterAccount: AccountEntity?,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    val relevantTransactions = if (viewFilterAccount == null) transactions else transactions.filter { it.accountId == viewFilterAccount.id }
    val displayBalance = relevantTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (viewFilterAccount == null) "Saldo Geral" else "Saldo: ${viewFilterAccount.name}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "R$ ${"%.2f".format(displayBalance)}",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (displayBalance >= 0) IncomeColor else ExpenseColor,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(accounts) { account ->
                val accountBalance = transactions
                    .filter { it.accountId == account.id }
                    .sumOf { if (it.isIncome) it.amount else -it.amount }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.size(width = 156.dp, height = 100.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 24.dp)
                            )
                            Text(
                                text = "R$ ${"%.2f".format(accountBalance)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (accountBalance >= 0) IncomeColor else ExpenseColor
                            )
                        }

                        if (accounts.size > 1) {
                            IconButton(
                                onClick = { onDeleteAccount(account) },
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir Conta", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))                         }
                        }
                    }
                }
            }
        }
    }
}
