package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.OrderInfo;
import com.company.roro.mapper.OrderInfoMapper;
import com.company.roro.service.OrderInfoService;
import org.springframework.stereotype.Service;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
}