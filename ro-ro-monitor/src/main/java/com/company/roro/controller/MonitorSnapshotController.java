package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.entity.MonitorSnapshot;
import com.company.roro.scheduler.SnapshotScheduler;
import com.company.roro.service.MonitorSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监控快照查询接口
 *
 * 用于历史回放：按tab类型和时间范围查询已保存的快照数据。
 */
@RestController
@RequestMapping("/api/snapshots")
public class MonitorSnapshotController {

    @Autowired
    private MonitorSnapshotService snapshotService;

    @Autowired
    private SnapshotScheduler snapshotScheduler;

    /**
     * 查询快照列表
     *
     * @param tabType   tab类型（segment / overall / three-section）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 快照列表，按快照时间升序
     */
    @GetMapping
    public Result<List<MonitorSnapshot>> list(
            @RequestParam(name = "tabType") String tabType,
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(snapshotService.listByTabAndTimeRange(tabType, startTime, endTime));
    }

    @PostMapping("/trigger")
    public Result<String> trigger() {
        snapshotScheduler.captureSnapshots();
        return Result.success("Snapshots captured");
    }
}
