package fetter.gui

import fetter.measurement.Instruments
import fetter.measurement.OutputMeasurement
import jisa.enums.Icon
import jisa.experiment.Measurement
import jisa.gui.Fields
import jisa.gui.Grid

object Output : Grid("Output Curve", 1) {

    val basic       = Fields("Basic Parameters")
    val enabled     = basic.addCheckBox("Enabled", false)

    init { basic.addSeparator() }

    val intTime     = basic.addDoubleField("Integration Time [s]", 1.0 / 50.0)
    val delTime     = basic.addDoubleField("Delay Time [s]", 0.5)

    val sourceDrain = Fields("Source-Drain Parameters")
    val minSDV      = sourceDrain.addDoubleField("Start [V]", 0.0)
    val maxSDV      = sourceDrain.addDoubleField("Stop [V]", 60.0)
    val numSDV      = sourceDrain.addIntegerField("No. Steps", 61)

    init { sourceDrain.addSeparator() }

    val symSDV      = sourceDrain.addCheckBox("Sweep Both Ways", true)

    val sourceGate  = Fields("Source-Gate Parameters")
    val minSGV      = sourceGate.addDoubleField("Start [V]", 0.0)
    val maxSGV      = sourceGate.addDoubleField("Stop [V]", 60.0)
    val numSGV      = sourceGate.addIntegerField("No. Steps", 7)

    init { sourceGate.addSeparator() }

    val symSGV      = sourceGate.addCheckBox("Sweep Both Ways", false)


    init {

        symSGV.isVisible = false

        addAll(basic, Grid(2, sourceDrain, sourceGate))
        setGrowth(true, false)
        setIcon(Icon.RHEOSTAT)

        basic.loadFromConfig("op-basic", Settings)
        sourceDrain.loadFromConfig("op-sd", Settings)
        sourceGate.loadFromConfig("op-sg", Settings)

        enabled.setOnChange(this::updateEnabled)
        updateEnabled()

    }

    fun updateEnabled() {

        val disabled = !enabled.get()

        intTime.isDisabled = disabled
        delTime.isDisabled = disabled

        sourceDrain.setFieldsDisabled(disabled)
        sourceGate.setFieldsDisabled(disabled)

    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
        sourceDrain.setFieldsDisabled(flag)
        sourceGate.setFieldsDisabled(flag)
        if (!flag) updateEnabled()
    }

    fun getMeasurement(devices: Instruments) : Measurement {

        if (devices.sdSMU == null || devices.sgSMU == null) {
            throw Exception("Source-Drain and Source-Gate SMU channels must be configured!")
        }

        val measurement = OutputMeasurement(
            devices.sdSMU,
            devices.sgSMU,
            devices.gdSMU,
            devices.fpp1,
            devices.fpp2,
            devices.tm
        )

        measurement.configureSD(
            minSDV.get(),
            maxSDV.get(),
            numSDV.get(),
            symSDV.get()
        ).configureSG(
            minSGV.get(),
            maxSGV.get(),
            numSGV.get(),
            symSGV.get()
        ).configureTimes(
            intTime.get(),
            delTime.get()
        )

        return measurement

    }

    val isEnabled: Boolean
        get() = enabled.get()

}