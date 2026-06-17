package com.company.roro.controller;

import com.company.roro.dto.ArrivedChartDataDTO;
import com.company.roro.dto.ArrivedSummaryDTO;
import com.company.roro.dto.ArrivedWeeklyMonthlyDTO;
import com.company.roro.dto.Result;
import com.company.roro.service.ArrivedVehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 已到达车辆监控接口
 *
 * 功能：查询已到达车辆的汇总数据、效率分桶图表、周/月趋势统计
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/arrived")
public class ArrivedController {

    @Autowired
    private ArrivedVehicleService arrivedVehicleService;

    /**
     * 获取已到达车辆汇总数据
     *
     * @param startTime 到店时间范围-起始（可选）
     * @param endTime   到店时间范围-结束（可选）
     * @param brandId   品牌 ID（可选）
     * @return 汇总 DTO，含高效/正常/延迟计数及平均效率
     */
    @GetMapping("/summary")
    public Result<ArrivedSummaryDTO> summary(
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "brandId", required = false) Long brandId) {
        return Result.success(arrivedVehicleService.calculateSummary(startTime, endTime, brandId));
    }

    /**
     * 获取已到达车辆图表数据
     *
     * @param type        图表类型：overall（整段）| segment（分段）| three-section（三段监控）
     * @param startTime   到店时间范围-起始（可选）
     * @param endTime     到店时间范围-结束（可选）
     * @param brandId     品牌 ID（可选）
     * @param sectionName 三段监控-段名称（前段/中段/后段），用于品牌钻取（可选）
     * @return 图表数据 DTO，含品牌/段 × 效率分桶矩阵
     */
    @GetMapping("/chart")
    public Result<ArrivedChartDataDTO> chart(
            @RequestParam(name = "type") String type,
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "brandId", required = false) Long brandId,
            @RequestParam(name = "sectionName", required = false) String sectionName) {
        return Result.success(arrivedVehicleService.calculateChartData(type, startTime, endTime, brandId, sectionName));
    }

    /**
     * 获取已到达车辆周/月趋势统计
     *
     * @param period    周期类型：week（按周）| month（按月）
     * @param startTime 到店时间范围-起始（可选）
     * @param endTime   到店时间范围-结束（可选）
     * @param brandId   品牌 ID（可选）
     * @return 按周期聚合的统计数据列表
     */
    @GetMapping("/statistics")
    public Result<List<ArrivedWeeklyMonthlyDTO>> statistics(
            @RequestParam(name = "period") String period,
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "brandId", required = false) Long brandId) {
        return Result.success(arrivedVehicleService.calculateWeeklyMonthly(period, startTime, endTime, brandId));
    }
}
