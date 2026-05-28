package com.company.roro.util;

import com.company.roro.entity.RouteOtdConfig;
import com.company.roro.entity.VehicleTransit;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusCalculatorTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 4, 29, 18, 0, 0);

    @Test
    void notDepartedShouldUseOrderReleaseTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");
        transit.setUpdatedAt(now.minusHours(1));

        RouteOtdConfig config = baseConfig();
        LocalDateTime orderReleaseTime = now.minusHours(49);

        String status = StatusCalculator.calculateMonitorStatus(transit, config, orderReleaseTime, now);

        assertEquals("OVERDUE", status);
    }

    @Test
    void toPortShouldUseDepartWarehouseTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("TO_PORT");
        transit.setDepartWarehouseTime(now.minusHours(13));

        String status = StatusCalculator.calculateMonitorStatus(transit, baseConfig(), now.minusHours(100), now);

        assertEquals("OVERDUE", status);
    }

    @Test
    void atPortWaitShipShouldUseArrivePortTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("AT_PORT_WAIT_SHIP");
        transit.setArrivePortTime(now.minusHours(49));

        String status = StatusCalculator.calculateMonitorStatus(transit, baseConfig(), now.minusHours(100), now);

        assertEquals("OVERDUE", status);
    }

    @Test
    void onSeaShouldUseShipDepartTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("ON_SEA");
        transit.setShipDepartTime(now.minusHours(49));

        String status = StatusCalculator.calculateMonitorStatus(transit, baseConfig(), now.minusHours(100), now);

        assertEquals("OVERDUE", status);
    }

    @Test
    void atDestWaitUnloadShouldUseShipArriveTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("AT_DEST_WAIT_UNLOAD");
        transit.setShipArriveTime(now.minusHours(13));

        String status = StatusCalculator.calculateMonitorStatus(transit, baseConfig(), now.minusHours(100), now);

        assertEquals("OVERDUE", status);
    }

    @Test
    void unloadedWaitDispatchShouldUseUnloadFinishTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("UNLOADED_WAIT_DISPATCH");
        transit.setUnloadFinishTime(now.minusHours(13));

        String status = StatusCalculator.calculateMonitorStatus(transit, baseConfig(), now.minusHours(100), now);

        assertEquals("OVERDUE", status);
    }

    @Test
    void dispatchingShouldUseDispatchTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("DISPATCHING");
        transit.setDispatchTime(now.minusHours(13));

        String status = StatusCalculator.calculateMonitorStatus(transit, baseConfig(), now.minusHours(100), now);

        assertEquals("OVERDUE", status);
    }

    @Test
    void arrivedShouldAlwaysBeNormal() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("ARRIVED");
        transit.setArriveShopTime(now.minusHours(100));

        String status = StatusCalculator.calculateMonitorStatus(transit, baseConfig(), now.minusHours(200), now);

        assertEquals("NORMAL", status);
    }

    // ==================== 整段监控状态测试 (RED phase — method does not exist yet) ====================

    @Test
    void notDeparted_underWarn() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        // cumulative OTD = 48, WARN threshold = 48 * 0.8 = 38.4 → floor(38.4) = 38
        // elapsed = 20h, 20 < 38 → NORMAL
        String status = StatusCalculator.calculateOverallMonitorStatus(
                transit, baseConfig(), now.minusHours(20), now, 0.8);

        assertEquals("NORMAL", status);
    }

    @Test
    void notDeparted_pastWarn_underOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        // cumulative OTD = 48, WARN threshold = floor(48 * 0.8) = 38
        // elapsed = 40h, 38 < 40 < 48 → WARN
        String status = StatusCalculator.calculateOverallMonitorStatus(
                transit, baseConfig(), now.minusHours(40), now, 0.8);

        assertEquals("WARN", status);
    }

    @Test
    void notDeparted_pastOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        // cumulative OTD = 48, elapsed = 50h, 50 > 48 → OVERDUE
        String status = StatusCalculator.calculateOverallMonitorStatus(
                transit, baseConfig(), now.minusHours(50), now, 0.8);

        assertEquals("OVERDUE", status);
    }

    @Test
    void toPort_cumulativeWarn() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("TO_PORT");

        // cumulative OTD = 48 (notDeparted) + 12 (toPort) = 60
        // WARN threshold = floor(60 * 0.8) = 48
        // elapsed = 50h, 48 < 50 < 60 → WARN
        String status = StatusCalculator.calculateOverallMonitorStatus(
                transit, baseConfig(), now.minusHours(50), now, 0.8);

        assertEquals("WARN", status);
    }

    @Test
    void onSea_cumulativeOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("ON_SEA");

        // cumulative OTD = 48 + 12 + 48 + 48 = 156
        // WARN threshold = floor(156 * 0.8) = 124
        // elapsed = 160h, 160 > 156 → OVERDUE
        String status = StatusCalculator.calculateOverallMonitorStatus(
                transit, baseConfig(), now.minusHours(160), now, 0.8);

        assertEquals("OVERDUE", status);
    }

    @Test
    void nullOtdConfig() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        String status = StatusCalculator.calculateOverallMonitorStatus(
                transit, null, now.minusHours(20), now, 0.8);

        assertEquals("NORMAL", status);
    }

    @Test
    void nullOrderReleaseTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        String status = StatusCalculator.calculateOverallMonitorStatus(
                transit, baseConfig(), null, now, 0.8);

        assertEquals("NORMAL", status);
    }

    // ==================== 三段监控状态测试 ====================

    // ---------- 前段 ----------

    @Test
    void section_notDeparted_underWarn() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        // section cumulative OTD = 48, WARN = floor(48 * 0.8) = 38
        // elapsed = 20h, 20 < 38 → NORMAL
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(20), now, 0.8);

        assertEquals("NORMAL", status);
    }

    @Test
    void section_notDeparted_pastWarn_underOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        // elapsed = 40h, 38 < 40 < 48 → WARN
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(40), now, 0.8);

        assertEquals("WARN", status);
    }

    @Test
    void section_notDeparted_pastOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        // elapsed = 50h, 50 > 48 → OVERDUE
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(50), now, 0.8);

        assertEquals("OVERDUE", status);
    }

    @Test
    void section_toPort_cumulativeWarn() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("TO_PORT");

        // section cumulative OTD = 48 + 12 = 60, WARN = floor(60 * 0.8) = 48
        // elapsed = 50h, 48 < 50 < 60 → WARN
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(50), now, 0.8);

        assertEquals("WARN", status);
    }

    @Test
    void section_atPortWaitShip_cumulativeOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("AT_PORT_WAIT_SHIP");

        // section cumulative OTD = 48 + 12 + 48 = 108, WARN = floor(108 * 0.8) = 86
        // elapsed = 110h, 110 > 108 → OVERDUE
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(110), now, 0.8);

        assertEquals("OVERDUE", status);
    }

    // ---------- 中段 ----------

    @Test
    void section_onSea_underWarn() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("ON_SEA");
        transit.setShipDepartTime(now.minusHours(30));

        // section cumulative OTD = 48, WARN = floor(48 * 0.8) = 38
        // elapsed = 30h, 30 < 38 → NORMAL
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(100), now, 0.8);

        assertEquals("NORMAL", status);
    }

    @Test
    void section_onSea_pastWarn() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("ON_SEA");
        transit.setShipDepartTime(now.minusHours(40));

        // elapsed = 40h, 38 < 40 < 48 → WARN
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(100), now, 0.8);

        assertEquals("WARN", status);
    }

    @Test
    void section_atDestWaitUnload_cumulativeOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("AT_DEST_WAIT_UNLOAD");
        transit.setShipDepartTime(now.minusHours(65));

        // section cumulative OTD = 48 + 12 = 60, WARN = floor(60 * 0.8) = 48
        // elapsed = 65h, 65 > 60 → OVERDUE
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(100), now, 0.8);

        assertEquals("OVERDUE", status);
    }

    // ---------- 后段 ----------

    @Test
    void section_unloadedWaitDispatch_underWarn() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("UNLOADED_WAIT_DISPATCH");
        transit.setUnloadFinishTime(now.minusHours(5));

        // section cumulative OTD = 12, WARN = floor(12 * 0.8) = 9
        // elapsed = 5h, 5 < 9 → NORMAL
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(100), now, 0.8);

        assertEquals("NORMAL", status);
    }

    @Test
    void section_dispatching_pastWarn_underOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("DISPATCHING");
        transit.setUnloadFinishTime(now.minusHours(20));

        // section cumulative OTD = 12 + 12 = 24, WARN = floor(24 * 0.8) = 19
        // elapsed = 20h, 19 < 20 < 24 → WARN
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(100), now, 0.8);

        assertEquals("WARN", status);
    }

    @Test
    void section_dispatching_pastOtd() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("DISPATCHING");
        transit.setUnloadFinishTime(now.minusHours(25));

        // elapsed = 25h, 25 > 24 → OVERDUE
        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(100), now, 0.8);

        assertEquals("OVERDUE", status);
    }

    // ---------- 边界与防御 ----------

    @Test
    void section_arrived_returnsNull() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("ARRIVED");

        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(20), now, 0.8);

        assertEquals(null, status);
    }

    @Test
    void section_nullOtdConfig() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, null, now.minusHours(20), now, 0.8);

        assertEquals("NORMAL", status);
    }

    @Test
    void section_nullTransit() {
        String status = StatusCalculator.calculateSectionMonitorStatus(
                null, baseConfig(), now.minusHours(20), now, 0.8);

        assertEquals("NORMAL", status);
    }

    @Test
    void section_nullOrderReleaseTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("NOT_DEPARTED");

        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), null, now, 0.8);

        assertEquals("NORMAL", status);
    }

    @Test
    void section_nullSectionStartTime() {
        VehicleTransit transit = new VehicleTransit();
        transit.setTransportStatus("ON_SEA");
        // shipDepartTime is null → getSectionStartTime returns null → NORMAL

        String status = StatusCalculator.calculateSectionMonitorStatus(
                transit, baseConfig(), now.minusHours(100), now, 0.8);

        assertEquals("NORMAL", status);
    }

    private RouteOtdConfig baseConfig() {
        RouteOtdConfig config = new RouteOtdConfig();
        config.setNotDepartedWarn(24);
        config.setNotDepartedOtd(48);
        config.setToPortWarn(10);
        config.setToPortOtd(12);
        config.setAtPortWaitWarn(40);
        config.setAtPortWaitOtd(48);
        config.setOnSeaWarn(40);
        config.setOnSeaOtd(48);
        config.setAtDestWaitWarn(10);
        config.setAtDestWaitOtd(12);
        config.setUnloadWaitDispatchWarn(10);
        config.setUnloadWaitDispatchOtd(12);
        config.setDispatchingWarn(10);
        config.setDispatchingOtd(12);
        return config;
    }
}
