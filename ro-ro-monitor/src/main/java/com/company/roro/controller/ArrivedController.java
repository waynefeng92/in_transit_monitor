package com.company.roro.controller;

import com.company.roro.dto.ArrivedChartDataDTO;
import com.company.roro.dto.ArrivedSummaryDTO;
import com.company.roro.dto.ArrivedWeeklyMonthlyDTO;
import com.company.roro.service.ArrivedVehicleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@Api(tags = "已到达车辆监控")
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
    @ApiOperation(value = "获取已到达车辆汇总", notes = "返回已到达车辆的高效、正常、延迟计数及平均效率，用于监控大屏顶部汇总卡片")
    @GetMapping("/summary")
    public ArrivedSummaryDTO summary(
            @ApiParam(value = "到店时间-起始", example = "2025-01-01T00:00:00")
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "到店时间-结束", example = "2025-12-31T23:59:59")
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ApiParam(value = "品牌ID", example = "1")
            @RequestParam(name = "brandId", required = false) Long brandId) {
        return arrivedVehicleService.calculateSummary(startTime, endTime, brandId);
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
    @ApiOperation(value = "获取已到达车辆图表数据",
            notes = "返回按类型（overall/segment/three-section）分组的效率分桶矩阵数据。type=three-section 时传入 sectionName 可进行品牌钻取。")
    @GetMapping("/chart")
    public ArrivedChartDataDTO chart(
            @ApiParam(value = "图表类型", required = true, allowableValues = "overall,segment,three-section", example = "overall")
            @RequestParam(name = "type") String type,
            @ApiParam(value = "到店时间-起始", example = "2025-01-01T00:00:00")
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "到店时间-结束", example = "2025-12-31T23:59:59")
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ApiParam(value = "品牌ID", example = "1")
            @RequestParam(name = "brandId", required = false) Long brandId,
            @ApiParam(value = "三段监控-段名称（前段/中段/后段），用于品牌钻取", example = "前段")
            @RequestParam(name = "sectionName", required = false) String sectionName) {
        return arrivedVehicleService.calculateChartData(type, startTime, endTime, brandId, sectionName);
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
    @ApiOperation(value = "获取已到达车辆周/月趋势统计", notes = "返回按周或按月聚合的到达数量与平均效率，用于趋势图展示")
    @GetMapping("/statistics")
    public List<ArrivedWeeklyMonthlyDTO> statistics(
            @ApiParam(value = "周期类型", required = true, allowableValues = "week,month", example = "week")
            @RequestParam(name = "period") String period,
            @ApiParam(value = "到店时间-起始", example = "2025-01-01T00:00:00")
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "到店时间-结束", example = "2025-12-31T23:59:59")
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ApiParam(value = "品牌ID", example = "1")
            @RequestParam(name = "brandId", required = false) Long brandId) {
        return arrivedVehicleService.calculateWeeklyMonthly(period, startTime, endTime, brandId);
    }
}
