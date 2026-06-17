package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.dto.TransitSummaryDTO;
import com.company.roro.entity.VehicleTransit;
import com.company.roro.service.TransitSummaryService;
import com.company.roro.service.VehicleTransitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private TransitSummaryService transitSummaryService;

    /**
     * 查询当前在途车辆列表
     *
     * @return 未到达的车辆列表
     */
    @GetMapping("/list")
    public Result<List<VehicleTransit>> list() {
        return Result.success(vehicleTransitService.lambdaQuery()
                .ne(VehicleTransit::getTransportStatus, "ARRIVED")
                .orderByDesc(VehicleTransit::getUpdatedAt)
                .list());
    }

    /**
     * 根据订单ID查询在途状态
     *
     * @param orderId 订单ID
     * @return 该订单的在途状态
     */
    @GetMapping("/order/{orderId}")
    public Result<VehicleTransit> getByOrderId(
            @PathVariable Integer orderId) {
        return Result.success(vehicleTransitService.lambdaQuery()
                .eq(VehicleTransit::getOrderId, orderId)
                .one());
    }

    /**
     * 根据ID查询在途状态
     *
     * @param id 在途记录ID
     * @return 在途状态信息
     */
    @GetMapping("/{id}")
    public Result<VehicleTransit> getById(
            @PathVariable Integer id) {
        return Result.success(vehicleTransitService.getById(id));
    }

    /**
     * 获取监控汇总数据
     *
     * @return 正常/预警/超期的数量统计
     */
    @GetMapping("/summary")
    public Result<TransitSummaryDTO> summary(
            @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(transitSummaryService.summary(startTime, endTime));
    }
}