package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.LocationAlias;
import com.company.roro.mapper.LocationAliasMapper;
import com.company.roro.service.LocationAliasService;
import org.springframework.stereotype.Service;

@Service
public class LocationAliasServiceImpl extends ServiceImpl<LocationAliasMapper, LocationAlias> implements LocationAliasService {
}
