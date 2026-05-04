/*  Copyright (C) 2015-2024 115ek, akasaka / Genjitsu Labs, Alicia Hormann,
    Andreas Böhler, Andreas Shimokawa, Andrew Watkins, angelpup, Carsten Pfeiffer,
    Cre3per, Damien Gaignon, DanialHanif, Daniel Dakhno, Daniele Gobbetti, Daniel
    Thompson, Da Pa, Dmytro Bielik, Frank Ertl, Gabriele Monaco, GeekosaurusR3x,
    Gordon Williams, Jean-François Greffier, jfgreffier, jhey, João Paulo
    Barraca, Jochen S, Johannes Krude, José Rebelo, ksiwczynski, ladbsoft,
    Lesur Frederic, Maciej Kuśnierz, mamucho, Manuel Ruß, Maxime Reyrolle,
    maxirnilian, Michael, narektor, Noodlez, odavo32nof, opavlov, pangwalla,
    Pavel Elagin, Petr Kadlec, Petr Vaněk, protomors, Quallenauge, Quang Ngô,
    Raghd Hamzeh, Sami Alaoui, Sebastian Kranz, sedy89, Sophanimus, Stefan Bora,
    Taavi Eomäe, thermatk, tiparega, Vadim Kaushan, x29a, xaos, Yoran Vulker,
    Yukai Li

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package id.icapps.savera.model;

import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.devices.UnknownDeviceCoordinator;
import id.icapps.savera.devices.asteroidos.AsteroidOSDeviceCoordinator;
import id.icapps.savera.devices.banglejs.BangleJSCoordinator;
import id.icapps.savera.devices.binary_sensor.coordinator.BinarySensorCoordinator;
import id.icapps.savera.devices.casio.gb6900.CasioGB6900DeviceCoordinator;
import id.icapps.savera.devices.casio.gbx100.CasioGBX100DeviceCoordinator;
import id.icapps.savera.devices.casio.gwb5600.CasioGMWB5000DeviceCoordinator;
import id.icapps.savera.devices.casio.gwb5600.CasioGWB5600DeviceCoordinator;
import id.icapps.savera.devices.cmfwatchpro.CmfWatchPro2Coordinator;
import id.icapps.savera.devices.cmfwatchpro.CmfWatchProCoordinator;
import id.icapps.savera.devices.colmi.ColmiR02Coordinator;
import id.icapps.savera.devices.colmi.ColmiR03Coordinator;
import id.icapps.savera.devices.colmi.ColmiR06Coordinator;
import id.icapps.savera.devices.colmi.ColmiR10Coordinator;
import id.icapps.savera.devices.cycling_sensor.coordinator.CyclingSensorCoordinator;
import id.icapps.savera.devices.divoom.PixooCoordinator;
import id.icapps.savera.devices.domyos.DomyosT540Coordinator;
import id.icapps.savera.devices.femometer.FemometerVinca2DeviceCoordinator;
import id.icapps.savera.devices.fitpro.FitProDeviceCoordinator;
import id.icapps.savera.devices.fitpro.colacao.ColaCao21Coordinator;
import id.icapps.savera.devices.fitpro.colacao.ColaCao23Coordinator;
import id.icapps.savera.devices.flipper.zero.FlipperZeroCoordinator;
import id.icapps.savera.devices.galaxy_buds.GalaxyBuds2DeviceCoordinator;
import id.icapps.savera.devices.galaxy_buds.GalaxyBuds2ProDeviceCoordinator;
import id.icapps.savera.devices.galaxy_buds.GalaxyBudsDeviceCoordinator;
import id.icapps.savera.devices.galaxy_buds.GalaxyBudsLiveDeviceCoordinator;
import id.icapps.savera.devices.galaxy_buds.GalaxyBudsProDeviceCoordinator;
import id.icapps.savera.devices.garmin.watches.enduro.GarminEnduro3Coordinator;
import id.icapps.savera.devices.garmin.watches.epix.GarminEpixProCoordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix5Coordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix5PlusCoordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix5XPlusCoordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix6Coordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix6SSapphireCoordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix6SapphireCoordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix7ProCoordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix7SCoordinator;
import id.icapps.savera.devices.garmin.watches.fenix.GarminFenix8Coordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner165Coordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner245Coordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner245MusicCoordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner255Coordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner255MusicCoordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner255SCoordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner255SMusicCoordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner265Coordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner265SCoordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner955Coordinator;
import id.icapps.savera.devices.garmin.watches.forerunner.GarminForerunner965Coordinator;
import id.icapps.savera.devices.garmin.watches.instinct.GarminInstinct2SCoordinator;
import id.icapps.savera.devices.garmin.watches.instinct.GarminInstinct2SSolarCoordinator;
import id.icapps.savera.devices.garmin.watches.instinct.GarminInstinct2SolTacCoordinator;
import id.icapps.savera.devices.garmin.watches.instinct.GarminInstinct2SolarCoordinator;
import id.icapps.savera.devices.garmin.watches.instinct.GarminInstinct2XSolarCoordinator;
import id.icapps.savera.devices.garmin.watches.instinct.GarminInstinctCoordinator;
import id.icapps.savera.devices.garmin.watches.instinct.GarminInstinctCrossoverCoordinator;
import id.icapps.savera.devices.garmin.watches.instinct.GarminInstinctSolarCoordinator;
import id.icapps.savera.devices.garmin.watches.swim.GarminSwim2Coordinator;
import id.icapps.savera.devices.garmin.watches.venu.GarminVenu2Coordinator;
import id.icapps.savera.devices.garmin.watches.venu.GarminVenu2PlusCoordinator;
import id.icapps.savera.devices.garmin.watches.venu.GarminVenu2SCoordinator;
import id.icapps.savera.devices.garmin.watches.venu.GarminVenuSq2Coordinator;
import id.icapps.savera.devices.garmin.watches.venu.GarminVenu3Coordinator;
import id.icapps.savera.devices.garmin.watches.venu.GarminVenu3SCoordinator;
import id.icapps.savera.devices.garmin.watches.venu.GarminVenuCoordinator;
import id.icapps.savera.devices.garmin.watches.venu.GarminVenuSqCoordinator;
import id.icapps.savera.devices.garmin.watches.vivoactive.GarminVivoActive3Coordinator;
import id.icapps.savera.devices.garmin.watches.vivoactive.GarminVivoActive4Coordinator;
import id.icapps.savera.devices.garmin.watches.vivoactive.GarminVivoActive4SCoordinator;
import id.icapps.savera.devices.garmin.watches.vivoactive.GarminVivoActive5Coordinator;
import id.icapps.savera.devices.garmin.watches.vivomove.GarminVivomoveHrCoordinator;
import id.icapps.savera.devices.garmin.watches.vivomove.GarminVivomoveStyleCoordinator;
import id.icapps.savera.devices.garmin.watches.vivomove.GarminVivomoveTrendCoordinator;
import id.icapps.savera.devices.garmin.watches.vivosmart.GarminVivosmart5Coordinator;
import id.icapps.savera.devices.garmin.watches.vivosport.GarminVivosportCoordinator;
import id.icapps.savera.devices.hama.fit6900.HamaFit6900DeviceCoordinator;
import id.icapps.savera.devices.hplus.EXRIZUK8Coordinator;
import id.icapps.savera.devices.hplus.HPlusCoordinator;
import id.icapps.savera.devices.hplus.MakibesF68Coordinator;
import id.icapps.savera.devices.hplus.Q8Coordinator;
import id.icapps.savera.devices.hplus.SG2Coordinator;
import id.icapps.savera.devices.huami.amazfitactive.AmazfitActiveCoordinator;
import id.icapps.savera.devices.huami.amazfitactiveedge.AmazfitActiveEdgeCoordinator;
import id.icapps.savera.devices.huami.amazfitbalance.AmazfitBalanceCoordinator;
import id.icapps.savera.devices.huami.amazfitband5.AmazfitBand5Coordinator;
import id.icapps.savera.devices.huami.amazfitband7.AmazfitBand7Coordinator;
import id.icapps.savera.devices.huami.amazfitbip.AmazfitBipCoordinator;
import id.icapps.savera.devices.huami.amazfitbip.AmazfitBipLiteCoordinator;
import id.icapps.savera.devices.huami.amazfitbip3.AmazfitBip3Coordinator;
import id.icapps.savera.devices.huami.amazfitbip3pro.AmazfitBip3ProCoordinator;
import id.icapps.savera.devices.huami.amazfitbip5.AmazfitBip5Coordinator;
import id.icapps.savera.devices.huami.amazfitbip5unity.AmazfitBip5UnityCoordinator;
import id.icapps.savera.devices.huami.amazfitbips.AmazfitBipSCoordinator;
import id.icapps.savera.devices.huami.amazfitbips.AmazfitBipSLiteCoordinator;
import id.icapps.savera.devices.huami.amazfitbipu.AmazfitBipUCoordinator;
import id.icapps.savera.devices.huami.amazfitbipupro.AmazfitBipUProCoordinator;
import id.icapps.savera.devices.huami.amazfitcheetahpro.AmazfitCheetahProCoordinator;
import id.icapps.savera.devices.huami.amazfitcheetahround.AmazfitCheetahRoundCoordinator;
import id.icapps.savera.devices.huami.amazfitcheetahsquare.AmazfitCheetahSquareCoordinator;
import id.icapps.savera.devices.huami.amazfitcor.AmazfitCorCoordinator;
import id.icapps.savera.devices.huami.amazfitcor2.AmazfitCor2Coordinator;
import id.icapps.savera.devices.huami.amazfitfalcon.AmazfitFalconCoordinator;
import id.icapps.savera.devices.huami.amazfitgtr.AmazfitGTRCoordinator;
import id.icapps.savera.devices.huami.amazfitgtr.AmazfitGTRLiteCoordinator;
import id.icapps.savera.devices.huami.amazfitgtr2.AmazfitGTR2Coordinator;
import id.icapps.savera.devices.huami.amazfitgtr2.AmazfitGTR2eCoordinator;
import id.icapps.savera.devices.huami.amazfitgtr3.AmazfitGTR3Coordinator;
import id.icapps.savera.devices.huami.amazfitgtr3pro.AmazfitGTR3ProCoordinator;
import id.icapps.savera.devices.huami.amazfitgtr4.AmazfitGTR4Coordinator;
import id.icapps.savera.devices.huami.amazfitgtrmini.AmazfitGTRMiniCoordinator;
import id.icapps.savera.devices.huami.amazfitgts.AmazfitGTSCoordinator;
import id.icapps.savera.devices.huami.amazfitgts2.AmazfitGTS2Coordinator;
import id.icapps.savera.devices.huami.amazfitgts2.AmazfitGTS2MiniCoordinator;
import id.icapps.savera.devices.huami.amazfitgts2.AmazfitGTS2eCoordinator;
import id.icapps.savera.devices.huami.amazfitgts3.AmazfitGTS3Coordinator;
import id.icapps.savera.devices.huami.amazfitgts4.AmazfitGTS4Coordinator;
import id.icapps.savera.devices.huami.amazfitgts4mini.AmazfitGTS4MiniCoordinator;
import id.icapps.savera.devices.huami.amazfitneo.AmazfitNeoCoordinator;
import id.icapps.savera.devices.huami.amazfitpop.AmazfitPopCoordinator;
import id.icapps.savera.devices.huami.amazfitpoppro.AmazfitPopProCoordinator;
import id.icapps.savera.devices.huami.amazfittrex.AmazfitTRexCoordinator;
import id.icapps.savera.devices.huami.amazfittrex2.AmazfitTRex2Coordinator;
import id.icapps.savera.devices.huami.amazfittrex3.AmazfitTRex3Coordinator;
import id.icapps.savera.devices.huami.amazfittrexpro.AmazfitTRexProCoordinator;
import id.icapps.savera.devices.huami.amazfittrexultra.AmazfitTRexUltraCoordinator;
import id.icapps.savera.devices.huami.amazfitvergel.AmazfitVergeLCoordinator;
import id.icapps.savera.devices.huami.amazfitx.AmazfitXCoordinator;
import id.icapps.savera.devices.huami.miband2.MiBand2Coordinator;
import id.icapps.savera.devices.huami.miband2.MiBand2HRXCoordinator;
import id.icapps.savera.devices.huami.miband3.MiBand3Coordinator;
import id.icapps.savera.devices.huami.miband4.MiBand4Coordinator;
import id.icapps.savera.devices.huami.miband5.MiBand5Coordinator;
import id.icapps.savera.devices.huami.miband6.MiBand6Coordinator;
import id.icapps.savera.devices.huami.miband7.MiBand7Coordinator;
import id.icapps.savera.devices.huami.zeppe.ZeppECoordinator;
import id.icapps.savera.devices.huawei.honorband3.HonorBand3Coordinator;
import id.icapps.savera.devices.huawei.honorband4.HonorBand4Coordinator;
import id.icapps.savera.devices.huawei.honorband5.HonorBand5Coordinator;
import id.icapps.savera.devices.huawei.honorband6.HonorBand6Coordinator;
import id.icapps.savera.devices.huawei.honorband7.HonorBand7Coordinator;
import id.icapps.savera.devices.huawei.honormagicwatch2.HonorMagicWatch2Coordinator;
import id.icapps.savera.devices.huawei.honorwatchgs3.HonorWatchGS3Coordinator;
import id.icapps.savera.devices.huawei.honorwatchgspro.HonorWatchGSProCoordinator;
import id.icapps.savera.devices.huawei.huaweiband4pro.HuaweiBand4ProCoordinator;
import id.icapps.savera.devices.huawei.huaweiband6.HuaweiBand6Coordinator;
import id.icapps.savera.devices.huawei.huaweiband7.HuaweiBand7Coordinator;
import id.icapps.savera.devices.huawei.huaweiband8.HuaweiBand8Coordinator;
import id.icapps.savera.devices.huawei.huaweiband9.HuaweiBand9Coordinator;
import id.icapps.savera.devices.huawei.huaweibandaw70.HuaweiBandAw70Coordinator;
import id.icapps.savera.devices.huawei.huaweitalkbandb6.HuaweiTalkBandB6Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatch3.HuaweiWatch3Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatch4pro.HuaweiWatch4ProCoordinator;
import id.icapps.savera.devices.huawei.huaweiwatchd2.HuaweiWatchD2Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatchfit.HuaweiWatchFitCoordinator;
import id.icapps.savera.devices.huawei.huaweiwatchfit2.HuaweiWatchFit2Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatchfit3.HuaweiWatchFit3Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatchgt.HuaweiWatchGTCoordinator;
import id.icapps.savera.devices.huawei.huaweiwatchgt2.HuaweiWatchGT2Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatchgt2e.HuaweiWatchGT2eCoordinator;
import id.icapps.savera.devices.huawei.huaweiwatchgt3.HuaweiWatchGT3Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatchgt4.HuaweiWatchGT4Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatchgt5.HuaweiWatchGT5Coordinator;
import id.icapps.savera.devices.huawei.huaweiwatchgtcyber.HuaweiWatchGTCyberCoordinator;
import id.icapps.savera.devices.huawei.huaweiwatchgtrunner.HuaweiWatchGTRunnerCoordinator;
import id.icapps.savera.devices.huawei.huaweiwatchultimate.HuaweiWatchUltimateCoordinator;
import id.icapps.savera.devices.id115.ID115Coordinator;
import id.icapps.savera.devices.itag.ITagCoordinator;
import id.icapps.savera.devices.idasen.IdasenCoordinator;
import id.icapps.savera.devices.jyou.BFH16DeviceCoordinator;
import id.icapps.savera.devices.jyou.TeclastH30.TeclastH30Coordinator;
import id.icapps.savera.devices.jyou.y5.Y5Coordinator;
import id.icapps.savera.devices.lefun.BohemicSmartBraceletDeviceCoordinator;
import id.icapps.savera.devices.lefun.LefunDeviceCoordinator;
import id.icapps.savera.devices.lefun.VivitarHrBpMonitorActivityTrackerCoordinator;
import id.icapps.savera.devices.lenovo.watchxplus.WatchXPlusDeviceCoordinator;
import id.icapps.savera.devices.liveview.LiveviewCoordinator;
import id.icapps.savera.devices.makibeshr3.MakibesHR3Coordinator;
import id.icapps.savera.devices.miband.MiBandCoordinator;
import id.icapps.savera.devices.mijia_lywsd.MijiaLywsd02Coordinator;
import id.icapps.savera.devices.mijia_lywsd.MijiaLywsd03Coordinator;
import id.icapps.savera.devices.mijia_lywsd.MijiaXmwsdj04Coordinator;
import id.icapps.savera.devices.mijia_lywsd.MijiaMhoC303Coordinator;
import id.icapps.savera.devices.miscale.MiSmartScaleCoordinator;
import id.icapps.savera.devices.miscale.MiCompositionScaleCoordinator;
import id.icapps.savera.devices.moondrop.MoondropSpaceTravelCoordinator;
import id.icapps.savera.devices.no1f1.No1F1Coordinator;
import id.icapps.savera.devices.nothing.CmfBudsPro2Coordinator;
import id.icapps.savera.devices.nothing.Ear1Coordinator;
import id.icapps.savera.devices.nothing.Ear2Coordinator;
import id.icapps.savera.devices.nothing.EarStickCoordinator;
import id.icapps.savera.devices.nut.NutCoordinator;
import id.icapps.savera.devices.pebble.PebbleCoordinator;
import id.icapps.savera.devices.pinetime.PineTimeJFCoordinator;
import id.icapps.savera.devices.qc35.QC35Coordinator;
import id.icapps.savera.devices.qhybrid.QHybridCoordinator;
import id.icapps.savera.devices.roidmi.Roidmi1Coordinator;
import id.icapps.savera.devices.roidmi.Roidmi3Coordinator;
import id.icapps.savera.devices.scannable.ScannableDeviceCoordinator;
import id.icapps.savera.devices.smaq2oss.SMAQ2OSSCoordinator;
import id.icapps.savera.devices.soflow.SoFlowCoordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyLinkBudsCoordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyLinkBudsSCoordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWF1000XM3Coordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWF1000XM4Coordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWF1000XM5Coordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWFC500Coordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWFC700NCoordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWFSP800NCoordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWH1000XM2Coordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWH1000XM3Coordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWH1000XM4Coordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWH1000XM5Coordinator;
import id.icapps.savera.devices.sony.headphones.coordinators.SonyWISP600NCoordinator;
import id.icapps.savera.devices.sony.wena3.SonyWena3Coordinator;
import id.icapps.savera.devices.sonyswr12.SonySWR12DeviceCoordinator;
import id.icapps.savera.devices.soundcore.liberty3_pro.SoundcoreLiberty3ProCoordinator;
import id.icapps.savera.devices.soundcore.liberty4_nc.SoundcoreLiberty4NCCoordinator;
import id.icapps.savera.devices.soundcore.motion300.SoundcoreMotion300Coordinator;
import id.icapps.savera.devices.supercars.SuperCarsCoordinator;
import id.icapps.savera.devices.test.TestDeviceCoordinator;
import id.icapps.savera.devices.tlw64.TLW64Coordinator;
import id.icapps.savera.devices.um25.Coordinator.UM25Coordinator;
import id.icapps.savera.devices.vesc.VescCoordinator;
import id.icapps.savera.devices.vibratissimo.VibratissimoCoordinator;
import id.icapps.savera.devices.waspos.WaspOSCoordinator;
import id.icapps.savera.devices.watch9.Watch9DeviceCoordinator;
import id.icapps.savera.devices.withingssteelhr.WithingsSteelHRDeviceCoordinator;
import id.icapps.savera.devices.xiaomi.miband7pro.MiBand7ProCoordinator;
import id.icapps.savera.devices.xiaomi.miband8.MiBand8Coordinator;
import id.icapps.savera.devices.xiaomi.miband8active.MiBand8ActiveCoordinator;
import id.icapps.savera.devices.xiaomi.miband8pro.MiBand8ProCoordinator;
import id.icapps.savera.devices.xiaomi.miband9.MiBand9Coordinator;
import id.icapps.savera.devices.xiaomi.miband10.MiBand10Coordinator;
import id.icapps.savera.devices.xiaomi.miwatch.MiWatchLiteCoordinator;
import id.icapps.savera.devices.xiaomi.miwatchcolorsport.MiWatchColorSportCoordinator;
import id.icapps.savera.devices.xiaomi.redmismartband2.RedmiSmartBand2Coordinator;
import id.icapps.savera.devices.xiaomi.redmismartbandpro.RedmiSmartBandProCoordinator;
import id.icapps.savera.devices.xiaomi.redmiwatch2.RedmiWatch2Coordinator;
import id.icapps.savera.devices.xiaomi.redmiwatch2lite.RedmiWatch2LiteCoordinator;
import id.icapps.savera.devices.xiaomi.redmiwatch3.RedmiWatch3Coordinator;
import id.icapps.savera.devices.xiaomi.redmiwatch3active.RedmiWatch3ActiveCoordinator;
import id.icapps.savera.devices.xiaomi.redmiwatch4.RedmiWatch4Coordinator;
import id.icapps.savera.devices.xiaomi.redmiwatch5active.RedmiWatch5ActiveCoordinator;
import id.icapps.savera.devices.xiaomi.watchs1.XiaomiWatchS1Coordinator;
import id.icapps.savera.devices.xiaomi.watchs1active.XiaomiWatchS1ActiveCoordinator;
import id.icapps.savera.devices.xiaomi.watchs1pro.XiaomiWatchS1ProCoordinator;
import id.icapps.savera.devices.xiaomi.watchs3.XiaomiWatchS3Coordinator;
import id.icapps.savera.devices.xwatch.XWatchCoordinator;
import id.icapps.savera.devices.zetime.ZeTimeCoordinator;
import id.icapps.savera.service.devices.gatt_client.BleGattClientCoordinator;

/**
 * For every supported device, a device type constant must exist.
 * <p>
 * Note: they name of the enum is stored in the DB, so it is fixed forever,
 * and may not be changed.
 * <p>
 * Migration note: As of <a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/3347">#3347</a>,
 * the numeric device id is not used anymore. If your database has development devices that still used
 * the numeric ID, you need to update assets/migrations/devicetype.json before installing Gadgetbridge
 * after rebasing, in order for your device to be migrated correctly. If you failed to do this and the
 * device is now not being displayed, please update the file and uncomment the call to migrateDeviceTypes
 * in GBApplication.
 */
