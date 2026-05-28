package com.company.roro.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.roro.entity.ExcelParseConfig;

public interface ExcelParseConfigService extends IService<ExcelParseConfig> {

    /**
     * 根据品牌ID获取解析配置（优先品牌专属，其次默认）
     */
    ExcelParseConfig getByBrandId(Integer brandId);
}