package com.company.roro.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExcelRowDTO {

    // ==================== 基础信息 ====================

    /** 车架号（VIN码） */
    private String vin;

    /** 品牌名称 */
    private String brandName;

    /** 订单释放时间 */
    private LocalDateTime orderReleaseTime;

    /** 出发地城市 */
    private String originCity;

    /** 目的地城市 */
    private String destCity;

    // ==================== 关键时间节点 ====================

    /** 出库时间：车辆离开仓库的时间 */
    private LocalDateTime departWarehouseTime;

    /** 集港到港时间 */
    private LocalDateTime arrivePortTime;

    /** 船离始发港时间 */
    private LocalDateTime shipDepartTime;

    /** 船到目的港时间 */
    private LocalDateTime shipArriveTime;

    /** 卸船完成时间 */
    private LocalDateTime unloadFinishTime;

    /** 分拨时间 */
    private LocalDateTime dispatchTime;

    /** 到店时间 */
    private LocalDateTime arriveShopTime;
}