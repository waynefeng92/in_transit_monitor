package com.company.roro.dto;

import lombok.Data;

import java.util.List;

/**
 * 已到达车辆图表数据 DTO
 *
 * 作用：返回给前端已到达监控页面的品牌 × 效率分桶矩阵数据
 *
 * 对应需求：
 * "图表要体现每个品牌在三种效率分桶（EFFICIENT / NORMAL / DELAYED）下各有多少车"
 *
 * 示例数据：
 * {
 *   "brands": ["宝马", "奔驰", "奥迪"],
 *   "categories": ["EFFICIENT", "NORMAL", "DELAYED"],
 *   "data": [
 *     [120, 30, 5],
 *     [200, 50, 10],
 *     [150, 40, 8]
 *   ]
 * }
 */
@Data
public class ArrivedChartDataDTO {

    /** 品牌名称列表 */
    private List<String> brands;

    /** 效率分桶类别列表 */
    private List<String> categories;

    /** 品牌 × 分桶的计数矩阵，data[i][j] 表示 brands[i] 在 categories[j] 分桶中的车辆数 */
    private List<List<Integer>> data;
}
