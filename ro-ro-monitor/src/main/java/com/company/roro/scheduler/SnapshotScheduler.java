package com.company.roro.scheduler;

import com.company.roro.entity.MonitorSnapshot;
import com.company.roro.service.ChartDataService;
import com.company.roro.service.MonitorSnapshotService;
import com.company.roro.service.TransitSummaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 监控数据定时快照
 *
 * 每隔 N 小时自动采集三个 tab（segment / overall / three-section）的
 * 汇总数据和图表数据，保存到 monitor_snapshot 表，供历史回放使用。
 *
 * 采集间隔通过 application.yml 中的 monitor.snapshot.cron 配置。
 */
@Component
public class SnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(SnapshotScheduler.class);

    private static final String[] TAB_TYPES = {"segment", "overall", "three-section"};

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private TransitSummaryService transitSummaryService;

    @Autowired
    private ChartDataService chartDataService;

    @Autowired
    private MonitorSnapshotService snapshotService;

    /**
     * 每4小时采集一次（默认 cron），可通过 monitor.snapshot.cron 覆盖
     */
    @Scheduled(cron = "${monitor.snapshot.cron:0 0 */4 * * ?}")
    public void captureSnapshots() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting snapshot capture at {}", now);

        for (String tabType : TAB_TYPES) {
            try {
                MonitorSnapshot snapshot = new MonitorSnapshot();
                snapshot.setSnapshotAt(now);
                snapshot.setTabType(tabType);

                // 汇总数据
                Object summary = transitSummaryService.summary(null, null);
                snapshot.setSummaryJson(objectMapper.writeValueAsString(summary));

                // 图表数据（品牌-状态分组）
                Object chartData = chartDataService.getBrandStatusChart(null, null, tabType, null);
                snapshot.setChartJson(objectMapper.writeValueAsString(chartData));

                snapshot.setCreatedAt(LocalDateTime.now());
                snapshotService.save(snapshot);

                log.info("Snapshot saved: tabType={}, snapshotAt={}", tabType, now);
            } catch (Exception e) {
                log.error("Failed to capture snapshot for tab: {}, error: {}", tabType, e.getMessage(), e);
            }
        }

        log.info("Snapshot capture completed at {}", now);
    }
}
