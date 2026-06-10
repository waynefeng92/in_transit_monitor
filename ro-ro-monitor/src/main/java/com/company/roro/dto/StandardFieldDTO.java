package com.company.roro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 标准字段信息
 */
@Data
@AllArgsConstructor
public class StandardFieldDTO {

    private String fieldName;

    private String fieldLabel;

    private String fieldType;

    private Boolean isDateType;
}