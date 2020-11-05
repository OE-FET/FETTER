package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import java.util.*

class TVCalibration : FMeasurement() {

    override val type = "Thermal Voltage Calibration"

    val label         = StringParameter("Basic", "Name", null, "TVC")
    val intTimeParam  = DoubleParameter("Basic", "Integration Time", "s", 20e-3)
    val avgCountParam = IntegerParameter("Basic", "Averaging Count", null,1)
    val probeParam    = IntegerParameter("Basic", "Strip Number", null, 0)
    val minHVParam    = DoubleParameter("Heater", "Start", "V", 0.0)
    val maxHVParam    = DoubleParameter("Heater", "Stop", "V", 10.0)
    val numHVParam    = IntegerParameter("Heater", "No. Steps", null, 11)
    val holdHVParam   = DoubleParameter("Heater", "Hold Time", "s", 60.0)
    val minSIParam    = DoubleParameter("Resistive Thermometer", "Start", "A", 0.0)
    val maxSIParam    = DoubleParameter("Resistive Thermometer", "Stop", "A", 100e-6)
    val numSIParam    = IntegerParameter("Resistive Thermometer", "No. Steps", null, 11)
    val holdSIParam   = DoubleParameter("Resistive Thermometer", "Hold Time", "s", 0.5)

    val intTime  get() = intTimeParam .value
    val avgCount get() = avgCountParam.value
    val probe    get() = probeParam.value
    val minHV    get() = minHVParam.value
    val maxHV    get() = maxHVParam.value
    val numHV    get() = numHVParam.value
    val holdHV   get() = (1e3 * holdHVParam.value).toInt()
    val minSI    get() = minSIParam.value
    val maxSI    get() = maxSIParam.value
    val numSI    get() = numSIParam.value
    val holdSI   get() = (1e3 * holdSIParam.value).toInt()

    override fun loadInstruments() {

        gdSMU    = Instruments.gdSMU
        heater   = Instruments.htSMU
        sdSMU    = Instruments.sdSMU
        fpp1     = Instruments.fpp1
        fpp2     = Instruments.fpp2
        tMeter   = Instruments.tMeter

    }

    override fun checkForErrors(): List<String> {

        val errors = LinkedList<String>()

        if (heater == null) {
            errors += "Heater is not configured"
        }

        if (sdSMU == null) {
            errors += "Source-drain channel is not configured"
        }

        if (tMeter == null) {
            errors += "No thermometer configured"
        }

        return errors

    }

    override fun run(results: ResultTable) {

        val tMeter = this.tMeter!!
        val heater = this.heater!!
        val sdSMU  = this.sdSMU!!

        results.setAttribute("Probe Number", probe.toString())

        gdSMU?.turnOff()
        heater.turnOff()
        sdSMU.turnOff()

        gdSMU?.integrationTime  = intTime
        gdSMU?.voltage          = 0.0
        heater.integrationTime  = intTime
        heater.voltage          = minHV
        sdSMU.integrationTime   = intTime
        sdSMU.current           = minSI
        sdSMU.averageMode       = AMode.MEAN_REPEAT
        sdSMU.averageCount      = avgCount

        gdSMU?.turnOn()

        for (heaterVoltage in Range.linear(minHV, maxHV, numHV)) {

            heater.voltage = heaterVoltage
            heater.turnOn()
            sleep(holdHV)

            for (stripCurrent in Range.linear(minSI, maxSI, numSI)) {

                sdSMU.current = stripCurrent
                sdSMU.turnOn()
                sleep(holdSI)

                results.addData(
                    heaterVoltage,
                    stripCurrent,
                    gdSMU?.current ?: Double.NaN,
                    heater.voltage,
                    heater.current,
                    sdSMU.voltage,
                    sdSMU.current,
                    tMeter.temperature
                )

            }

        }

    }

    override fun onFinish() {
        runRegardless { heater?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { sdSMU?.turnOff() }
    }

    override fun getLabel(): String {
        return label.value
    }

    override fun getName(): String {
        return "Thermal Voltage Calibration Measurement"
    }

    companion object {
        const val SET_HEATER_VOLTAGE = 0
        const val SET_STRIP_CURRENT  = 1
        const val GROUND_CURRENT     = 2
        const val HEATER_VOLTAGE     = 3
        const val HEATER_CURRENT     = 4
        const val HEATER_POWER       = 5
        const val STRIP_VOLTAGE      = 6
        const val STRIP_CURRENT      = 7
        const val TEMPERATURE        = 8
    }

    override fun getColumns(): Array<Col> {
        return arrayOf(
            Col("Set Heater Voltage", "V"),
            Col("Set Strip Current", "A"),
            Col("Ground Current", "A"),
            Col("Heater Voltage", "V"),
            Col("Heater Current", "A"),
            Col("Heater Power", "W") { it[HEATER_VOLTAGE] * it[HEATER_CURRENT] },
            Col("Strip Voltage", "V"),
            Col("Strip Current", "A"),
            Col("Temperature", "K")
        )
    }

    override fun setLabel(value: String?) {
        label.value = value
    }

    override fun onInterrupt() {

    }

}