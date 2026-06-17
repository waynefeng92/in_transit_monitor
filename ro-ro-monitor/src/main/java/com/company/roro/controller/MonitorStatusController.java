package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.entity.MonitorStatusDict;
import com.company.roro.service.MonitorStatusDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 监控状态字典接口
 *
 * 功能：提供3种监控状态的查询
 *
 * 3种状态：
 * - NORMAL  ：正常
 * - WARN    ：预警
 * - OVERDUE ：已超期
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/monitor-status")
public class MonitorStatusController {

    @Autowired
    private MonitorStatusDictService monitorStatusDictService;

    /**
     * 查询所有监控状态
     *
     * @return 监控状态列表（正常、预警、已超期）
     */
    @GetMapping("/list")
    public Result<List<MonitorStatusDict>> list() {
        return Result.success(monitorStatusDictService.list());
    }
}
