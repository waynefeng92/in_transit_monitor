package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 标准字段信息
 */
@Data
@AllArgsConstructor
@ApiModel("标准字段信息")
public class StandardFieldDTO {

    @ApiModelProperty("标准字段名（英文）")
    private String fieldName;

    @ApiModelProperty("标准字段中文名")
    private String fieldLabel;

    @ApiModelProperty("字段类型：STRING/DATE/DATETIME")
    private String fieldType;

    @ApiModelProperty("是否时间类型")
    private Boolean isDateType;
}