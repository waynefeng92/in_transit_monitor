package com.company.roro.dto;

import lombok.Data;
import java.util.List;

@Data
public class RouteImportPreviewDTO {

    private List<RouteImportRowDTO> rows;

    private List<String> missingBrands;

    private List<String> missingPorts;

    private int canImportCount;

    private int cannotImportCount;
}