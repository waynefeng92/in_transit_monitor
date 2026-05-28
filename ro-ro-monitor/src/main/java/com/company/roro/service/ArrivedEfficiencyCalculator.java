package com.company.roro.service;

import com.company.roro.entity.RouteOtdConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 已到达车辆效率计算器
 *
 * 负责计算：效率值（小时）、效率分桶（EFFICIENT / NORMAL / DELAYED）
 *
 * 效率分桶规则：
 * - EFFICIENT: 实际耗时 ≤ 预警时效（cumulativeOTD * warnRatio）
 * - NORMAL:    预警时效 < 实际耗时 ≤ 标准OTD时效
 * - DELAYED:   实际耗时 > 标准OTD时效
 *
 * 参考：StatusCalculator.calculateOverallMonitorStatus 的累积OTD计算逻辑
 */
@Component
public class ArrivedEfficiencyCalculator {

    private static final String EFFICIENT = "EFFICIENT";
    private static final String NORMAL = "NORMAL";
    private static final String DELAYED = "DELAYED";

    // ==================== 7段顺序定义 ====================

    public static final String[] SEGMENT_ORDER = {
            "NOT_DEPARTED", "TO_PORT", "AT_PORT_WAIT_SHIP",
            "ON_SEA", "AT_DEST_WAIT_UNLOAD",
            "UNLOADED_WAIT_DISPATCH", "DISPATCHING"
    };

    // ==================== 三段映射 ====================

    public static String getSectionName(String segment) {
        if (segment == null) return null;
        switch (segment) {
            case "NOT_DEPARTED":
            case "TO_PORT":
            case "AT_PORT_WAIT_SHIP":
                return "前段";
            case "ON_SEA":
            case "AT_DEST_WAIT_UNLOAD":
                return "中段";
            case "UNLOADED_WAIT_DISPATCH":
            case "DISPATCHING":
                return "后段";
            default:
                return null;
        }
    }

    // ==================== 整体效率计算 ====================

    /**
     * 计算效率值（小时）
     * @param orderReleaseTime 订单释放时间
     * @param arriveShopTime   到店时间
     * @return 效率值（小时），如果任一时间为 null 则返回 null
     */
    public Double calculateEfficiency(LocalDateTime orderReleaseTime, LocalDateTime arriveShopTime) {
        if (orderReleaseTime == null || arriveShopTime == null) {
            return null;
        }
        return (double) Duration.between(orderReleaseTime, arriveShopTime).toHours();
    }

    /**
     * 确定整体效率分桶
     * @param orderReleaseTime 订单释放时间
     * @param arriveShopTime   到店时间
     * @param config           线路 OTD 配置（可为 null）
     * @param warnRatio        预警比率
     * @return EFFICIENT / NORMAL / DELAYED，如果无法判定则返回 null
     */
    public String determineBucket(LocalDateTime orderReleaseTime, LocalDateTime arriveShopTime,
                                   RouteOtdConfig config, double warnRatio) {
        if (orderReleaseTime == null || arriveShopTime == null || config == null) {
            return null;
        }
        long elapsed = Duration.between(orderReleaseTime, arriveShopTime).toHours();
        long totalOtd = getTotalOtd(config);
        long totalWarn = (long) Math.floor(totalOtd * warnRatio);

        if (elapsed <= totalWarn) return EFFICIENT;
        if (elapsed <= totalOtd) return NORMAL;
        return DELAYED;
    }

    // ==================== 单段效率计算 ====================

    /**
     * 确定单段效率分桶
     * @param startTime 段起始时间
     * @param endTime   段结束时间
     * @param otdHours  该段标准 OTD 时效（可为 null）
     * @param warnHours 该段预警时效（可为 null）
     * @return EFFICIENT / NORMAL / DELAYED，如果无法判定则返回 null
     */
    public String determineSegmentBucket(LocalDateTime startTime, LocalDateTime endTime,
                                          Integer otdHours, Integer warnHours) {
        if (startTime == null || endTime == null || otdHours == null) {
            return null;
        }
        long elapsed = Duration.between(startTime, endTime).toHours();
        if (warnHours != null && elapsed <= warnHours) return EFFICIENT;
        if (elapsed <= otdHours) return NORMAL;
        return DELAYED;
    }

