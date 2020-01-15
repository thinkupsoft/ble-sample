package com.thinkup.connectivity.impl

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import androidx.lifecycle.Observer
import com.thinkup.connectivity.BleSession
import com.thinkup.connectivity.BleSetting
import com.thinkup.connectivity.common.BaseBleImpl
import com.thinkup.connectivity.mesh.NrfMeshRepository
import com.thinkup.connectivity.messges.ControlParams
import com.thinkup.connectivity.messges.EventType
import com.thinkup.connectivity.messges.NO_CONFIG
import com.thinkup.connectivity.messges.config.NodeConfigMessageUnacked
import com.thinkup.connectivity.messges.control.NodeControlMessage
import com.thinkup.connectivity.messges.control.NodeControlMessageStatus
import com.thinkup.connectivity.messges.event.NodeEventStatus
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class BleSessionImpl(context: Context, setting: BleSetting, repository: NrfMeshRepository) : BaseBleImpl(context, setting, repository), BleSession {

    private val handler = Handler()
    private val runnable = Runnable { keepAlive() }
    private var keepAliveProgressing = false
    /**
     * Config a node when receive a HELLO event message
     */
    private val configObserver: Observer<NodeEventStatus?> =
        Observer { event ->
            if (event != null && event.eventType == EventType.HELLO && !repository.isSending) {
                Log.d("TKUP-NEURAL::Received::", event.toString())
                getNode(event.srcAddress)?.let {
                    it.isOnline = true
                    val element: Element? = getElement(it)
                    if (element != null) {
                        val model = getModel<VendorModel>(element)
                        if (model != null) {
                            val appKey = getAppKey(model.boundAppKeyIndexes[0])
                            appKey?.let { key ->
                                sendMessage(
                                    it,
                                    NodeConfigMessageUnacked(it.nodeName.toInt(), NO_CONFIG, NO_CONFIG, key, model.modelId, model.companyIdentifier)
                                )
                            }
                        }
                    }
                }
            }
        }

    private val connectionObserver: Observer<Boolean> = Observer {
        if (it) {
            // Listen for a HELLO event message
            repository.getEventMessageLiveData().removeObserver(configObserver)
            repository.getEventMessageLiveData().observeForever(configObserver)
            keepAlive()
        } else autoConnect()
    }

    private val bluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                BluetoothAdapter.STATE_OFF
            )

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    autoConnect()
                }
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    disconnect()
                    repository.updateNodes(listOf())
                }
            }
        }
    }

    override fun start(): Unit = executeService {
        // Check if connect and re-connect message are enable
        if (autoConnectCondition()) {
            // Register for bluetooth setting change and reconnect
            registerCallback()
            // Listen for connection changes
            repository.isConnectedToProxy().removeObserver(connectionObserver)
            repository.isConnectedToProxy().observeForever(connectionObserver)
            autoConnect()
        } else {
            keepAlive()
        }
    }

    private fun autoConnectCondition(): Boolean = setting.enabledStartConfig() && !checkConnectivity() && !repository.isSending

    private fun autoConnect() {
        if (autoConnectCondition()) Handler().postDelayed({ autoConnect { start() } }, 1000)
    }

    private fun registerCallback() {
        context.registerReceiver(
            bluetoothStateBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }


    override fun keepAlive(): Unit = executeService {
        if (isEnabledKeepAlive()) {
            keepAliveProgressing = true
            val connected = mutableListOf<ProvisionedMeshNode>()
            val nodes = getNodes()
            val observer = Observer<MeshMessage?> {
                synchronized(connected) {
                    if (it is NodeControlMessageStatus) {
                        Log.d("TKUP-NEURAL::Rec-KA::", it.toString())
                        checkNode(it)?.let { node ->
                            node.batteryLevel = it.batteryLevel
                            connected.add(node)
                        }
                    }
                }
            }
            repository.getKeepMessageLiveData().observeForever(observer)
            bulkMessaging(nodes) {
                Log.d("TKUP-NEURAL::Send-KA::", it.toString())
                keepAliveMessage(it)
            }
            Handler().postDelayed({
                repository.getKeepMessageLiveData().removeObserver(observer)
                repository.flushKeepMessageLiveData()
                repository.updateNodes(connected)
                keepAliveProgressing = false
                scheduleKeepAlive()
            }, KEEP_ALIVE_WAIT)
        }
    }

    private fun scheduleKeepAlive() {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, if (isRetryKeepAlive()) KEEP_ALIVE_RETRY else KEEP_ALIVE)
    }

    private fun isEnabledKeepAlive() = setting.enabledKeepAlive() && !repository.isSending && !keepAliveProgressing

    private fun isRetryKeepAlive() = setting.enabledKeepAlive() && repository.isSending

    override fun stop() {
        handler.removeCallbacks(runnable)
    }

    private fun checkNode(message: NodeControlMessageStatus): ProvisionedMeshNode? {
        return getNode(message.srcAddress)
    }

    private fun keepAliveMessage(node: ProvisionedMeshNode) {
        val handler = Handler()
        val element: Element? = getElement(node)
        if (element != null) {
            val model = getModel<VendorModel>(element)
            if (model != null) {
                val appKey = getAppKey(model.boundAppKeyIndexes[0])
                appKey?.let {
                    handler.postDelayed({
                        sendMessage(node, NodeControlMessage(ControlParams.KEEP_ALIVE, appKey, model.modelId, model.companyIdentifier))
                    }, BULK_DELAY)
                }
            }
        }
    }
}