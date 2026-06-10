package com.company.roro.dto;

import lombok.Data;

@Data
public class RouteImportRowDTO {

    private int rowNum;

    private String brandName;

    private String originCity;

    private String originPortName;

    private String destPortName;

    private String destCity;

    private boolean brandExists;

    private boolean originPortExists;

    private boolean destPortExists;

    private boolean canImport;
}