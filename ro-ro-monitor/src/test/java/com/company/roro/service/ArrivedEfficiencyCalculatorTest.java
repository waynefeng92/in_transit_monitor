package com.company.roro.service;

import com.company.roro.entity.RouteOtdConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArrivedEfficiencyCalculatorTest {

    private final ArrivedEfficiencyCalculator calculator = new ArrivedEfficiencyCalculator();
    private final LocalDateTime now = LocalDateTime.of(2026, 4, 29, 18, 0, 0);

    // ==================== calculateEfficiency ====================

    @Test
    void calculateEfficiencyNormal() {
        // actual=100h, totalOtd=192h → efficiency=192/100*100=192%
        RouteOtdConfig config = baseConfig();
        Double efficiency = calculator.calculateEfficiency(now.minusHours(100), now, config);
        assertNotNull(efficiency);
        assertEquals(192.0, efficiency, 0.5);
    }

    @Test
    void calculateEfficiencyNullStart() {
        assertNull(calculator.calculateEfficiency(null, now, baseConfig()));
    }

    @Test
    void calculateEfficiencyNullEnd() {
        assertNull(calculator.calculateEfficiency(now, null, baseConfig()));
    }

    // ==================== determineBucket ====================

    @Test
    void bucketEfficient() {
        // 96h elapsed, 192h total OTD, warnRatio=0.8 → warn=153.6h
        // 96 <= 153.6 → EFFICIENT
        RouteOtdConfig config = baseConfig();
        String bucket = calculator.determineBucket(now.minusHours(96), now, config, 0.8);
        assertEquals("EFFICIENT", bucket);
    }

    @Test
    void bucketNormal() {
        // 172h elapsed, 192h total, warn=153.6h
        // 172 > 153.6 and 172 <= 192 → NORMAL
        RouteOtdConfig config = baseConfig();
        String bucket = calculator.determineBucket(now.minusHours(172), now, config, 0.8);
        assertEquals("NORMAL", bucket);
    }

    @Test
    void bucketDelayed() {
        // 230h elapsed, 192h total
        // 230 > 192 → DELAYED
        RouteOtdConfig config = baseConfig();
        String bucket = calculator.determineBucket(now.minusHours(230), now, config, 0.8);
        assertEquals("DELAYED", bucket);
    }

    @Test
    void bucketBoundaryAtWarn() {
        // Exactly at warn threshold: 153.6h → EFFICIENT (<=)
        RouteOtdConfig config = baseConfig();
        String bucket = calculator.determineBucket(now.minusHours(153), now, config, 0.8);
        // 153 <= 153.6 → EFFICIENT
        assertEquals("EFFICIENT", bucket);
    }

    @Test
    void bucketBoundaryAtOtd() {
        // Exactly at OTD: 192h → NORMAL (<=)
        RouteOtdConfig config = baseConfig();
        String bucket = calculator.determineBucket(now.minusHours(192), now, config, 0.8);
        assertEquals("NORMAL", bucket);
    }

    @Test
    void bucketNullParams() {
        assertNull(calculator.determineBucket(null, now, baseConfig(), 0.8));
        assertNull(calculator.determineBucket(now.minusHours(100), null, baseConfig(), 0.8));
        assertNull(calculator.determineBucket(now.minusHours(100), now, null, 0.8));
    }

    // ==================== determineSegmentBucket ====================

    @Test
    void segmentBucketEfficient() {
        // 10h elapsed, 48h OTD, 38.4h warn → EFFICIENT
        String bucket = calculator.determineSegmentBucket(now.minusHours(10), now, 48, 38);
        assertEquals("EFFICIENT", bucket);
    }

    @Test
    void segmentBucketNormal() {
        // 40h elapsed, 48h OTD, 38h warn → NORMAL
        String bucket = calculator.determineSegmentBucket(now.minusHours(40), now, 48, 38);
        assertEquals("NORMAL", bucket);
    }

    @Test
    void segmentBucketDelayed() {
        // 50h elapsed, 48h OTD → DELAYED
        String bucket = calculator.determineSegmentBucket(now.minusHours(50), now, 48, 38);
        assertEquals("DELAYED", bucket);
    }

    @Test
    void segmentBucketNullParams() {
        assertNull(calculator.determineSegmentBucket(null, now, 48, 38));
        assertNull(calculator.determineSegmentBucket(now.minusHours(10), null, 48, 38));
        assertNull(calculator.determineSegmentBucket(now.minusHours(10), now, null, 38));
    }

    // ==================== determineSectionBucket ====================

    @Test
    void sectionBucketEfficient() {
        // 80h=4800min elapsed, 108h=6480min cumulative OTD, warn=5184min → EFFICIENT
        String bucket = calculator.determineSectionBucket(now.minusHours(80), now, 6480, 0.8);
        assertEquals("EFFICIENT", bucket);
    }

    @Test
    void sectionBucketNormal() {
        // 100h=6000min elapsed, 108h=6480min cumulative, warn=5184min → NORMAL
        String bucket = calculator.determineSectionBucket(now.minusHours(100), now, 6480, 0.8);
        assertEquals("NORMAL", bucket);
    }

    @Test
    void sectionBucketDelayed() {
        // 120h=7200min elapsed, 108h=6480min cumulative → DELAYED
        String bucket = calculator.determineSectionBucket(now.minusHours(120), now, 6480, 0.8);
        assertEquals("DELAYED", bucket);
    }

    // ==================== getTotalOtd ====================

    @Test
    void getTotalOtd() {
        RouteOtdConfig config = baseConfig();
        Double total = calculator.getTotalOtd(config);
        assertEquals(192.0, total, 0.01);
    }

    @Test
    void getTotalOtdNullConfig() {
        assertEquals(0.0, calculator.getTotalOtd(null), 0.01);
    }

    // ==================== getSectionName ====================

    @Test
    void getSectionNameMapping() {
        assertEquals("前段", ArrivedEfficiencyCalculator.getSectionName("NOT_DEPARTED"));
        assertEquals("前段", ArrivedEfficiencyCalculator.getSectionName("TO_PORT"));
        assertEquals("前段", ArrivedEfficiencyCalculator.getSectionName("AT_PORT_WAIT_SHIP"));
        assertEquals("中段", ArrivedEfficiencyCalculator.getSectionName("ON_SEA"));
        assertEquals("中段", ArrivedEfficiencyCalculator.getSectionName("AT_DEST_WAIT_UNLOAD"));
        assertEquals("后段", ArrivedEfficiencyCalculator.getSectionName("UNLOADED_WAIT_DISPATCH"));
        assertEquals("后段", ArrivedEfficiencyCalculator.getSectionName("DISPATCHING"));
        assertNull(ArrivedEfficiencyCalculator.getSectionName("ARRIVED"));
        assertNull(ArrivedEfficiencyCalculator.getSectionName(null));
    }

    // ==================== getSectionCumulativeOtd ====================

    @Test
    void getSectionCumulativeOtd() {
        RouteOtdConfig config = baseConfig();
        assertEquals(6480, calculator.getSectionCumulativeOtd(config, "前段")); // 108h*60
        assertEquals(3600, calculator.getSectionCumulativeOtd(config, "中段")); // 60h*60
        assertEquals(1440, calculator.getSectionCumulativeOtd(config, "后段")); // 24h*60
        assertEquals(0, calculator.getSectionCumulativeOtd(config, "未知段"));
        assertEquals(0, calculator.getSectionCumulativeOtd(null, "前段"));
    }

    // ==================== 辅助方法 ====================

    private RouteOtdConfig baseConfig() {
        RouteOtdConfig config = new RouteOtdConfig();
        config.setNotDepartedOtd(48.0);
        config.setToPortOtd(12.0);
        config.setAtPortWaitOtd(48.0);
        config.setOnSeaOtd(48.0);
        config.setAtDestWaitOtd(12.0);
        config.setUnloadWaitDispatchOtd(12.0);
        config.setDispatchingOtd(12.0);
        return config;
    }
}
