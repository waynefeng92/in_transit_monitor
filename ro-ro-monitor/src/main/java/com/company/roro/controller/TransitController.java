package com.company.roro.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.roro.dto.TransitSummaryDTO;
import com.company.roro.entity.OrderInfo;
import com.company.roro.entity.VehicleTransit;
import com.company.roro.service.OrderInfoService;
import com.company.roro.service.VehicleTransitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在途监控接口
 *
 * 功能：查询车辆在途状态、监控状态，提供监控大屏汇总数据
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/transit")
public class TransitController {

    @Autowired
    private VehicleTransitService vehicleTransitService;

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 查询当前在途车辆列表
     *
     * @return 未到达的车辆列表
     */
    @GetMapping("/list")
    public List<VehicleTransit> list() {
        return vehicleTransitService.lambdaQuery()
                .ne(VehicleTransit::getTransportStatus, "ARRIVED")
                .orderByDesc(VehicleTransit::getUpdatedAt)
                .list();
    }

    /**
     * 根据订单ID查询在途状态
     *
     * @param orderId 订单ID
     * @return 该订单的在途状态
     */
    @GetMapping("/order/{orderId}")
    public VehicleTransit getByOrderId(
            @PathVariable Integer orderId) {
        return vehicleTransitService.lambdaQuery()
                .eq(VehicleTransit::getOrderId, orderId)
                .one();
    }

    /**
     * 根据ID查询在途状态
     *
     * @param id 在途记录ID
     * @return 在途状态信息
     */
    @GetMapping("/{id}")
    public VehicleTransit getById(
            @PathVariable Integer id) {
        return vehicleTransitService.getById(id);
    }

    /**
     * 获取监控汇总数据
     *
     * @return 正常/预警/超期的数量统计
     */
    @GetMapping("/summary")
    public TransitSummaryDTO summary(
            @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        LambdaQueryWrapper<VehicleTransit> query = new LambdaQueryWrapper<VehicleTransit>()
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
                TransitSummaryDTO empty = new TransitSummaryDTO();
                empty.setNormal(0L);
                empty.setWarn(0L);
                empty.setOverdue(0L);
                empty.setTotal(0L);
                return empty;
            }
            query.in(VehicleTransit::getOrderId, orderIds);
        }

        List<VehicleTransit> list = vehicleTransitService.list(query);

        long normal = list.stream().filter(v -> "NORMAL".equals(v.getMonitorStatus())).count();
        long warn = list.stream().filter(v -> "WARN".equals(v.getMonitorStatus())).count();
        long overdue = list.stream().filter(v -> "OVERDUE".equals(v.getMonitorStatus())).count();

        TransitSummaryDTO dto = new TransitSummaryDTO();
        dto.setNormal(normal);
        dto.setWarn(warn);
        dto.setOverdue(overdue);
        dto.setTotal((long) list.size());

        // 整段监控统计
        long overallNormal = list.stream()
            .filter(v -> v.getOverallMonitorStatus() == null || "NORMAL".equals(v.getOverallMonitorStatus()))
            .count();
        long overallWarn = list.stream()
            .filter(v -> "WARN".equals(v.getOverallMonitorStatus()))
            .count();
        long overallOverdue = list.stream()
            .filter(v -> "OVERDUE".equals(v.getOverallMonitorStatus()))
            .count();

        dto.setOverallNormal(overallNormal);
        dto.setOverallWarn(overallWarn);
        dto.setOverallOverdue(overallOverdue);

        long sectionNormal = list.stream()
            .filter(v -> v.getSectionMonitorStatus() == null || "NORMAL".equals(v.getSectionMonitorStatus()))
            .count();
        long sectionWarn = list.stream()
            .filter(v -> "WARN".equals(v.getSectionMonitorStatus()))
            .count();
        long sectionOverdue = list.stream()
            .filter(v -> "OVERDUE".equals(v.getSectionMonitorStatus()))
            .count();

        dto.setSectionNormal(sectionNormal);
        dto.setSectionWarn(sectionWarn);
        dto.setSectionOverdue(sectionOverdue);

        return dto;
    }
}