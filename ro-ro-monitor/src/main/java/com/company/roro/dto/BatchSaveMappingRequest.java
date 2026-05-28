package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 批量保存配置请求
 */
@Data
@ApiModel("批量保存配置请求")
public class BatchSaveMappingRequest {

    @ApiModelProperty(value = "品牌ID", required = true)
    private Integer brandId;

    @ApiModelProperty(value = "配置列表", required = true)
    private List<ExcelMappingDTO> mappings;
}