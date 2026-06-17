package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监控数据快照
 *
 * 每4小时自动采集一次，保存各tab的汇总数据和图表数据JSON，
 * 用于历史回放和趋势分析。
 */
@Data
@TableName("monitor_snapshot")
public class MonitorSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快照时间 */
    private LocalDateTime snapshotAt;

    /** tab类型：segment | overall | three-section */
    private String tabType;

    /** 汇总数据JSON */
    private String summaryJson;

    /** 图表数据JSON */
    private String chartJson;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
