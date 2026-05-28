package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 复制配置请求
 */
@Data
@ApiModel("复制配置请求")
public class CopyMappingRequest {

    @ApiModelProperty(value = "源品牌ID", required = true, example = "1")
    private Integer sourceBrandId;

    @ApiModelProperty(value = "目标品牌ID", required = true, example = "2")
    private Integer targetBrandId;
}