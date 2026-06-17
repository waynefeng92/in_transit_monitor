package com.company.roro.util;

import com.company.roro.entity.RouteOtdConfig;
import com.company.roro.entity.VehicleTransit;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 状态计算工具类
 *
 * 8种在途状态：
 * 1. NOT_DEPARTED           - 未出库（订单已释放，车还在仓库）
 * 2. TO_PORT                - 集港在途（车已出库，正在运往港口）
 * 3. AT_PORT_WAIT_SHIP      - 已集港待装船（车已到港口，等待装船）
 * 4. ON_SEA                 - 水运在途（船舶航行中）
 * 5. AT_DEST_WAIT_UNLOAD    - 已到港待卸船（船已到港，等待卸车）
 * 6. UNLOADED_WAIT_DISPATCH - 已卸船待分拨（车已卸下，等待分拨）
 * 7. DISPATCHING            - 分拨在途（最后一公里配送中）
 * 8. ARRIVED                - 已到达（已交付经销商/4S店）
 */
public class StatusCalculator {

    /**
     * 根据时间节点推断在途状态
     *
     * 判断顺序：从后往前判断，因为后面的时间节点有值，说明前面的阶段已经完成
     *
     * @param transit 在途记录对象
     * @return 状态码
     */
    public static String calculateTransportStatus(VehicleTransit transit) {
        // 8. 已到店 → 已到达
        if (transit.getArriveShopTime() != null) {
            return "ARRIVED";
        }
        // 7. 已分拨 → 分拨在途
        if (transit.getDispatchTime() != null) {
            return "DISPATCHING";
        }
        // 6. 已卸船 → 已卸船待分拨
        if (transit.getUnloadFinishTime() != null) {
            return "UNLOADED_WAIT_DISPATCH";
        }
        // 5. 船已到港 → 已到港待卸船
        if (transit.getShipArriveTime() != null) {
            return "AT_DEST_WAIT_UNLOAD";
        }
        // 4. 船已离港 → 水运在途
        if (transit.getShipDepartTime() != null) {
            return "ON_SEA";
        }
        // 3. 已集港 → 已集港待装船
        if (transit.getArrivePortTime() != null) {
            return "AT_PORT_WAIT_SHIP";
        }
        // 2. 已出库但未集港 → 集港在途
        if (transit.getDepartWarehouseTime() != null) {
            return "TO_PORT";
        }
        // 1. 未出库 → 未出库
        return "NOT_DEPARTED";
    }

    /**
     * 计算监控状态（正常/预警/超期）
     *
     * @param transit 在途记录对象
     * @param otdConfig 该线路的 OTD 配置
     * @return 监控状态码：NORMAL / WARN / OVERDUE
     */
    public static String calculateMonitorStatus(VehicleTransit transit, RouteOtdConfig otdConfig,
                                                LocalDateTime orderReleaseTime) {
        return calculateMonitorStatus(transit, otdConfig, orderReleaseTime, LocalDateTime.now());
    }

    static String calculateMonitorStatus(VehicleTransit transit, RouteOtdConfig otdConfig,
                                         LocalDateTime orderReleaseTime, LocalDateTime now) {
        if (otdConfig == null) {
            return "NORMAL";
        }

        String transportStatus = transit.getTransportStatus();

        // 已到达状态不需要监控
        if ("ARRIVED".equals(transportStatus)) {
            return "NORMAL";
        }

        // 获取当前状态的开始时间
        LocalDateTime startTime = getStartTime(transit, transportStatus, orderReleaseTime);
        if (startTime == null) {
            return "NORMAL";
        }

        // 计算已经过了多少分钟
        long elapsedMinutes = Duration.between(startTime, now).toMinutes();

        // 获取该状态的标准OTD时效和预警时效（小时）
        Double otdHours = getOtdHours(otdConfig, transportStatus);
        Double warnHours = getWarnHours(otdConfig, transportStatus);

        // 判断监控状态（使用分钟精度）
        if (otdHours != null && elapsedMinutes > otdHours * 60) {
            return "OVERDUE";  // 超过标准OTD → 已超期
        } else if (warnHours != null && elapsedMinutes > warnHours * 60) {
            return "WARN";     // 超过预警时效 → 预警
        } else {
            return "NORMAL";   // 其他情况 → 正常
        }
    }

