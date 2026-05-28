package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.BrandDict;
import com.company.roro.mapper.BrandDictMapper;
import com.company.roro.service.BrandDictService;
import org.springframework.stereotype.Service;

@Service
public class BrandDictServiceImpl extends ServiceImpl<BrandDictMapper, BrandDict> implements BrandDictService {
}