    /**
     * 确定单段效率分桶（使用累计 OTD 和 warnRatio）
     * @param startTime      段起始时间
     * @param endTime        段结束时间
     * @param cumulativeOtd  该段累积 OTD（小时）
     * @param warnRatio      预警比率
     * @return EFFICIENT / NORMAL / DELAYED，如果无法判定则返回 null
     */
    public String determineSectionBucket(LocalDateTime startTime, LocalDateTime endTime,
                                          long cumulativeOtd, double warnRatio) {
        if (startTime == null || endTime == null || cumulativeOtd <= 0) {
            return null;
        }
        long elapsed = Duration.between(startTime, endTime).toHours();
        long cumulativeWarn = (long) Math.floor(cumulativeOtd * warnRatio);

        if (elapsed <= cumulativeWarn) return EFFICIENT;
        if (elapsed <= cumulativeOtd) return NORMAL;
        return DELAYED;
    }

    // ==================== OTD 累积计算（参考 StatusCalculator） ====================

    /**
     * 获取全链路总标准 OTD 时效
     */
    public long getTotalOtd(RouteOtdConfig config) {
        if (config == null) return 0;
        return (long) config.getNotDepartedOtd() + config.getToPortOtd()
                + config.getAtPortWaitOtd() + config.getOnSeaOtd()
                + config.getAtDestWaitOtd() + config.getUnloadWaitDispatchOtd()
                + config.getDispatchingOtd();
    }

    /**
     * 获取指定段的标准 OTD 时效
     */
    public Integer getSegmentOtd(RouteOtdConfig config, String segment) {
        if (config == null || segment == null) return null;
        switch (segment) {
            case "NOT_DEPARTED":             return config.getNotDepartedOtd();
            case "TO_PORT":                  return config.getToPortOtd();
            case "AT_PORT_WAIT_SHIP":        return config.getAtPortWaitOtd();
            case "ON_SEA":                   return config.getOnSeaOtd();
            case "AT_DEST_WAIT_UNLOAD":      return config.getAtDestWaitOtd();
            case "UNLOADED_WAIT_DISPATCH":   return config.getUnloadWaitDispatchOtd();
            case "DISPATCHING":              return config.getDispatchingOtd();
            default:                         return null;
        }
    }

    /**
     * 获取指定段的预警时效
     */
    public Integer getSegmentWarn(RouteOtdConfig config, String segment) {
        if (config == null || segment == null) return null;
        switch (segment) {
            case "NOT_DEPARTED":             return config.getNotDepartedWarn();
            case "TO_PORT":                  return config.getToPortWarn();
            case "AT_PORT_WAIT_SHIP":        return config.getAtPortWaitWarn();
            case "ON_SEA":                   return config.getOnSeaWarn();
            case "AT_DEST_WAIT_UNLOAD":      return config.getAtDestWaitWarn();
            case "UNLOADED_WAIT_DISPATCH":   return config.getUnloadWaitDispatchWarn();
            case "DISPATCHING":              return config.getDispatchingWarn();
            default:                         return null;
        }
    }

    /**
     * 获取三段累积 OTD 时效
     * <p>
     * 前段 = notDepartedOtd + toPortOtd + atPortWaitOtd<br>
     * 中段 = onSeaOtd + atDestWaitOtd<br>
     * 后段 = unloadWaitDispatchOtd + dispatchingOtd
     */
    public long getSectionCumulativeOtd(RouteOtdConfig config, String sectionName) {
        if (config == null || sectionName == null) return 0;
        switch (sectionName) {
            case "前段":
                return (long) config.getNotDepartedOtd() + config.getToPortOtd() + config.getAtPortWaitOtd();
            case "中段":
                return (long) config.getOnSeaOtd() + config.getAtDestWaitOtd();
            case "后段":
                return (long) config.getUnloadWaitDispatchOtd() + config.getDispatchingOtd();
            default:
                return 0;
        }
    }
}
