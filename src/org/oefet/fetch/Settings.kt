package org.oefet.fetch

import jisa.control.ConfigFile

object Settings : ConfigFile("FetCh") {

    val instruments       = subBlock("instruments")
    val measureBasic      = subBlock("measureBasic")
    val repeatBasic       = subBlock("repeatBasic")
    val condBasic         = subBlock("condBasic")
    val condSD            = subBlock("condSD")
    val outputBasic       = subBlock("outputBasic")
    val outputSD          = subBlock("outputSD")
    val outputSG          = subBlock("outputSG")
    val transferBasic     = subBlock("transferBasic")
    val transferSD        = subBlock("transferSD")
    val transferSG        = subBlock("transferSG")
    val fppBasic          = subBlock("fppBasic")
    val fppSD             = subBlock("fppSD")
    val fppSG             = subBlock("fppSG")
    val syncBasic         = subBlock("syncBasic")
    val syncSD            = subBlock("syncSD")
    val syncSG            = subBlock("syncSG")
    val holdBasic         = subBlock("holdBasic")
    val holdSD            = subBlock("holdSD")
    val holdSG            = subBlock("holdSG")
    val stressBasic       = subBlock("stressBasic")
    val stressInterval    = subBlock("stressInterval")
    val tempBasic         = subBlock("tempBasic")
    val tempSingleBasic   = subBlock("tempSingleBasic")
    val groundConfig      = subBlock("groundConfig")
    val sourceDrainConfig = subBlock("sourceDrainConfig")
    val sourceGateConfig  = subBlock("sourceGateConfig")
    val fourPP1Config     = subBlock("fourPP1Config")
    val fourPP2Config     = subBlock("fourPP2Config")
    val tControlConfig    = subBlock("tControlConfig")
    val tMeterConfig      = subBlock("tMeterConfig")

}