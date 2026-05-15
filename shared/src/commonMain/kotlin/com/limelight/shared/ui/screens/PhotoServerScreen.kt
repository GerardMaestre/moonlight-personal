package com.limelight.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun PhotoServerScreen(
    statusMessage: String?,
    onBack: () -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudSync,
            contentDescription = "Immich",
            modifier = Modifier.size(80.dp),
            tint = MoonlightColors.Cyan
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Servidor de Fotos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Arranca el stack de contenedores de Immich en el ordenador remoto sin mostrar ventanas visibles.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        FilledTonalButton(
            onClick = onStartServer,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MoonlightColors.Cyan.copy(alpha = 0.2f),
                contentColor = MoonlightColors.Cyan
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("ARRANCAR SERVIDOR IMMICH", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FilledTonalButton(
            onClick = onStopServer,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MoonlightColors.Red.copy(alpha = 0.2f),
                contentColor = MoonlightColors.Red
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("APAGAR SERVIDOR IMMICH", fontWeight = FontWeight.Bold)
        }
        
        statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(24.dp))
            val isError = msg.startsWith("Error") || msg.contains("falló")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MoonlightColors.Red.copy(alpha = 0.1f) else MoonlightColors.Green.copy(alpha = 0.1f)
                )
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) MoonlightColors.Red else MoonlightColors.Green,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        TextButton(onClick = onBack) {
            Text("Volver", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
