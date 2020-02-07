package fetter.gui

import fetter.analysis.FETMeasurement
import fetter.analysis.OCurve
import fetter.analysis.TCurve
import fetter.gui.Measure.addToolbarButton
import fetter.measurement.OutputMeasurement
import fetter.measurement.TransferMeasurement
import jisa.Util
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.experiment.Measurement
import jisa.gui.*
import java.io.File

object Measure : Grid("Measurement", 1) {

    val queue = ActionQueue()
    val queueList = ActionQueueDisplay("Measurements", queue)
    val basic = Fields("Data Output Settings")
    val bSection = Section(basic.title, Grid(2, basic, queueList))
    val cSection = Section("Current Measurement")
    val name = basic.addTextField("Name")
    val dir = basic.addDirectorySelect("Output Directory")

    init {
        basic.addSeparator()
    }

    val length = basic.addDoubleField("Channel Length [m]")
    val width = basic.addDoubleField("Channel Width [m]")
    val thick = basic.addDoubleField("Dielectric Thickness [m]")
    val dielectric = basic.addChoice("Dielectric Material", "CYTOP", "PMMA", "Other")
    val dielConst = basic.addDoubleField("Dielectric Constant", 1.0)

    init {
        basic.addSeparator()
    }

    val makeTables = basic.addCheckBox("Display Tables", true)
    val makePlots = basic.addCheckBox("Display Plots", true)

    val toolbarStart = addToolbarButton("Start", this::runMeasurement)
    val toolbarStop = addToolbarButton("Stop", this::stopMeasurement)
    val baseFile: String
        get() = Util.joinPath(dir.get(), name.get())

    init {
        addToolbarSeparator()
    }

    val loadButton = addToolbarButton("Load Previous Measurement", this::loadMeasurement)

    val grid = Grid(1)
    val measurements = HashMap<String, Measurement>()

    init {

        queueList.addToolbarButton("Add...") {

            val result = GUI.choiceWindow(
                "Add Action",
                "Add Measurement/Action",
                "",
                "Output Measurement",
                "Transfer Measurement",
                "Change Temperature",
                "Temperature Sweep",
                "Wait",
                "Cancel"
            )

            when (result) {

                0 -> Output.askForMeasurement(queue)
                1 -> Transfer.askForMeasurement(queue)
                2 -> Temperature.askForSingle(queue)
                3 -> Temperature.askForSweep(queue)
                4 -> Time.askWait(queue)

            }

        }

        queueList.addToolbarButton("Clear") { queue.clear() }

        toolbarStop.isDisabled = true

        setGrowth(true, false)
        setIcon(Icon.FLASK)

        addAll(Grid(2, basic, queueList), cSection, grid)

        basic.loadFromConfig("measure-basic", Settings)

        dielectric.setOnChange(this::setDielectric)
        setDielectric()

    }

    fun showMeasurement(action: ActionQueue.MeasureAction) {

        System.gc()

        val grid = Grid(2)

        if (makeTables.get()) {
            val table = Table("Data", action.data)
            grid.add(table)
        }

        if (makePlots.get()) {

            val plot = when (action.measurement) {

                is OutputMeasurement -> OutputPlot(
                    OCurve(
                        length.get(),
                        width.get(),
                        FETMeasurement.EPSILON * dielConst.get() / thick.get(),
                        action.data
                    )
                )
                is TransferMeasurement -> TransferPlot(
                    TCurve(
                        length.get(),
                        width.get(),
                        FETMeasurement.EPSILON * dielConst.get() / thick.get(),
                        action.data
                    )
                )
                else -> Plot("Unknown")

            }

            grid.add(plot)

        }

        cSection.title = action.name
        cSection.setElement(grid)

    }

    private fun setDielectric() {

        when (dielectric.get()) {

            0 -> {
                dielConst.isDisabled = true
                dielConst.set(2.05)
            }

            1 -> {
                dielConst.isDisabled = true
                dielConst.set(2.22)
            }

            2 -> {
                dielConst.isDisabled = false
            }

        }

    }

    private fun runMeasurement() {

        disable(true)
        Results.clear()

        when (queue.start()) {

            ActionQueue.Result.COMPLETED -> GUI.infoAlert("Measurement sequence completed successfully")
            ActionQueue.Result.INTERRUPTED -> GUI.warningAlert("Measurement sequence was stopped before completion")
            ActionQueue.Result.ERROR -> GUI.errorAlert("Error(s) were encountered during the measurement sequence")
            else -> {
            }

        }

        System.gc()

        disable(false)

    }

    private fun loadMeasurement() {

        val file = File(GUI.openFileSelect() ?: return)

        if (!file.name.endsWith("-info.txt")) {
            GUI.errorAlert("That is not a measurement info file!\nPlease choose the *-info.txt file generated by the measurement you wish to load.")
            return
        }

        val progress = Progress("Loading")
        progress.setTitle("Loading Data")
        progress.setStatus("Please Wait...")
        progress.setProgress(-1.0)
        progress.show()
        bSection.isExpanded = false

        // Reset everything
        grid.clear()
        measurements.clear()

        for (data in FETMeasurement(file.name.removeSuffix("-info.txt"), file.parent)) {

            val temperature = data.temperature
            val sectionName = if (temperature > -1) "$temperature K" else "No Temperature Control"
            val cols = (if (makeTables.get()) 1 else 0) + (if (makePlots.get()) 1 else 0)
            val container = Grid(cols)

            if (data.output != null) {

                if (makeTables.get()) container.add(Table("Output Data", data.output.data))
                if (makePlots.get()) container.add(OutputPlot(data.output))

            }

            if (data.transfer != null) {

                if (makeTables.get()) container.add(Table("Transfer Data", data.transfer.data))
                if (makePlots.get()) container.add(TransferPlot(data.transfer))

            }

            grid.add(Section(sectionName, container))

        }

        progress.close()

    }


    private fun disable(flag: Boolean) {

        toolbarStart.isDisabled = flag
        toolbarStop.isDisabled = !flag

        basic.setFieldsDisabled(flag)
        Temperature.disable(flag)
        Transfer.disable(flag)
        Output.disable(flag)

    }

    private fun stopMeasurement() {
        queue.stop()
    }

}