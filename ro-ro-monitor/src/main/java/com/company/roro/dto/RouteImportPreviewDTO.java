package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
@ApiModel("线路导入预览结果")
public class RouteImportPreviewDTO {

    @ApiModelProperty("数据行列表")
    private List<RouteImportRowDTO> rows;

    @ApiModelProperty("缺失的品牌")
    private List<String> missingBrands;

    @ApiModelProperty("缺失的港口")
    private List<String> missingPorts;

    @ApiModelProperty("可导入数量")
    private int canImportCount;

    @ApiModelProperty("不可导入数量")
    private int cannotImportCount;
}