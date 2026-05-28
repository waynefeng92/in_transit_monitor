package com.company.roro.dto;

import lombok.Data;

/**
 * 整段监控图表数据 DTO — 品牌维度的整体监控状态统计
 *
 * 作用：返回给前端监控大屏的图表数据格式（整体维度，不含在途状态）
 *
 * 示例数据：
 * {
 *   "brand": "宝马",
 *   "normal": 120,
 *   "warn": 5,
 *   "overdue": 2
 * }
 */
@Data
public class OverallChartDataDTO {

    /** 品牌名称 */
    private String brand;

    /** 整段监控正常数量 */
    private Long normal;

    /** 整段监控预警数量 */
    private Long warn;

    /** 整段监控超期数量 */
    private Long overdue;
}
