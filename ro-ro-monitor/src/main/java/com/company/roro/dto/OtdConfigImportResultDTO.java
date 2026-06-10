package com.company.roro.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class OtdConfigImportResultDTO {

    private int successCount;

    private int failCount;

    private List<String> failDetails = new ArrayList<>();
}