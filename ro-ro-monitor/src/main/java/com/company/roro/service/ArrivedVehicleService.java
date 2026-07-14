package com.company.roro.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.roro.config.MonitorConfig;
import com.company.roro.dto.ArrivedChartDataDTO;
import com.company.roro.dto.ArrivedSummaryDTO;
import com.company.roro.dto.ArrivedVehicleDTO;
import com.company.roro.dto.ArrivedWeeklyMonthlyDTO;
import com.company.roro.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 已到达车辆服务
 *
 * 提供已到达车辆的查询、汇总、图表数据和周/月趋势统计。
 * 使用 ArrivedEfficiencyCalculator 进行效率计算和分桶判定。
 */
@Service
@RequiredArgsConstructor
public class ArrivedVehicleService {

    private final VehicleTransitService vehicleTransitService;
    private final OrderInfoService orderInfoService;
    private final BrandDictService brandDictService;
    private final RouteOtdConfigService routeOtdConfigService;
    private final TransportStatusDictService transportStatusDictService;
    private final ArrivedEfficiencyCalculator efficiencyCalculator;
    private final MonitorConfig monitorConfig;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final List<String> SEGMENT_LIST = Arrays.asList(ArrivedEfficiencyCalculator.SEGMENT_ORDER);
    private static final List<String> SECTION_LIST = Arrays.asList("前段", "中段", "后段");
    private static final List<String> BUCKET_LIST = Arrays.asList("EFFICIENT", "NORMAL", "DELAYED");

    // ==================== 查询已到达车辆列表 ====================

    /**
     * 分页查询已到达车辆列表
     *
     * @param startTime 到店时间范围-起始（可选）
     * @param endTime   到店时间范围-结束（可选）
     * @param brandId   品牌 ID（可选）
     * @param page      页码（从1开始）
     * @param size      每页数量
     * @return 已到达车辆 DTO 列表
     */
    public List<ArrivedVehicleDTO> listArrivedVehicles(LocalDateTime startTime, LocalDateTime endTime,
                                                        Long brandId, int page, int size) {
        // 1. 构建 ARRIVED 车辆查询
        List<VehicleTransit> allTransits = queryArrivedVehicles(startTime, endTime, brandId);
        if (allTransits.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量加载关联数据
        List<Integer> orderIds = allTransits.stream()
                .map(VehicleTransit::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, OrderInfo> orderMap = orderInfoService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getId, o -> o));
        Map<Integer, String> brandMap = brandDictService.list().stream()
                .collect(Collectors.toMap(BrandDict::getId, BrandDict::getBrandName));
        Map<String, String> statusNameMap = transportStatusDictService.list().stream()
                .collect(Collectors.toMap(
                        TransportStatusDict::getStatusCode,
                        TransportStatusDict::getStatusName,
                        (a, b) -> a));

        // 3. 批量加载 OTD 配置（通过 routeId）
        Set<Integer> routeIds = orderMap.values().stream()
                .map(OrderInfo::getRouteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, RouteOtdConfig> otdConfigMap = Collections.emptyMap();
        if (!routeIds.isEmpty()) {
            otdConfigMap = routeOtdConfigService.lambdaQuery()
                    .in(RouteOtdConfig::getRouteId, routeIds)
                    .eq(RouteOtdConfig::getIsActive, 1)
                    .list().stream()
                    .collect(Collectors.toMap(RouteOtdConfig::getRouteId, c -> c, (a, b) -> a));
        }

        // 4. 转换为 DTO 列表
        List<ArrivedVehicleDTO> allDtos = new ArrayList<>();
        for (VehicleTransit transit : allTransits) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;

            ArrivedVehicleDTO dto = new ArrivedVehicleDTO();
            dto.setVehicleId(order.getVin());
            dto.setBrandName(brandMap.getOrDefault(order.getBrandId(), "未知品牌"));
            dto.setTransportStatus(statusNameMap.getOrDefault(transit.getTransportStatus(), transit.getTransportStatus()));
            dto.setArriveShopTime(transit.getArriveShopTime() != null ? transit.getArriveShopTime().toString() : null);
            dto.setOrderReleaseTime(order.getOrderReleaseTime() != null ? order.getOrderReleaseTime().toString() : null);

            // 计算效率
            RouteOtdConfig config = order.getRouteId() != null ? otdConfigMap.get(order.getRouteId()) : null;
            Double efficiency = efficiencyCalculator.calculateEfficiency(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config);
            dto.setEfficiency(efficiency);

            dto.setEfficiencyBucket(efficiencyCalculator.determineBucket(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(),
                    config, monitorConfig.getOverallWarnRatio()));

            allDtos.add(dto);
        }

        // 5. 分页截取
        int fromIndex = (page - 1) * size;
        if (fromIndex >= allDtos.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + size, allDtos.size());
        return allDtos.subList(fromIndex, toIndex);
    }

