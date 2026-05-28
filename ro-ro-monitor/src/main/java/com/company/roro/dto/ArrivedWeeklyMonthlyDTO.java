package com.company.roro.dto;

import lombok.Data;

/**
 * 已到达车辆周/月趋势 DTO
 *
 * 作用：返回已到达监控趋势图（按周或按月聚合）的数据点
 *
 * 示例数据：
 * {
 *   "period": "2026-W01",
 *   "arrivalCount": 180,
 *   "avgEfficiency": 3.2
 * }
 */
@Data
public class ArrivedWeeklyMonthlyDTO {

    /** 周期标识，如 "2026-W01"（周）或 "2026-01"（月） */
    private String period;

    /** 该周期内到达的车辆数 */
    private Integer arrivalCount;

    /** 该周期内的平均效率值（小时） */
    private Double avgEfficiency;
}
