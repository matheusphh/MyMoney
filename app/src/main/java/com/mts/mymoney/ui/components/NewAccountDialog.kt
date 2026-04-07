package com.mts.mymoney.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp


data class SupportedBank(val name: String, val packageName: String)

val popularBanks = listOf(
    SupportedBank("Nenhum (Apenas Manual)", ""),
    SupportedBank("Nubank", "com.nu.production"),
    SupportedBank("Banco Inter", "br.com.intermedium"),
    SupportedBank("Itaú", "com.itau"),
    SupportedBank("Bradesco", "com.bradesco"),
    SupportedBank("Santander", "com.santander.app"),
    SupportedBank("Caixa Tem", "br.gov.caixa.tem"),
    SupportedBank("PicPay", "com.picpay"),
    SupportedBank("Mercado Pago", "com.mercadopago.wallet")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAccountDialog(onDismiss: () -> Unit, onConfirm: (String, String?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf(popularBanks[0]) }
    var expanded by remember { mutableStateOf(false) }
    var initialBalance by remember { mutableStateOf("") } // NOVO: Estado do saldo

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Conta", color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedBank.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vincular leitura automática") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        popularBanks.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank.name) },
                                onClick = {
                                    selectedBank = bank
                                    expanded = false
                                    if (name.isBlank() && bank.packageName.isNotEmpty()) {
                                        name = bank.name
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Conta no App") },
                    placeholder = { Text("Ex: Nubank, Banco Inter, etc.") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Saldo Inicial") },
                    placeholder = { Text("Ex: 150,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val pkg = selectedBank.packageName.ifBlank { null }
                    onConfirm(name, pkg, initialBalance)
                }
            }) { Text("Gravar", color = MaterialTheme.colorScheme.primaryContainer) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)
    )
}