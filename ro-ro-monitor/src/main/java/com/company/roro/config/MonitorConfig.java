package com.company.roro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 监控配置类
 *
 * 作用：
 * 1. 从 application.yml 中读取 monitor.* 配置项
 * 2. 提供全局告警阈值等监控参数
 *
 * 使用场景：
 * - 控制告警触发条件（如整体预警比例）
 */
@Data
@ConfigurationProperties(prefix = "monitor")
public class MonitorConfig {

    /**
     * 整体预警比例
     * 当在途车辆中异常/预警比例超过此阈值时触发告警
     * 默认值: 0.8 (即 80%)
     */
    private Double overallWarnRatio = 0.8;
}
