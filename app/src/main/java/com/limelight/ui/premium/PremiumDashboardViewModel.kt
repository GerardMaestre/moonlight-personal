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
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

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
                    
                    mainHandler.post {
                        dashboardState.updateComputer(info)
                    }
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
    
    fun addComputer(ip: String) {
        val binder = managerBinder ?: return
        kotlin.concurrent.thread {
            try {
                val details = ComputerDetails()
                details.manualAddress = ComputerDetails.AddressTuple(ip, com.limelight.nvstream.http.NvHTTP.DEFAULT_HTTP_PORT)
                val success = binder.addComputerBlocking(details)
                mainHandler.post {
                    if (success) {
                        dashboardState.showMessage("Ordenador añadido correctamente.")
                    } else {
                        dashboardState.showMessage("No se pudo encontrar el ordenador en la IP: $ip")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    dashboardState.showMessage("Error al añadir: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        managerBinder?.stopPolling()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
