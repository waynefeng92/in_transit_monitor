package com.company.roro.service;

import com.company.roro.dto.ExcelRowDTO;
import com.company.roro.dto.ImportResultDTO;

import java.util.List;

/**
 * 在途数据处理服务
 */
public interface TransitDataService {

    /**
     * 处理 Excel 解析后的数据，入库
     *
     * @param rows 解析后的数据行
     * @param batchId 批次号
     * @return 导入结果统计（总数、成功数、失败数、路线匹配数等）
     */
    ImportResultDTO processExcelData(List<ExcelRowDTO> rows, String batchId);
}