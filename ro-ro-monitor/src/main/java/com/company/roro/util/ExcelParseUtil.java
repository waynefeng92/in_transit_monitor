package com.company.roro.util;

import cn.hutool.core.util.StrUtil;  // 只保留 StrUtil
import com.company.roro.dto.ExcelRowDTO;
import com.company.roro.dto.SheetInfoDTO;
import com.company.roro.entity.ExcelFieldMapping;
import com.company.roro.entity.ExcelParseConfig;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Excel 解析工具类（增强版）
 *
 * 功能：
 * 1. 获取 Excel 中的所有 Sheet 信息（供前端预览）
 * 2. 根据配置定位正确的 Sheet
 * 3. 根据字段映射配置动态解析数据
 *
 * 支持多种定位方式：
 * - NAME：按 Sheet 名称定位
 * - INDEX：按 Sheet 索引定位
 * - AUTO：智能识别（根据表头特征）
 *
 * @author roro-team
 */
public class ExcelParseUtil {

    /**
     * 获取 Excel 文件中的所有 Sheet 信息
     *
     * @param workbook Excel 工作簿
     * @return Sheet 信息列表
     */
    public static List<SheetInfoDTO> getSheetInfo(Workbook workbook) {
        List<SheetInfoDTO> sheets = new ArrayList<>();
        int numberOfSheets = workbook.getNumberOfSheets();

        for (int i = 0; i < numberOfSheets; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();
            int rowCount = sheet.getLastRowNum() + 1;
            sheets.add(new SheetInfoDTO(i, sheetName, rowCount));
        }

        return sheets;
    }

    /**
     * 获取预览数据（前5行）
     *
     * @param sheet Sheet 对象
     * @param headerRowIndex 表头行号（从0开始）
     * @return 预览数据，每行是一个字符串列表
     */
    public static List<List<String>> getPreviewData(Sheet sheet, int headerRowIndex) {
        List<List<String>> previewData = new ArrayList<>();

        // 预览5行数据（从数据起始行开始）
        int dataStartRow = headerRowIndex + 1;
        int endRow = Math.min(dataStartRow + 5, sheet.getLastRowNum() + 1);

        for (int i = dataStartRow; i < endRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            List<String> rowData = new ArrayList<>();
            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                rowData.add(getCellValueAsString(cell));
            }
            previewData.add(rowData);
        }

