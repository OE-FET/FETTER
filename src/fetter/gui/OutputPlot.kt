package fetter.gui

import fetter.analysis.OCurve
import fetter.analysis.TCurve
import jisa.experiment.ResultTable
import jisa.gui.*
import kotlin.math.abs

class OutputPlot(data: OCurve) : Plot("Output Curve", "SD Voltage [V]", "Current [A]") {

    init {

        useMouseCommands(true)
        setYAxisType(AxisType.LINEAR)
        setPointOrdering(Sort.ORDER_ADDED)

        createSeries()
            .showMarkers(false)
            .watch(data.data, { it[SD_VOLTAGE] }, { abs(it[SD_CURRENT]) })
            .split(SET_SG, "D (SG: %s V)")

        createSeries()
            .showMarkers(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data.data, { it[SD_VOLTAGE] }, { abs(it[SD_CURRENT]) })
            .split(SET_SG, "G (SG: %sV)")


        addSaveButton("Save")
        addToolbarSeparator()
        addToolbarButton("Linear") { setYAxisType(AxisType.LINEAR) }
        addToolbarButton("Logarithmic") { setYAxisType(AxisType.LOGARITHMIC) }

        addToolbarSeparator()

        addToolbarButton("Mobility") {

            if (!data.calculated) data.calculate()

            val mobPlot = Plot("Mobility", "SD Voltage [V]", "Mobility [cm^2/Vs]")
            val fwdTable = Table("Forward Sweep", data.fwdMob)
            val bwdTable = Table("Backward Sweep", data.fwdMob)

            mobPlot.createSeries()
                .showMarkers(false)
                .watch(data.fwdMob, TCurve.SD_VOLTAGE, TCurve.MOBILITY)
                .split(TCurve.SG_VOLTAGE, "Fwd SG: %s V")

            mobPlot.createSeries()
                .setLineDash(Series.Dash.DOTTED)
                .showMarkers(false)
                .watch(data.bwdMob, TCurve.SD_VOLTAGE, TCurve.MOBILITY)
                .split(TCurve.SG_VOLTAGE, "Bwd SG: %s V")

            mobPlot.useMouseCommands(true)

            Grid("Mobilities", 1, mobPlot, Grid(2, fwdTable, bwdTable)).apply {

                addToolbarButton("Save Data") {
                    val file = GUI.saveFileSelect()

                    if (file != null) {
                        data.fwdMob.output("$file-transfer-fwd.csv")
                        data.bwdMob.output("$file-transfer-bwd.csv")
                    }

                }
                addToolbarSeparator()
                addToolbarButton("Save Plot") { mobPlot.showSaveDialog() }
                addToolbarSeparator()
                addToolbarButton("Linear Plot") { mobPlot.setYAxisType(AxisType.LINEAR) }
                addToolbarButton("Logarithmic Plot") { mobPlot.setYAxisType(AxisType.LOGARITHMIC) }

            }.show()

        }

    }

}