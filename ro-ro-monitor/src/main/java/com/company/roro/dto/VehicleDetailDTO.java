package com.company.roro.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆详情 DTO — 运输监控大屏右侧详情面板数据
 *
 * <p>包含车辆基本信息、整段时效汇总 + 7段详情。</p>
 *
 * <h3>字段来源映射</h3>
 * <ul>
 *   <li>{@code vin} — OrderInfo.vin</li>
 *   <li>{@code brandName} — BrandDict.brandName</li>
 *   <li>{@code originCity} — OrderInfo.originCity</li>
 *   <li>{@code destCity} — OrderInfo.destCity</li>
 *   <li>{@code routeName} — RouteDict (originCity-destCity)，nullable</li>
 *   <li>{@code transportStatus} — VehicleTransit.transportStatus</li>
 *   <li>{@code transportStatusName} — TransportStatusDict.statusName</li>
 *   <li>{@code orderReleaseTime} — OrderInfo.orderReleaseTime (段1锚点)</li>
 *   <li>{@code totalStandardOtdHours} — RouteOtdConfig 7 字段之和（无配置时为 null）</li>
 *   <li>{@code totalActualHours} — orderReleaseTime → arriveShopTime（在途未到达取 now）</li>
 *   <li>{@code segments} — 固定 7 段，按 1-7 排序</li>
 * </ul>
 *
 * <h3>示例数据</h3>
 * <pre>{@code
 * {
 *   "vin": "LSVAU2A36N2112345",
 *   "brandName": "上汽名爵",
 *   "originCity": "上海",
 *   "destCity": "广州",
 *   "routeName": "上海-广州",
 *   "transportStatus": "ON_SEA",
 *   "transportStatusName": "水运在途",
 *   "orderReleaseTime": "2026-06-20T08:00:00",
 *   "totalStandardOtdHours": 168.5,
 *   "totalActualHours": 72.3,
 *   "segments": [ ... ]
 * }
 * }</pre>
 */
@Data
public class VehicleDetailDTO {

    /** VIN 车架号 */
    private String vin;

    /** 品牌名称 */
    private String brandName;

    /** 出发城市 */
    private String originCity;

    /** 目的城市 */
    private String destCity;

    /** 线路名称（originCity-destCity），无对应线路时为 null */
    private String routeName;

    /** 在途状态码 */
    private String transportStatus;

    /** 在途状态中文名 */
    private String transportStatusName;

    /** 订单释放时间 — 段 1 的起点锚点 */
    private LocalDateTime orderReleaseTime;

    /** 整段标准 OTD 总小时（RouteOtdConfig 7 字段之和，无配置时为 null） */
    private Double totalStandardOtdHours;

    /** 整段实际耗时小时（orderReleaseTime → arriveShopTime，在途未到达取当前时间） */
    private Double totalActualHours;

    /** 7 段运输详情，固定长度 7，按 segmentIndex 1-7 排序 */
    private List<SegmentDetail> segments;

    // ==================== 内嵌类 ====================

    /**
     * 运输段详情 — 车辆详情 DTO 的嵌套内部类
     *
     * <p>每辆车固定 7 段：</p>
     * <ol>
     *   <li>未出库 — NOT_DEPARTED</li>
     *   <li>集港在途 — TO_PORT</li>
     *   <li>已集港待装船 — AT_PORT_WAIT_SHIP</li>
     *   <li>水运在途 — ON_SEA</li>
     *   <li>已到港待卸船 — AT_DEST_WAIT_UNLOAD</li>
     *   <li>已卸船待分拨 — UNLOADED_WAIT_DISPATCH</li>
     *   <li>分拨在途 — DISPATCHING</li>
     * </ol>
     *
     * <h3>段状态判定逻辑</h3>
     * <ul>
     *   <li>{@code NORMAL} — 实际耗时 ≤ 预警阈值</li>
     *   <li>{@code WARN} — 预警阈值 ＜ 实际耗时 ≤ OTD 标准</li>
     *   <li>{@code OVERDUE} — 实际耗时 ＞ OTD 标准</li>
     *   <li>{@code PENDING} — 段尚在进行中，未结束</li>
     *   <li>{@code N/A} — 无 OTD 配置，无法判定</li>
     * </ul>
     */
    @Data
    public static class SegmentDetail {

        /** 段序号 1-7 */
        private Integer segmentIndex;

        /** 段中文名 */
        private String segmentName;

        /** 段状态码 */
        private String statusCode;

        /** 段开始时间（段 1 = orderReleaseTime，其余为上一段时间戳） */
        private LocalDateTime startTime;

        /** 段结束时间（未到达该段时为 null，即进行中/未来） */
        private LocalDateTime endTime;

        /** 该段标准 OTD 小时（来自 RouteOtdConfig.*Otd 字段，无配置时为 null） */
        private Double standardOtdHours;

        /** 该段预警阈值小时（来自 RouteOtdConfig.*Warn 字段，无配置时为 null） */
        private Double warnThresholdHours;

        /** 该段实际耗时小时（endTime - startTime，endTime 为 null 时为 null） */
        private Double actualDurationHours;

        /** 段监控状态：NORMAL / WARN / OVERDUE / PENDING / N/A */
        private String status;
    }
}
