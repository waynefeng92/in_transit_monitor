package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.MonitorStatusDict;
import com.company.roro.mapper.MonitorStatusDictMapper;
import com.company.roro.service.MonitorStatusDictService;
import org.springframework.stereotype.Service;

@Service
public class MonitorStatusDictServiceImpl extends ServiceImpl<MonitorStatusDictMapper, MonitorStatusDict> implements MonitorStatusDictService {
}