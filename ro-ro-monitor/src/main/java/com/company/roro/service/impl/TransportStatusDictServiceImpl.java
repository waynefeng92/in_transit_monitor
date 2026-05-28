package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.TransportStatusDict;
import com.company.roro.mapper.TransportStatusDictMapper;
import com.company.roro.service.TransportStatusDictService;
import org.springframework.stereotype.Service;

@Service
public class TransportStatusDictServiceImpl extends ServiceImpl<TransportStatusDictMapper, TransportStatusDict> implements TransportStatusDictService {
}