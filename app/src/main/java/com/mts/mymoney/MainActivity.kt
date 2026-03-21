package com.mts.mymoney // Certifique-se de que este é o pacote correto do seu projeto

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.flow.first

// ==========================================
// 1. CAMADA DE DADOS (ROOM ENTITIES & DAO)
// ==========================================

// Define a tabela de Contas
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String
)

// Define a tabela de Transações
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val accountId: String, // Chave estrangeira (vinculação)
    val description: String,
    val amount: Double,
    val isIncome: Boolean
)

@Dao
interface FinanceDao {
    // --- Operações de Contas ---
    @Query("SELECT * FROM accounts")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>> // Retorna atualizações em tempo real

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    // --- Operações de Transações ---
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Deleta todas as transações de uma conta específica (integridade)
    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccount(accountId: String)
}

// ==========================================
// 2. CONFIGURAÇÃO DO BANCO DE DADOS (ROOM)
// ==========================================

@Database(entities = [AccountEntity::class, TransactionEntity::class], version = 1)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    // Padrão Singleton para garantir instância única
    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database"
                )
                    .fallbackToDestructiveMigration() // Recria o DB se houver mudança de versão sem migração definida
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 3. CAMADA DE VIEWMODEL (PONTE ENTRE UI E DB)
// ==========================================

class FinanceViewModel(private val dao: FinanceDao) : ViewModel() {

    // Transforma o Flow do Room em um StateFlow observável pelo Compose
    val accountsFlow: StateFlow<List<AccountEntity>> = dao.getAllAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsFlow: StateFlow<List<TransactionEntity>> = dao.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Ao abrir o app, o ViewModel verifica o banco de dados real UMA vez
        viewModelScope.launch {
            val currentAccounts = dao.getAllAccountsFlow().first()
            // Só cria a "Carteira Física" se o banco realmente estiver vazio
            if (currentAccounts.isEmpty()) {
                dao.insertAccount(AccountEntity(name = "Carteira Física"))
            }
        }
    }

    // Ações de persistência (rodando em coroutines)
    fun addAccount(name: String) {
        viewModelScope.launch {
            dao.insertAccount(AccountEntity(name = name))
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            // 1. Limpa transações vinculadas
            dao.deleteTransactionsByAccount(account.id)
            // 2. Deleta a conta
            dao.deleteAccount(account)
        }
    }

    fun addTransaction(accountId: String, description: String, amount: Double, isIncome: Boolean) {
        viewModelScope.launch {
            dao.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    description = description,
                    amount = amount,
                    isIncome = isIncome
                )
            )
        }
    }
}

// Factory para injetar o DAO no ViewModel
class FinanceViewModelFactory(private val dao: FinanceDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==========================================
// 4. DESIGN & TEMA (M3 EXPRESSIVE DARK)
// ==========================================

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

// ==========================================
// 5. ACTIVITY PRINCIPAL (PONTO DE ENTRADA)
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o banco de dados e obtém o DAO
        val database = FinanceDatabase.getDatabase(this)
        val dao = database.financeDao()
        val viewModelFactory = FinanceViewModelFactory(dao)

        setContent {
            MaterialTheme(colorScheme = FinanceDarkColorScheme) {
                // Obtém o ViewModel usando a Factory
                val viewModel: FinanceViewModel = viewModel(factory = viewModelFactory)

                FinanceApp(viewModel)
            }
        }
    }
}

// ==========================================
// 6. INTERFACE GRÁFICA (COMPOSE UI)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    // Coleta os estados do DB em tempo real (Persistente)
    val accounts by viewModel.accountsFlow.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle()

    var showNewAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Minhas Finanças",
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
        // Fix: Avoid crash by only showing content if there's at least one account
        if (accounts.isNotEmpty()) {
            FinanceScreenContent(
                modifier = Modifier.padding(paddingValues),
                accounts = accounts,
                transactions = transactions,
                onAddTransaction = { accountId, desc, amount, isIncome ->
                    viewModel.addTransaction(accountId, desc, amount, isIncome)
                },
                onDeleteAccount = { viewModel.deleteAccount(it) }
            )
        } else {
            // Optional: Loading or empty state
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (showNewAccountDialog) {
            NewAccountDialog(
                onDismiss = { showNewAccountDialog = false },
                onConfirm = { name ->
                    viewModel.addAccount(name)
                    showNewAccountDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreenContent(
    modifier: Modifier = Modifier,
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onAddTransaction: (String, String, Double, Boolean) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }

    // Inicializa com a primeira conta disponível (Garantido não vazio pelo if no FinanceApp)
    var selectedAccount by remember(accounts) { mutableStateOf(accounts.first()) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    var viewFilterAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    val filteredTransactions = if (viewFilterAccount == null) {
        transactions
    } else {
        transactions.filter { it.accountId == viewFilterAccount!!.id }
    }

    LaunchedEffect(accounts) {
        if (!accounts.contains(selectedAccount)) {
            selectedAccount = accounts.lastOrNull() ?: accounts.first()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        DashboardHeader(
            accounts = accounts,
            transactions = transactions,
            viewFilterAccount = viewFilterAccount,
            onDeleteAccount = { accountToDelete = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier
            .padding(horizontal = 16.dp)
            .weight(1f)) {

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
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
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
                                    onAddTransaction(selectedAccount.id, description, amountValue, isIncome)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Movimentações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FilterChip(
                    selected = viewFilterAccount != null,
                    onClick = { viewFilterAccount = if (viewFilterAccount?.id == selectedAccount.id) null else selectedAccount },
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

    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Excluir Conta") },
            text = { Text("Tem certeza que deseja excluir a conta '${accountToDelete!!.name}'? Todas as movimentações vinculadas a ela também serão apagadas permanentemente.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
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

@Composable
fun DashboardHeader(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    viewFilterAccount: AccountEntity?,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    val relevantTransactions = if (viewFilterAccount == null) {
        transactions
    } else {
        transactions.filter { it.accountId == viewFilterAccount.id }
    }

    val displayBalance = relevantTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }

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
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "R$ ${"%.2f".format(accountBalance)}",
                                color = if (accountBalance >= 0) IncomeColor else ExpenseColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        IconButton(
                            onClick = { onDeleteAccount(account) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, accountName: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, fontWeight = FontWeight.SemiBold)
                Text(accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "${if (transaction.isIncome) "+" else "-"} R$ ${"%.2f".format(transaction.amount)}",
                color = if (transaction.isIncome) IncomeColor else ExpenseColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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
                label = { Text("Nome da Conta") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
