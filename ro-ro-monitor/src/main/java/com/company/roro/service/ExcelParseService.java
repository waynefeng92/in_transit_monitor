package com.company.roro.service;

import com.company.roro.dto.ExcelPreviewDTO;
import com.company.roro.dto.ExcelRowDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Excel 解析服务接口
 */
public interface ExcelParseService {

    /**
     * 预览 Excel 文件（获取 Sheet 列表和前5行数据）
     *
     * @param file Excel 文件
     * @param brandId 品牌ID
     * @return 预览信息
     */
    ExcelPreviewDTO previewExcel(MultipartFile file, Integer brandId, Integer sheetIndex) throws Exception;

    /**
     * 解析 Excel 文件
     *
     * @param file Excel 文件
     * @param brandId 品牌ID
     * @param sheetIndex 用户选择的 Sheet 索引（可选，null表示使用配置）
     * @return 解析后的数据行列表
     */
    List<ExcelRowDTO> parseExcel(MultipartFile file, Integer brandId, Integer sheetIndex) throws Exception;
}