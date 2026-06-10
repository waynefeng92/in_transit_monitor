package com.company.roro.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量保存配置请求
 */
@Data
public class BatchSaveMappingRequest {

    private Integer brandId;

    private List<ExcelMappingDTO> mappings;
}