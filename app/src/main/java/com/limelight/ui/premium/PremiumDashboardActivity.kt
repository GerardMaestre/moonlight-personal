package com.limelight.ui.premium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.limelight.AppView
import com.limelight.custom.NetworkProfileApplier
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.wol.WakeOnLanSender
import com.limelight.preferences.AddComputerManually
import com.limelight.preferences.StreamSettings
import kotlin.concurrent.thread
import com.limelight.shared.model.NetworkProfiles
import com.limelight.shared.platform.PlatformActions
import com.limelight.shared.ui.screens.DashboardScreen

class PremiumDashboardActivity : ComponentActivity() {
    private val viewModel: PremiumDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val actions = object : PlatformActions {
            override fun onAddPc() {
                startActivity(Intent(this@PremiumDashboardActivity, AddComputerManually::class.java))
            }

            override fun onOpenSettings() {
                startActivity(Intent(this@PremiumDashboardActivity, StreamSettings::class.java))
            }

            override fun onPcClick(computerId: String, computerName: String) {
                val intent = Intent(this@PremiumDashboardActivity, AppView::class.java)
                intent.putExtra(AppView.NAME_EXTRA, computerName)
                intent.putExtra(AppView.UUID_EXTRA, computerId)
                startActivity(intent)
            }

            override fun onApplyNetworkProfile(profileId: String) {
                val applierProfile = when (profileId) {
                    NetworkProfiles.HOME.id -> NetworkProfileApplier.Profile.HOME
                    NetworkProfiles.FIVE_G.id -> NetworkProfileApplier.Profile.FIVE_G
                    NetworkProfiles.SAVER.id -> NetworkProfileApplier.Profile.SAVER
                    else -> NetworkProfileApplier.Profile.HOME
                }
                NetworkProfileApplier.apply(this@PremiumDashboardActivity, applierProfile)
            }

            override fun onWakeOnLan(macAddress: String) {
                thread {
                    val fakeComputer = ComputerDetails().apply {
                        this.macAddress = macAddress
                        // Set a dummy manual address to satisfy WakeOnLanSender expectations
                        this.manualAddress = ComputerDetails.AddressTuple("255.255.255.255", 9)
                    }
                    WakeOnLanSender.sendWolPacket(fakeComputer)
                }
            }

            override fun onNavigateBack() {
                finish()
            }
        }

        setContent {
            DashboardScreen(
                state = viewModel.dashboardState,
                actions = actions
            )
        }
    }
}
