package com.thinkup.blesample.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.thinkup.blesample.R
import com.thinkup.blesample.Utils
import com.thinkup.blesample.renderers.EventRenderer
import com.thinkup.connectivity.BleNode
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.config.NodeConfigMessageStatus
import com.thinkup.connectivity.messges.control.NodeControlMessageStatus
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.connectivity.messges.peripheral.NodePeripheralMessageStatus
import com.thinkup.connectivity.messges.status.NodeGetMessageStatus
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_node_detail.*
import kotlinx.android.synthetic.main.toolbar.*
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.ext.android.inject

class NodeDetailActivity : BaseActivity() {

    companion object {
        const val NODE = "node"
    }

    val bleNode: BleNode by inject()
    var node: ProvisionedMeshNode? = null
    val adapter = RendererAdapter()
    val list = mutableListOf<NodeEventStatus>()

    override fun title(): String = "Node ${node?.nodeName}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_node_detail)
        node = intent.getParcelableExtra(NODE)
        identify.setOnClickListener { bleNode.identify(node!!) }
        startUI()

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {
                p0?.let {
                    when (it.position) {
                        0 -> {
                            control.visibility = View.GONE
                        }
                        1 -> {
                            config.visibility = View.GONE
                        }
                        2 -> {
                            periferal.visibility = View.GONE
                        }
                        3 -> {
                            events.visibility = View.GONE
                        }
                    }
                }
            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                p0?.let {
                    when (it.position) {
                        0 -> {
                            control.visibility = View.VISIBLE
                        }
                        1 -> {
                            config.visibility = View.VISIBLE
                        }
                        2 -> {
                            periferal.visibility = View.VISIBLE
                        }
                        3 -> {
                            events.visibility = View.VISIBLE
                        }
                    }
                }
            }

        })
        startBase()
        startEvent()
        startControl()
        startConfig()
        startPeripheral()
        messages()
    }

    private fun messages() {
        bleNode.getMessages().observe(this, Observer {
            when {
                it is NodeControlMessageStatus -> {
                    controlResponse.text = it.toString()
                }
                it is NodeConfigMessageStatus -> {
                    configResponse.text = it.toString()
                }
                it is NodePeripheralMessageStatus -> {
                    periferalResponse.text = it.toString()
                }
                it is NodeGetMessageStatus -> {
                    statusResponse.text = "RESPONSE: $it"
                }
            }
        })
    }

    private fun startUI() {
        // Events
        adapter.addRenderer(EventRenderer())
        eventsList.adapter = adapter
        eventsList.layoutManager = LinearLayoutManager(this)
        eventsList.isNestedScrollingEnabled = false

        // Config
        timeout.minValue = 0
        timeout.maxValue = 10
        timeout.wrapSelectorWheel = true
        nodeIdText.setText(node?.nodeName ?: "")

        // Peripheral
        val shapesArray = Utils.getAttrs(ShapeParams.javaClass).filter { it != "INSTANCE" }
        shape.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, shapesArray)
        val colorsArray = Utils.getAttrs(ColorParams.javaClass).filter { it != "INSTANCE" }
        color.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, colorsArray)
        val dimmerValues = (0x00..0x64).toList().map { it.toString() }
        dimmer.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dimmerValues)
        sound.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayOf("NO_SOUND", "BIP_START", "BIP_HIT", "BIP_START_HIT"))
        led.adapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayOf("LED_PERMANENT", "LED_FLICKER", "LED_FLASH", "LED_FAST_FLASH"))
    }

    private fun startEvent() {
        bleNode.getEvents().observe(this, Observer {
            it?.let {
                list.add(it)
                adapter.setItems(list)
            }
        })
    }

    private fun startBase() {
        nodeStatus.setOnClickListener { bleNode.getStatus(node!!) }
    }

    private fun startControl() {
        controlStart.setOnClickListener { bleNode.controlMessage(node!!, ControlParams.START, ackMessage.isChecked) }
        controlStop.setOnClickListener { bleNode.controlMessage(node!!, ControlParams.STOP, ackMessage.isChecked) }
        controlPause.setOnClickListener { bleNode.controlMessage(node!!, ControlParams.PAUSE, ackMessage.isChecked) }
        controlLedOn.setOnClickListener { bleNode.controlMessage(node!!, ControlParams.SET_LED_ON, ackMessage.isChecked) }
        controlLedOff.setOnClickListener { bleNode.controlMessage(node!!, ControlParams.SET_LED_OFF, ackMessage.isChecked) }
        controlRecalibrar.setOnClickListener { bleNode.controlMessage(node!!, ControlParams.RECALIBRAR, ackMessage.isChecked) }
    }

    private fun startPeripheral() {
        periferalSet.setOnClickListener {
            val selShape = Utils.getValue(ShapeParams.javaClass, shape.selectedItem.toString())?.toString()?.toInt() ?: NO_CONFIG
            val selColor = Utils.getValue(ColorParams.javaClass, color.selectedItem.toString())?.toString()?.toInt() ?: NO_CONFIG
            val selLed = Utils.getValue(PeripheralParams.javaClass, led.selectedItem.toString())?.toString()?.toInt() ?: NO_CONFIG
            val selSound = Utils.getValue(PeripheralParams.javaClass, sound.selectedItem.toString())?.toString()?.toInt() ?: NO_CONFIG
            val fill: Int = when (fillGroup.checkedRadioButtonId) {
                R.id.solid -> PeripheralParams.FILL
                else -> PeripheralParams.STROKE
            }
            val gesture: Int = when (gestureGroup.checkedRadioButtonId) {
                R.id.hover -> PeripheralParams.HOVER
                R.id.touch -> PeripheralParams.TOUCH
                else -> PeripheralParams.BOTH
            }
            val distance: Int = when (distanceGroup.checkedRadioButtonId) {
                R.id.high -> PeripheralParams.HIGH
                R.id.medium -> PeripheralParams.MIDDLE
                else -> PeripheralParams.LOW
            }
            val filter: Int = when (filterGroup.checkedRadioButtonId) {
                R.id.sunny -> PeripheralParams.SUN
                else -> PeripheralParams.INDOOR
            }
            bleNode.setPeripheralMessage(
                node!!, selShape, selColor, dimmer.selectedItem.toString().toInt(), selLed, fill,
                gesture, distance, filter, NO_CONFIG, selSound, ackMessage.isChecked
            )
        }
    }

    private fun startConfig() {
        configSet.setOnClickListener {
            bleNode.configMessage(
                node!!,
                id = if (nodeIdText.text.toString().isNullOrEmpty()) NO_CONFIG else nodeIdText.text.toString().toInt(),
                timeoutConfig = if (timeout.value == 0) NO_CONFIG else ConfigParams.TIMEOUT_CONFIG,
                timeout = timeout.value * 1000, // to milliseconds
                ack = ackMessage.isChecked
            )
        }
    }
}