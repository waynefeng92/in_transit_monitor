package com.company.roro.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.roro.config.MonitorConfig;
import com.company.roro.dto.*;
import com.company.roro.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final VehicleTransitService vehicleTransitService;
    private final OrderInfoService orderInfoService;
    private final BrandDictService brandDictService;
    private final RouteOtdConfigService routeOtdConfigService;
    private final RouteDictService routeDictService;
    private final ArrivedEfficiencyCalculator efficiencyCalculator;
    private final MonitorConfig monitorConfig;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final Set<String> BUCKET_EFFICIENT = Set.of("EFFICIENT");
    private static final Set<String> BUCKET_NORMAL = Set.of("NORMAL");
    private static final Set<String> BUCKET_DELAYED = Set.of("DELAYED");

    // ==================== 汇总 ====================

    public StatisticsSummaryDTO calculateSummary(LocalDateTime startTime, LocalDateTime endTime,
                                                  Long brandId, Integer routeId) {
        List<VehicleTransit> transits = queryArrivedVehicles(startTime, endTime, brandId, routeId);
        if (transits.isEmpty()) {
            return emptySummary();
        }

        Map<Integer, OrderInfo> orderMap = loadOrderMap(transits);
        Map<Integer, RouteOtdConfig> otdMap = loadOtdConfigMap(orderMap);
        double warnRatio = monitorConfig.getOverallWarnRatio();

        long efficient = 0, normal = 0, delayed = 0, total = 0;
        double totalEfficiency = 0;
        int efficiencyCount = 0;

        for (VehicleTransit transit : transits) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;
            RouteOtdConfig config = order.getRouteId() != null ? otdMap.get(order.getRouteId()) : null;

            String bucket = efficiencyCalculator.determineBucket(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config, warnRatio);
            if (bucket == null) bucket = "NORMAL";

            switch (bucket) {
                case "EFFICIENT": efficient++; break;
                case "NORMAL": normal++; break;
                case "DELAYED": delayed++; break;
            }
            total++;

            Double eff = efficiencyCalculator.calculateConsumptionRatio(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config);
            if (eff != null) {
                totalEfficiency += eff;
                efficiencyCount++;
            }
        }

        StatisticsSummaryDTO dto = new StatisticsSummaryDTO();
        dto.setTotalArrivals(total);
        dto.setEfficientCount(efficient);
        dto.setNormalCount(normal);
        dto.setDelayedCount(delayed);
        dto.setAvgEfficiency(efficiencyCount > 0 ? totalEfficiency / efficiencyCount : 0.0);
        dto.setOtdComplianceRate(total > 0 ? (double) (efficient + normal) / total * 100.0 : 0.0);
        return dto;
    }

    // ==================== 趋势 ====================

    public List<TrendStatDTO> calculateTrend(String period, LocalDateTime startTime, LocalDateTime endTime,
                                              Long brandId, Integer routeId) {
        List<VehicleTransit> transits = queryArrivedVehicles(startTime, endTime, brandId, routeId);
        if (transits.isEmpty()) return Collections.emptyList();

        Map<Integer, OrderInfo> orderMap = loadOrderMap(transits);
        Map<Integer, RouteOtdConfig> otdMap = loadOtdConfigMap(orderMap);
        double warnRatio = monitorConfig.getOverallWarnRatio();

        Map<String, long[]> periodStats = new LinkedHashMap<>();

        for (VehicleTransit transit : transits) {
            if (transit.getArriveShopTime() == null) continue;
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;

            String periodKey;
            if ("month".equals(period)) {
                periodKey = transit.getArriveShopTime().format(MONTH_FORMATTER);
            } else {
                int year = transit.getArriveShopTime().get(WeekFields.ISO.weekBasedYear());
                int week = transit.getArriveShopTime().get(WeekFields.ISO.weekOfWeekBasedYear());
                periodKey = year + "-W" + String.format("%02d", week);
            }

            RouteOtdConfig config = order.getRouteId() != null ? otdMap.get(order.getRouteId()) : null;
            String bucket = efficiencyCalculator.determineBucket(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config, warnRatio);
            if (bucket == null) bucket = "NORMAL";

            Double eff = efficiencyCalculator.calculateConsumptionRatio(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config);

            long[] stats = periodStats.computeIfAbsent(periodKey, k -> new long[5]);
            stats[0]++; // total
            if (eff != null) {
                stats[1] = Double.doubleToRawLongBits(Double.longBitsToDouble(stats[1]) + eff);
                stats[4]++; // efficiency count
            }
            switch (bucket) {
                case "EFFICIENT": stats[2]++; break;
                case "DELAYED": stats[3]++; break;
            }
        }

        List<TrendStatDTO> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : periodStats.entrySet()) {
            TrendStatDTO dto = new TrendStatDTO();
            dto.setPeriod(entry.getKey());
            long[] stats = entry.getValue();
            int count = (int) stats[0];
            int effCount = (int) stats[4];
            dto.setArrivalCount(count);
            dto.setEfficientCount((int) stats[2]);
            dto.setDelayedCount((int) stats[3]);
            dto.setNormalCount(count - (int) stats[2] - (int) stats[3]);
            dto.setAvgEfficiency(effCount > 0 ? Double.longBitsToDouble(stats[1]) / effCount : 0.0);
            result.add(dto);
        }
        result.sort(Comparator.comparing(TrendStatDTO::getPeriod));
        return result;
    }

    // ==================== 按路线 ====================

    public List<DimensionStatDTO> calculateByRoute(LocalDateTime startTime, LocalDateTime endTime,
                                                    Long brandId, Integer routeId) {
        List<VehicleTransit> transits = queryArrivedVehicles(startTime, endTime, brandId, routeId);
        if (transits.isEmpty()) return Collections.emptyList();

        Map<Integer, OrderInfo> orderMap = loadOrderMap(transits);
        Map<Integer, RouteOtdConfig> otdMap = loadOtdConfigMap(orderMap);

        Map<Integer, String> routeNameMap = routeDictService.list().stream()
                .collect(Collectors.toMap(RouteDict::getId, r ->
                        r.getOriginCity() + " → " + r.getDestCity()));

        Map<Integer, long[]> routeStats = new LinkedHashMap<>();

        for (VehicleTransit transit : transits) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null || order.getRouteId() == null) continue;
            RouteOtdConfig config = otdMap.get(order.getRouteId());

            long[] stats = routeStats.computeIfAbsent(order.getRouteId(), k -> new long[3]);
            stats[0]++;

            String bucket = efficiencyCalculator.determineBucket(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config, monitorConfig.getOverallWarnRatio());
            if (bucket != null && !"DELAYED".equals(bucket)) stats[1]++;

            Double eff = efficiencyCalculator.calculateConsumptionRatio(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config);
            if (eff != null) {
                stats[2] = Double.doubleToRawLongBits(Double.longBitsToDouble(stats[2]) + eff);
            }
        }

        List<DimensionStatDTO> result = new ArrayList<>();
        for (Map.Entry<Integer, long[]> entry : routeStats.entrySet()) {
            DimensionStatDTO dto = new DimensionStatDTO();
            dto.setName(routeNameMap.getOrDefault(entry.getKey(), "未知线路"));
            long[] stats = entry.getValue();
            long count = stats[0];
            dto.setTotalCount(count);
            dto.setOtdComplianceRate(count > 0 ? (double) stats[1] / count * 100.0 : 0.0);
            dto.setAvgEfficiency(count > 0 ? Double.longBitsToDouble(stats[2]) / count : 0.0);
            result.add(dto);
        }
        result.sort(Comparator.comparing(DimensionStatDTO::getTotalCount).reversed());
        return result;
    }

    // ==================== 辅助方法 ====================

    private List<VehicleTransit> queryArrivedVehicles(LocalDateTime startTime, LocalDateTime endTime,
                                                       Long brandId, Integer routeId) {
        LambdaQueryWrapper<VehicleTransit> transitQuery = new LambdaQueryWrapper<VehicleTransit>()
                .eq(VehicleTransit::getTransportStatus, "ARRIVED");

        if (startTime != null) transitQuery.ge(VehicleTransit::getArriveShopTime, startTime);
        if (endTime != null) transitQuery.le(VehicleTransit::getArriveShopTime, endTime);

        boolean hasOrderFilter = brandId != null || routeId != null;
        if (hasOrderFilter) {
            LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
            if (brandId != null) orderQuery.eq(OrderInfo::getBrandId, brandId);
            if (routeId != null) orderQuery.eq(OrderInfo::getRouteId, routeId);

            List<Integer> orderIds = orderInfoService.list(orderQuery).stream()
                    .map(OrderInfo::getId)
                    .collect(Collectors.toList());
            if (orderIds.isEmpty()) return Collections.emptyList();
            transitQuery.in(VehicleTransit::getOrderId, orderIds);
        }

        return vehicleTransitService.list(transitQuery);
    }

    private Map<Integer, OrderInfo> loadOrderMap(List<VehicleTransit> transits) {
        List<Integer> orderIds = transits.stream()
                .map(VehicleTransit::getOrderId).distinct().collect(Collectors.toList());
        if (orderIds.isEmpty()) return Collections.emptyMap();
        return orderInfoService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getId, o -> o));
    }

    private Map<Integer, RouteOtdConfig> loadOtdConfigMap(Map<Integer, OrderInfo> orderMap) {
        Set<Integer> routeIds = orderMap.values().stream()
                .map(OrderInfo::getRouteId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (routeIds.isEmpty()) return Collections.emptyMap();
        return routeOtdConfigService.lambdaQuery()
                .in(RouteOtdConfig::getRouteId, routeIds)
                .eq(RouteOtdConfig::getIsActive, 1)
                .list().stream()
                .collect(Collectors.toMap(RouteOtdConfig::getRouteId, c -> c, (a, b) -> a));
    }

    private StatisticsSummaryDTO emptySummary() {
        StatisticsSummaryDTO dto = new StatisticsSummaryDTO();
        dto.setTotalArrivals(0L);
        dto.setEfficientCount(0L);
        dto.setNormalCount(0L);
        dto.setDelayedCount(0L);
        dto.setAvgEfficiency(0.0);
        dto.setOtdComplianceRate(0.0);
        return dto;
    }
}
