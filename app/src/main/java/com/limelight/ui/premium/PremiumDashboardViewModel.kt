package com.limelight.ui.premium

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.limelight.computers.ComputerManagerListener
import com.limelight.computers.ComputerManagerService
import com.limelight.nvstream.http.ComputerDetails

class PremiumDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private var managerBinder: ComputerManagerService.ComputerManagerBinder? = null
    val computers = mutableStateListOf<ComputerDetails>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ComputerManagerService.ComputerManagerBinder
            managerBinder = binder
            
            binder.startPolling(object : ComputerManagerListener {
                override fun notifyComputerUpdated(details: ComputerDetails) {
                    val index = computers.indexOfFirst { it.uuid == details.uuid }
                    if (index != -1) {
                        computers[index] = details
                    } else {
                        computers.add(details)
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

    override fun onCleared() {
        super.onCleared()
        managerBinder?.stopPolling()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
