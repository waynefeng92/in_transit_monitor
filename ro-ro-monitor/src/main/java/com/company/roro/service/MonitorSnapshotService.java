package com.company.roro.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.roro.entity.MonitorSnapshot;

import java.time.LocalDateTime;
import java.util.List;

public interface MonitorSnapshotService extends IService<MonitorSnapshot> {

    /**
     * 按tab类型和时间范围查询快照
     *
     * @param tabType  tab类型（segment / overall / three-section）
     * @param start    开始时间（含）
     * @param end      结束时间（含）
     * @return 快照列表，按快照时间升序
     */
    List<MonitorSnapshot> listByTabAndTimeRange(String tabType, LocalDateTime start, LocalDateTime end);
}
