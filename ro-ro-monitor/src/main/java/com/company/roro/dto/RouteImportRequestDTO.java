package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("线路导入请求")
public class RouteImportRequestDTO {

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
}
