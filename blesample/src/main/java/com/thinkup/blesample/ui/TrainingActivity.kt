package com.thinkup.blesample.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.thinkup.blesample.R
import com.thinkup.blesample.Utils
import com.thinkup.blesample.renderers.EventRenderer
import com.thinkup.connectivity.BleFastTraining
import com.thinkup.connectivity.common.FastOptions
import com.thinkup.connectivity.messges.*
import com.thinkup.connectivity.messges.event.NodeEventStatus
import com.thinkup.easylist.RendererAdapter
import kotlinx.android.synthetic.main.activity_group_trianing.*
import kotlinx.android.synthetic.main.activity_group_trianing.dimmer
import kotlinx.android.synthetic.main.activity_group_trianing.sound
import kotlinx.android.synthetic.main.activity_group_trianing.timeout
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import org.koin.android.ext.android.inject

class TrainingActivity : BaseActivity(), BleFastTraining.TrainingCallback {

    private val bleFastTraining: BleFastTraining by inject()
    private val adapter = RendererAdapter()
    private val list = mutableListOf<NodeEventStatus>()

    private val timeoutValues = getRange(0.0, 5.0, 0.5)
    private val delayValues = getRange(0.1, 5.0, 0.1)
    private val threeValues = arrayOf("Low", "Medium", "High")
    private val touchesValues = (1..20 step 1).map { it.toString() }.toTypedArray()

    override fun title(): String = "Training"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_trianing)

        timeout.displayedValues = timeoutValues
        timeout.maxValue = timeoutValues.size - 1
        timeout.minValue = 0
        timeout.wrapSelectorWheel = false
        delay.displayedValues = delayValues
        delay.maxValue = delayValues.size - 1
        delay.minValue = 0
        delay.wrapSelectorWheel = false
        touches.displayedValues = touchesValues
        touches.maxValue = touchesValues.size - 1
        touches.minValue = 0
        touches.wrapSelectorWheel = false
        dimmer.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, threeValues)
        distance.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, threeValues)
        val shapesArray = Utils.getAttrs(ShapeParams.javaClass).filter { it != "INSTANCE" }
        val colorsArray = Utils.getAttrs(ColorParams.javaClass).filter { it != "INSTANCE" && it != "COLOR_CUSTOM" }
        val groupsList = bleFastTraining.getGroups().value
        shapes.setItems(shapesArray)
        colors.setItems(colorsArray)
        groupsList?.let {
            groups.setItems(it)
        }

        executeTraining.setOnClickListener { execute() }
        messages()
    }

    private fun messages() {
        adapter.addRenderer(EventRenderer())
        trainingList.adapter = adapter
        trainingList.layoutManager = LinearLayoutManager(this)
        trainingList.isNestedScrollingEnabled = false
    }

    private fun execute() {
        val opTouches = touchesValues[touches.value].toInt()
        val opTimeout = timeoutValues[timeout.value].toDouble() * 1000
        val opDelay = delayValues[delay.value].toDouble() * 1000
        val selectedShapes = Utils.getValues(ShapeParams.javaClass, shapes.getSelecteds() as List<String>).map { it as Int }
        val selectedColors = Utils.getValues(ColorParams.javaClass, colors.getSelecteds() as List<String>).map { it as Int }
        val options = FastOptions(
            opTouches, opTimeout.toInt(), opDelay.toInt(),
            selectedShapes, selectedColors,
            countdown.isChecked, if (ledmode.isChecked) PeripheralParams.LED_FLASH else PeripheralParams.LED_PERMANENT,
            distance.selectedItemPosition, dimmer.selectedItemPosition,
            sound.isChecked, endlight.isChecked
        )
        actualConfig.text = "ACTUAL CONFIG :: $options"


        val selectedGroups = groups.getSelecteds() as List<Group>
        bleFastTraining.set(
            groups = selectedGroups,
            options = options,
            callback = this
        )
    }

    private fun getRange(min: Double, max: Double, step: Double): Array<String> {
        val minInt = (min * 10).toInt()
        val maxInt = (max * 10).toInt()
        val stepInt = (step * 10).toInt()
        val range = (minInt..maxInt step stepInt)
        return range.map { (it / 10.0).toString() }.toTypedArray()
    }

    override fun onSettingStart() {

    }

    override fun onSettingComplete() {
        bleFastTraining.startTraining()
    }

    override fun onCountdown() {
    }

    override fun onAction(group: Group?, node: ProvisionedMeshNode?, nodeEventStatus: NodeEventStatus, event: EventType?, time: Long) {
        list.add(nodeEventStatus)
        adapter.setItems(list)
    }

    override fun onStartTraining() {
        list.clear()
        adapter.setItems(list)
    }

    override fun onStopTraining() {
    }

    override fun onCompleteTraining() {
        Toast.makeText(this, "Complete", Toast.LENGTH_SHORT).show()
    }
}