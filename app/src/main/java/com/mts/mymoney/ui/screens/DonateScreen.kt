package com.mts.mymoney.ui.screens

import com.mts.mymoney.R
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TelaDoacaoPix(chavePix: String = "00020101021126580014br.gov.bcb.pix01369771d54a-224e-4123-8b8f-62bc6028db795204000053039865802BR5914MATEUS C ROCHA6010SAO FELIPE62070503***63048B3D") {
    // Pegamos as referências do Clipboard (Área de transferência) e do Contexto (para o Toast)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Título e Instruções
        Text(
            text = "Apoie nosso projeto",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Escaneie o QR Code abaixo com o aplicativo do seu banco:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. O QR Code
        // Nota: Você pode usar uma imagem estática nos seus 'drawables' ou
        // uma biblioteca como o ZXing para gerar o bitmap a partir da chave copia e cola.
        Image(
            painter = painterResource(id = R.drawable.img), // Substitua pela imagem do seu QR Code
            contentDescription = "QR Code para doação via Pix",
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Ou copie a chave Pix:",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Exibição da Chave Pix
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = chavePix,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Botão de Copiar
        Button(
            onClick = {
                // Copia o texto para a área de transferência
                clipboardManager.setText(AnnotatedString(chavePix))

                // Mostra um aviso (Toast) na tela de que foi copiado
                Toast.makeText(context, "Chave Pix copiada!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Copiar Chave Pix",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}