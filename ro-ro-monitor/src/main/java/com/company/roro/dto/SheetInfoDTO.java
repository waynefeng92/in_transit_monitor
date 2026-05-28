package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel("Sheet信息")
public class SheetInfoDTO {

    @ApiModelProperty("Sheet索引（从0开始）")
    private Integer index;

    @ApiModelProperty("Sheet名称")
    private String name;

    @ApiModelProperty("该Sheet的行数（预览）")
    private Integer rowCount;
}