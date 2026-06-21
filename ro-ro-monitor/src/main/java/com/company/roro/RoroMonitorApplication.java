package com.company.roro;

import com.company.roro.config.MonitorConfig;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 滚装船在途监控系统 - 启动类
 *
 * 功能说明：
 * 1. 监控滚装船运输车辆的在途状态
 * 2. 支持 Excel 批量上传在途数据
 * 3. 提供监控大屏图表数据接口
 * 4. 定时抓取图表快照，支持历史回放
 *
 * 技术栈：
 * - Spring Boot 2.7
 * - MyBatis-Plus
 * - MySQL 8.0
 * - EasyExcel
 *
 * @author roro-team
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(MonitorConfig.class)
@MapperScan({"com.company.roro.mapper", "com.company.roro.repository"})  // 扫描 Mapper 和 Repository 接口
@EnableScheduling                        // 开启定时任务支持
public class RoroMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoroMonitorApplication.class, args);
        log.info("========================================");
        log.info("  滚装船在途监控系统启动成功！");
        log.info("  访问地址: http://localhost:8080");
        log.info("========================================");
    }
}