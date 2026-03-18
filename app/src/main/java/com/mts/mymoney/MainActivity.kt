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

    // Garante que haja pelo menos uma conta no primeiro boot
    LaunchedEffect(accounts) {
        if (accounts.isEmpty()) {
            viewModel.addAccount("Carteira Física")
        }
    }

    // Enquanto o DB inicializa e cria a primeira conta, mostra tela de carregamento
    if (accounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

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
        FinanceScreenContent(
            modifier = Modifier.padding(paddingValues),
            accounts = accounts,
            transactions = transactions,
            // Conecta ações visuais às funções do ViewModel
            onAddTransaction = { accountId, desc, amount, isIncome ->
                viewModel.addTransaction(accountId, desc, amount, isIncome)
            },
            onDeleteAccount = { viewModel.deleteAccount(it) }
        )

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
    // Estados do formulário e filtro (ainda voláteis, resetam ao reiniciar)
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }

    // Inicializa com a primeira conta disponível
    var selectedAccount by remember { mutableStateOf(accounts.first()) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    // Estado para filtragem do histórico
    var viewFilterAccount by remember { mutableStateOf<AccountEntity?>(null) }

    // Estado para diálogo de exclusão
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    // Aplica o filtro nas transações coletadas do DB
    val filteredTransactions = if (viewFilterAccount == null) {
        transactions
    } else {
        transactions.filter { it.accountId == viewFilterAccount!!.id }
    }

    // Garante que a conta selecionada no formulário existe (caso outras sejam deletadas)
    LaunchedEffect(accounts) {
        if (!accounts.contains(selectedAccount)) {
            selectedAccount = accounts.last()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // --- DASHBOARD: SALDO CENTRALIZADO + BLOCOS DAS CONTAS ---
        DashboardHeader(
            accounts = accounts,
            transactions = transactions,
            viewFilterAccount = viewFilterAccount,
            onDeleteAccount = { accountToDelete = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- FORMULÁRIO E HISTÓRICO ---
        Column(modifier = Modifier
            .padding(horizontal = 16.dp)
            .weight(1f)) {

            // Formulário Expressive
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nova Transação", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Menu Suspenso (Dropdown) de Contas
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable) // Correção da função taxada
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
                                    // Chama a função de persistência
                                    onAddTransaction(selectedAccount.id, description, amountValue, isIncome)
                                    // Limpa o formulário localmente
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

            // --- HISTÓRICO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Movimentações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FilterChip(
                    selected = viewFilterAccount != null,
                    // Alterna o filtro: Se já estiver filtrando essa conta, remove o filtro. Senão, aplica.
                    onClick = { viewFilterAccount = if (viewFilterAccount?.id == selectedAccount.id) null else selectedAccount },
                    label = { Text(if (viewFilterAccount == null) "Todas" else selectedAccount.name) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Filtrar") } // Ícone corrigido
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Mostra o histórico persistente filtrado
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(filteredTransactions) { transaction ->
                    val accName = accounts.find { it.id == transaction.accountId }?.name ?: "Desconhecida"
                    TransactionItem(transaction, accName)
                }
            }
        }
    }

    // --- DIÁLOGO DE CONFIRMAÇÃO DE EXCLUSÃO DE CONTA ---
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Excluir Conta") },
            text = { Text("Tem certeza que deseja excluir a conta '${accountToDelete!!.name}'? Todas as movimentações vinculadas a ela também serão apagadas permanentemente.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        // Se a conta excluída estava filtrando a tela, removemos o filtro
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

// Componente do Dashboard (Saldo Centralizado + Blocos de Contas)
@Composable
fun DashboardHeader(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    viewFilterAccount: AccountEntity?,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    // Calcula o saldo com base no filtro atual
    val relevantTransactions = if (viewFilterAccount == null) {
        transactions
    } else {
        transactions.filter { it.accountId == viewFilterAccount.id }
    }

    val displayBalance = relevantTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally // Centraliza o Saldo Total
    ) {
        // Título do Saldo
        Text(
            text = if (viewFilterAccount == null) "Saldo Geral" else "Saldo: ${viewFilterAccount.name}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Valor do Saldo Gigante Centralizado
        Text(
            text = "R$ ${"%.2f".format(displayBalance)}",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (displayBalance >= 0) IncomeColor else ExpenseColor,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Row com os Blocos das Contas (Carrossel Horizontal)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(accounts) { account ->
                // Calcula o saldo específico desta conta para mostrar no bloco
                val accountBalance = transactions
                    .filter { it.accountId == account.id }
                    .sumOf { if (it.isIncome) it.amount else -it.amount }

                // Bloco da Conta (Formato premium)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.size(width = 150.dp, height = 100.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 24.dp) // Abre espaço para não encavalar no ícone de deletar
                            )
                            Text(
                                text = "R$ ${"%.2f".format(accountBalance)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (accountBalance >= 0) IncomeColor else ExpenseColor
                            )
                        }

                        // Botão de Deletar (Só aparece se houver mais de 1 conta)
                        if (accounts.size > 1) {
                            IconButton(
                                onClick = { onDeleteAccount(account) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
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

// Item visual da lista de transações persistente
@Composable
fun TransactionItem(transaction: TransactionEntity, accountName: String) {
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

// Diálogo para criação de nova conta persistente
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
                Text("Gravar") // "Gravar" para diferenciar do "Adicionar" transação
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