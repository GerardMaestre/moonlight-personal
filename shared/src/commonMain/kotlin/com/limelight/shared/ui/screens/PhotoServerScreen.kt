package com.limelight.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.limelight.shared.platform.PhotoServerActions
import com.limelight.shared.platform.PhotoServerState
import com.limelight.shared.platform.PhotoServerStatus
import com.limelight.shared.platform.PreviewPhotoServerActions
import com.limelight.shared.platform.StartCommandResult

@Composable
fun PhotoServerScreen(
    state: PhotoServerState,
    actions: PhotoServerActions = PreviewPhotoServerActions,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Servidor de Fotos", style = MaterialTheme.typography.headlineSmall)

        when (val status = state.status) {
            PhotoServerStatus.Stopped -> Text("Estado: detenido")
            PhotoServerStatus.Starting -> Text("Estado: iniciando...")
            is PhotoServerStatus.Running -> {
                Text("Estado: ejecutándose")
                Text("Puerto: ${status.port}")
                Text("URL: ${status.url}")
            }
            is PhotoServerStatus.Error -> Text("Estado: error (${status.message})", color = MaterialTheme.colorScheme.error)
        }

        state.lastError?.let {
            Text("Último error: $it", color = MaterialTheme.colorScheme.error)
        }

        Text("Healthcheck: ${state.healthMessage}")
        if (state.recentLogs.isNotEmpty()) {
            Text("Logs recientes:")
            state.recentLogs.takeLast(5).forEach { line -> Text("• $line") }
        }
        when (val result = state.lastCommandResult) {
            StartCommandResult.Success -> Text("Último inicio: OK")
            is StartCommandResult.Failed -> Text("Último inicio: ERROR (${result.reason})", color = MaterialTheme.colorScheme.error)
            null -> Unit
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { actions.startPhotoServer() }, modifier = Modifier.fillMaxWidth()) { Text("Iniciar") }
        OutlinedButton(onClick = actions::stopPhotoServer, modifier = Modifier.fillMaxWidth()) { Text("Detener") }
        OutlinedButton(onClick = actions::restartPhotoServer, modifier = Modifier.fillMaxWidth()) { Text("Reiniciar") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
