package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * Excel 预览信息 DTO
 */
@Data
@ApiModel("Excel预览信息")
public class ExcelPreviewDTO {

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("Sheet列表")
    private List<SheetInfoDTO> sheets;

    @ApiModelProperty("默认选中的Sheet索引")
    private Integer defaultSheetIndex;

    @ApiModelProperty("预览数据（前5行）")
    private List<List<String>> previewData;

    @ApiModelProperty("表头行（第一行）")
    private List<String> headerRow;
}