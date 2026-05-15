package com.limelight.ui.premium

import android.app.Application
import android.app.Activity
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
import com.limelight.shared.ui.screens.AppController

class PremiumDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private var managerBinder: ComputerManagerService.ComputerManagerBinder? = null
    private var appListPoller: ComputerManagerService.ApplistPoller? = null
    
    val controller = AppController()
    val dashboardState get() = controller.dashboardState
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ComputerManagerService.ComputerManagerBinder
            managerBinder = binder
            
            Thread {
                binder.waitForReady()
                
                binder.startPolling(object : ComputerManagerListener {
                    override fun notifyComputerUpdated(details: ComputerDetails) {
                        val status = when (details.state) {
                            ComputerDetails.State.ONLINE -> com.limelight.shared.model.ComputerStatus.ONLINE
                            ComputerDetails.State.OFFLINE -> com.limelight.shared.model.ComputerStatus.OFFLINE
                            else -> com.limelight.shared.model.ComputerStatus.UNKNOWN
                        }
                        
                        val info = com.limelight.shared.model.ComputerInfo(
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
                            
                            // If this is the currently selected computer, update games too
                            if (dashboardState.selectedComputer?.id == details.uuid && details.rawAppList != null) {
                                updateGamesFromRawList(details.rawAppList)
                            }
                        }
                    }
                })
            }.start()
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

    private fun updateGamesFromRawList(rawList: String) {
        try {
            val apps = com.limelight.nvstream.http.NvHTTP.getAppListByReader(java.io.StringReader(rawList))
            dashboardState.games.clear()
            dashboardState.games.addAll(apps.map { app ->
                com.limelight.shared.model.GameInfo(
                    id = app.appId,
                    name = app.appName,
                    boxArtUrl = "", // We can add box art support later
                    isHdrSupported = app.isHdrSupported
                )
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectComputer(computerId: String) {
        val binder = managerBinder ?: return
        val computer = binder.getComputer(computerId) ?: return
        
        controller.openComputer(computerId)
        
        // Stop previous poller if any
        appListPoller?.stop()
        
        // Start polling for games
        appListPoller = binder.createAppListPoller(computer)
        appListPoller?.start()
    }

    fun pair(computerId: String, activity: Activity) {
        val binder = managerBinder ?: return
        val computer = binder.getComputer(computerId) ?: return
        
        kotlin.concurrent.thread {
            try {
                val httpConn = com.limelight.nvstream.http.NvHTTP(
                    com.limelight.utils.ServerHelper.getCurrentAddressFromComputer(computer),
                    computer.httpsPort,
                    binder.getUniqueId(),
                    computer.serverCert,
                    com.limelight.binding.PlatformBinding.getCryptoProvider(activity)
                )
                
                val pinStr = com.limelight.nvstream.http.PairingManager.generatePinString()
                
                mainHandler.post {
                    com.limelight.utils.Dialog.displayDialog(
                        activity,
                        "Vincular Dispositivo",
                        "Introduce este PIN en tu PC: $pinStr",
                        false
                    )
                }

                val pm = httpConn.pairingManager
                val pairState = pm.pair(httpConn.getServerInfo(true), pinStr)
                
                mainHandler.post {
                    com.limelight.utils.Dialog.closeDialogs()
                    if (pairState == com.limelight.nvstream.http.PairingManager.PairState.PAIRED) {
                        binder.getComputer(computer.uuid).serverCert = pm.pairedCert
                        binder.invalidateStateForComputer(computer.uuid)
                        dashboardState.showMessage("Vinculado correctamente.")
                    } else {
                        dashboardState.showMessage("Error al vincular. Inténtalo de nuevo.")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    com.limelight.utils.Dialog.closeDialogs()
                    dashboardState.showMessage("Error: ${e.message}")
                }
            }
        }
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
        appListPoller?.stop()
        managerBinder?.stopPolling()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
