package com.thinkup.connectivity.messges.event

import com.thinkup.connectivity.messges.EventType
import com.thinkup.connectivity.messges.OpCodes
import com.thinkup.connectivity.utils.ComparableEvent
import no.nordicsemi.android.meshprovisioner.transport.AccessMessage
import no.nordicsemi.android.meshprovisioner.transport.GenericStatusMessage
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.abs

class NodeEventStatus(accessMessage: AccessMessage) : GenericStatusMessage(accessMessage), ComparableEvent.ComparableLiveData {

    private val REJECT_TIME_ELLAPSED = 1000

    init {
        mParameters = accessMessage.parameters
        parseStatusParameters()
    }

    // 5 bytes
    var eventType: EventType? = null
    var value: Int = 0 // TIME_SPENT (ms) or TIME_SPENT (ms)
    var timestamp: Long = 0 // Mark to filter duplicated events

    override fun getOpCode(): Int {
        return OpCodes.NT_OPCODE_EVENT
    }

    override fun parseStatusParameters() {
        val buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN)

        val type = buffer.get(0)
        eventType = EventType.getType(MeshParserUtils.unsignedByteToInt(type))
        value = buffer.getInt(1)
        timestamp = Calendar.getInstance().timeInMillis
    }

    override fun getKey(): Int {
        return srcAddress
    }

    override fun equals(other: Any?): Boolean {
        val comparable = other as NodeEventStatus?
        comparable?.let {
            return if (comparable.eventType == eventType && comparable.value == value && comparable.srcAddress == srcAddress)
                return abs(timestamp - comparable.timestamp) <= REJECT_TIME_ELLAPSED
            else false
        } ?: run { return false }
    }

    override fun toString(): String {
        return "SRC=${MeshAddress.formatAddress(srcAddress, true)}, TYPE=${eventType}, VALUE=${value}, TIME=${timestamp}"
    }
}