package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Excel 字段映射配置 DTO
 */
@Data
@ApiModel("Excel字段映射配置")
public class ExcelMappingDTO {

    @ApiModelProperty("配置ID")
    private Integer id;

    @ApiModelProperty("品牌ID")
    private Integer brandId;

    @ApiModelProperty("品牌名称")
    private String brandName;

    @ApiModelProperty("标准字段名")
    private String standardField;

    @ApiModelProperty("标准字段中文名")
    private String standardFieldName;

    @ApiModelProperty("Excel表头名称，多个用逗号分隔")
    private String excelColumnNames;

    @ApiModelProperty("时间格式")
    private String dateFormat;

    @ApiModelProperty("是否必填：1必填，0可选")
    private Integer isRequired;

    @ApiModelProperty("默认值")
    private String defaultValue;

    @ApiModelProperty("排序")
    private Integer sortOrder;

    @ApiModelProperty("是否启用：1启用，0禁用")
    private Integer isActive;
}