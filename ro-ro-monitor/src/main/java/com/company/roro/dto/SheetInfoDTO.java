package com.company.roro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sheet 信息 DTO
 *
 * 用于前端展示 Excel 文件中的 Sheet 列表，供用户选择
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SheetInfoDTO {

    private Integer index;

    private String name;

    private Integer rowCount;
}