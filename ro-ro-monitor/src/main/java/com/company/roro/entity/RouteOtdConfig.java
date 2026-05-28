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

    // ==================== 7段标准OTD时效（单位：小时） ====================

    /** 未出库 → 集港在途 标准时效 */
    private Integer notDepartedOtd;

    /** 集港在途 → 已集港待装船 标准时效 */
    private Integer toPortOtd;

    /** 已集港待装船 → 水运在途 标准时效 */
    private Integer atPortWaitOtd;

    /** 水运在途 → 已到港待卸船 标准时效 */
    private Integer onSeaOtd;

    /** 已到港待卸船 → 已卸船待分拨 标准时效 */
    private Integer atDestWaitOtd;

    /** 已卸船待分拨 → 分拨在途 标准时效 */
    private Integer unloadWaitDispatchOtd;

    /** 分拨在途 → 已到达 标准时效 */
    private Integer dispatchingOtd;

    // ==================== 7段预警时效（单位：小时） ====================

    /** 未出库预警时效 */
    private Integer notDepartedWarn;

    /** 集港在途预警时效 */
    private Integer toPortWarn;

    /** 已集港待装船预警时效 */
    private Integer atPortWaitWarn;

    /** 水运在途预警时效 */
    private Integer onSeaWarn;

    /** 已到港待卸船预警时效 */
    private Integer atDestWaitWarn;

    /** 已卸船待分拨预警时效 */
    private Integer unloadWaitDispatchWarn;

    /** 分拨在途预警时效 */
    private Integer dispatchingWarn;

    /** 是否启用：1启用，0禁用 */
    private Integer isActive;
}