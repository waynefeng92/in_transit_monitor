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
        Double efficiency = calculator.calculateEfficiency(now.minusHours(100), now);
        assertNotNull(efficiency);
        assertEquals(100.0, efficiency, 0.01);
    }

    @Test
    void calculateEfficiencyNullStart() {
        assertNull(calculator.calculateEfficiency(null, now));
    }

    @Test
    void calculateEfficiencyNullEnd() {
        assertNull(calculator.calculateEfficiency(now, null));
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
        // 80h elapsed, 108h cumulative OTD, warn=86.4h → EFFICIENT
        String bucket = calculator.determineSectionBucket(now.minusHours(80), now, 108, 0.8);
        assertEquals("EFFICIENT", bucket);
    }

    @Test
    void sectionBucketNormal() {
        // 100h elapsed, 108h cumulative, warn=86.4h → NORMAL
        String bucket = calculator.determineSectionBucket(now.minusHours(100), now, 108, 0.8);
        assertEquals("NORMAL", bucket);
    }

    @Test
    void sectionBucketDelayed() {
        // 120h elapsed, 108h cumulative → DELAYED
        String bucket = calculator.determineSectionBucket(now.minusHours(120), now, 108, 0.8);
        assertEquals("DELAYED", bucket);
    }

    // ==================== getTotalOtd ====================

    @Test
    void getTotalOtd() {
        RouteOtdConfig config = baseConfig();
        long total = calculator.getTotalOtd(config);
        assertEquals(192, total);
    }

    @Test
    void getTotalOtdNullConfig() {
        assertEquals(0, calculator.getTotalOtd(null));
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
        assertEquals(108, calculator.getSectionCumulativeOtd(config, "前段")); // 48+12+48
        assertEquals(60, calculator.getSectionCumulativeOtd(config, "中段"));  // 48+12
        assertEquals(24, calculator.getSectionCumulativeOtd(config, "后段")); // 12+12
        assertEquals(0, calculator.getSectionCumulativeOtd(config, "未知段"));
        assertEquals(0, calculator.getSectionCumulativeOtd(null, "前段"));
    }

    // ==================== 辅助方法 ====================

    private RouteOtdConfig baseConfig() {
        RouteOtdConfig config = new RouteOtdConfig();
        config.setNotDepartedOtd(48);
        config.setToPortOtd(12);
        config.setAtPortWaitOtd(48);
        config.setOnSeaOtd(48);
        config.setAtDestWaitOtd(12);
        config.setUnloadWaitDispatchOtd(12);
        config.setDispatchingOtd(12);
        return config;
    }
}