    /**
     * 计算整段监控状态（正常/预警/超期）
     * <p>
     * 与 calculateMonitorStatus 不同，此方法从订单释放时间开始计算整段累计时效，
     * 而非仅监控当前单段状态。到达则使用到店时间计算，否则使用当前时间。
     *
     * @param transit          在途记录对象
     * @param otdConfig        该线路的 OTD 配置
     * @param orderReleaseTime 订单释放时间
     * @param now              当前时间
     * @param warnRatio        预警比率（如 0.8 表示达到累计 OTD 的 80% 即触发预警）
     * @return 监控状态码：NORMAL / WARN / OVERDUE
     */
    public static String calculateOverallMonitorStatus(VehicleTransit transit, RouteOtdConfig otdConfig,
                                                       LocalDateTime orderReleaseTime, LocalDateTime now, double warnRatio) {
        if (otdConfig == null) {
            return "NORMAL";
        }

        if (orderReleaseTime == null) {
            return "NORMAL";
        }

        String transportStatus = transit.getTransportStatus();
        LocalDateTime endTime;
        long cumulativeOtd;

        if ("ARRIVED".equals(transportStatus)) {
            endTime = transit.getArriveShopTime();
            cumulativeOtd = getCumulativeOtd(otdConfig, "ARRIVED");
        } else {
            endTime = now;
            cumulativeOtd = getCumulativeOtd(otdConfig, transportStatus);
        }

        if (endTime == null) {
            return "NORMAL";
        }

        long elapsedMinutes = Duration.between(orderReleaseTime, endTime).toMinutes();
        long cumulativeWarn = (long) Math.floor(cumulativeOtd * warnRatio);

        if (elapsedMinutes > cumulativeOtd) {
            return "OVERDUE";
        } else if (elapsedMinutes > cumulativeWarn) {
            return "WARN";
        } else {
            return "NORMAL";
        }
    }

