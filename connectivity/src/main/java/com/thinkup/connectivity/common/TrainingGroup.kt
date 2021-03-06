package com.thinkup.connectivity.common

import android.os.Handler
import android.os.Looper
import android.util.Log
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

class TrainingGroup(
    val address: Int,
    val group: Group,
    val nodes: List<ProvisionedMeshNode>,
    val nodeIds: List<Int>,
    var lastReceivedStep: Int,
    var currentStep: Int,
    var starts: Array<StartAction?> = emptyArray()
) {

    companion object {
        const val MISSED_STEP_TIMEOUT = 100L
    }
    val handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null

    fun stopFallback() {
        handler.removeCallbacks(runnable)
        runnable = null
    }

    fun missedStepFallback(timeout: Int, action: () -> Unit) {
        runnable = Runnable {
            println("Thinkup: Missed step fallback ")
            Log.d("TKUP-NEURAL::", "Missed step fallback")
//            lastReceivedStep++
            action.invoke()
        }
        handler.postDelayed(runnable!!, timeout + MISSED_STEP_TIMEOUT)
    }

    fun isFromThis(srcAddress: Int): Boolean {
        nodes.find { node -> node.unicastAddress == srcAddress }?.let { unwrappednode ->
            return true
        }
        return false
    }
}