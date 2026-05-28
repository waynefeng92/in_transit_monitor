package com.company.roro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.roro.entity.ExcelParseConfig;
import com.company.roro.mapper.ExcelParseConfigMapper;
import com.company.roro.service.ExcelParseConfigService;
import org.springframework.stereotype.Service;

@Service
public class ExcelParseConfigServiceImpl
        extends ServiceImpl<ExcelParseConfigMapper, ExcelParseConfig>
        implements ExcelParseConfigService {

    @Override
    public ExcelParseConfig getByBrandId(Integer brandId) {
        // 先查品牌专属配置
        ExcelParseConfig config = lambdaQuery()
                .eq(ExcelParseConfig::getBrandId, brandId)
                .eq(ExcelParseConfig::getIsActive, 1)
                .one();

        // 没有则查默认配置
        if (config == null) {
            config = lambdaQuery()
                    .isNull(ExcelParseConfig::getBrandId)
                    .eq(ExcelParseConfig::getIsActive, 1)
                    .one();
        }

        return config;
    }
}