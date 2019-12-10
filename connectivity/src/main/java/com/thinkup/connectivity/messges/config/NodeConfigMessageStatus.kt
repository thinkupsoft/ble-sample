package com.thinkup.connectivity.messges.config

import com.thinkup.connectivity.messges.OpCodes
import dalvik.bytecode.Opcodes
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.SecureUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NodeConfigMessageStatus(accessMessage: AccessMessage, modelIdentifier: Int) :
    VendorModelMessageStatus(accessMessage, modelIdentifier) {

    // total = 8 bytes
    var nodeId: Int = 0 // 4 bytes
    var timeout: Int = 0 // 4 bytes

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_CONFIG_STATUS
    }

    override fun parseStatusParameters() {
        super.parseStatusParameters()
        val buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN)

        nodeId = buffer.getInt(0)
        timeout = buffer.getInt(4)
    }

    override fun toString(): String {
        return "NODE_ID = ${nodeId}, TIMEOUT = ${timeout}"
    }

}