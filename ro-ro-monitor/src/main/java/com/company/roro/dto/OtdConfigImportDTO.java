package com.company.roro.dto;

import lombok.Data;

@Data
public class OtdConfigImportDTO {

    private Integer routeId;

    private String brandName;

    private String originCity;

    private String destCity;

    // 7段标准OTD（单位：小时，支持小数）
    private Double notDepartedOtd;
    private Double toPortOtd;
    private Double atPortWaitOtd;
    private Double onSeaOtd;
    private Double atDestWaitOtd;
    private Double unloadWaitDispatchOtd;
    private Double dispatchingOtd;

    // 7段预警时效（单位：小时，支持小数）
    private Double notDepartedWarn;
    private Double toPortWarn;
    private Double atPortWaitWarn;
    private Double onSeaWarn;
    private Double atDestWaitWarn;
    private Double unloadWaitDispatchWarn;
    private Double dispatchingWarn;
}