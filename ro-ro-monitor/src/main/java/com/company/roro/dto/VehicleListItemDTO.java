package com.company.roro.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 车辆列表项 DTO — 监控大屏品牌/状态/监控状态钻取结果
 *
 * <p>用于品牌状态图表点击钻取后展示匹配车辆的列表。</p>
 *
 * <h3>示例数据</h3>
 * <pre>{@code
 * {
 *   "vin": "LSVAU2A36N2112345",
 *   "brandName": "上汽名爵",
 *   "transportStatus": "ON_SEA",
 *   "transportStatusName": "水运在途",
 *   "monitorStatus": "NORMAL",
 *   "originCity": "上海",
 *   "destCity": "广州",
 *   "orderReleaseTime": "2026-06-20T08:00:00"
 * }
 * }</pre>
 */
@Data
public class VehicleListItemDTO {

    /** VIN 车架号 */
    private String vin;

    /** 品牌名称 */
    private String brandName;

    /** 在途状态码 */
    private String transportStatus;

    /** 在途状态中文名 */
    private String transportStatusName;

    /** 监控状态：NORMAL / WARN / OVERDUE */
    private String monitorStatus;

    /** 出发城市 */
    private String originCity;

    /** 目的城市 */
    private String destCity;

    /** 订单释放时间 */
    private LocalDateTime orderReleaseTime;
}
