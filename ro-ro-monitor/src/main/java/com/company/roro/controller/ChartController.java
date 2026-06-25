package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.service.ChartDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 图表数据接口
 */
@RestController
@RequestMapping("/api/chart")
public class ChartController {

    @Autowired
    private ChartDataService chartDataService;

    /**
     * 获取品牌-状态分组统计数据
     */
    @GetMapping("/brand-status")
    public Result<Object> getBrandStatusChart(
            @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "type", required = false, defaultValue = "segment") String type,
            @RequestParam(name = "sectionName", required = false) String sectionName,
            @RequestParam(name = "brandName", required = false) String brandName) {
        return Result.success(chartDataService.getBrandStatusChart(startTime, endTime, type, sectionName, brandName));
    }
}