    private static long getCumulativeOtd(RouteOtdConfig config, String status) {
        double sum;
        switch (status) {
            case "NOT_DEPARTED":
                sum = nvl(config.getNotDepartedOtd());
                break;
            case "TO_PORT":
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd());
                break;
            case "AT_PORT_WAIT_SHIP":
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd()) + nvl(config.getAtPortWaitOtd());
                break;
            case "ON_SEA":
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd()) + nvl(config.getAtPortWaitOtd()) + nvl(config.getOnSeaOtd());
                break;
            case "AT_DEST_WAIT_UNLOAD":
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd()) + nvl(config.getAtPortWaitOtd()) + nvl(config.getOnSeaOtd())
                        + nvl(config.getAtDestWaitOtd());
                break;
            case "UNLOADED_WAIT_DISPATCH":
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd()) + nvl(config.getAtPortWaitOtd()) + nvl(config.getOnSeaOtd())
                        + nvl(config.getAtDestWaitOtd()) + nvl(config.getUnloadWaitDispatchOtd());
                break;
            case "DISPATCHING":
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd()) + nvl(config.getAtPortWaitOtd()) + nvl(config.getOnSeaOtd())
                        + nvl(config.getAtDestWaitOtd()) + nvl(config.getUnloadWaitDispatchOtd()) + nvl(config.getDispatchingOtd());
                break;
            default:
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd()) + nvl(config.getAtPortWaitOtd()) + nvl(config.getOnSeaOtd())
                        + nvl(config.getAtDestWaitOtd()) + nvl(config.getUnloadWaitDispatchOtd()) + nvl(config.getDispatchingOtd());
        }
        return (long)(sum * 60);
    }

    private static double nvl(Double val) {
        return val != null ? val : 0.0;
    }

    /**
     * 获取当前状态的开始时间
     */
    private static LocalDateTime getStartTime(VehicleTransit transit, String status, LocalDateTime orderReleaseTime) {
        switch (status) {
            case "NOT_DEPARTED":
                return orderReleaseTime;
            case "TO_PORT":
                return transit.getDepartWarehouseTime();
            case "AT_PORT_WAIT_SHIP":
                return transit.getArrivePortTime();
            case "ON_SEA":
                return transit.getShipDepartTime();
            case "AT_DEST_WAIT_UNLOAD":
                return transit.getShipArriveTime();
            case "UNLOADED_WAIT_DISPATCH":
                return transit.getUnloadFinishTime();
            case "DISPATCHING":
                return transit.getDispatchTime();
            default:
                return null;
        }
    }

    /**
     * 获取指定状态的标准 OTD 时效
     */
    private static Double getOtdHours(RouteOtdConfig config, String status) {
        switch (status) {
            case "NOT_DEPARTED":
                return config.getNotDepartedOtd();
            case "TO_PORT":
                return config.getToPortOtd();
            case "AT_PORT_WAIT_SHIP":
                return config.getAtPortWaitOtd();
            case "ON_SEA":
                return config.getOnSeaOtd();
            case "AT_DEST_WAIT_UNLOAD":
                return config.getAtDestWaitOtd();
            case "UNLOADED_WAIT_DISPATCH":
                return config.getUnloadWaitDispatchOtd();
            case "DISPATCHING":
                return config.getDispatchingOtd();
            default:
                return null;
        }
    }

    /**
     * 获取指定状态的预警时效
     */
    private static Double getWarnHours(RouteOtdConfig config, String status) {
        switch (status) {
            case "NOT_DEPARTED":
                return config.getNotDepartedWarn();
            case "TO_PORT":
                return config.getToPortWarn();
            case "AT_PORT_WAIT_SHIP":
                return config.getAtPortWaitWarn();
            case "ON_SEA":
                return config.getOnSeaWarn();
            case "AT_DEST_WAIT_UNLOAD":
                return config.getAtDestWaitWarn();
            case "UNLOADED_WAIT_DISPATCH":
                return config.getUnloadWaitDispatchWarn();
            case "DISPATCHING":
                return config.getDispatchingWarn();
            default:
                return null;
        }
    }

    // ==================== 三段监控 ====================

    /**
     * 计算三段监控状态（正常/预警/超期）
     * <p>
     * 将8种在途状态划分为三段：前段、中段、后段。
     * 每段从该段起点时间开始计算累计时效，与 calculateOverallMonitorStatus 相比，
     * 累积时效仅从当前段的第一个状态开始累加，而非从 NOT_DEPARTED 开始。
     *
     * @param transit          在途记录对象
     * @param otdConfig        该线路的 OTD 配置
     * @param orderReleaseTime 订单释放时间
     * @param now              当前时间
     * @param warnRatio        预警比率（如 0.8 表示达到累计 OTD 的 80% 即触发预警）
     * @return 监控状态码：NORMAL / WARN / OVERDUE；ARRIVED 时返回 null
     */
    public static String calculateSectionMonitorStatus(VehicleTransit transit, RouteOtdConfig otdConfig,
                                                       LocalDateTime orderReleaseTime, LocalDateTime now, double warnRatio) {
        if (otdConfig == null) {
            return "NORMAL";
        }

        if (transit == null || orderReleaseTime == null) {
            return "NORMAL";
        }

        String transportStatus = transit.getTransportStatus();

        // ARRIVED 不属于任何段，返回 null
        if ("ARRIVED".equals(transportStatus)) {
            return null;
        }

        if (transportStatus == null) {
            return "NORMAL";
        }

        String section = getSectionFromStatus(transportStatus);
        if (section == null) {
            return "NORMAL";
        }

        LocalDateTime startTime = getSectionStartTime(transit, section, orderReleaseTime);
        if (startTime == null) {
            return "NORMAL";
        }

        long cumulativeOtd = getSectionCumulativeOtd(otdConfig, transportStatus);

        long elapsedMinutes = Duration.between(startTime, now).toMinutes();
        long cumulativeWarn = (long) Math.floor(cumulativeOtd * warnRatio);

        if (elapsedMinutes > cumulativeOtd) {
            return "OVERDUE";
        } else if (elapsedMinutes > cumulativeWarn) {
            return "WARN";
        } else {
            return "NORMAL";
        }
    }

    /**
     * 根据在途状态获取所属段
     * <p>
     * 前段：NOT_DEPARTED, TO_PORT, AT_PORT_WAIT_SHIP<br>
     * 中段：ON_SEA, AT_DEST_WAIT_UNLOAD<br>
     * 后段：UNLOADED_WAIT_DISPATCH, DISPATCHING
     *
     * @param status 在途状态
     * @return 段名称（"前段"/"中段"/"后段"），或 null
     */
    private static String getSectionFromStatus(String status) {
        if (status == null) {
            return null;
        }
        switch (status) {
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

    /**
     * 获取段的起始时间
     * <p>
     * 前段从订单释放时间开始，中段从船舶离港时间开始，后段从卸船完成时间开始。
     *
     * @param transit          在途记录对象
     * @param section          段名称
     * @param orderReleaseTime 订单释放时间
     * @return 段的起始时间，或 null
     */
    private static LocalDateTime getSectionStartTime(VehicleTransit transit, String section,
                                                     LocalDateTime orderReleaseTime) {
        if (section == null) {
            return null;
        }
        switch (section) {
            case "前段":
                return orderReleaseTime;
            case "中段":
                return transit.getShipDepartTime();
            case "后段":
                return transit.getUnloadFinishTime();
            default:
                return null;
        }
    }

    /**
     * 获取段内相对累积 OTD（小时）
     * <p>
     * 与 {@link #getCumulativeOtd(RouteOtdConfig, String)} 不同，此方法从当前段的第一个状态
     * 开始累加，而非从 NOT_DEPARTED 开始。例如中段 ON_SEA 只累加 onSeaOtd，不包含前段的时效。
     *
     * @param config OTD 配置
     * @param status 当前在途状态
     * @return 段内累积 OTD 时效（小时）
     */
    private static long getSectionCumulativeOtd(RouteOtdConfig config, String status) {
        if (status == null) {
            return 0;
        }
        double sum;
        switch (status) {
            // 前段：从 NOT_DEPARTED 开始累加
            case "NOT_DEPARTED":
                sum = nvl(config.getNotDepartedOtd());
                break;
            case "TO_PORT":
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd());
                break;
            case "AT_PORT_WAIT_SHIP":
                sum = nvl(config.getNotDepartedOtd()) + nvl(config.getToPortOtd()) + nvl(config.getAtPortWaitOtd());
                break;
            // 中段：从 ON_SEA 开始累加
            case "ON_SEA":
                sum = nvl(config.getOnSeaOtd());
                break;
            case "AT_DEST_WAIT_UNLOAD":
                sum = nvl(config.getOnSeaOtd()) + nvl(config.getAtDestWaitOtd());
                break;
            // 后段：从 UNLOADED_WAIT_DISPATCH 开始累加
            case "UNLOADED_WAIT_DISPATCH":
                sum = nvl(config.getUnloadWaitDispatchOtd());
                break;
            case "DISPATCHING":
                sum = nvl(config.getUnloadWaitDispatchOtd()) + nvl(config.getDispatchingOtd());
                break;
            default:
                return 0;
        }
        return (long)(sum * 60);
    }
}
