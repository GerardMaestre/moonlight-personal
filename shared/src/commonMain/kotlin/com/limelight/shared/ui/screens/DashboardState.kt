package com.limelight.shared.ui.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.model.ComputerStatus
import com.limelight.shared.model.NetworkProfiles

/**
 * Platform-independent state holder for the Dashboard screen.
 * On Android, this can be wrapped by an AndroidViewModel.
 * On Desktop, it can be instantiated directly for previewing.
 */
class DashboardState {
    val computers = mutableStateListOf<ComputerInfo>()
    var selectedProfileId by mutableStateOf(NetworkProfiles.HOME.id)
        private set

    fun selectProfile(profileId: String) {
        selectedProfileId = profileId
    }

    fun updateComputer(computer: ComputerInfo) {
        val index = computers.indexOfFirst { it.id == computer.id }
        if (index != -1) {
            computers[index] = computer
        } else {
            computers.add(computer)
        }
    }

    fun removeComputer(computerId: String) {
        computers.removeAll { it.id == computerId }
    }

    companion object {
        /**
         * Creates a state pre-populated with mock data for previews and testing.
         */
        fun preview(): DashboardState = DashboardState().apply {
            computers.addAll(
                listOf(
                    ComputerInfo(
                        id = "1",
                        name = "Gaming Desktop",
                        status = ComputerStatus.ONLINE,
                        localAddress = "192.168.1.100",
                        macAddress = "AA:BB:CC:DD:EE:FF",
                        isPaired = true,
                        runningGameId = 0
                    ),
                    ComputerInfo(
                        id = "2",
                        name = "Streaming Server",
                        status = ComputerStatus.ONLINE,
                        localAddress = "192.168.1.101",
                        macAddress = "11:22:33:44:55:66",
                        isPaired = true,
                        runningGameId = 42
                    ),
                    ComputerInfo(
                        id = "3",
                        name = "Office PC",
                        status = ComputerStatus.OFFLINE,
                        localAddress = "192.168.1.102",
                        macAddress = "FF:EE:DD:CC:BB:AA",
                        isPaired = true
                    ),
                    ComputerInfo(
                        id = "4",
                        name = "Living Room HTPC",
                        status = ComputerStatus.UNKNOWN,
                        isPaired = false
                    )
                )
            )
        }
    }
}
