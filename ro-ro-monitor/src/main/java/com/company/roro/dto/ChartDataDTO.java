package com.company.roro.dto;

import lombok.Data;

/**
 * 图表数据 DTO
 *
 * 作用：返回给前端监控大屏的图表数据格式
 *
 * 对应需求：
 * "图表要体现每个品牌、每种在途状态下，三种监控状态各有多少车"
 *
 * 示例数据：
 * {
 *   "brand": "宝马",
 *   "transportStatus": "水运在途",
 *   "normal": 120,
 *   "warn": 5,
 *   "overdue": 2
 * }
 */
@Data
public class ChartDataDTO {

    /** 品牌名称 */
    private String brand;

    /** 在途状态名称（中文） */
    private String transportStatus;

    /** 正常状态的车辆数量 */
    private Long normal;

    /** 预警状态的车辆数量 */
    private Long warn;

    /** 已超期状态的车辆数量 */
    private Long overdue;
}