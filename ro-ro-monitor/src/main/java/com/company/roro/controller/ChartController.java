package com.company.roro.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.roro.dto.ChartDataDTO;
import com.company.roro.dto.OverallChartDataDTO;
import com.company.roro.dto.SectionChartDataDTO;
import com.company.roro.dto.SectionBrandChartDataDTO;
import com.company.roro.entity.*;
import com.company.roro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图表数据接口
 */
@RestController
@RequestMapping("/api/chart")
public class ChartController {

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
     */
    @GetMapping("/brand-status")
    public Object getBrandStatusChart(
            @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "type", required = false, defaultValue = "segment") String type,
            @RequestParam(name = "sectionName", required = false) String sectionName) {
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
            return getOverallBrandStatus(transitList, orderIds, orderMap, brandMap);
        }

        // 如果是三段监控模式
        if ("three-section".equals(type)) {
            return getThreeSectionBrandStatus(transitList, orderIds, orderMap, brandMap, sectionName);
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
                dto.setTransportStatus(statusName);  // 已经是中文名
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

    private List<OverallChartDataDTO> getOverallBrandStatus(List<VehicleTransit> transitList,
                                                             List<Integer> orderIds,
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
                overallMonitorStatus = "NORMAL";  // NULL → NORMAL for aggregation
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

    private Object getThreeSectionBrandStatus(List<VehicleTransit> transitList,
                                               List<Integer> orderIds,
                                               Map<Integer, OrderInfo> orderMap,
                                               Map<Integer, String> brandMap,
                                               String sectionName) {
        if (sectionName != null && !sectionName.isEmpty()) {
            return getSectionBrandDrillDown(transitList, orderIds, orderMap, brandMap, sectionName);
        }
        return getSectionLevelAggregation(transitList);
    }

    private List<SectionChartDataDTO> getSectionLevelAggregation(List<VehicleTransit> transitList) {
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

    private List<SectionBrandChartDataDTO> getSectionBrandDrillDown(List<VehicleTransit> transitList,
                                                                      List<Integer> orderIds,
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