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
     * 计算效率值（百分比）
     * @param orderReleaseTime 订单释放时间
     * @param arriveShopTime   到店时间
     * @param config           线路 OTD 配置
     * @return 效率值（百分比），如果任一参数无效则返回 null
     */
    public Double calculateEfficiency(LocalDateTime orderReleaseTime, LocalDateTime arriveShopTime, RouteOtdConfig config) {
        if (orderReleaseTime == null || arriveShopTime == null || config == null) {
            return null;
        }
        double actualHours = Duration.between(orderReleaseTime, arriveShopTime).toMinutes() / 60.0;
        if (actualHours <= 0) return null;
        double totalOtd = getTotalOtd(config);
        if (totalOtd <= 0) return null;
        return totalOtd / actualHours * 100.0;
    }

    /**
     * 计算 OTD 消耗比（实际耗时 / 标准OTD × 100）
     * > 100% = 超时，< 100% = 省时，更符合直觉
     */
    public Double calculateConsumptionRatio(LocalDateTime orderReleaseTime, LocalDateTime arriveShopTime, RouteOtdConfig config) {
        if (orderReleaseTime == null || arriveShopTime == null || config == null) {
            return null;
        }
        double actualHours = Duration.between(orderReleaseTime, arriveShopTime).toMinutes() / 60.0;
        if (actualHours <= 0) return null;
        double totalOtd = getTotalOtd(config);
        if (totalOtd <= 0) return null;
        return actualHours / totalOtd * 100.0;
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
        long elapsedMinutes = Duration.between(orderReleaseTime, arriveShopTime).toMinutes();
        long totalOtdMinutes = (long)(getTotalOtd(config) * 60);
        long totalWarnMinutes = (long) Math.floor(totalOtdMinutes * warnRatio);

        if (elapsedMinutes <= totalWarnMinutes) return EFFICIENT;
        if (elapsedMinutes <= totalOtdMinutes) return NORMAL;
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
        long elapsedMinutes = Duration.between(startTime, endTime).toMinutes();
        long otdMinutes = (long)(otdHours * 60);
        long warnMinutes = warnHours != null ? (long)(warnHours * 60) : otdMinutes;

        if (elapsedMinutes <= warnMinutes) return EFFICIENT;
        if (elapsedMinutes <= otdMinutes) return NORMAL;
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
        long elapsedMinutes = Duration.between(startTime, endTime).toMinutes();
        long cumulativeWarnMinutes = (long) Math.floor(cumulativeOtd * warnRatio);

        if (elapsedMinutes <= cumulativeWarnMinutes) return EFFICIENT;
        if (elapsedMinutes <= cumulativeOtd) return NORMAL;
        return DELAYED;
    }

    // ==================== OTD 累积计算（参考 StatusCalculator） ====================

    /**
     * 获取全链路总标准 OTD 时效
     */
    public Double getTotalOtd(RouteOtdConfig config) {
        if (config == null) return 0.0;
        double d1 = config.getNotDepartedOtd() != null ? config.getNotDepartedOtd() : 0.0;
        double d2 = config.getToPortOtd() != null ? config.getToPortOtd() : 0.0;
        double d3 = config.getAtPortWaitOtd() != null ? config.getAtPortWaitOtd() : 0.0;
        double d4 = config.getOnSeaOtd() != null ? config.getOnSeaOtd() : 0.0;
        double d5 = config.getAtDestWaitOtd() != null ? config.getAtDestWaitOtd() : 0.0;
        double d6 = config.getUnloadWaitDispatchOtd() != null ? config.getUnloadWaitDispatchOtd() : 0.0;
        double d7 = config.getDispatchingOtd() != null ? config.getDispatchingOtd() : 0.0;
        return d1 + d2 + d3 + d4 + d5 + d6 + d7;
    }

    /**
     * 获取指定段的标准 OTD 时效
     */
    public Integer getSegmentOtd(RouteOtdConfig config, String segment) {
        if (config == null || segment == null) return null;
        Double val;
        switch (segment) {
            case "NOT_DEPARTED":             val = config.getNotDepartedOtd(); break;
            case "TO_PORT":                  val = config.getToPortOtd(); break;
            case "AT_PORT_WAIT_SHIP":        val = config.getAtPortWaitOtd(); break;
            case "ON_SEA":                   val = config.getOnSeaOtd(); break;
            case "AT_DEST_WAIT_UNLOAD":      val = config.getAtDestWaitOtd(); break;
            case "UNLOADED_WAIT_DISPATCH":   val = config.getUnloadWaitDispatchOtd(); break;
            case "DISPATCHING":              val = config.getDispatchingOtd(); break;
            default:                         return null;
        }
        return val != null ? val.intValue() : null;
    }

    /**
     * 获取指定段的预警时效
     */
    public Integer getSegmentWarn(RouteOtdConfig config, String segment) {
        if (config == null || segment == null) return null;
        Double val;
        switch (segment) {
            case "NOT_DEPARTED":             val = config.getNotDepartedWarn(); break;
            case "TO_PORT":                  val = config.getToPortWarn(); break;
            case "AT_PORT_WAIT_SHIP":        val = config.getAtPortWaitWarn(); break;
            case "ON_SEA":                   val = config.getOnSeaWarn(); break;
            case "AT_DEST_WAIT_UNLOAD":      val = config.getAtDestWaitWarn(); break;
            case "UNLOADED_WAIT_DISPATCH":   val = config.getUnloadWaitDispatchWarn(); break;
            case "DISPATCHING":              val = config.getDispatchingWarn(); break;
            default:                         return null;
        }
        return val != null ? val.intValue() : null;
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
        Double result = 0.0;
        switch (sectionName) {
            case "前段":
                result = (config.getNotDepartedOtd() != null ? config.getNotDepartedOtd() : 0.0)
                       + (config.getToPortOtd() != null ? config.getToPortOtd() : 0.0)
                       + (config.getAtPortWaitOtd() != null ? config.getAtPortWaitOtd() : 0.0);
                break;
            case "中段":
                result = (config.getOnSeaOtd() != null ? config.getOnSeaOtd() : 0.0)
                       + (config.getAtDestWaitOtd() != null ? config.getAtDestWaitOtd() : 0.0);
                break;
            case "后段":
                result = (config.getUnloadWaitDispatchOtd() != null ? config.getUnloadWaitDispatchOtd() : 0.0)
                       + (config.getDispatchingOtd() != null ? config.getDispatchingOtd() : 0.0);
                break;
        }
        return (long)(result * 60);
    }
}
