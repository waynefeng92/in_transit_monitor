package com.company.roro.dto;

import lombok.Data;

/**
 * 分段监控图表数据 DTO — 前三段（前段/中段/后段）的监控状态统计
 *
 * 作用：返回给前端监控大屏的图表数据格式（分段维度，不含品牌）
 *
 * 示例数据：
 * {
 *   "sectionName": "前段",
 *   "normal": 120,
 *   "warn": 5,
 *   "overdue": 2
 * }
 */
@Data
public class SectionChartDataDTO {

    /** 分段名称（前段/中段/后段） */
    private String sectionName;

    /** 正常状态的车辆数量 */
    private Long normal;

    /** 预警状态的车辆数量 */
    private Long warn;

    /** 已超期状态的车辆数量 */
    private Long overdue;
}
