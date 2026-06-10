package com.company.roro.dto;

import lombok.Data;

@Data
public class RouteImportRequestDTO {

    private String brandName;

    private String originCity;

    private String originPortName;

    private String destPortName;

    private String destCity;
}
