package com.company.roro.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.roro.dto.*;
import com.company.roro.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图表数据服务
 *
 * 从 ChartController.getBrandStatusChart() 提取的业务逻辑。
 * SnapshotScheduler 和 ChartController 共用此服务。
 */
@Service
public class ChartDataService {

    private static final Set<String> SECTION_NAMES = Set.of("前段", "中段", "后段");

    @Autowired
    private VehicleTransitService vehicleTransitService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private BrandDictService brandDictService;

    @Autowired
    private TransportStatusDictService transportStatusDictService;

    /**
     * 获取三段监控中某段对应的运输状态码集合
     */
    private Set<String> getTransportStatusesForSection(String sectionName) {
        if (sectionName == null) return null;
        switch (sectionName) {
            case "前段": return Set.of("NOT_DEPARTED", "TO_PORT", "AT_PORT_WAIT_SHIP");
            case "中段": return Set.of("ON_SEA", "AT_DEST_WAIT_UNLOAD");
            case "后段": return Set.of("UNLOADED_WAIT_DISPATCH", "DISPATCHING");
            default: return null;
        }
    }

    /**
     * 获取品牌-状态分组统计数据
     *
     * @param startTime   订单释放时间范围-起始（可选）
     * @param endTime     订单释放时间范围-结束（可选）
     * @param type        图表类型：segment / overall / three-section
     * @param sectionName 三段监控-段名称（可选，仅 three-section 类型时用于品牌钻取）
     * @return 图表数据 DTO 列表
     */
    @Transactional(readOnly = true)
    public Object getBrandStatusChart(LocalDateTime startTime, LocalDateTime endTime,
                                       String type, String sectionName, String filterBrandName) {
        // 1. 查询所有未到达的在途车辆
        LambdaQueryWrapper<VehicleTransit> transitQuery = new LambdaQueryWrapper<VehicleTransit>()
                .ne(VehicleTransit::getTransportStatus, "ARRIVED");

        if (startTime != null || endTime != null) {
            LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
            if (startTime != null) {
                orderQuery.ge(OrderInfo::getOrderReleaseTime, startTime);
            }
            if (endTime != null) {
                orderQuery.le(OrderInfo::getOrderReleaseTime, endTime);
            }
            List<Integer> orderIds = orderInfoService.list(orderQuery).stream()
                    .map(OrderInfo::getId)
                    .collect(Collectors.toList());
            if (orderIds.isEmpty()) {
                return new ArrayList<>();
            }
            transitQuery.in(VehicleTransit::getOrderId, orderIds);
        }

        List<VehicleTransit> transitList = vehicleTransitService.list(transitQuery);

        if (transitList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取状态码 → 中文名的映射
        Map<String, String> statusNameMap = transportStatusDictService.list().stream()
                .collect(Collectors.toMap(
                        TransportStatusDict::getStatusCode,
                        TransportStatusDict::getStatusName,
                        (a, b) -> a
                ));

        // 3. 获取所有关联的订单ID
        List<Integer> orderIds = transitList.stream()
                .map(VehicleTransit::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        // 4. 批量查询订单信息
        Map<Integer, OrderInfo> orderMap = orderInfoService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getId, o -> o));

        // 5. 批量查询品牌信息
        Map<Integer, String> brandMap = brandDictService.list().stream()
                .collect(Collectors.toMap(BrandDict::getId, BrandDict::getBrandName));

        // 如果是整段监控模式
        if ("overall".equals(type)) {
            return buildOverallBrandStatus(transitList, orderMap, brandMap, filterBrandName);
        }

        // 如果是三段监控模式
        if ("three-section".equals(type)) {
            return buildThreeSectionBrandStatus(transitList, orderMap, brandMap, sectionName, filterBrandName);
        }

        // 6. 分组统计：品牌 → 在途状态（中文） → 监控状态 → 数量
        // 段类型（segment）的 sectionName 是运输状态中文名，需要反查状态码
        String segmentTransportStatusCode = null;
        if (sectionName != null && !sectionName.isEmpty()) {
            segmentTransportStatusCode = transportStatusDictService.list().stream()
                    .filter(d -> sectionName.equals(d.getStatusName()))
                    .map(TransportStatusDict::getStatusCode)
                    .findFirst()
                    .orElse(null);
        }

        Map<String, Map<String, Map<String, Long>>> result = new LinkedHashMap<>();

        for (VehicleTransit transit : transitList) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) {
                continue;
            }

