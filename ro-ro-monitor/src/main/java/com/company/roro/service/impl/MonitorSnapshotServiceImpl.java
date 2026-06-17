package com.company.roro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.MonitorSnapshot;
import com.company.roro.mapper.MonitorSnapshotMapper;
import com.company.roro.service.MonitorSnapshotService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitorSnapshotServiceImpl extends ServiceImpl<MonitorSnapshotMapper, MonitorSnapshot>
        implements MonitorSnapshotService {

    @Override
    public List<MonitorSnapshot> listByTabAndTimeRange(String tabType, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<MonitorSnapshot> wrapper = new LambdaQueryWrapper<MonitorSnapshot>()
                .eq(MonitorSnapshot::getTabType, tabType);
        if (start != null) {
            wrapper.ge(MonitorSnapshot::getSnapshotAt, start);
        }
        if (end != null) {
            wrapper.le(MonitorSnapshot::getSnapshotAt, end);
        }
        wrapper.orderByAsc(MonitorSnapshot::getSnapshotAt);
        return list(wrapper);
    }
}
