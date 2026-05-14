package com.limelight.ui.premium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.limelight.preferences.AddComputerManually
import com.limelight.preferences.StreamSettings

class PremiumDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PremiumDashboard(
                onAddPc = {
                    startActivity(Intent(this, AddComputerManually::class.java))
                },
                onSettings = {
                    startActivity(Intent(this, StreamSettings::class.java))
                }
            )
        }
    }
}
