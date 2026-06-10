package com.company.roro.dto;

import lombok.Data;

@Data
public class OtdConfigExportDTO {

    private Integer routeId;

    private String brandName;

    private String originCity;

    private String originPortName;

    private String destPortName;

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