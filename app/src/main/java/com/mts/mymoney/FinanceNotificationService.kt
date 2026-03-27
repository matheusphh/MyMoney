package com.mts.mymoney

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FinanceNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = sbn?.packageName ?: return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""

        Log.d("FinanceService", "Notification received from $packageName: $title - $text")

        serviceScope.launch {
            val db = FinanceDatabase.getDatabase(applicationContext)
            val dao = db.financeDao()
            
            // Get all accounts to see if any is linked to this package
            val accounts = dao.getAllAccountsFlow().first()
            val linkedAccount = accounts.find { it.linkedPackageName == packageName }
            
            if (linkedAccount != null) {
                parseNotification(text, title, packageName)?.let { (amount, description, isIncome) ->
                    dao.insertTransaction(
                        TransactionEntity(
                            accountId = linkedAccount.id,
                            description = description,
                            amount = amount,
                            isIncome = isIncome
                        )
                    )
                }
            }
        }
    }

    private fun parseNotification(text: String, title: String, packageName: String): Triple<Double, String, Boolean>? {
        // Basic parser for demonstration.
        // Regex to find "R$ XX,XX" or "R$XX.XX"
        val amountRegex = Regex("""R\$\s?(\d+(?:[.,]\d{2})?)""")
        val match = amountRegex.find(text) ?: amountRegex.find(title)
        
        if (match != null) {
            val amountString = match.groupValues[1].replace(".", "").replace(",", ".")
            val amount = amountString.toDoubleOrNull() ?: return null
            
            // Heuristic: check for income keywords
            val incomeKeywords = listOf("Pix", "recebido","recebeu","creditado", "transferência recebida", "pix recebido", "depósito", "estorno", "credito")
            val isIncome = incomeKeywords.any { 
                text.lowercase().contains(it) || title.lowercase().contains(it) 
            }

            val description = "Auto: ${title.ifBlank { text.take(20) }}"
            
            return Triple(amount, description, isIncome)
        }
        
        return null
    }
}
