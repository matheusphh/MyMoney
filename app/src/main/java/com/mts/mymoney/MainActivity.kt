package com.mts.mymoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import androidx.compose.material.icons.filled.Delete

// 1. Modelos de Dados
data class Account(val id: String = UUID.randomUUID().toString(), val name: String)
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val description: String,
    val amount: Double,
    val isIncome: Boolean
)

// 2. Paleta Material 3 Escura (Expressive)
val FinanceDarkColorScheme = darkColorScheme(
    primary = Color(0xFF82D5C8),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFA2F2E4),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF292A2D),
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF141218),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val IncomeColor = Color(0xFF81C784)
val ExpenseColor = Color(0xFFFFB4AB)

// 3. Activity Principal
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = FinanceDarkColorScheme) {
                FinanceApp()
            }
        }
    }
}

// 4. Estrutura do Ecrã
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp() {
    var accounts by remember { mutableStateOf(listOf(Account(name = "Carteira Física"), Account(name = "Banco Principal"))) }
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var showNewAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "As Minhas Finanças",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { showNewAccountDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nova Conta", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        FinanceScreenContent(
            modifier = Modifier.padding(paddingValues),
            accounts = accounts,
            transactions = transactions,
            onAddTransaction = { transactions = listOf(it) + transactions },
            onAddAccount = {
                accounts = accounts + it
                showNewAccountDialog = false
            },
            // NOVO: Lógica que remove a conta e limpa as transações dela
            onDeleteAccount = { accountToRemove ->
                accounts = accounts.filter { it.id != accountToRemove.id }
                transactions = transactions.filter { it.accountId != accountToRemove.id }
            }
        )

        if (showNewAccountDialog) {
            NewAccountDialog(
                onDismiss = { showNewAccountDialog = false },
                onConfirm = { name ->
                    accounts = accounts + Account(name = name)
                    showNewAccountDialog = false
                }
            )
        }
    }
}

// 5. Conteúdo Principal da Interface
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreenContent(
    modifier: Modifier = Modifier,
    accounts: List<Account>,
    transactions: List<Transaction>,
    onAddTransaction: (Transaction) -> Unit,
    onAddAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit // NOVO: Passando a ação de deletar para a tela principal
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }
    var selectedAccount by remember { mutableStateOf(accounts.first()) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var viewFilterAccount by remember { mutableStateOf<Account?>(null) }

    // NOVO: Estado para controlar o diálogo de exclusão
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    val filteredTransactions = if (viewFilterAccount == null) transactions else transactions.filter { it.accountId == viewFilterAccount!!.id }

    // Garante que a conta selecionada existe na lista
    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && !accounts.contains(selectedAccount)) {
            selectedAccount = accounts.last()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        DashboardHeader(
            accounts = accounts,
            transactions = transactions,
            viewFilterAccount = viewFilterAccount,
            onDeleteAccount = { accountToDelete = it } // NOVO: Aciona o diálogo ao clicar na lixeira
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
            // Formulário Expressive
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nova Transação", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = accountDropdownExpanded,
                        onExpandedChange = { accountDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount.name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        selectedAccount = account
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descrição") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Valor") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Despesa", color = if (!isIncome) ExpenseColor else Color.Gray, fontSize = 14.sp)
                            Switch(
                                checked = isIncome,
                                onCheckedChange = { isIncome = it },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = IncomeColor,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                                    uncheckedTrackColor = ExpenseColor
                                )
                            )
                            Text("Receita", color = if (isIncome) IncomeColor else Color.Gray, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                val amountValue = amount.replace(",", ".").toDoubleOrNull()
                                if (description.isNotBlank() && amountValue != null) {
                                    onAddTransaction(Transaction(
                                        accountId = selectedAccount.id,
                                        description = description,
                                        amount = amountValue,
                                        isIncome = isIncome
                                    ))
                                    description = ""
                                    amount = ""
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Adicionar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Histórico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Movimentações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FilterChip(
                    selected = viewFilterAccount != null,
                    onClick = { viewFilterAccount = if (viewFilterAccount == null) selectedAccount else null },
                    label = { Text(if (viewFilterAccount == null) "Todas" else selectedAccount.name) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Filtrar") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(filteredTransactions) { transaction ->
                    val accName = accounts.find { it.id == transaction.accountId }?.name ?: "Desconhecida"
                    TransactionItem(transaction, accName)
                }
            }
        }
    }

    // NOVO: Diálogo de Confirmação de Exclusão
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Excluir Conta") },
            text = { Text("Tem certeza que deseja excluir a conta '${accountToDelete!!.name}'? Todas as movimentações vinculadas a ela também serão apagadas. Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        // Se a conta que estava filtrando a tela for excluída, removemos o filtro
                        if (viewFilterAccount?.id == accountToDelete!!.id) {
                            viewFilterAccount = null
                        }
                        onDeleteAccount(accountToDelete!!)
                        accountToDelete = null
                    }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// 6. Componente do Dashboard (Saldo Centralizado + Blocos de Contas)
@Composable
fun DashboardHeader(
    accounts: List<Account>,
    transactions: List<Transaction>,
    viewFilterAccount: Account?,
    onDeleteAccount: (Account) -> Unit // NOVO
) {
    val filteredTransactions = if (viewFilterAccount == null) transactions else transactions.filter { it.accountId == viewFilterAccount.id }
    val displayBalance = filteredTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (viewFilterAccount == null) "Saldo Geral" else "Saldo: ${viewFilterAccount.name}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
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
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(accounts) { account ->
                val accountBalance = transactions
                    .filter { it.accountId == account.id }
                    .sumOf { if (it.isIncome) it.amount else -it.amount }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.size(width = 150.dp, height = 100.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Textos normais da conta
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 24.dp) // Abre espaço para não encavalar no ícone
                            )
                            Text(
                                text = "R$ ${"%.2f".format(accountBalance)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (accountBalance >= 0) IncomeColor else ExpenseColor
                            )
                        }

                        // NOVO: Botão de Lixeira (Só aparece se houver mais de 1 conta)
                        if (accounts.size > 1) {
                            IconButton(
                                onClick = { onDeleteAccount(account) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp) // Ajuste fino da posição
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir Conta",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7. Item da Lista de Transações
@Composable
fun TransactionItem(transaction: Transaction, accountName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = transaction.description, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(text = accountName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "${if (transaction.isIncome) "+" else "-"} R$ ${"%.2f".format(transaction.amount)}",
                color = if (transaction.isIncome) IncomeColor else ExpenseColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 8. Diálogo de Nova Conta
@Composable
fun NewAccountDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Conta") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome (ex: Nubank)") },
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Gravar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)
    )
}