        return previewData;
    }

    /**
     * 获取表头行数据
     *
     * @param sheet Sheet 对象
     * @param headerRowIndex 表头行号（从0开始）
     * @return 表头数据
     */
    public static List<String> getHeaderRow(Sheet sheet, int headerRowIndex) {
        List<String> headerRow = new ArrayList<>();
        Row row = sheet.getRow(headerRowIndex);

        if (row != null) {
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                String value = getCellValueAsString(cell);
                headerRow.add(value != null ? value : "");
            }
        }

        return headerRow;
    }

    /**
     * 根据配置定位正确的 Sheet
     *
     * 优先级：
     * 1. 用户选择的 Sheet 索引（最高）
     * 2. 配置的定位规则（NAME/INDEX/AUTO）
     * 3. 第一个 Sheet（兜底）
     *
     * @param workbook Excel 工作簿
     * @param config 解析配置
     * @param userSelectedSheetIndex 用户选择的 Sheet 索引（可选，优先级最高）
     * @return 目标 Sheet
     */
    public static Sheet locateSheet(Workbook workbook, ExcelParseConfig config, Integer userSelectedSheetIndex) {
        // 1. 优先使用用户选择的 Sheet
        if (userSelectedSheetIndex != null && userSelectedSheetIndex >= 0
                && userSelectedSheetIndex < workbook.getNumberOfSheets()) {
            return workbook.getSheetAt(userSelectedSheetIndex);
        }

        // 2. 根据配置定位
        if (config != null) {
            String locateType = config.getSheetLocateType();

            if ("NAME".equals(locateType) && StrUtil.isNotBlank(config.getSheetName())) {
                // 按名称查找（精确匹配）
                Sheet sheet = workbook.getSheet(config.getSheetName());
                if (sheet != null) {
                    return sheet;
                }
                // 找不到时尝试忽略大小写匹配
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    String sheetName = workbook.getSheetName(i);
                    if (config.getSheetName().equalsIgnoreCase(sheetName)) {
                        return workbook.getSheetAt(i);
                    }
                }
            }

            if ("INDEX".equals(locateType)) {
                // 按索引查找
                int index = config.getSheetIndex() != null ? config.getSheetIndex() : 0;
                if (index >= 0 && index < workbook.getNumberOfSheets()) {
                    return workbook.getSheetAt(index);
                }
            }

            if ("AUTO".equals(locateType)) {
                // 智能识别：根据表头特征自动找到正确的 Sheet
                Sheet autoSheet = autoDetectSheet(workbook);
                if (autoSheet != null) {
                    return autoSheet;
                }
            }
        }

        // 3. 兜底：返回第一个 Sheet
        return workbook.getSheetAt(0);
    }

    /**
     * 智能识别正确的 Sheet
     *
     * 识别策略：找到包含"车架号"或"VIN"等关键词的 Sheet
     *
     * @param workbook Excel 工作簿
     * @return 识别出的 Sheet，找不到返回 null
     */
    private static Sheet autoDetectSheet(Workbook workbook) {
        List<String> vinKeywords = Arrays.asList("车架号", "VIN", "vin", "VIN码", "车架码");

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            Row headerRow = sheet.getRow(0);

            if (headerRow != null) {
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    String headerValue = getCellValueAsString(headerRow.getCell(j));
                    if (headerValue != null) {
                        for (String keyword : vinKeywords) {
                            if (headerValue.contains(keyword)) {
                                return sheet;
                            }
                        }
                    }
                }
            }
        }

        // 没找到，返回 null
        return null;
    }

    /**
     * 解析 Excel 行数据
     *
     * @param row Excel 行对象
     * @param headerMap 表头名称 → 列索引 的映射
     * @param fieldMappings 该品牌的字段映射配置
     * @return ExcelRowDTO 解析后的数据对象
     */
    public static ExcelRowDTO parseRow(Row row, Map<String, Integer> headerMap,
                                       List<ExcelFieldMapping> fieldMappings) {
        ExcelRowDTO dto = new ExcelRowDTO();

        for (ExcelFieldMapping mapping : fieldMappings) {
            String standardField = mapping.getStandardField();

            // 添加调试日志
            if ("departWarehouseTime".equals(standardField)) {
                System.out.println("===== 调试 departWarehouseTime =====");
                System.out.println("  表头名称配置: " + mapping.getExcelColumnNames());
            }

            String[] excelNames = mapping.getExcelColumnNames().split(",");
            String dateFormat = mapping.getDateFormat();

            Integer colIndex = findColumnIndex(headerMap, excelNames);

            // 针对 departWarehouseTime 添加完整日志
            if ("departWarehouseTime".equals(standardField)) {
                System.out.println("===== 调试 departWarehouseTime =====");
                System.out.println("  表头名称配置: " + mapping.getExcelColumnNames());
                System.out.println("  列索引: " + colIndex);
            }

            if (colIndex == null) {
                if ("departWarehouseTime".equals(standardField)) {
                    System.out.println("  结果: 未找到匹配列");
                }
                if (StrUtil.isNotBlank(mapping.getDefaultValue())) {
                    setFieldValue(dto, standardField, mapping.getDefaultValue(), dateFormat);
                }
                continue;
            }

            Cell cell = row.getCell(colIndex);
            String cellValue = getCellValueAsString(cell);

            if ("departWarehouseTime".equals(standardField)) {
                System.out.println("  单元格原始值: " + cellValue);
            }

            if (StrUtil.isNotBlank(cellValue)) {
                setFieldValue(dto, standardField, cellValue, dateFormat);
                if ("departWarehouseTime".equals(standardField)) {
                    System.out.println("  解析结果: " + dto.getDepartWarehouseTime());
                }
            } else if (StrUtil.isNotBlank(mapping.getDefaultValue())) {
                setFieldValue(dto, standardField, mapping.getDefaultValue(), dateFormat);
            }

            if ("departWarehouseTime".equals(standardField)) {
                System.out.println("  列索引: " + colIndex);
                System.out.println("  单元格原始值: " + cellValue);
                System.out.println("  解析结果: " + dto.getDepartWarehouseTime());
            }
        }

        return dto;
    }

    /**
     * 解析 Excel 表头，建立列名 → 列索引的映射
     *
     * @param headerRow 表头行
     * @return 列名 → 列索引 的映射
     */
    public static Map<String, Integer> parseHeader(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        if (headerRow == null) {
            return headerMap;
        }

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            String headerName = getCellValueAsString(cell);
            if (StrUtil.isNotBlank(headerName)) {
                // 清理表头：去掉方括号、换行符、回车符、多余空格
                headerName = headerName.replace("[", "")
                        .replace("]", "")
                        .replace("\n", "")
                        .replace("\r", "")
                        .trim();
                headerMap.put(headerName, i);
            }
        }
        return headerMap;
    }

    /**
     * 在表头映射中查找匹配的列索引
     *
     * 支持：
     * 1. 精确匹配
     * 2. 忽略大小写匹配
     * 3. 多个备选名称（用逗号分隔）
     *
     * @param headerMap 表头映射
     * @param excelNames 备选的 Excel 列名
     * @return 列索引，找不到返回 null
     */
    private static Integer findColumnIndex(Map<String, Integer> headerMap, String[] excelNames) {
        for (String name : excelNames) {
            String trimmedName = name.trim();

            // 精确匹配
            if (headerMap.containsKey(trimmedName)) {
                return headerMap.get(trimmedName);
            }

            // 忽略大小写匹配
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(trimmedName)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 获取单元格的字符串值
     *
     * 支持类型：
     * - STRING：字符串
     * - NUMERIC：数字（日期或普通数字）
     * - BOOLEAN：布尔值
     * - FORMULA：公式
     *
     * @param cell 单元格
     * @return 字符串值
     */
    /**
     * 获取单元格的字符串值
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();

                case NUMERIC:
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                    } else {
                        double numericValue = cell.getNumericCellValue();
                        if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                            return String.valueOf((long) numericValue);
                        } else {
                            return String.valueOf(numericValue);
                        }
                    }

                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());

                case FORMULA:
                    // 关键修复：根据公式计算结果类型处理
                    try {
                        // 先尝试获取公式计算后的类型
                        CellType resultType = cell.getCachedFormulaResultType();

                        if (resultType == CellType.NUMERIC) {
                            // 公式结果是数字，检查是否是日期
                            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                            } else {
                                double numericValue = cell.getNumericCellValue();
                                if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                                    return String.valueOf((long) numericValue);
                                } else {
                                    return String.valueOf(numericValue);
                                }
                            }
                        } else if (resultType == CellType.STRING) {
                            return cell.getStringCellValue().trim();
                        } else if (resultType == CellType.BOOLEAN) {
                            return String.valueOf(cell.getBooleanCellValue());
                        } else {
                            // 兜底：尝试作为数字处理
                            return String.valueOf(cell.getNumericCellValue());
                        }
                    } catch (Exception e) {
                        // 公式计算失败，尝试返回公式字符串
                        return cell.getCellFormula();
                    }

                case BLANK:
                    return null;

                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("获取单元格值失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据标准字段名设置 DTO 的值
     *
     * @param dto 目标 DTO
     * @param standardField 标准字段名
     * @param value 字段值
     * @param dateFormat 时间格式（如果是时间类型）
     */
    private static void setFieldValue(ExcelRowDTO dto, String standardField,
                                      String value, String dateFormat) {
        if (StrUtil.isBlank(value)) {
            return;
        }

        try {
            switch (standardField) {
                case "vin":
                    dto.setVin(value.trim());
                    break;

                case "brandName":
                    dto.setBrandName(value.trim());
                    break;

                case "orderReleaseTime":
                    dto.setOrderReleaseTime(parseDateTime(value, dateFormat));
                    break;

                case "originCity":
                    dto.setOriginCity(value.trim());
                    break;

                case "destCity":
                    dto.setDestCity(value.trim());
                    break;

                case "departWarehouseTime":
                    dto.setDepartWarehouseTime(parseDateTime(value, dateFormat));
                    break;

                case "arrivePortTime":
                    dto.setArrivePortTime(parseDateTime(value, dateFormat));
                    break;

                case "shipDepartTime":
                    dto.setShipDepartTime(parseDateTime(value, dateFormat));
                    break;

                case "shipArriveTime":
                    dto.setShipArriveTime(parseDateTime(value, dateFormat));
                    break;

                case "unloadFinishTime":
                    dto.setUnloadFinishTime(parseDateTime(value, dateFormat));
                    break;

                case "dispatchTime":
                    dto.setDispatchTime(parseDateTime(value, dateFormat));
                    break;

                case "arriveShopTime":
                    dto.setArriveShopTime(parseDateTime(value, dateFormat));
                    break;

                default:
                    // 未知字段，忽略
                    System.err.println("未知的标准字段: " + standardField);
                    break;
            }
        } catch (Exception e) {
            System.err.println("字段 " + standardField + " 解析失败，值: " + value + "，错误: " + e.getMessage());
        }
    }

    /**
     * 解析时间字符串
     *
     * 支持：
     * 1. 指定格式解析
     * 2. 自动识别常见格式
     *
     * @param value 时间字符串
     * @param dateFormat 时间格式（如：yyyy-MM-dd HH:mm:ss、yyyy/MM/dd、yyyy年MM月dd日）
     * @return LocalDateTime 对象
     */
    private static LocalDateTime parseDateTime(String value, String dateFormat) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            if (StrUtil.isNotBlank(dateFormat)) {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                Date date = sdf.parse(value.trim());
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }

            // 自动识别常见格式
            String[] commonFormats = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy/MM/dd",
                    "yyyy年MM月dd日 HH:mm:ss",
                    "yyyy年MM月dd日",
                    "MM/dd/yyyy HH:mm",
                    "MM/dd/yyyy"
            };

            for (String format : commonFormats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    Date date = sdf.parse(value.trim());
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                } catch (Exception ignored) {
                    // 继续尝试
                }
            }

            return null;

        } catch (Exception e) {
            System.err.println("时间解析失败: " + value);
            return null;
        }
    }

    /**
     * 验证 DTO 的必填字段
     *
     * @param dto 数据对象
     * @param fieldMappings 字段映射配置
     * @return 验证错误信息，为空表示验证通过
     */
    public static String validateRequired(ExcelRowDTO dto, List<ExcelFieldMapping> fieldMappings) {
        for (ExcelFieldMapping mapping : fieldMappings) {
            if (mapping.getIsRequired() != 1) {
                continue;
            }

            String standardField = mapping.getStandardField();
            Object value = getFieldValue(dto, standardField);

            if (value == null || (value instanceof String && StrUtil.isBlank((String) value))) {
                return "必填字段 " + standardField + " 为空";
            }
        }
        return null;
    }

    /**
     * 获取 DTO 字段的值（用于验证）
     */
    private static Object getFieldValue(ExcelRowDTO dto, String standardField) {
        switch (standardField) {
            case "vin": return dto.getVin();
            case "brandName": return dto.getBrandName();
            case "orderReleaseTime": return dto.getOrderReleaseTime();
            case "originCity": return dto.getOriginCity();
            case "destCity": return dto.getDestCity();
            case "departWarehouseTime": return dto.getDepartWarehouseTime();
            case "arrivePortTime": return dto.getArrivePortTime();
            case "shipDepartTime": return dto.getShipDepartTime();
            case "shipArriveTime": return dto.getShipArriveTime();
            case "unloadFinishTime": return dto.getUnloadFinishTime();
            case "dispatchTime": return dto.getDispatchTime();
            case "arriveShopTime": return dto.getArriveShopTime();
            default: return null;
        }
    }
}