package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.ExcelFieldMapping;
import com.company.roro.mapper.ExcelFieldMappingMapper;
import com.company.roro.service.ExcelFieldMappingService;
import org.springframework.stereotype.Service;

@Service
public class ExcelFieldMappingServiceImpl
        extends ServiceImpl<ExcelFieldMappingMapper, ExcelFieldMapping>
        implements ExcelFieldMappingService {
}