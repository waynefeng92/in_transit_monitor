package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("OTD配置导入DTO")
public class OtdConfigImportDTO {

    @ApiModelProperty("线路ID")
    private Integer routeId;

    @ApiModelProperty("品牌名称")
    private String brandName;

    @ApiModelProperty("出发地")
    private String originCity;

    @ApiModelProperty("目的地")
    private String destCity;

    // 7段标准OTD
    private Integer notDepartedOtd;
    private Integer toPortOtd;
    private Integer atPortWaitOtd;
    private Integer onSeaOtd;
    private Integer atDestWaitOtd;
    private Integer unloadWaitDispatchOtd;
    private Integer dispatchingOtd;

    // 7段预警时效
    private Integer notDepartedWarn;
    private Integer toPortWarn;
    private Integer atPortWaitWarn;
    private Integer onSeaWarn;
    private Integer atDestWaitWarn;
    private Integer unloadWaitDispatchWarn;
    private Integer dispatchingWarn;
}