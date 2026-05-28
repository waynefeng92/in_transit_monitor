package com.company.roro.dto;

import lombok.Data;

/**
 * 已到达车辆明细 DTO
 *
 * 作用：返回已到达车辆列表中的单条车辆数据
 *
 * 示例数据：
 * {
 *   "vehicleId": "VIN123456789",
 *   "brandName": "宝马",
 *   "transportStatus": "水路运输",
 *   "arriveShopTime": "2026-05-26 10:30:00",
 *   "orderReleaseTime": "2026-05-26 14:00:00",
 *   "efficiency": 3.5,
 *   "efficiencyBucket": "EFFICIENT"
 * }
 */
@Data
public class ArrivedVehicleDTO {

    /** 车辆 VIN 码 */
    private String vehicleId;

    /** 品牌名称 */
    private String brandName;

    /** 运输状态（中文） */
    private String transportStatus;

    /** 到店时间 */
    private String arriveShopTime;

    /** 放单时间 */
    private String orderReleaseTime;

    /** 效率值（小时） */
    private Double efficiency;

    /** 效率分桶：EFFICIENT / NORMAL / DELAYED */
    private String efficiencyBucket;
}
