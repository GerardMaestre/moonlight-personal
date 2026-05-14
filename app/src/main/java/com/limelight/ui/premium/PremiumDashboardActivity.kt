package com.limelight.ui.premium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.limelight.AppView
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.preferences.AddComputerManually
import com.limelight.preferences.StreamSettings

class PremiumDashboardActivity : ComponentActivity() {
    private val viewModel: PremiumDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PremiumDashboard(
                onBack = {
                    finish()
                },
                onAddPc = {
                    startActivity(Intent(this, AddComputerManually::class.java))
                },
                onSettings = {
                    startActivity(Intent(this, StreamSettings::class.java))
                },
                onPcClick = { computer ->
                    launchAppList(computer)
                },
                viewModel = viewModel
            )
        }
    }

    private fun launchAppList(computer: ComputerDetails) {
        val intent = Intent(this, AppView::class.java)
        intent.putExtra(AppView.NAME_EXTRA, computer.name)
        intent.putExtra(AppView.UUID_EXTRA, computer.uuid)
        startActivity(intent)
    }
}