    // ==================== 汇总卡 ====================

    /**
     * 计算已到达车辆汇总数据
     *
     * @param startTime 到店时间范围-起始（可选）
     * @param endTime   到店时间范围-结束（可选）
     * @param brandId   品牌 ID（可选）
     * @return 汇总 DTO，含各效率分桶计数和平均效率
     */
    public ArrivedSummaryDTO calculateSummary(LocalDateTime startTime, LocalDateTime endTime, Long brandId) {
        List<VehicleTransit> allTransits = queryArrivedVehicles(startTime, endTime, brandId);
        if (allTransits.isEmpty()) {
            return emptySummary();
        }

        // 批量加载关联数据
        List<Integer> orderIds = allTransits.stream()
                .map(VehicleTransit::getOrderId).distinct().collect(Collectors.toList());
        Map<Integer, OrderInfo> orderMap = orderInfoService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getId, o -> o));

        Set<Integer> routeIds = orderMap.values().stream()
                .map(OrderInfo::getRouteId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, RouteOtdConfig> otdConfigMap = Collections.emptyMap();
        if (!routeIds.isEmpty()) {
            otdConfigMap = routeOtdConfigService.lambdaQuery()
                    .in(RouteOtdConfig::getRouteId, routeIds)
                    .eq(RouteOtdConfig::getIsActive, 1)
                    .list().stream()
                    .collect(Collectors.toMap(RouteOtdConfig::getRouteId, c -> c, (a, b) -> a));
        }

        long efficientCount = 0, normalCount = 0, delayedCount = 0;
        double totalEfficiency = 0;
        int efficiencyCount = 0;

        for (VehicleTransit transit : allTransits) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;

            RouteOtdConfig config = order.getRouteId() != null ? otdConfigMap.get(order.getRouteId()) : null;
            String bucket = efficiencyCalculator.determineBucket(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(),
                    config, monitorConfig.getOverallWarnRatio());

            if ("EFFICIENT".equals(bucket)) efficientCount++;
            else if ("NORMAL".equals(bucket)) normalCount++;
            else if ("DELAYED".equals(bucket)) delayedCount++;

            Double efficiency = efficiencyCalculator.calculateEfficiency(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config);
            if (efficiency != null) {
                totalEfficiency += efficiency;
                efficiencyCount++;
            }
        }

        ArrivedSummaryDTO summary = new ArrivedSummaryDTO();
        summary.setEfficientCount(efficientCount);
        summary.setNormalCount(normalCount);
        summary.setDelayedCount(delayedCount);
        summary.setTotalCount((long) allTransits.size());
        summary.setAvgEfficiency(efficiencyCount > 0 ? totalEfficiency / efficiencyCount : 0.0);
        return summary;
    }

    // ==================== 图表数据 ====================

    /**
     * 计算已到达车辆图表数据
     *
     * @param type        图表类型：overall / segment / three-section
     * @param startTime   到店时间范围-起始（可选）
     * @param endTime     到店时间范围-结束（可选）
     * @param brandId     品牌 ID（可选）
     * @param sectionName 三段监控-段名称（可选，仅 three-section 类型时用于品牌钻取）
     * @return 图表数据 DTO
     */
    public ArrivedChartDataDTO calculateChartData(String type, LocalDateTime startTime,
                                                   LocalDateTime endTime, Long brandId,
                                                   String sectionName) {
        if ("overall".equals(type)) {
            return calculateOverallChart(startTime, endTime, brandId);
        }
        if ("segment".equals(type)) {
            return calculateSegmentChart(startTime, endTime, brandId);
        }
        if ("three-section".equals(type)) {
            return calculateThreeSectionChart(startTime, endTime, brandId, sectionName);
        }
        return emptyChart();
    }

    /**
     * 按计算好的分组 Map 构建 ArrivedChartDataDTO
     */
    private ArrivedChartDataDTO buildChartFromGroups(Map<String, Map<String, Long>> groups,
                                                      List<String> dimOrder) {
        List<String> dimensions = new ArrayList<>();
        List<List<Integer>> data = new ArrayList<>();

        for (String dim : dimOrder) {
            Map<String, Long> bucketCounts = groups.get(dim);
            if (bucketCounts == null) continue;

            dimensions.add(dim);
            List<Integer> row = new ArrayList<>();
            for (String bucket : BUCKET_LIST) {
                row.add(bucketCounts.getOrDefault(bucket, 0L).intValue());
            }
            data.add(row);
        }

        ArrivedChartDataDTO result = new ArrivedChartDataDTO();
        result.setBrands(dimensions);
        result.setCategories(new ArrayList<>(BUCKET_LIST));
        result.setData(data);
        return result;
    }

    // --- overall: 品牌 × 效率分桶 ---

    private ArrivedChartDataDTO calculateOverallChart(LocalDateTime startTime, LocalDateTime endTime,
                                                       Long brandId) {
        List<VehicleTransit> transits = queryArrivedVehicles(startTime, endTime, brandId);
        if (transits.isEmpty()) return emptyChart();

        Map<Integer, OrderInfo> orderMap = loadOrderMap(transits);
        Map<Integer, String> brandMap = brandDictService.list().stream()
                .collect(Collectors.toMap(BrandDict::getId, BrandDict::getBrandName));
        Map<Integer, RouteOtdConfig> otdMap = loadOtdConfigMap(orderMap);

        // brandName → { EFFICIENT: n, NORMAL: n, DELAYED: n }
        Map<String, Map<String, Long>> groups = new LinkedHashMap<>();
        for (VehicleTransit transit : transits) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;
            String brandName = brandMap.getOrDefault(order.getBrandId(), "未知品牌");
            RouteOtdConfig config = order.getRouteId() != null ? otdMap.get(order.getRouteId()) : null;
            String bucket = efficiencyCalculator.determineBucket(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(),
                    config, monitorConfig.getOverallWarnRatio());
            if (bucket == null) bucket = "NORMAL";

            groups.computeIfAbsent(brandName, k -> new LinkedHashMap<>())
                    .merge(bucket, 1L, Long::sum);
        }

        // 按品牌名排序
        List<String> brandOrder = groups.keySet().stream().sorted().collect(Collectors.toList());
        return buildChartFromGroups(groups, brandOrder);
    }

    // --- segment: 段 × 效率分桶 ---

    private ArrivedChartDataDTO calculateSegmentChart(LocalDateTime startTime, LocalDateTime endTime,
                                                       Long brandId) {
        List<VehicleTransit> transits = queryArrivedVehicles(startTime, endTime, brandId);
        if (transits.isEmpty()) return emptyChart();

        Map<Integer, OrderInfo> orderMap = loadOrderMap(transits);
        Map<Integer, RouteOtdConfig> otdMap = loadOtdConfigMap(orderMap);

        // segment → { EFFICIENT: n, NORMAL: n, DELAYED: n }
        Map<String, Map<String, Long>> groups = new LinkedHashMap<>();

        for (VehicleTransit transit : transits) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;
            RouteOtdConfig config = order.getRouteId() != null ? otdMap.get(order.getRouteId()) : null;
            if (config == null) continue;

            // 遍历 7 段，计算每段效率分桶
            computeSegmentBuckets(transit, order, config, groups);
        }

        return buildChartFromGroups(groups, SEGMENT_LIST);
    }

    /**
     * 对单个 ARRIVED 车辆的 7 段逐一计算效率分桶并汇聚到 groups
     */
    private void computeSegmentBuckets(VehicleTransit transit, OrderInfo order,
                                        RouteOtdConfig config,
                                        Map<String, Map<String, Long>> groups) {
        for (String segment : SEGMENT_LIST) {
            LocalDateTime segStart = getSegmentStart(transit, order, segment);
            LocalDateTime segEnd = getSegmentEnd(transit, order, segment);
            Integer otdHours = efficiencyCalculator.getSegmentOtd(config, segment);
            Integer warnHours = efficiencyCalculator.getSegmentWarn(config, segment);

            // 缺失中间时间戳时，标 null 但不跳过 —— 该段不计数，其他段正常
            String bucket = efficiencyCalculator.determineSegmentBucket(segStart, segEnd, otdHours, warnHours);
            if (bucket == null) continue;

            groups.computeIfAbsent(segment, k -> new LinkedHashMap<>())
                    .merge(bucket, 1L, Long::sum);
        }
    }

    // --- three-section: 段汇总 × 效率分桶，支持品牌钻取 ---

    private ArrivedChartDataDTO calculateThreeSectionChart(LocalDateTime startTime, LocalDateTime endTime,
                                                            Long brandId, String sectionName) {
        List<VehicleTransit> transits = queryArrivedVehicles(startTime, endTime, brandId);
        if (transits.isEmpty()) return emptyChart();

        Map<Integer, OrderInfo> orderMap = loadOrderMap(transits);
        Map<Integer, String> brandMap = brandDictService.list().stream()
                .collect(Collectors.toMap(BrandDict::getId, BrandDict::getBrandName));
        Map<Integer, RouteOtdConfig> otdMap = loadOtdConfigMap(orderMap);

        if (sectionName != null && !sectionName.isEmpty()) {
            // 品牌钻取：指定段内按品牌 × 效率分桶
            return calculateSectionBrandDrillDown(transits, orderMap, brandMap, otdMap, sectionName);
        }

        // 段级汇总：section → { EFFICIENT: n, NORMAL: n, DELAYED: n }
        Map<String, Map<String, Long>> groups = new LinkedHashMap<>();

        for (VehicleTransit transit : transits) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;
            RouteOtdConfig config = order.getRouteId() != null ? otdMap.get(order.getRouteId()) : null;
            if (config == null) continue;

            computeSectionBuckets(transit, order, config, groups);
        }

        return buildChartFromGroups(groups, SECTION_LIST);
    }

    /**
     * 对单个 ARRIVED 车辆的三段计算效率分桶
     */
    private void computeSectionBuckets(VehicleTransit transit, OrderInfo order,
                                        RouteOtdConfig config,
                                        Map<String, Map<String, Long>> groups) {
        // 前段：orderReleaseTime → shipDepartTime
        String bucketFront = efficiencyCalculator.determineSectionBucket(
                order.getOrderReleaseTime(), transit.getShipDepartTime(),
                efficiencyCalculator.getSectionCumulativeOtd(config, "前段"),
                monitorConfig.getOverallWarnRatio());
        if (bucketFront != null) {
            groups.computeIfAbsent("前段", k -> new LinkedHashMap<>()).merge(bucketFront, 1L, Long::sum);
        }

        // 中段：shipDepartTime → unloadFinishTime
        String bucketMid = efficiencyCalculator.determineSectionBucket(
                transit.getShipDepartTime(), transit.getUnloadFinishTime(),
                efficiencyCalculator.getSectionCumulativeOtd(config, "中段"),
                monitorConfig.getOverallWarnRatio());
        if (bucketMid != null) {
            groups.computeIfAbsent("中段", k -> new LinkedHashMap<>()).merge(bucketMid, 1L, Long::sum);
        }

        // 后段：unloadFinishTime → arriveShopTime
        String bucketRear = efficiencyCalculator.determineSectionBucket(
                transit.getUnloadFinishTime(), transit.getArriveShopTime(),
                efficiencyCalculator.getSectionCumulativeOtd(config, "后段"),
                monitorConfig.getOverallWarnRatio());
        if (bucketRear != null) {
            groups.computeIfAbsent("后段", k -> new LinkedHashMap<>()).merge(bucketRear, 1L, Long::sum);
        }
    }

    /**
     * 三段-品牌钻取：指定 sectionName 下按品牌 × 效率分桶
     */
    private ArrivedChartDataDTO calculateSectionBrandDrillDown(List<VehicleTransit> transits,
                                                                Map<Integer, OrderInfo> orderMap,
                                                                Map<Integer, String> brandMap,
                                                                Map<Integer, RouteOtdConfig> otdMap,
                                                                String sectionName) {
        Map<String, Map<String, Long>> groups = new LinkedHashMap<>();

        for (VehicleTransit transit : transits) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;
            RouteOtdConfig config = order.getRouteId() != null ? otdMap.get(order.getRouteId()) : null;
            if (config == null) continue;

            String brandName = brandMap.getOrDefault(order.getBrandId(), "未知品牌");
            String bucket = null;

            switch (sectionName) {
                case "前段":
                    bucket = efficiencyCalculator.determineSectionBucket(
                            order.getOrderReleaseTime(), transit.getShipDepartTime(),
                            efficiencyCalculator.getSectionCumulativeOtd(config, "前段"),
                            monitorConfig.getOverallWarnRatio());
                    break;
                case "中段":
                    bucket = efficiencyCalculator.determineSectionBucket(
                            transit.getShipDepartTime(), transit.getUnloadFinishTime(),
                            efficiencyCalculator.getSectionCumulativeOtd(config, "中段"),
                            monitorConfig.getOverallWarnRatio());
                    break;
                case "后段":
                    bucket = efficiencyCalculator.determineSectionBucket(
                            transit.getUnloadFinishTime(), transit.getArriveShopTime(),
                            efficiencyCalculator.getSectionCumulativeOtd(config, "后段"),
                            monitorConfig.getOverallWarnRatio());
                    break;
            }

            if (bucket != null) {
                groups.computeIfAbsent(brandName, k -> new LinkedHashMap<>())
                        .merge(bucket, 1L, Long::sum);
            }
        }

        List<String> brandOrder = groups.keySet().stream().sorted().collect(Collectors.toList());
        return buildChartFromGroups(groups, brandOrder);
    }

    // ==================== 周/月趋势统计 ====================

    /**
     * 计算已到达车辆周/月趋势统计
     *
     * @param period    周期类型：week / month
     * @param startTime 到店时间范围-起始（可选）
     * @param endTime   到店时间范围-结束（可选）
     * @param brandId   品牌 ID（可选）
     * @return 按周期聚合的统计数据列表
     */
    public List<ArrivedWeeklyMonthlyDTO> calculateWeeklyMonthly(String period, LocalDateTime startTime,
                                                                 LocalDateTime endTime, Long brandId) {
        List<VehicleTransit> transits = queryArrivedVehicles(startTime, endTime, brandId);
        if (transits.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, OrderInfo> orderMap = loadOrderMap(transits);
        Map<Integer, RouteOtdConfig> otdMap = loadOtdConfigMap(orderMap);

        // periodKey → { count, totalEfficiency }
        Map<String, long[]> periodStats = new LinkedHashMap<>();

        for (VehicleTransit transit : transits) {
            if (transit.getArriveShopTime() == null) continue;
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;

            String periodKey;
            if ("month".equals(period)) {
                periodKey = transit.getArriveShopTime().format(MONTH_FORMATTER);
            } else {
                // ISO week
                int year = transit.getArriveShopTime().get(WeekFields.ISO.weekBasedYear());
                int week = transit.getArriveShopTime().get(WeekFields.ISO.weekOfWeekBasedYear());
                periodKey = year + "-W" + String.format("%02d", week);
            }

            RouteOtdConfig config = order.getRouteId() != null ? otdMap.get(order.getRouteId()) : null;
            Double efficiency = efficiencyCalculator.calculateConsumptionRatio(
                    order.getOrderReleaseTime(), transit.getArriveShopTime(), config);

            long[] stats = periodStats.computeIfAbsent(periodKey, k -> new long[2]);
            stats[0]++; // count
            if (efficiency != null) {
                // [1] accumulates efficiency value - we'll compute avg later
                // Use double-precision via long bits to avoid losing precision
                stats[1] = Double.doubleToRawLongBits(
                        Double.longBitsToDouble(stats[1]) + efficiency);
            }
        }

        List<ArrivedWeeklyMonthlyDTO> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : periodStats.entrySet()) {
            ArrivedWeeklyMonthlyDTO dto = new ArrivedWeeklyMonthlyDTO();
            dto.setPeriod(entry.getKey());
            long[] stats = entry.getValue();
            dto.setArrivalCount((int) stats[0]);
            dto.setAvgEfficiency(stats[0] > 0 ? Double.longBitsToDouble(stats[1]) / stats[0] : 0.0);
            result.add(dto);
        }

        // 按周期排序
        result.sort(Comparator.comparing(ArrivedWeeklyMonthlyDTO::getPeriod));
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 查询已到达车辆（公共查询逻辑）
     */
    private List<VehicleTransit> queryArrivedVehicles(LocalDateTime startTime, LocalDateTime endTime,
                                                       Long brandId) {
        LambdaQueryWrapper<VehicleTransit> transitQuery = new LambdaQueryWrapper<VehicleTransit>()
                .eq(VehicleTransit::getTransportStatus, "ARRIVED");

        // 到店时间范围过滤
        if (startTime != null) {
            transitQuery.ge(VehicleTransit::getArriveShopTime, startTime);
        }
        if (endTime != null) {
            transitQuery.le(VehicleTransit::getArriveShopTime, endTime);
        }

        // 品牌过滤：通过 OrderInfo 获取 orderIds
        if (brandId != null) {
            List<Integer> orderIds = orderInfoService.lambdaQuery()
                    .eq(OrderInfo::getBrandId, brandId)
                    .list().stream()
                    .map(OrderInfo::getId)
                    .collect(Collectors.toList());
            if (orderIds.isEmpty()) {
                return Collections.emptyList();
            }
            transitQuery.in(VehicleTransit::getOrderId, orderIds);
        }

        return vehicleTransitService.list(transitQuery);
    }

    /**
     * 批量加载订单 Map
     */
    private Map<Integer, OrderInfo> loadOrderMap(List<VehicleTransit> transits) {
        List<Integer> orderIds = transits.stream()
                .map(VehicleTransit::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) return Collections.emptyMap();
        return orderInfoService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getId, o -> o));
    }

    /**
     * 批量加载 OTD 配置 Map（按 routeId 索引）
     */
    private Map<Integer, RouteOtdConfig> loadOtdConfigMap(Map<Integer, OrderInfo> orderMap) {
        Set<Integer> routeIds = orderMap.values().stream()
                .map(OrderInfo::getRouteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (routeIds.isEmpty()) return Collections.emptyMap();
        return routeOtdConfigService.lambdaQuery()
                .in(RouteOtdConfig::getRouteId, routeIds)
                .eq(RouteOtdConfig::getIsActive, 1)
                .list().stream()
                .collect(Collectors.toMap(RouteOtdConfig::getRouteId, c -> c, (a, b) -> a));
    }

    /**
     * 获取段起始时间
     */
    private LocalDateTime getSegmentStart(VehicleTransit transit, OrderInfo order, String segment) {
        if (segment == null) return null;
        switch (segment) {
            case "NOT_DEPARTED":         return order.getOrderReleaseTime();
            case "TO_PORT":              return transit.getDepartWarehouseTime();
            case "AT_PORT_WAIT_SHIP":    return transit.getArrivePortTime();
            case "ON_SEA":               return transit.getShipDepartTime();
            case "AT_DEST_WAIT_UNLOAD":  return transit.getShipArriveTime();
            case "UNLOADED_WAIT_DISPATCH": return transit.getUnloadFinishTime();
            case "DISPATCHING":          return transit.getDispatchTime();
            default:                     return null;
        }
    }

    /**
     * 获取段结束时间
     */
    private LocalDateTime getSegmentEnd(VehicleTransit transit, OrderInfo order, String segment) {
        if (segment == null) return null;
        switch (segment) {
            case "NOT_DEPARTED":         return transit.getDepartWarehouseTime();
            case "TO_PORT":              return transit.getArrivePortTime();
            case "AT_PORT_WAIT_SHIP":    return transit.getShipDepartTime();
            case "ON_SEA":               return transit.getShipArriveTime();
            case "AT_DEST_WAIT_UNLOAD":  return transit.getUnloadFinishTime();
            case "UNLOADED_WAIT_DISPATCH": return transit.getDispatchTime();
            case "DISPATCHING":          return transit.getArriveShopTime();
            default:                     return null;
        }
    }

    /**
     * 空汇总
     */
    private ArrivedSummaryDTO emptySummary() {
        ArrivedSummaryDTO summary = new ArrivedSummaryDTO();
        summary.setEfficientCount(0L);
        summary.setNormalCount(0L);
        summary.setDelayedCount(0L);
        summary.setTotalCount(0L);
        summary.setAvgEfficiency(0.0);
        return summary;
    }

    /**
     * 空图表
     */
    private ArrivedChartDataDTO emptyChart() {
        ArrivedChartDataDTO chart = new ArrivedChartDataDTO();
        chart.setBrands(Collections.emptyList());
        chart.setCategories(Collections.emptyList());
        chart.setData(Collections.emptyList());
        return chart;
    }
}
