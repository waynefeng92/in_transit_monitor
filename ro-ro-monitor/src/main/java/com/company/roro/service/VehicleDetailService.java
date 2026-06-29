package com.company.roro.service;

import com.company.roro.dto.VehicleDetailDTO;
import com.company.roro.dto.VehicleDetailDTO.SegmentDetail;
import com.company.roro.entity.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车辆详情服务 — 运输监控大屏右侧详情面板
 *
 * <p>根据 VIN 查询车辆运输详情，包含 7 段运输时效信息。
 * 每段独立计算回溯状态（NORMAL / WARN / OVERDUE / PENDING / N/A）。</p>
 *
 * <h3>段定义</h3>
 * <table>
 * <tr><th>段</th><th>状态码</th><th>开始时间</th><th>结束时间</th></tr>
 * <tr><td>1 未出库</td><td>NOT_DEPARTED</td><td>orderReleaseTime</td><td>departWarehouseTime</td></tr>
 * <tr><td>2 集港在途</td><td>TO_PORT</td><td>departWarehouseTime</td><td>arrivePortTime</td></tr>
 * <tr><td>3 已集港待装船</td><td>AT_PORT_WAIT_SHIP</td><td>arrivePortTime</td><td>shipDepartTime</td></tr>
 * <tr><td>4 水运在途</td><td>ON_SEA</td><td>shipDepartTime</td><td>shipArriveTime</td></tr>
 * <tr><td>5 已到港待卸船</td><td>AT_DEST_WAIT_UNLOAD</td><td>shipArriveTime</td><td>unloadFinishTime</td></tr>
 * <tr><td>6 已卸船待分拨</td><td>UNLOADED_WAIT_DISPATCH</td><td>unloadFinishTime</td><td>dispatchTime</td></tr>
 * <tr><td>7 分拨在途</td><td>DISPATCHING</td><td>dispatchTime</td><td>arriveShopTime</td></tr>
 * </table>
 */
@Service
@RequiredArgsConstructor
public class VehicleDetailService {

    private final OrderInfoService orderInfoService;
    private final VehicleTransitService vehicleTransitService;
    private final RouteOtdConfigService routeOtdConfigService;
    private final BrandDictService brandDictService;
    private final RouteDictService routeDictService;
    private final TransportStatusDictService transportStatusDictService;

    // package-private for test spy
    LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 根据 VIN 获取车辆运输详情
     *
     * @param vin 车架号
     * @return 车辆详情 DTO，VIN 不存在时返回 null
     */
    public VehicleDetailDTO getVehicleDetail(String vin) {
        // 1. 查询订单 — 取最新
        List<OrderInfo> orders = orderInfoService.lambdaQuery()
                .eq(OrderInfo::getVin, vin)
                .orderByDesc(OrderInfo::getOrderReleaseTime)
                .list();
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        OrderInfo order = orders.get(0);

        // 2. 查询在途记录
        VehicleTransit transit = vehicleTransitService.lambdaQuery()
                .eq(VehicleTransit::getOrderId, order.getId())
                .one();

        // 3. 查询品牌
        BrandDict brand = brandDictService.getById(order.getBrandId());

        // 4. 查询运输状态字典
        List<TransportStatusDict> statusDicts = transportStatusDictService.list();
        Map<String, String> statusNameMap = statusDicts != null
                ? statusDicts.stream().collect(Collectors.toMap(
                        TransportStatusDict::getStatusCode, TransportStatusDict::getStatusName, (a, b) -> a))
                : Map.of();

        // 5. 无在途记录 → 返回基本 DTO
        if (transit == null) {
            return buildNoTransitDto(order, brand);
        }

        // 6. 查询 OTD 配置
        RouteOtdConfig otdConfig = routeOtdConfigService.lambdaQuery()
                .eq(RouteOtdConfig::getRouteId, order.getRouteId())
                .eq(RouteOtdConfig::getIsActive, 1)
                .one();

        // 7. 查询线路
        String routeName = null;
        if (order.getRouteId() != null) {
            RouteDict route = routeDictService.getById(order.getRouteId());
            if (route != null) {
                routeName = route.getOriginCity() + "-" + route.getDestCity();
            }
        }

        // 8. 翻译在途状态
        String transportStatusName = statusNameMap.getOrDefault(
                transit.getTransportStatus(), transit.getTransportStatus());

        // 9. 构建 DTO
        VehicleDetailDTO dto = new VehicleDetailDTO();
        dto.setVin(vin);
        dto.setBrandName(brand != null ? brand.getBrandName() : null);
        dto.setOriginCity(order.getOriginCity());
        dto.setDestCity(order.getDestCity());
        dto.setRouteName(routeName);
        dto.setTransportStatus(transit.getTransportStatus());
        dto.setTransportStatusName(transportStatusName);
        dto.setOrderReleaseTime(order.getOrderReleaseTime());

        // 10. 构建 7 段
        dto.setSegments(buildSegments(order, transit, otdConfig));

        // 11. 整段汇总
        dto.setTotalStandardOtdHours(computeTotalStandardOtd(otdConfig));
        dto.setTotalActualHours(computeTotalActualHours(
                order.getOrderReleaseTime(), transit.getArriveShopTime()));

        return dto;
    }

