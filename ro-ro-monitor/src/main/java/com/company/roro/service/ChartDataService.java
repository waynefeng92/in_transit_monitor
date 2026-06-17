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

    @Autowired
    private VehicleTransitService vehicleTransitService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private BrandDictService brandDictService;

    @Autowired
    private TransportStatusDictService transportStatusDictService;

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
                                       String type, String sectionName) {
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
            return buildOverallBrandStatus(transitList, orderMap, brandMap);
        }

        // 如果是三段监控模式
        if ("three-section".equals(type)) {
            return buildThreeSectionBrandStatus(transitList, orderMap, brandMap, sectionName);
        }

        // 6. 分组统计：品牌 → 在途状态（中文） → 监控状态 → 数量
        Map<String, Map<String, Map<String, Long>>> result = new LinkedHashMap<>();

        for (VehicleTransit transit : transitList) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) {
                continue;
            }

            String brandName = brandMap.getOrDefault(order.getBrandId(), "未知品牌");
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
                                                               Map<Integer, String> brandMap) {
        // 1. 分组统计：品牌 → 整段监控状态 → 数量
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();

        for (VehicleTransit transit : transitList) {
            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) continue;

            String brandName = brandMap.getOrDefault(order.getBrandId(), "未知品牌");
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
                                                 String sectionName) {
        if (sectionName != null && !sectionName.isEmpty()) {
            return buildSectionBrandDrillDown(transitList, orderMap, brandMap, sectionName);
        }
        return buildSectionLevelAggregation(transitList);
    }

    /**
     * 段级汇总：前三段（前段/中段/后段）的监控状态统计
     */
    private List<SectionChartDataDTO> buildSectionLevelAggregation(List<VehicleTransit> transitList) {
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
     * 单段-品牌钻取：指定段下按品牌统计监控状态
     */
    private List<SectionBrandChartDataDTO> buildSectionBrandDrillDown(List<VehicleTransit> transitList,
                                                                       Map<Integer, OrderInfo> orderMap,
                                                                       Map<Integer, String> brandMap,
                                                                       String sectionName) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();

        for (VehicleTransit transit : transitList) {
            String section = getSectionName(transit.getTransportStatus());
            if (!sectionName.equals(section)) {
                continue;
            }

            OrderInfo order = orderMap.get(transit.getOrderId());
            if (order == null) {
                continue;
            }

            String brandName = brandMap.getOrDefault(order.getBrandId(), "未知品牌");
            String monitorStatus = transit.getSectionMonitorStatus();
            if (monitorStatus == null) {
                monitorStatus = "NORMAL";
            }

            result.computeIfAbsent(brandName, k -> new LinkedHashMap<>())
                    .merge(monitorStatus, 1L, Long::sum);
        }

        List<SectionBrandChartDataDTO> chartData = new ArrayList<>();
        for (String brand : result.keySet()) {
            Map<String, Long> counts = result.get(brand);

            SectionBrandChartDataDTO dto = new SectionBrandChartDataDTO();
            dto.setBrand(brand);
            dto.setSectionName(sectionName);
            dto.setNormal(counts.getOrDefault("NORMAL", 0L));
            dto.setWarn(counts.getOrDefault("WARN", 0L));
            dto.setOverdue(counts.getOrDefault("OVERDUE", 0L));
            chartData.add(dto);
        }

        chartData.sort(Comparator.comparing(SectionBrandChartDataDTO::getBrand));
        return chartData;
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
