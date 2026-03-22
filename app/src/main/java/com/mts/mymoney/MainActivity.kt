package com.mts.mymoney // Certifique-se de que este é o pacote correto do seu projeto

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // NOVO: Import da Splash Screen
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

// ==========================================
// 1. CAMADA DE DADOS (ROOM ENTITIES & DAO)
// ==========================================

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val description: String,
    val amount: Double,
    val isIncome: Boolean
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccount(accountId: String)
}

// ==========================================
// 2. CONFIGURAÇÃO DO BANCO DE DADOS (ROOM)
// ==========================================

@Database(entities = [AccountEntity::class, TransactionEntity::class], version = 1)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

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
                    .fallbackToDestructiveMigration()
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

    val accountsFlow: StateFlow<List<AccountEntity>> = dao.getAllAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsFlow: StateFlow<List<TransactionEntity>> = dao.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CORREÇÃO: CÓDIGO DE INICIALIZAÇÃO PARA PREVENIR DUPLICAÇÃO ---
    init {
        viewModelScope.launch {
            val currentAccounts = dao.getAllAccountsFlow().first() // Verifica o DB real
            if (currentAccounts.isEmpty()) {
                dao.insertAccount(AccountEntity(name = "Carteira Física"))
            }
        }
    }

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
                    description = description.trim(), // Salva descrição (pode ser vazia)
                    amount = amount,
                    isIncome = isIncome
                )
            )
        }
    }
}

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
    background = Color(0xFF141218), // Cor de fundo principal
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

        // ==========================================
        // NOVO: ATIVAÇÃO DA SPLASH SCREEN NATIVA
        // ==========================================
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val database = FinanceDatabase.getDatabase(this)
        val dao = database.financeDao()
        val viewModelFactory = FinanceViewModelFactory(dao)

        setContent {
            MaterialTheme(colorScheme = FinanceDarkColorScheme) {
                val viewModel: FinanceViewModel = viewModel(factory = viewModelFactory)
                FinanceApp(viewModel)
            }
        }
    }
}

// ==========================================
// 6. INTERFACE GRÁFICA (COMPOSE UI)
// ==========================================

// Definindo as rotas de navegação simples
const val SCREEN_DASHBOARD = "dashboard"
const val SCREEN_HISTORY = "history"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    val accounts by viewModel.accountsFlow.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle()

    // Estado de navegação simples
    var currentScreen by rememberSaveable { mutableStateOf(SCREEN_DASHBOARD) }

    var showNewAccountDialog by remember { mutableStateOf(false) }

    // Enquanto o DB inicializa, mostra tela de carregamento (Garante que há conta antes de processar)
    if (accounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Lida com o botão 'Voltar' físico do sistema
    BackHandler(enabled = currentScreen == SCREEN_HISTORY) {
        currentScreen = SCREEN_DASHBOARD
    }

    // Estrutura de navegação usando when
    when (currentScreen) {
        SCREEN_DASHBOARD -> {
            Scaffold(
                topBar = {
                    // --- CORREÇÃO: TopAppBar Padrão (Coloca Título e Ações na mesma linha no topo) ---
                    TopAppBar(
                        title = { Text("Minhas Finanças", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        actions = {
                            IconButton(onClick = { showNewAccountDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Nova Conta", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                // CORREÇÃO: Verificação de segurança para garantir conta antes de carregar
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

    // Diálogo de nova conta
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

// --- TELA 1: DASHBOARD E INSERÇÃO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onAddTransaction: (String, String, Double, Boolean) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onNavigateToHistory: () -> Unit // Função para navegar
) {
    // Estados do formulário (Locais)
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }

    // Inicializa com a primeira conta disponível (Garantido não vazio)
    var selectedAccount by remember { mutableStateOf(accounts.first()) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    // Garante que a conta selecionada no formulário existe (caso outras sejam deletadas)
    LaunchedEffect(accounts) {
        if (!accounts.contains(selectedAccount)) {
            selectedAccount = accounts.last()
        }
    }

    // --- CORREÇÃO: Alinhamento no Topo ---
    // Layout principal ajustado: Column com fillMaxSize e verticalScroll
    // faz com que os elementos colem no topo e rolem se necessário (sem centralizar weight(1f)).
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Permite rolar o dashboard inteiro
            .padding(horizontal = 16.dp)
    ) {

        // --- DASHBOARD: SALDO TOTAL E BLOCOS DAS CONTAS ---
        DashboardHeader(
            accounts = accounts,
            transactions = transactions,
            viewFilterAccount = null, // Saldo Geral Total
            onDeleteAccount = { accountToDelete = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- FORMULÁRIO EXPRESSIVE (Removido o peso weight(1f) para não centralizar verticalmente) ---
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
                        // --- CORREÇÃO: Descrição Opcional (UI) ---
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
                            // --- CORREÇÃO: Descrição não é mais obrigatória (removido isNotBlank()) ---
                            if (amountValue != null) {
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

        // --- NOVO: BOTÃO DEDICADO PARA HISTÓRICO ---
        ElevatedButton(
            onClick = onNavigateToHistory,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ver Histórico Completo", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp)) // Espaço final para não colar no fundo
    }

    // Diálogo de exclusão de conta
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Excluir Conta") },
            text = { Text("Tem certeza que deseja excluir a conta '${accountToDelete!!.name}'? Todas as movimentações vinculadas a ela também serão apagadas permanentemente.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
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

// --- TELA 2: HISTÓRICO DE MOVIMENTAÇÕES (SEPARADA) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit
) {
    // Estado do filtro (local desta tela)
    var selectedFilterAccount by remember { mutableStateOf<AccountEntity?>(null) }

    // Aplica o filtro
    val filteredTransactions = if (selectedFilterAccount == null) {
        transactions
    } else {
        transactions.filter { it.accountId == selectedFilterAccount!!.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Movimentações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
            // Seletor de filtro (Chips)
            Text("Filtrar por conta:", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Opção 'Todas'
                item {
                    FilterChip(
                        selected = selectedFilterAccount == null,
                        onClick = { selectedFilterAccount = null },
                        label = { Text("Todas") },
                        leadingIcon = { if (selectedFilterAccount == null) Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) } // Ícone corrigido
                    )
                }
                // Lista de contas
                items(accounts) { account ->
                    FilterChip(
                        selected = selectedFilterAccount?.id == account.id,
                        onClick = { selectedFilterAccount = account },
                        label = { Text(account.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de Movimentações (LazyColumn aqui)
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

// ==========================================
// 7. COMPONENTES REUTILIZÁVEIS (COMPONENTES VISUAIS)
// ==========================================

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
        horizontalAlignment = Alignment.CenterHorizontally // Centraliza o Saldo Geral
    ) {
        // Título do Saldo
        Text(
            text = if (viewFilterAccount == null) "Saldo Geral" else "Saldo: ${viewFilterAccount.name}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Valor do Saldo Gigante
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
            contentPadding = PaddingValues(horizontal = 0.dp), // Ajustado para colar nas bordas do dashboard
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
                                modifier = Modifier.padding(end = 24.dp) // Abre espaço para o ícone de deletar
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

// Componente visual do Item da Lista de Transações (Persistente)
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
            Column(modifier = Modifier.weight(1f)) {
                // --- CORREÇÃO: Exibe "(Sem descrição)" se estiver vazio ---
                Text(
                    text = transaction.description.ifBlank { "(Sem descrição)" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (transaction.description.isBlank()) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
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
                singleLine = true,
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