public enum DeviceType {
    UNKNOWN(UnknownDeviceCoordinator.class),
    PEBBLE(PebbleCoordinator.class),
    MIBAND(MiBandCoordinator.class),
    MIBAND2(MiBand2Coordinator.class),
    MIBAND2_HRX(MiBand2HRXCoordinator.class),
    AMAZFITBIP(AmazfitBipCoordinator.class),
    AMAZFITCOR(AmazfitCorCoordinator.class),
    MIBAND3(MiBand3Coordinator.class),
    AMAZFITCOR2(AmazfitCor2Coordinator.class),
    MIBAND4(MiBand4Coordinator.class),
    AMAZFITBIP_LITE(AmazfitBipLiteCoordinator.class),
    AMAZFITGTR(AmazfitGTRCoordinator.class),
    AMAZFITGTS(AmazfitGTSCoordinator.class),
    AMAZFITBIPS(AmazfitBipSCoordinator.class),
    AMAZFITGTR_LITE(AmazfitGTRLiteCoordinator.class),
    AMAZFITTREX(AmazfitTRexCoordinator.class),
    MIBAND5(MiBand5Coordinator.class),
    AMAZFITBAND5(AmazfitBand5Coordinator.class),
    AMAZFITBIPS_LITE(AmazfitBipSLiteCoordinator.class),
    AMAZFITGTR2(AmazfitGTR2Coordinator.class),
    AMAZFITGTS2(AmazfitGTS2Coordinator.class),
    AMAZFITBIPU(AmazfitBipUCoordinator.class),
    AMAZFITVERGEL(AmazfitVergeLCoordinator.class),
    AMAZFITBIPUPRO(AmazfitBipUProCoordinator.class),
    AMAZFITNEO(AmazfitNeoCoordinator.class),
    AMAZFITGTS2_MINI(AmazfitGTS2MiniCoordinator.class),
    ZEPP_E(ZeppECoordinator.class),
    AMAZFITGTR2E(AmazfitGTR2eCoordinator.class),
    AMAZFITGTS2E(AmazfitGTS2eCoordinator.class),
    AMAZFITX(AmazfitXCoordinator.class),
    MIBAND6(MiBand6Coordinator.class),
    AMAZFITTREXPRO(AmazfitTRexProCoordinator.class),
    AMAZFITPOP(AmazfitPopCoordinator.class),
    AMAZFITPOPPRO(AmazfitPopProCoordinator.class),
    MIBAND7(MiBand7Coordinator.class),
    MIBAND7PRO(MiBand7ProCoordinator.class),
    MIBAND8(MiBand8Coordinator.class),
    MIBAND8ACTIVE(MiBand8ActiveCoordinator.class),
    MIBAND8PRO(MiBand8ProCoordinator.class),
    MIBAND9(MiBand9Coordinator.class),
    MIBAND10(MiBand10Coordinator.class),
    MIWATCHLITE(MiWatchLiteCoordinator.class),
    MIWATCHCOLORSPORT(MiWatchColorSportCoordinator.class),
    REDMIWATCH3ACTIVE(RedmiWatch3ActiveCoordinator.class),
    REDMIWATCH3(RedmiWatch3Coordinator.class),
    REDMISMARTBAND2(RedmiSmartBand2Coordinator.class),
    REDMIWATCH2(RedmiWatch2Coordinator.class),
    REDMIWATCH2LITE(RedmiWatch2LiteCoordinator.class),
    REDMISMARTBANDPRO(RedmiSmartBandProCoordinator.class),
    REDMIWATCH4(RedmiWatch4Coordinator.class),
    REDMIWATCH5ACTIVE(RedmiWatch5ActiveCoordinator.class),
    XIAOMI_WATCH_S1_ACTIVE(XiaomiWatchS1ActiveCoordinator.class),
    XIAOMI_WATCH_S1_PRO(XiaomiWatchS1ProCoordinator.class),
    XIAOMI_WATCH_S1(XiaomiWatchS1Coordinator.class),
    XIAOMI_WATCH_S3(XiaomiWatchS3Coordinator.class),
    AMAZFITGTS3(AmazfitGTS3Coordinator.class),
    AMAZFITGTR3(AmazfitGTR3Coordinator.class),
    AMAZFITGTR4(AmazfitGTR4Coordinator.class),
    AMAZFITBAND7(AmazfitBand7Coordinator.class),
    AMAZFITGTS4(AmazfitGTS4Coordinator.class),
    AMAZFITGTS4MINI(AmazfitGTS4MiniCoordinator.class),
    AMAZFITTREX2(AmazfitTRex2Coordinator.class),
    AMAZFITTREX3(AmazfitTRex3Coordinator.class),
    AMAZFITGTR3PRO(AmazfitGTR3ProCoordinator.class),
    AMAZFITBIP3(AmazfitBip3Coordinator.class),
    AMAZFITBIP3PRO(AmazfitBip3ProCoordinator.class),
    AMAZFITCHEETAHPRO(AmazfitCheetahProCoordinator.class),
    AMAZFITCHEETAHSQUARE(AmazfitCheetahSquareCoordinator.class),
    AMAZFITCHEETAHROUND(AmazfitCheetahRoundCoordinator.class),
    AMAZFITBIP5(AmazfitBip5Coordinator.class),
    AMAZFITBIP5UNITY(AmazfitBip5UnityCoordinator.class),
    AMAZFITTREXULTRA(AmazfitTRexUltraCoordinator.class),
    AMAZFITGTRMINI(AmazfitGTRMiniCoordinator.class),
    AMAZFITFALCON(AmazfitFalconCoordinator.class),
    AMAZFITBALANCE(AmazfitBalanceCoordinator.class),
    AMAZFITACTIVE(AmazfitActiveCoordinator.class),
    AMAZFITACTIVEEDGE(AmazfitActiveEdgeCoordinator.class),
    HPLUS(HPlusCoordinator.class),
    MAKIBESF68(MakibesF68Coordinator.class),
    EXRIZUK8(EXRIZUK8Coordinator.class),
    Q8(Q8Coordinator.class),
    SG2(SG2Coordinator.class),
    NO1F1(No1F1Coordinator.class),
    TECLASTH30(TeclastH30Coordinator.class),
    Y5(Y5Coordinator.class),
    XWATCH(XWatchCoordinator.class),
    ZETIME(ZeTimeCoordinator.class),
    ID115(ID115Coordinator.class),
    WATCH9(Watch9DeviceCoordinator.class),
    WATCHXPLUS(WatchXPlusDeviceCoordinator.class),
    ROIDMI(Roidmi1Coordinator.class),
    ROIDMI3(Roidmi3Coordinator.class),
    CASIOGB6900(CasioGB6900DeviceCoordinator.class),
    CASIOGBX100(CasioGBX100DeviceCoordinator.class),
    CASIOGWB5600(CasioGWB5600DeviceCoordinator.class),
    CASIOGMWB5000(CasioGMWB5000DeviceCoordinator.class),
    MISMARTSCALE(MiSmartScaleCoordinator.class),
    MICOMPOSITIONSCALE(MiCompositionScaleCoordinator.class),
    BFH16(BFH16DeviceCoordinator.class),
    MAKIBESHR3(MakibesHR3Coordinator.class),
    BANGLEJS(BangleJSCoordinator.class),
    FOSSILQHYBRID(QHybridCoordinator.class),
    TLW64(TLW64Coordinator.class),
    PINETIME_JF(PineTimeJFCoordinator.class),
    MIJIA_LYWSD02(MijiaLywsd02Coordinator.class),
    MIJIA_LYWSD03(MijiaLywsd03Coordinator.class),
    MIJIA_XMWSDJ04(MijiaXmwsdj04Coordinator.class),
    MIJIA_MHO_C303(MijiaMhoC303Coordinator.class),
    LEFUN(LefunDeviceCoordinator.class),
    VIVITAR_HR_BP_MONITOR_ACTIVITY_TRACKER(VivitarHrBpMonitorActivityTrackerCoordinator.class),
    BOHEMIC_SMART_BRACELET(BohemicSmartBraceletDeviceCoordinator.class),
    SMAQ2OSS(SMAQ2OSSCoordinator.class),
    FITPRO(FitProDeviceCoordinator.class),
    COLACAO21(ColaCao21Coordinator.class),
    COLACAO23(ColaCao23Coordinator.class),
    ITAG(ITagCoordinator.class),
    IKEA_IDASEN(IdasenCoordinator.class),
    NUTMINI(NutCoordinator.class),
    VIVOMOVE_HR(GarminVivomoveHrCoordinator.class),
    GARMIN_ENDURO_3(GarminEnduro3Coordinator.class),
    GARMIN_EPIX_PRO(GarminEpixProCoordinator.class),
    GARMIN_FENIX_5(GarminFenix5Coordinator.class),
    GARMIN_FENIX_5_PLUS(GarminFenix5PlusCoordinator.class),
    GARMIN_FENIX_5X_PLUS(GarminFenix5XPlusCoordinator.class),
    GARMIN_FENIX_6(GarminFenix6Coordinator.class),
    GARMIN_FENIX_6_SAPPHIRE(GarminFenix6SapphireCoordinator.class),
    GARMIN_FENIX_6S_SAPPHIRE(GarminFenix6SSapphireCoordinator.class),
    GARMIN_FENIX_7S(GarminFenix7SCoordinator.class),
    GARMIN_FENIX_7_PRO(GarminFenix7ProCoordinator.class),
    GARMIN_FENIX_8(GarminFenix8Coordinator.class),
    GARMIN_FORERUNNER_165(GarminForerunner165Coordinator.class),
    GARMIN_FORERUNNER_245(GarminForerunner245Coordinator.class),
    GARMIN_FORERUNNER_245_MUSIC(GarminForerunner245MusicCoordinator.class),
    GARMIN_FORERUNNER_255(GarminForerunner255Coordinator.class),
    GARMIN_FORERUNNER_255_MUSIC(GarminForerunner255MusicCoordinator.class),
    GARMIN_FORERUNNER_255S(GarminForerunner255SCoordinator.class),
    GARMIN_FORERUNNER_255S_MUSIC(GarminForerunner255SMusicCoordinator.class),
    GARMIN_FORERUNNER_265(GarminForerunner265Coordinator.class),
    GARMIN_FORERUNNER_265S(GarminForerunner265SCoordinator.class),
    GARMIN_FORERUNNER_955(GarminForerunner955Coordinator.class),
    GARMIN_FORERUNNER_965(GarminForerunner965Coordinator.class),
    GARMIN_SWIM_2(GarminSwim2Coordinator.class),
    GARMIN_INSTINCT(GarminInstinctCoordinator.class),
    GARMIN_INSTINCT_SOLAR(GarminInstinctSolarCoordinator.class),
    GARMIN_INSTINCT_2S(GarminInstinct2SCoordinator.class),
    GARMIN_INSTINCT_2S_SOLAR(GarminInstinct2SSolarCoordinator.class),
    GARMIN_INSTINCT_2X_SOLAR(GarminInstinct2XSolarCoordinator.class),
    GARMIN_INSTINCT_2_SOLAR(GarminInstinct2SolarCoordinator.class),
    GARMIN_INSTINCT_2_SOLTAC(GarminInstinct2SolTacCoordinator.class),
    GARMIN_INSTINCT_CROSSOVER(GarminInstinctCrossoverCoordinator.class),
    GARMIN_VIVOMOVE_STYLE(GarminVivomoveStyleCoordinator.class),
    GARMIN_VIVOMOVE_TREND(GarminVivomoveTrendCoordinator.class),
    GARMIN_VENU(GarminVenuCoordinator.class),
    GARMIN_VENU_SQ(GarminVenuSqCoordinator.class),
    GARMIN_VENU_SQ_2(GarminVenuSq2Coordinator.class),
    GARMIN_VENU_2(GarminVenu2Coordinator.class),
    GARMIN_VENU_2S(GarminVenu2SCoordinator.class),
    GARMIN_VENU_2_PLUS(GarminVenu2PlusCoordinator.class),
    GARMIN_VENU_3(GarminVenu3Coordinator.class),
    GARMIN_VENU_3S(GarminVenu3SCoordinator.class),
    GARMIN_VIVOACTIVE_3(GarminVivoActive3Coordinator.class),
    GARMIN_VIVOACTIVE_4(GarminVivoActive4Coordinator.class),
    GARMIN_VIVOACTIVE_4S(GarminVivoActive4SCoordinator.class),
    GARMIN_VIVOACTIVE_5(GarminVivoActive5Coordinator.class),
    GARMIN_VIVOSMART_5(GarminVivosmart5Coordinator.class),
    GARMIN_VIVOSPORT(GarminVivosportCoordinator.class),
    VIBRATISSIMO(VibratissimoCoordinator.class),
    SONY_SWR12(SonySWR12DeviceCoordinator.class),
    LIVEVIEW(LiveviewCoordinator.class),
    WASPOS(WaspOSCoordinator.class),
    UM25(UM25Coordinator.class),
    DOMYOS_T540(DomyosT540Coordinator.class),
    NOTHING_EAR1(Ear1Coordinator.class),
    NOTHING_EAR2(Ear2Coordinator.class),
    NOTHING_EAR_STICK(EarStickCoordinator.class),
    NOTHING_CMF_BUDS_PRO_2(CmfBudsPro2Coordinator.class),
    NOTHING_CMF_WATCH_PRO(CmfWatchProCoordinator.class),
    NOTHING_CMF_WATCH_PRO_2(CmfWatchPro2Coordinator.class),
    GALAXY_BUDS_PRO(GalaxyBudsProDeviceCoordinator.class),
    GALAXY_BUDS_LIVE(GalaxyBudsLiveDeviceCoordinator.class),
    GALAXY_BUDS(GalaxyBudsDeviceCoordinator.class),
    GALAXY_BUDS2(GalaxyBuds2DeviceCoordinator.class),
    GALAXY_BUDS2_PRO(GalaxyBuds2ProDeviceCoordinator.class),
    SONY_WH_1000XM3(SonyWH1000XM3Coordinator.class),
    SONY_WF_SP800N(SonyWFSP800NCoordinator.class),
    SONY_WI_SP600N(SonyWISP600NCoordinator.class),
    SONY_WH_1000XM4(SonyWH1000XM4Coordinator.class),
    SONY_WF_1000XM3(SonyWF1000XM3Coordinator.class),
    SONY_WH_1000XM2(SonyWH1000XM2Coordinator.class),
    SONY_WF_1000XM4(SonyWF1000XM4Coordinator.class),
    SONY_LINKBUDS(SonyLinkBudsCoordinator.class),
    SONY_LINKBUDS_S(SonyLinkBudsSCoordinator.class),
    SONY_WH_1000XM5(SonyWH1000XM5Coordinator.class),
    SONY_WF_1000XM5(SonyWF1000XM5Coordinator.class),
    SONY_WF_C500(SonyWFC500Coordinator.class),
    SONY_WF_C700N(SonyWFC700NCoordinator.class),
    SOUNDCORE_LIBERTY3_PRO(SoundcoreLiberty3ProCoordinator.class),
    SOUNDCORE_LIBERTY4_NC(SoundcoreLiberty4NCCoordinator.class),
    SOUNDCORE_MOTION300(SoundcoreMotion300Coordinator.class),
    MOONDROP_SPACE_TRAVEL(MoondropSpaceTravelCoordinator.class),
    BOSE_QC35(QC35Coordinator.class),
    HONORBAND3(HonorBand3Coordinator.class),
    HONORBAND4(HonorBand4Coordinator.class),
    HONORBAND5(HonorBand5Coordinator.class),
    HUAWEIBANDAW70(HuaweiBandAw70Coordinator.class),
    HUAWEIBAND6(HuaweiBand6Coordinator.class),
    HUAWEIWATCHGT(HuaweiWatchGTCoordinator.class),
    HUAWEIBAND4PRO(HuaweiBand4ProCoordinator.class),
    HUAWEIWATCHGT2(HuaweiWatchGT2Coordinator.class),
    HUAWEIWATCHGT2E(HuaweiWatchGT2eCoordinator.class),
    HUAWEITALKBANDB6(HuaweiTalkBandB6Coordinator.class),
    HUAWEIBAND7(HuaweiBand7Coordinator.class),
    HONORBAND6(HonorBand6Coordinator.class),
    HONORBAND7(HonorBand7Coordinator.class),
    HONORMAGICWATCH2(HonorMagicWatch2Coordinator.class),
    HONORWATCHGS3(HonorWatchGS3Coordinator.class),
    HONORWATCHGSPRO(HonorWatchGSProCoordinator.class),
    HUAWEIWATCHD2(HuaweiWatchD2Coordinator.class),
    HUAWEIWATCHGT3(HuaweiWatchGT3Coordinator.class),
    HUAWEIWATCHGT4(HuaweiWatchGT4Coordinator.class),
    HUAWEIWATCHGT5(HuaweiWatchGT5Coordinator.class),
    HUAWEIWATCHGTRUNNER(HuaweiWatchGTRunnerCoordinator.class),
    HUAWEIWATCHGTCYBER(HuaweiWatchGTCyberCoordinator.class),
    HUAWEIBAND8(HuaweiBand8Coordinator.class),
    HUAWEIBAND9(HuaweiBand9Coordinator.class),
    HUAWEIWATCHFIT(HuaweiWatchFitCoordinator.class),
    HUAWEIWATCHFIT2(HuaweiWatchFit2Coordinator.class),
    HUAWEIWATCHFIT3(HuaweiWatchFit3Coordinator.class),
    HUAWEIWATCHULTIMATE(HuaweiWatchUltimateCoordinator.class),
    HUAWEIWATCH3(HuaweiWatch3Coordinator.class),
    HUAWEIWATCH4PRO(HuaweiWatch4ProCoordinator.class),
    VESC(VescCoordinator.class),
    BINARY_SENSOR(BinarySensorCoordinator.class),
    FLIPPER_ZERO(FlipperZeroCoordinator.class),
    SUPER_CARS(SuperCarsCoordinator.class),
    ASTEROIDOS(AsteroidOSDeviceCoordinator.class),
    SOFLOW_SO6(SoFlowCoordinator.class),
    WITHINGS_STEEL_HR(WithingsSteelHRDeviceCoordinator.class),
    SONY_WENA_3(SonyWena3Coordinator.class),
    FEMOMETER_VINCA2(FemometerVinca2DeviceCoordinator.class),
    PIXOO(PixooCoordinator.class),
    HAMA_FIT6900(HamaFit6900DeviceCoordinator.class),
    COLMI_R02(ColmiR02Coordinator.class),
    COLMI_R03(ColmiR03Coordinator.class),
    COLMI_R06(ColmiR06Coordinator.class),
    COLMI_R10(ColmiR10Coordinator.class),
    SCANNABLE(ScannableDeviceCoordinator.class),
    CYCLING_SENSOR(CyclingSensorCoordinator.class),
    BLE_GATT_CLIENT(BleGattClientCoordinator.class),
    TEST(TestDeviceCoordinator.class);

    private DeviceCoordinator coordinator;

    private Class<? extends DeviceCoordinator> coordinatorClass;

    DeviceType(Class<? extends DeviceCoordinator> coordinatorClass) {
        this.coordinatorClass = coordinatorClass;
    }

    public boolean isSupported() {
        return this != UNKNOWN;
    }

    public static DeviceType fromName(String name) {
        for (DeviceType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return DeviceType.UNKNOWN;
    }

    public DeviceCoordinator getDeviceCoordinator() {
        if (coordinator == null) {
            try {
                coordinator = coordinatorClass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return coordinator;
    }
}
