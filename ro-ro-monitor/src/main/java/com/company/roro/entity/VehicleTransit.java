package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 在途状态表实体类
 *
 * 对应表：vehicle_transit
 */
@Data
@TableName("vehicle_transit")
public class VehicleTransit {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 订单ID，关联 order_info 表 */
    private Integer orderId;

    /** 出库时间：车辆离开仓库的时间 */
    private LocalDateTime departWarehouseTime;

    // ==================== 关键时间节点 ====================

    /** 集港到港时间：车辆到达始发港口的时间 */
    private LocalDateTime arrivePortTime;

    /** 船离始发港时间：船舶离开始发港口的时间 */
    private LocalDateTime shipDepartTime;

    /** 船到目的港时间：船舶到达目的港口的时间 */
    private LocalDateTime shipArriveTime;

    /** 卸船完成时间：车辆从船上卸载完成的时间 */
    private LocalDateTime unloadFinishTime;

    /** 分拨时间：车辆开始从港口分拨的时间 */
    private LocalDateTime dispatchTime;

    /** 到店时间：车辆到达经销商/4S店的时间 */
    private LocalDateTime arriveShopTime;

    // ==================== 状态字段 ====================

    /** 在途状态：NOT_DEPARTED / TO_PORT / AT_PORT_WAIT_SHIP / ON_SEA / AT_DEST_WAIT_UNLOAD / UNLOADED_WAIT_DISPATCH / DISPATCHING / ARRIVED */
    private String transportStatus;

    /** 监控状态：NORMAL / WARN / OVERDUE */
    private String monitorStatus;

    /** 整段监控状态：NORMAL / WARN / OVERDUE */
    private String overallMonitorStatus;

    /** 三段监控状态：NORMAL / WARN / OVERDUE */
    private String sectionMonitorStatus;

    // ==================== 元数据 ====================

    /** 上传批次号 */
    private String batchId;

    /** 数据来源：EXCEL / MANUAL / API */
    private String dataSource;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}