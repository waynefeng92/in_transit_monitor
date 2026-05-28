package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("线路导入行数据")
public class RouteImportRowDTO {

    @ApiModelProperty("Excel行号")
    private int rowNum;

    @ApiModelProperty("品牌名称")
    private String brandName;

    @ApiModelProperty("出发地")
    private String originCity;

    @ApiModelProperty("出发港名称")
    private String originPortName;

    @ApiModelProperty("目的港名称")
    private String destPortName;

    @ApiModelProperty("目的地")
    private String destCity;

    @ApiModelProperty("品牌是否存在")
    private boolean brandExists;

    @ApiModelProperty("出发港是否存在")
    private boolean originPortExists;

    @ApiModelProperty("目的港是否存在")
    private boolean destPortExists;

    @ApiModelProperty("是否可导入")
    private boolean canImport;
}