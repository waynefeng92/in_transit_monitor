package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.RouteDict;
import com.company.roro.mapper.RouteDictMapper;
import com.company.roro.service.RouteDictService;
import org.springframework.stereotype.Service;

@Service
public class RouteDictServiceImpl extends ServiceImpl<RouteDictMapper, RouteDict> implements RouteDictService {
}