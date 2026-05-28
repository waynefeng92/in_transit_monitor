package com.company.roro.dto;

import lombok.Data;

/**
 * 在途监控汇总数据 DTO
 *
 * 作用：返回监控大屏顶部的汇总卡片数据
 *
 * 示例：
 * {
 *   "normal": 1520,
 *   "warn": 45,
 *   "overdue": 12,
 *   "total": 1577,
 *   "overallNormal": 1480,
 *   "overallWarn": 72,
 *   "overallOverdue": 25
 * }
 */
@Data
public class TransitSummaryDTO {

    /** 正常状态车辆数 */
    private Long normal;

    /** 预警状态车辆数 */
    private Long warn;

    /** 超期状态车辆数 */
    private Long overdue;

    /** 在途车辆总数（不含已到达） */
    private Long total;

    /** 整段监控正常数量 */
    private Long overallNormal;

    /** 整段监控预警数量 */
    private Long overallWarn;

    /** 整段监控超期数量 */
    private Long overallOverdue;

    /** 分段监控正常数量 */
    private Long sectionNormal;

    /** 分段监控预警数量 */
    private Long sectionWarn;

    /** 分段监控超期数量 */
    private Long sectionOverdue;
}