package com.company.roro.dto;

import lombok.Data;

/**
 * 已到达车辆汇总 DTO
 *
 * 作用：返回已到达监控顶部汇总卡片数据
 *
 * 示例数据：
 * {
 *   "efficientCount": 850,
 *   "normalCount": 320,
 *   "delayedCount": 45,
 *   "totalCount": 1215,
 *   "avgEfficiency": 2.8
 * }
 */
@Data
public class ArrivedSummaryDTO {

    /** 高效车辆数 */
    private Long efficientCount;

    /** 正常车辆数 */
    private Long normalCount;

    /** 延迟车辆数 */
    private Long delayedCount;

    /** 已到达车辆总数 */
    private Long totalCount;

    /** 平均效率值（小时） */
    private Double avgEfficiency;
}
