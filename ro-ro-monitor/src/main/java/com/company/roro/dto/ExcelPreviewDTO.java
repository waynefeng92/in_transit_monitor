package com.company.roro.dto;

import lombok.Data;
import java.util.List;

/**
 * Excel 预览信息 DTO
 */
@Data
public class ExcelPreviewDTO {

    private String fileName;

    private List<SheetInfoDTO> sheets;

    private Integer defaultSheetIndex;

    private List<List<String>> previewData;

    private List<String> headerRow;
}