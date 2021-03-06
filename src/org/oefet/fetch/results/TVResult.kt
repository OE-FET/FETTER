package org.oefet.fetch.results

import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.maths.fits.Fitting
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.TVMeasurement
import org.oefet.fetch.quantities.*
import kotlin.math.pow
import kotlin.math.sqrt

class TVResult(data: ResultTable, extraParams: List<Quantity> = emptyList()) :
    FetChResult("Thermal Voltage Measurement", "Thermal Voltage", Images.getImage("fire.png"), data, extraParams) {

    val SET_GATE              = data.findColumn(TVMeasurement.SET_GATE)
    val TEMPERATURE           = data.findColumn(TVMeasurement.TEMPERATURE)
    val HEATER_POWER          = data.findColumn(TVMeasurement.HEATER_POWER)
    val THERMAL_VOLTAGE       = data.findColumn(TVMeasurement.THERMAL_VOLTAGE)

    private val possibleParameters = listOf(
        Device::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class
    )

    init {

        for ((gate, data) in data.split(SET_GATE)) {

            val params = ArrayList(parameters)
            params    += Gate(gate, 0.0)

            val fit = data.linearFit(HEATER_POWER, THERMAL_VOLTAGE)

            if (fit != null) addQuantity(SeebeckPower(fit.gradient, fit.gradientError, params, possibleParameters))

        }

    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {

        val calibrationLeft = otherQuantities.filter {
            it is LeftStripResistance
            && it.isCompatibleWith(quantities.first())
            && it.getParameter(Drain::class)?.value ?: 1.0 == 0.0
        }

        val calibrationRight = otherQuantities.filter {
            it is RightStripResistance
            && it.isCompatibleWith(quantities.first())
            && it.getParameter(Drain::class)?.value ?: 1.0 == 0.0
        }

        if (calibrationLeft.isEmpty() || calibrationRight.isEmpty()) {
            return emptyList()
        }

        val left  = ResultList("Resistance", "Temperature")
        val right = ResultList("Resistance", "Temperature")

        for (resistance in calibrationLeft) {

            val temperature = resistance.getParameter(Temperature::class) ?: continue
            left.addData(resistance.value, temperature.value)

        }

        for (resistance in calibrationRight) {

            val temperature = resistance.getParameter(Temperature::class) ?: continue
            right.addData(resistance.value, temperature.value)

        }

        val leftFit  = left.linearFit(0, 1)
        val rightFit = right.linearFit(0, 1)

        val temperature = parameters.find { it is Temperature }?.value

        val powerLeft = otherQuantities.filter {
            it is LeftStripResistance
            && it.isCompatibleWith(quantities.first())
            && it.getParameter(Temperature::class)?.value == temperature
        }

        val powerRight = otherQuantities.filter {
            it is RightStripResistance
            && it.isCompatibleWith(quantities.first())
            && it.getParameter(Temperature::class)?.value == temperature
        }

        val dataLeft  = ResultList("Power", "Temperature", "Error")
        val dataRight = ResultList("Power", "Temperature", "Error")

        for (resistance in powerLeft) {

            val power        = resistance.getParameter(Power::class) ?: continue
            val temp         = leftFit.gradient * resistance.value + leftFit.intercept
            val intermediate = leftFit.gradient * resistance.value * sqrt((leftFit.gradientError/leftFit.gradient).pow(2) + (resistance.error/resistance.value).pow(2))
            val tempError    = sqrt(intermediate.pow(2) + leftFit.interceptError.pow(2))

            dataLeft.addData(power.value, temp, tempError)

        }

        for (resistance in powerRight) {

            val power        = resistance.getParameter(Power::class) ?: continue
            val temp         = rightFit.gradient * resistance.value + rightFit.intercept
            val intermediate = rightFit.gradient * resistance.value * sqrt((rightFit.gradientError/rightFit.gradient).pow(2) + (resistance.error/resistance.value).pow(2))
            val tempError    = sqrt(intermediate.pow(2) + rightFit.interceptError.pow(2))

            dataRight.addData(power.value, temp, tempError)

        }

        val leftPowerFit  = dataLeft.linearFit(0, 1).function
        val rightPowerFit = dataRight.linearFit(0, 1).function

        val extras = ArrayList<Quantity>()

        for ((gate, data) in data.split(SET_GATE)) {

            val params = ArrayList(parameters)
            params    += Gate(gate, 0.0)
            val dT     = rightPowerFit.value(data.getColumns(HEATER_POWER)) - leftPowerFit.value(data.getColumns(HEATER_POWER))
            val fit    = Fitting.linearFit(dT, data.getColumns(THERMAL_VOLTAGE))

            if (fit != null) extras += SeebeckCoefficient(fit.gradient, fit.gradientError, params, possibleParameters)

        }

        return extras

    }

}