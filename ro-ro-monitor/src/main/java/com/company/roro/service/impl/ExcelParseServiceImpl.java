package com.company.roro.service.impl;

import cn.hutool.core.util.StrUtil;
import com.company.roro.dto.ExcelPreviewDTO;
import com.company.roro.dto.ExcelRowDTO;
import com.company.roro.dto.SheetInfoDTO;
import com.company.roro.entity.BrandDict;
import com.company.roro.entity.ExcelFieldMapping;
import com.company.roro.entity.ExcelParseConfig;
import com.company.roro.service.BrandDictService;
import com.company.roro.service.ExcelFieldMappingService;
import com.company.roro.service.ExcelParseConfigService;
import com.company.roro.service.ExcelParseService;
import com.company.roro.util.ExcelParseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 解析服务实现类
 *
 * 功能：
 * 1. 预览 Excel 文件（获取 Sheet 列表和预览数据）
 * 2. 解析 Excel 文件（根据配置解析数据行）
 *
 * @author roro-team
 */
@Slf4j
@Service
public class ExcelParseServiceImpl implements ExcelParseService {

    @Autowired
    private ExcelFieldMappingService excelFieldMappingService;

    @Autowired
    private ExcelParseConfigService excelParseConfigService;

    @Autowired
    private BrandDictService brandDictService;

    /**
     * 预览 Excel 文件
     *
     * @param file Excel 文件
     * @param brandId 品牌ID
     * @return 预览信息（Sheet列表、表头、前5行数据）
     */
    @Override
    public ExcelPreviewDTO previewExcel(MultipartFile file, Integer brandId, Integer sheetIndex) throws Exception {
        ExcelPreviewDTO previewDTO = new ExcelPreviewDTO();
        previewDTO.setFileName(file.getOriginalFilename());

        ZipSecureFile.setMinInflateRatio(0.001);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            // 1. 获取所有 Sheet 信息
            List<SheetInfoDTO> sheets = ExcelParseUtil.getSheetInfo(workbook);
            previewDTO.setSheets(sheets);

            // 2. 获取解析配置
            ExcelParseConfig config = excelParseConfigService.getByBrandId(brandId);
            int headerRowIndex = config != null ? config.getHeaderRowIndex() : 0;

            // 3. 定位目标 Sheet（优先使用用户选择的，否则用配置）
            Sheet targetSheet = ExcelParseUtil.locateSheet(workbook, config, sheetIndex);

            if (targetSheet != null) {
                int actualSheetIndex = workbook.getSheetIndex(targetSheet);
                previewDTO.setDefaultSheetIndex(actualSheetIndex);

                // 4. 获取表头行
                List<String> headerRow = ExcelParseUtil.getHeaderRow(targetSheet, headerRowIndex);
                previewDTO.setHeaderRow(headerRow);

                // 5. 获取预览数据
                List<List<String>> previewData = ExcelParseUtil.getPreviewData(targetSheet, headerRowIndex);
                previewDTO.setPreviewData(previewData);
            }
        }

        return previewDTO;
    }

    /**
     * 解析 Excel 文件
     *
     * @param file Excel 文件
     * @param brandId 品牌ID
     * @param sheetIndex 用户选择的 Sheet 索引（可选，null表示使用配置）
     * @return 解析后的数据行列表
     */
    @Override
    public List<ExcelRowDTO> parseExcel(MultipartFile file, Integer brandId, Integer sheetIndex) throws Exception {
        List<ExcelRowDTO> result = new ArrayList<>();

        // 获取该品牌的字段映射配置
        List<ExcelFieldMapping> mappings = getFieldMappings(brandId);
        if (mappings.isEmpty()) {
            throw new RuntimeException("品牌 " + brandId + " 未找到字段映射配置");
        }

        // 获取解析配置
        ExcelParseConfig config = excelParseConfigService.getByBrandId(brandId);
        int headerRowIndex = config != null ? config.getHeaderRowIndex() : 0;
        int dataStartRowIndex = config != null ? config.getDataStartRowIndex() : 1;

        ZipSecureFile.setMinInflateRatio(0.001);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            // 定位目标 Sheet
            Sheet sheet = ExcelParseUtil.locateSheet(workbook, config, sheetIndex);
            if (sheet == null) {
                throw new RuntimeException("未找到有效的 Sheet");
            }

            // 解析表头
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                throw new RuntimeException("表头行为空");
            }
            Map<String, Integer> headerMap = ExcelParseUtil.parseHeader(headerRow);

            log.info("===== Excel 实际表头 =====");
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                log.info("  [{}]", entry.getKey());
            }
            log.info("=========================");

            // 逐行解析数据
            for (int i = dataStartRowIndex; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                ExcelRowDTO dto = ExcelParseUtil.parseRow(row, headerMap, mappings);

                // 验证必填字段
                String validationError = ExcelParseUtil.validateRequired(dto, mappings);
                if (validationError != null) {
                    log.error("第 {} 行验证失败: {}", i + 1, validationError);
                    continue;
                }

                // 如果没有品牌名称，尝试根据 VIN 前三位自动匹配
                if (StrUtil.isBlank(dto.getBrandName()) && StrUtil.isNotBlank(dto.getVin())) {
                    String wmi = dto.getVin().substring(0, 3);
                    BrandDict brand = brandDictService.lambdaQuery()
                            .eq(BrandDict::getWmiCode, wmi)
                            .one();
                    if (brand != null) {
                        dto.setBrandName(brand.getBrandName());
                    }
                }

                result.add(dto);
            }

            // 设置品牌名称：优先使用品牌ID查找，比WMI检测更可靠
            if (brandId != null) {
                BrandDict brand = brandDictService.getById(brandId);
                if (brand != null) {
                    for (ExcelRowDTO row : result) {
                        if (StrUtil.isBlank(row.getBrandName())) {
                            row.setBrandName(brand.getBrandName());
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * 获取字段映射配置
     *
     * 优先使用品牌专属配置，其次使用默认配置
     *
     * @param brandId 品牌ID
     * @return 字段映射配置列表
     */
    private List<ExcelFieldMapping> getFieldMappings(Integer brandId) {
        // 查询品牌专属配置 + 默认配置
        List<ExcelFieldMapping> mappings = excelFieldMappingService.lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(ExcelFieldMapping::getBrandId, brandId)
                        .or()
                        .isNull(ExcelFieldMapping::getBrandId))
                .eq(ExcelFieldMapping::getIsActive, 1)
                .orderByAsc(ExcelFieldMapping::getSortOrder)
                .list();

        // 按品牌专属优先去重（同一 standardField 保留品牌专属的）
        Map<String, ExcelFieldMapping> dedupMap = new java.util.LinkedHashMap<>();
        for (ExcelFieldMapping mapping : mappings) {
            String key = mapping.getStandardField();
            ExcelFieldMapping existing = dedupMap.get(key);
            // 品牌专属优先于默认
            if (existing == null || mapping.getBrandId() != null) {
                dedupMap.put(key, mapping);
            }
        }

        return new ArrayList<>(dedupMap.values());
    }
}