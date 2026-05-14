package com.limelight.ui.premium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.limelight.AppView
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.wol.WakeOnLanSender
import com.limelight.preferences.StreamSettings
import kotlin.concurrent.thread
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.ui.screens.DashboardScreen
import com.limelight.shared.ui.screens.AppNavigation
import com.limelight.shared.ui.screens.AppScreen
import com.limelight.shared.ui.screens.MainMenuScreen
import com.limelight.shared.ui.theme.MoonlightTheme

class PremiumDashboardActivity : ComponentActivity() {
    private val viewModel: PremiumDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val actions = object : PlatformActions {
            override fun onAddPc() {
                val intent = Intent(this@PremiumDashboardActivity, ComputerAddActivity::class.java)
                startActivity(intent)
            }

            override fun onOpenSettings() {
                val intent = Intent(this@PremiumDashboardActivity, StreamSettings::class.java)
                startActivity(intent)
            }

            override fun onPcClick(computerId: String, computerName: String) {
                val intent = Intent(this@PremiumDashboardActivity, AppView::class.java)
                intent.putExtra(ComputerManagerService.EXTRA_HOST_ID, computerId)
                intent.putExtra(AppView.EXTRA_HOST_NAME, computerName)
                startActivity(intent)
            }

            override fun onApplyNetworkProfile(profileId: String) {
                // Feature removed
            }

            override fun onWakeOnLan(macAddress: String) {
                thread {
                    val fakeComputer = ComputerDetails().apply {
                        this.macAddress = macAddress
                        this.manualAddress = ComputerDetails.AddressTuple("255.255.255.255", 9)
                    }
                    WakeOnLanSender.sendWolPacket(fakeComputer)
                }
            }

            override fun onNavigateBack() {
                // Now handled by the navigation state
            }
        }

        setContent {
            val nav = androidx.compose.runtime.remember { AppNavigation() }

            MoonlightTheme {
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    when (nav.currentScreen) {
                        AppScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                onNavigate = { screen -> nav.navigateTo(screen) }
                            )
                        }
                        AppScreen.MOONLIGHT -> {
                            // We need to pass the real goBack action to the platform actions
                            val moonlightActions = object : PlatformActions by actions {
                                override fun onNavigateBack() {
                                    nav.goBack()
                                }
                            }
                            DashboardScreen(
                                state = viewModel.dashboardState,
                                actions = moonlightActions
                            )
                        }
                        AppScreen.POWER_CONTROL -> {
                            androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.Text("Modo: Mi PC (Encender/Apagar)", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                                androidx.compose.material3.Button(onClick = { nav.goBack() }, modifier = androidx.compose.ui.Modifier.padding(top = 100.dp)) {
                                    androidx.compose.material3.Text("Volver")
                                }
                            }
                        }
                        AppScreen.PHOTO_SERVER -> {
                            androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.Text("Modo: Servidor de Fotos", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                                androidx.compose.material3.Button(onClick = { nav.goBack() }, modifier = androidx.compose.ui.Modifier.padding(top = 100.dp)) {
                                    androidx.compose.material3.Text("Volver")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
