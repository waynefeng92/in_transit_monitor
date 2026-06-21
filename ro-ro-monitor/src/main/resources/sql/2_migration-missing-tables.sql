CREATE TABLE IF NOT EXISTS `monitor_snapshot` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `snapshot_at` DATETIME NOT NULL COMMENT '快照时间',
    `tab_type` VARCHAR(50) NOT NULL COMMENT 'tab类型：segment/overall/three-section',
    `summary_json` TEXT COMMENT '汇总数据JSON',
    `chart_json` TEXT COMMENT '图表数据JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监控数据快照';

CREATE TABLE IF NOT EXISTS `location_alias` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `standard_name` VARCHAR(100) NOT NULL COMMENT '标准名称',
    `alias` VARCHAR(200) NOT NULL COMMENT '别名',
    UNIQUE KEY `uk_standard_alias` (`standard_name`, `alias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地点别名表';
