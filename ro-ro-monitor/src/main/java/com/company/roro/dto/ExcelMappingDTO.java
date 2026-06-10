package com.company.roro.dto;

import lombok.Data;

/**
 * Excel 字段映射配置 DTO
 */
@Data
public class ExcelMappingDTO {

    private Integer id;

    private Integer brandId;

    private String brandName;

    private String standardField;

    private String standardFieldName;

    private String excelColumnNames;

    private String dateFormat;

    private Integer isRequired;

    private String defaultValue;

    private Integer sortOrder;

    private Integer isActive;
}