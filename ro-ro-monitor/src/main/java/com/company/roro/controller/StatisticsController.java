package com.company.roro.controller;

import com.company.roro.dto.*;
import com.company.roro.entity.BrandDict;
import com.company.roro.service.BrandDictService;
import com.company.roro.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private BrandDictService brandDictService;

    private Long resolveBrandId(String brandName) {
        if (brandName == null || brandName.isEmpty()) return null;
        BrandDict brand = brandDictService.lambdaQuery()
                .eq(BrandDict::getBrandName, brandName).one();
        return brand != null ? Long.valueOf(brand.getId()) : null;
    }

    @GetMapping("/summary")
    public Result<StatisticsSummaryDTO> summary(
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "brandName", required = false) String brandName,
            @RequestParam(name = "routeId", required = false) Integer routeId) {
        Long brandId = resolveBrandId(brandName);
        return Result.success(statisticsService.calculateSummary(startTime, endTime, brandId, routeId));
    }

    @GetMapping("/trend")
    public Result<List<TrendStatDTO>> trend(
            @RequestParam(name = "period", defaultValue = "week") String period,
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "brandName", required = false) String brandName,
            @RequestParam(name = "routeId", required = false) Integer routeId) {
        Long brandId = resolveBrandId(brandName);
        return Result.success(statisticsService.calculateTrend(period, startTime, endTime, brandId, routeId));
    }

    @GetMapping("/by-route")
    public Result<List<DimensionStatDTO>> byRoute(
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "brandName", required = false) String brandName,
            @RequestParam(name = "routeId", required = false) Integer routeId) {
        Long brandId = resolveBrandId(brandName);
        return Result.success(statisticsService.calculateByRoute(startTime, endTime, brandId, routeId));
    }
}
