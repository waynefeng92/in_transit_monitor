package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.PortDict;
import com.company.roro.mapper.PortDictMapper;
import com.company.roro.service.PortDictService;
import org.springframework.stereotype.Service;

@Service
public class PortDictServiceImpl extends ServiceImpl<PortDictMapper, PortDict> implements PortDictService {
}