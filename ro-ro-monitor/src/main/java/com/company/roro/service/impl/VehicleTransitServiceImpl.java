package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.VehicleTransit;
import com.company.roro.mapper.VehicleTransitMapper;
import com.company.roro.service.VehicleTransitService;
import org.springframework.stereotype.Service;

@Service
public class VehicleTransitServiceImpl extends ServiceImpl<VehicleTransitMapper, VehicleTransit> implements VehicleTransitService {
}