    // ==================== 无在途记录 DTO ====================

    private VehicleDetailDTO buildNoTransitDto(OrderInfo order, BrandDict brand) {
        VehicleDetailDTO dto = new VehicleDetailDTO();
        dto.setVin(order.getVin());
        dto.setBrandName(brand != null ? brand.getBrandName() : null);
        dto.setOriginCity(order.getOriginCity());
        dto.setDestCity(order.getDestCity());
        dto.setTransportStatus("无在途记录");
        dto.setTransportStatusName("无在途记录");
        dto.setOrderReleaseTime(order.getOrderReleaseTime());
        dto.setTotalStandardOtdHours(null);
        dto.setTotalActualHours(null);
        dto.setSegments(List.of());
        return dto;
    }

    // ==================== 7 段构建 ====================

    private List<SegmentDetail> buildSegments(OrderInfo order, VehicleTransit transit,
                                               RouteOtdConfig otdConfig) {
        List<SegmentDetail> segments = new ArrayList<>();

        // Segment 1: NOT_DEPARTED — orderReleaseTime → departWarehouseTime
        segments.add(computeSegment(1, "未出库", "NOT_DEPARTED",
                order.getOrderReleaseTime(), transit.getDepartWarehouseTime(),
                otdConfig, otdField(otdConfig, 1), warnField(otdConfig, 1)));

        // Segment 2: TO_PORT — departWarehouseTime → arrivePortTime
        segments.add(computeSegment(2, "集港在途", "TO_PORT",
                transit.getDepartWarehouseTime(), transit.getArrivePortTime(),
                otdConfig, otdField(otdConfig, 2), warnField(otdConfig, 2)));

        // Segment 3: AT_PORT_WAIT_SHIP — arrivePortTime → shipDepartTime
        segments.add(computeSegment(3, "已集港待装船", "AT_PORT_WAIT_SHIP",
                transit.getArrivePortTime(), transit.getShipDepartTime(),
                otdConfig, otdField(otdConfig, 3), warnField(otdConfig, 3)));

        // Segment 4: ON_SEA — shipDepartTime → shipArriveTime
        segments.add(computeSegment(4, "水运在途", "ON_SEA",
                transit.getShipDepartTime(), transit.getShipArriveTime(),
                otdConfig, otdField(otdConfig, 4), warnField(otdConfig, 4)));

        // Segment 5: AT_DEST_WAIT_UNLOAD — shipArriveTime → unloadFinishTime
        segments.add(computeSegment(5, "已到港待卸船", "AT_DEST_WAIT_UNLOAD",
                transit.getShipArriveTime(), transit.getUnloadFinishTime(),
                otdConfig, otdField(otdConfig, 5), warnField(otdConfig, 5)));

        // Segment 6: UNLOADED_WAIT_DISPATCH — unloadFinishTime → dispatchTime
        segments.add(computeSegment(6, "已卸船待分拨", "UNLOADED_WAIT_DISPATCH",
                transit.getUnloadFinishTime(), transit.getDispatchTime(),
                otdConfig, otdField(otdConfig, 6), warnField(otdConfig, 6)));

        // Segment 7: DISPATCHING — dispatchTime → arriveShopTime
        segments.add(computeSegment(7, "分拨在途", "DISPATCHING",
                transit.getDispatchTime(), transit.getArriveShopTime(),
                otdConfig, otdField(otdConfig, 7), warnField(otdConfig, 7)));

        // 隐式完成：运输状态传播 — 结合时间戳和运输状态
        // 1) 时间戳：若段 N 有开始时间，则 0..N-1 段为隐式已完成
        // 2) 运输状态：车辆当前所在段之前的所有段必须已完成
        // 即使它们的结束时间为 null，也标记为 NORMAL（前端显示 "—"）
        int maxStartedIdx = -1;
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getStartTime() != null) {
                maxStartedIdx = i;
            }
        }
        // 运输状态决定当前段索引 — 之前所有段必须已结束
        int currentIdx = getStatusIndex(transit.getTransportStatus());
        int completionBoundary = Math.max(maxStartedIdx, currentIdx);
        for (int i = 0; i < completionBoundary; i++) {
            SegmentDetail seg = segments.get(i);
            if (seg.getEndTime() == null) {
                seg.setStatus("NORMAL");
                seg.setActualDurationHours(null);
                // endTime 保持 null → 前端显示 "—"
            }
        }

        return segments;
    }

    /**
     * 运输状态 → 段索引映射
     * <p>返回运输状态对应的段索引，所有在该索引之前的段必须已完成。</p>
     */
    private int getStatusIndex(String transportStatus) {
        if (transportStatus == null) return 0;
        return switch (transportStatus) {
            case "NOT_DEPARTED" -> 0;
            case "TO_PORT" -> 1;
            case "AT_PORT_WAIT_SHIP" -> 2;
            case "ON_SEA" -> 3;
            case "AT_DEST_WAIT_UNLOAD" -> 4;
            case "UNLOADED_WAIT_DISPATCH" -> 5;
            case "DISPATCHING" -> 6;
            case "ARRIVED" -> 6;
            default -> 0;
        };
    }

    // ==================== 单段计算 ====================

    /**
     * 计算单段运输详情
     */
    private SegmentDetail computeSegment(int index, String name, String statusCode,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          RouteOtdConfig otdConfig,
                                          Double standardOtd, Double warnThreshold) {
        SegmentDetail seg = new SegmentDetail();
        seg.setSegmentIndex(index);
        seg.setSegmentName(name);
        seg.setStatusCode(statusCode);
        seg.setStartTime(startTime);
        seg.setEndTime(endTime);
        seg.setStandardOtdHours(standardOtd);
        seg.setWarnThresholdHours(warnThreshold);

        // 状态判定
        if (otdConfig == null) {
            // 无 OTD 配置 → N/A（但实际时长如有仍填入）
            seg.setStatus("N/A");
            seg.setActualDurationHours(computeDuration(startTime, endTime));
        } else if (startTime == null && endTime == null) {
            // 未开始 → PENDING
            seg.setStatus("PENDING");
            seg.setActualDurationHours(null);
        } else if (startTime != null && endTime == null) {
            // 进行中 → 使用当前时间作为结束时间
            LocalDateTime effectiveEnd = now();
            double durationHours = Duration.between(startTime, effectiveEnd).toMinutes() / 60.0;
            seg.setActualDurationHours(durationHours);
            seg.setStatus(evaluateStatus(durationHours, standardOtd, warnThreshold));
        } else if (startTime != null) {
            // 已完成 (endTime != null)
            double durationHours = Duration.between(startTime, endTime).toMinutes() / 60.0;
            seg.setActualDurationHours(durationHours);
            seg.setStatus(evaluateStatus(durationHours, standardOtd, warnThreshold));
        } else if (endTime != null) {
            // 有结束时间但无开始时间 → 已结束，但缺起始数据，无法计算实际耗时
            seg.setStatus("NORMAL");
            seg.setActualDurationHours(null);
        } else {
            // 没有开始时间也没有结束时间 → PENDING (不可达，上行已处理所有 endTime!=null 情况)
            seg.setStatus("PENDING");
            seg.setActualDurationHours(null);
        }

        return seg;
    }

    /**
     * 计算耗时（小时），endTime 为 null 时使用 now()
     */
    private Double computeDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime).toMinutes() / 60.0;
        }
        if (startTime != null) {
            return Duration.between(startTime, now()).toMinutes() / 60.0;
        }
        return null;
    }

    /**
     * 判定段状态：OVERDUE / WARN / NORMAL
     */
    private String evaluateStatus(double actualDuration, Double standardOtd, Double warnThreshold) {
        if (standardOtd != null && actualDuration > standardOtd) {
            return "OVERDUE";
        } else if (warnThreshold != null && actualDuration > warnThreshold) {
            return "WARN";
        } else {
            return "NORMAL";
        }
    }

    // ==================== OTD/Warn 字段提取 ====================

    private static Double otdField(RouteOtdConfig config, int segment) {
        if (config == null) return null;
        return switch (segment) {
            case 1 -> config.getNotDepartedOtd();
            case 2 -> config.getToPortOtd();
            case 3 -> config.getAtPortWaitOtd();
            case 4 -> config.getOnSeaOtd();
            case 5 -> config.getAtDestWaitOtd();
            case 6 -> config.getUnloadWaitDispatchOtd();
            case 7 -> config.getDispatchingOtd();
            default -> null;
        };
    }

    private static Double warnField(RouteOtdConfig config, int segment) {
        if (config == null) return null;
        return switch (segment) {
            case 1 -> config.getNotDepartedWarn();
            case 2 -> config.getToPortWarn();
            case 3 -> config.getAtPortWaitWarn();
            case 4 -> config.getOnSeaWarn();
            case 5 -> config.getAtDestWaitWarn();
            case 6 -> config.getUnloadWaitDispatchWarn();
            case 7 -> config.getDispatchingWarn();
            default -> null;
        };
    }

    // ==================== 整段汇总 ====================

    /**
     * 计算整段标准 OTD 总小时（7 字段之和，无配置时为 null）
     */
    private Double computeTotalStandardOtd(RouteOtdConfig otdConfig) {
        if (otdConfig == null) {
            return null;
        }
        return nvl(otdConfig.getNotDepartedOtd())
                + nvl(otdConfig.getToPortOtd())
                + nvl(otdConfig.getAtPortWaitOtd())
                + nvl(otdConfig.getOnSeaOtd())
                + nvl(otdConfig.getAtDestWaitOtd())
                + nvl(otdConfig.getUnloadWaitDispatchOtd())
                + nvl(otdConfig.getDispatchingOtd());
    }

    /**
     * 计算整段实际耗时（小时）
     * orderReleaseTime → arriveShopTime，arriveShopTime 为 null 时取当前时间
     */
    private Double computeTotalActualHours(LocalDateTime orderReleaseTime,
                                            LocalDateTime arriveShopTime) {
        if (orderReleaseTime == null) {
            return null;
        }
        LocalDateTime endTime = arriveShopTime != null ? arriveShopTime : now();
        return Duration.between(orderReleaseTime, endTime).toMinutes() / 60.0;
    }

    private static double nvl(Double val) {
        return val != null ? val : 0.0;
    }
}
