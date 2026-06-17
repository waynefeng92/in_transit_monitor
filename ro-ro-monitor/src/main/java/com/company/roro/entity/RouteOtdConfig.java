package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OTD时效配置表实体类
 *
 * 对应表：route_otd_config
 */
@Data
@TableName("route_otd_config")
public class RouteOtdConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 线路ID，关联 route_dict 表 */
    private Integer routeId;

    // ==================== 7段标准OTD时效（单位：小时，支持小数） ====================

    /** 未出库 → 集港在途 标准时效 */
    private Double notDepartedOtd;

    /** 集港在途 → 已集港待装船 标准时效 */
    private Double toPortOtd;

    /** 已集港待装船 → 水运在途 标准时效 */
    private Double atPortWaitOtd;

    /** 水运在途 → 已到港待卸船 标准时效 */
    private Double onSeaOtd;

    /** 已到港待卸船 → 已卸船待分拨 标准时效 */
    private Double atDestWaitOtd;

    /** 已卸船待分拨 → 分拨在途 标准时效 */
    private Double unloadWaitDispatchOtd;

    /** 分拨在途 → 已到达 标准时效 */
    private Double dispatchingOtd;

    // ==================== 7段预警时效（单位：小时，支持小数） ====================

    /** 未出库预警时效 */
    private Double notDepartedWarn;

    /** 集港在途预警时效 */
    private Double toPortWarn;

    /** 已集港待装船预警时效 */
    private Double atPortWaitWarn;

    /** 水运在途预警时效 */
    private Double onSeaWarn;

    /** 已到港待卸船预警时效 */
    private Double atDestWaitWarn;

    /** 已卸船待分拨预警时效 */
    private Double unloadWaitDispatchWarn;

    /** 分拨在途预警时效 */
    private Double dispatchingWarn;

    /** 是否启用：1启用，0禁用 */
    private Integer isActive;
}