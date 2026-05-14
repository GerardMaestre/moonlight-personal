package com.limelight.ui.premium

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import com.limelight.computers.ComputerManagerListener
import com.limelight.computers.ComputerManagerService
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.shared.model.ComputerInfo
import com.limelight.shared.model.ComputerStatus
import com.limelight.shared.ui.screens.DashboardState

class PremiumDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private var managerBinder: ComputerManagerService.ComputerManagerBinder? = null
    
    // Use the shared state
    val dashboardState = DashboardState()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ComputerManagerService.ComputerManagerBinder
            managerBinder = binder
            
            binder.startPolling(object : ComputerManagerListener {
                override fun notifyComputerUpdated(details: ComputerDetails) {
                    val status = when (details.state) {
                        ComputerDetails.State.ONLINE -> ComputerStatus.ONLINE
                        ComputerDetails.State.OFFLINE -> ComputerStatus.OFFLINE
                        else -> ComputerStatus.UNKNOWN
                    }
                    
                    val info = ComputerInfo(
                        id = details.uuid ?: "",
                        name = details.name ?: "Unknown PC",
                        status = status,
                        localAddress = details.localAddress?.address,
                        remoteAddress = details.remoteAddress?.address,
                        macAddress = details.macAddress,
                        isPaired = details.pairState == com.limelight.nvstream.http.PairingManager.PairState.PAIRED,
                        runningGameId = details.runningGameId,
                        isNvidiaServer = details.nvidiaServer
                    )
                    
                    dashboardState.updateComputer(info)
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            managerBinder = null
        }
    }

    init {
        application.bindService(
            Intent(application, ComputerManagerService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun getBinder(): ComputerManagerService.ComputerManagerBinder? = managerBinder

    override fun onCleared() {
        super.onCleared()
        managerBinder?.stopPolling()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
