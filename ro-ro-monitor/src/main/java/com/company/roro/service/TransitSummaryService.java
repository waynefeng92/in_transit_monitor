package com.company.roro.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.roro.dto.TransitSummaryDTO;
import com.company.roro.entity.OrderInfo;
import com.company.roro.entity.VehicleTransit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在途车辆汇总服务
 *
 * 从 TransitController.summary() 提取的业务逻辑。
 * SnapshotScheduler 和 TransitController 共用此服务。
 */
@Service
public class TransitSummaryService {

    @Autowired
    private VehicleTransitService vehicleTransitService;

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 获取在途监控汇总数据
     *
     * @param startTime 订单释放时间范围-起始（可选）
     * @param endTime   订单释放时间范围-结束（可选）
     * @return 正常/预警/超期的数量统计，含整段和分段监控维度
     */
    @Transactional(readOnly = true)
    public TransitSummaryDTO summary(LocalDateTime startTime, LocalDateTime endTime) {
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
