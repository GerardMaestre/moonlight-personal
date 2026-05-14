import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.limelight.shared.platform.PreviewPlatformActions
import com.limelight.shared.ui.screens.DashboardScreen
import com.limelight.shared.ui.screens.DashboardState
import com.limelight.shared.ui.theme.MoonlightTheme

/**
 * Desktop entry point for previewing and testing the Moonlight UI.
 *
 * Run with: ./gradlew :desktopApp:run
 *
 * This provides a native window with mock data — no Android emulator needed.
 * Changes to shared UI code are reflected here instantly via Hot Reload.
 */
fun main() = application {
    val state = remember { DashboardState.preview() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Moonlight Preview — Desktop",
        state = WindowState(size = DpSize(420.dp, 900.dp))
    ) {
        MoonlightTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                DashboardScreen(
                    state = state,
                    actions = object : com.limelight.shared.platform.PlatformActions {
                        override fun onAddPc() {
                            println("[Preview] Add PC clicked")
                        }
                        override fun onOpenSettings() {
                            println("[Preview] Settings clicked")
                        }
                        override fun onPcClick(computerId: String, computerName: String) {
                            println("[Preview] PC clicked: $computerName ($computerId)")
                        }
                        override fun onApplyNetworkProfile(profileId: String) {
                            println("[Preview] Profile applied: $profileId")
                            state.selectProfile(profileId)
                        }
                        override fun onWakeOnLan(macAddress: String) {
                            println("[Preview] WOL sent to: $macAddress")
                        }
                        override fun onNavigateBack() {
                            println("[Preview] Navigate back")
                        }
                    }
                )
            }
        }
    }
}