            // 段类型 sectionName 过滤：按运输状态码过滤
            if (segmentTransportStatusCode != null
                    && !segmentTransportStatusCode.equals(transit.getTransportStatus())) {
                continue;
            }

            String brandName = brandMap.getOrDefault(order.getBrandId(), "未知品牌");

            // 品牌过滤（服务器端）
            if (filterBrandName != null && !filterBrandName.isEmpty() && !filterBrandName.equals(brandName)) {
                continue;
            }

            String statusCode = transit.getTransportStatus();
            String statusName = statusNameMap.getOrDefault(statusCode, statusCode);
            String monitorStatus = transit.getMonitorStatus();

            result.computeIfAbsent(brandName, k -> new LinkedHashMap<>())
                    .computeIfAbsent(statusName, k -> new LinkedHashMap<>())
                    .merge(monitorStatus, 1L, Long::sum);
        }

        // 7. 转换为 DTO 列表
        List<ChartDataDTO> chartData = new ArrayList<>();
        for (String brand : result.keySet()) {
            for (String statusName : result.get(brand).keySet()) {
                Map<String, Long> counts = result.get(brand).get(statusName);

                ChartDataDTO dto = new ChartDataDTO();
                dto.setBrand(brand);
                dto.setTransportStatus(statusName);
                dto.setNormal(counts.getOrDefault("NORMAL", 0L));
                dto.setWarn(counts.getOrDefault("WARN", 0L));
                dto.setOverdue(counts.getOrDefault("OVERDUE", 0L));
                chartData.add(dto);
            }
        }

        // 8. 按品牌和状态排序
        chartData.sort(Comparator
                .comparing(ChartDataDTO::getBrand)
                .thenComparing(ChartDataDTO::getTransportStatus));

        return chartData;
    }

    /**
     * 构建整段监控品牌-状态图表数据
     */
    private List<OverallChartDataDTO> buildOverallBrandStatus(List<VehicleTransit> transitList,
                                                               Map<Integer, OrderInfo> orderMap,
                                                               Map<Integer, String> brandMap,
                                                               String filterBrandName) {
        // 1. 分组统计：品牌 → 整段监控状态 → 数量
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();

        for (VehicleTransit transit : transitList) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;

            String brandName = brandMap.getOrDefault(order.getBrandId(), "未知品牌");

            // 品牌过滤（服务器端）
            if (filterBrandName != null && !filterBrandName.isEmpty() && !filterBrandName.equals(brandName)) {
                continue;
            }

            String overallMonitorStatus = transit.getOverallMonitorStatus();
            if (overallMonitorStatus == null) {
                overallMonitorStatus = "NORMAL";
            }

            result.computeIfAbsent(brandName, k -> new LinkedHashMap<>())
                    .merge(overallMonitorStatus, 1L, Long::sum);
        }

        // 2. 转换为 OverallChartDataDTO 列表
        List<OverallChartDataDTO> chartData = new ArrayList<>();
        for (String brand : result.keySet()) {
            Map<String, Long> counts = result.get(brand);

            OverallChartDataDTO dto = new OverallChartDataDTO();
            dto.setBrand(brand);
            dto.setNormal(counts.getOrDefault("NORMAL", 0L));
            dto.setWarn(counts.getOrDefault("WARN", 0L));
            dto.setOverdue(counts.getOrDefault("OVERDUE", 0L));
            chartData.add(dto);
        }

        // 3. 按品牌排序
        chartData.sort(Comparator.comparing(OverallChartDataDTO::getBrand));

        return chartData;
    }

    /**
     * 构建三段监控品牌-状态图表数据
     */
    private Object buildThreeSectionBrandStatus(List<VehicleTransit> transitList,
                                                 Map<Integer, OrderInfo> orderMap,
                                                 Map<Integer, String> brandMap,
                                                 String sectionName, String filterBrandName) {
        return buildSectionLevelAggregation(transitList, orderMap, brandMap, filterBrandName, sectionName);
    }

    /**
     * 段级汇总：前三段（前段/中段/后段）的监控状态统计
     */
    private List<SectionChartDataDTO> buildSectionLevelAggregation(
            List<VehicleTransit> transitList,
            Map<Integer, OrderInfo> orderMap,
            Map<Integer, String> brandMap,
            String filterBrandName,
            String filterSectionName) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();

        for (VehicleTransit transit : transitList) {
            String section = getSectionName(transit.getTransportStatus());
            if (section == null) {
                continue;
            }
            String monitorStatus = transit.getSectionMonitorStatus();
            if (monitorStatus == null) {
                monitorStatus = "NORMAL";
            }

            // sectionName 筛选（下拉过滤）
            if (filterSectionName != null && !filterSectionName.isEmpty()
                    && !filterSectionName.equals(section)) {
                continue;
            }

            // 品牌过滤
            if (filterBrandName != null && !filterBrandName.isEmpty()) {
                OrderInfo order = orderMap.get(transit.getOrderId());
                if (order == null) continue;
                String b = brandMap.getOrDefault(order.getBrandId(), "");
                if (!filterBrandName.equals(b)) continue;
            }

            result.computeIfAbsent(section, k -> new LinkedHashMap<>())
                    .merge(monitorStatus, 1L, Long::sum);
        }

        List<SectionChartDataDTO> chartData = new ArrayList<>();
        for (String section : result.keySet()) {
            Map<String, Long> counts = result.get(section);

            SectionChartDataDTO dto = new SectionChartDataDTO();
            dto.setSectionName(section);
            dto.setNormal(counts.getOrDefault("NORMAL", 0L));
            dto.setWarn(counts.getOrDefault("WARN", 0L));
            dto.setOverdue(counts.getOrDefault("OVERDUE", 0L));
            chartData.add(dto);
        }

        chartData.sort(Comparator.comparing(SectionChartDataDTO::getSectionName));
        return chartData;
    }

    /**
     * 获取车辆详情列表（品牌/状态/监控状态钻取）
     *
     * <p>从 getBrandStatusChart 的同源数据中过滤出匹配的车辆明细，
     * 用于监控大屏图表点击钻取时展示车辆列表。</p>
     *
     * @param startTime           订单释放时间范围-起始（可选）
     * @param endTime             订单释放时间范围-结束（可选）
     * @param type                图表类型：segment / overall / three-section
     * @param brandName           品牌名称（可选）
     * @param transportStatusName 在途状态中文名（可选）
     * @param monitorStatus       监控状态：NORMAL / WARN / OVERDUE（可选）
     * @return 匹配的车辆列表，按品牌名称和VIN排序
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getVehicleDetails(
            LocalDateTime startTime, LocalDateTime endTime,
            String type, String brandName, String transportStatusName,
            String monitorStatus, String sectionName, Integer page, Integer size) {
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 20;
        // 1. 查询所有未到达的在途车辆（与 getBrandStatusChart 相同的基查询）
        LambdaQueryWrapper<VehicleTransit> transitQuery = new LambdaQueryWrapper<VehicleTransit>()
                .ne(VehicleTransit::getTransportStatus, "ARRIVED");

        if (startTime != null || endTime != null) {
            LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
            if (startTime != null) {
                orderQuery.ge(OrderInfo::getOrderReleaseTime, startTime);
            }
            if (endTime != null) {
                orderQuery.le(OrderInfo::getOrderReleaseTime, endTime);
            }
            List<Integer> timeFilteredOrderIds = orderInfoService.list(orderQuery).stream()
                    .map(OrderInfo::getId)
                    .collect(Collectors.toList());
            if (timeFilteredOrderIds.isEmpty()) {
                Map<String, Object> emptyResult = new LinkedHashMap<>();
                emptyResult.put("records", Collections.emptyList());
                emptyResult.put("total", 0);
                emptyResult.put("page", pageNum);
                emptyResult.put("size", pageSize);
                return emptyResult;
            }
            transitQuery.in(VehicleTransit::getOrderId, timeFilteredOrderIds);
        }

        List<VehicleTransit> transitList = vehicleTransitService.list(transitQuery);

        if (transitList.isEmpty()) {
            Map<String, Object> emptyResult = new LinkedHashMap<>();
            emptyResult.put("records", Collections.emptyList());
            emptyResult.put("total", 0);
            emptyResult.put("page", pageNum);
            emptyResult.put("size", pageSize);
            return emptyResult;
        }

        // 2. 获取状态码 → 中文名的映射
        Map<String, String> statusNameMap = transportStatusDictService.list().stream()
                .collect(Collectors.toMap(
                        TransportStatusDict::getStatusCode,
                        TransportStatusDict::getStatusName,
                        (a, b) -> a
                ));

        // 3. 获取所有关联的订单ID
        List<Integer> orderIds = transitList.stream()
                .map(VehicleTransit::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        // 4. 批量查询订单信息
        Map<Integer, OrderInfo> orderMap = orderInfoService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getId, o -> o));

        // 5. 批量查询品牌信息
        Map<Integer, String> brandMap = brandDictService.list().stream()
                .collect(Collectors.toMap(BrandDict::getId, BrandDict::getBrandName));

        // 6. 反查运输状态码（通过中文名查找状态码）
        String transportStatusCode = null;
        if (transportStatusName != null && !transportStatusName.isEmpty()) {
            transportStatusCode = transportStatusDictService.list().stream()
                    .filter(d -> transportStatusName.equals(d.getStatusName()))
                    .map(TransportStatusDict::getStatusCode)
                    .findFirst()
                    .orElse(null);
        }

        // 7. 过滤并构建 DTO 列表
        List<VehicleListItemDTO> result = new ArrayList<>();

        for (VehicleTransit transit : transitList) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) {
                continue;
            }

            String bName = brandMap.getOrDefault(order.getBrandId(), "未知品牌");

            // sectionName 段过滤（新增参数，优先判断）
            if (sectionName != null && !sectionName.isEmpty()) {
                Set<String> statuses = getTransportStatusesForSection(sectionName);
                if (statuses == null || !statuses.contains(transit.getTransportStatus())) {
                    continue;
                }
            } else if ("three-section".equals(type) && brandName != null && SECTION_NAMES.contains(brandName)) {
                // 向后兼容：brandName 为段名称时按段过滤
                Set<String> legacyStatuses = getTransportStatusesForSection(brandName);
                if (legacyStatuses == null || !legacyStatuses.contains(transit.getTransportStatus())) {
                    continue;
                }
            }

            // 品牌过滤（只在 brandName 不是段名称时生效）
            if (brandName != null && !brandName.isEmpty() && !SECTION_NAMES.contains(brandName) && !brandName.equals(bName)) {
                continue;
            }

            // 运输状态过滤（通过反查的状态码）
            if (transportStatusCode != null && !transportStatusCode.equals(transit.getTransportStatus())) {
                continue;
            }

            // 监控状态过滤：根据 type 选择监控状态字段
            // 当 transportStatusName 被指定时，始终使用默认 monitorStatus 字段
            String effectiveMonitorStatus;
            if (transportStatusName != null && !transportStatusName.isEmpty()) {
                effectiveMonitorStatus = transit.getMonitorStatus();
            } else if ("overall".equals(type)) {
                effectiveMonitorStatus = transit.getOverallMonitorStatus();
            } else if ("three-section".equals(type)) {
                effectiveMonitorStatus = transit.getSectionMonitorStatus();
            } else {
                effectiveMonitorStatus = transit.getMonitorStatus();
            }

            if (monitorStatus != null && !monitorStatus.isEmpty()
                    && !monitorStatus.equals(effectiveMonitorStatus)) {
                continue;
            }

            // 构建 DTO
            VehicleListItemDTO dto = new VehicleListItemDTO();
            dto.setVin(order.getVin());
            dto.setBrandName(bName);
            dto.setTransportStatus(transit.getTransportStatus());
            dto.setTransportStatusName(statusNameMap.getOrDefault(transit.getTransportStatus(), transit.getTransportStatus()));
            dto.setMonitorStatus(effectiveMonitorStatus);
            dto.setOriginCity(order.getOriginCity());
            dto.setDestCity(order.getDestCity());
            dto.setOrderReleaseTime(order.getOrderReleaseTime());

            result.add(dto);
        }

        // 8. 按品牌名称和VIN排序
        result.sort(Comparator
                .comparing(VehicleListItemDTO::getBrandName)
                .thenComparing(VehicleListItemDTO::getVin));

        // 9. 分页
        int total = result.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<VehicleListItemDTO> pageRecords = fromIndex < total ? result.subList(fromIndex, toIndex) : Collections.emptyList();

        Map<String, Object> paginatedResult = new LinkedHashMap<>();
        paginatedResult.put("records", pageRecords);
        paginatedResult.put("total", total);
        paginatedResult.put("page", pageNum);
        paginatedResult.put("size", pageSize);
        return paginatedResult;
    }

    /**
     * 将运输状态码映射到前/中/后三段
     */
    private String getSectionName(String transportStatus) {
        if (transportStatus == null) {
            return null;
        }
        switch (transportStatus) {
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
}
