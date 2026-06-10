package com.company.roro.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RouteImportResultDTO {

    private int successCount;

    private int failCount;

    private int skipCount;

    private List<String> failDetails = new ArrayList<>();
}