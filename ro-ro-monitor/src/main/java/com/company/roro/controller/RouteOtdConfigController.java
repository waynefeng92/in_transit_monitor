package com.company.roro.controller;

import com.company.roro.dto.OtdConfigImportDTO;
import com.company.roro.dto.OtdConfigImportResultDTO;
import com.company.roro.dto.Result;
import com.company.roro.entity.BrandDict;
import com.company.roro.entity.PortDict;
import com.company.roro.entity.RouteDict;
import com.company.roro.entity.RouteOtdConfig;
import com.company.roro.service.BrandDictService;
import com.company.roro.service.PortDictService;
import com.company.roro.service.RouteDictService;
import com.company.roro.service.RouteOtdConfigService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.openxml4j.util.ZipSecureFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OTD时效配置接口
 *
 * 功能：管理各线路的7段标准OTD时效和预警时效
 *
 * 7段时效：
 * 1. 未出库 → 集港在途
 * 2. 集港在途 → 已集港待装船
 * 3. 已集港待装船 → 水运在途
 * 4. 水运在途 → 已到港待卸船
 * 5. 已到港待卸船 → 已卸船待分拨
 * 6. 已卸船待分拨 → 分拨在途
 * 7. 分拨在途 → 已到达
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/otd-config")
public class RouteOtdConfigController {

    @Autowired
    private RouteOtdConfigService routeOtdConfigService;

    @Autowired
    private RouteDictService routeDictService;

    @Autowired
    private BrandDictService brandDictService;

    @Autowired
    private PortDictService portDictService;

    /**
     * 根据线路ID查询OTD配置
     *
     * @param routeId 线路ID
     * @return OTD配置信息
     */
    @GetMapping("/route/{routeId}")
    public Result<RouteOtdConfig> getByRouteId(
            @PathVariable Integer routeId) {
        return Result.success(routeOtdConfigService.lambdaQuery()
                .eq(RouteOtdConfig::getRouteId, routeId)
                .eq(RouteOtdConfig::getIsActive, 1)
                .one());
    }

    /**
     * 根据ID查询OTD配置
     *
     * @param id 配置ID
     * @return OTD配置信息
     */
    @GetMapping("/{id}")
    public Result<RouteOtdConfig> getById(
            @PathVariable Integer id) {
        return Result.success(routeOtdConfigService.getById(id));
    }

    /**
     * 新增OTD配置
     *
     * @param config OTD配置信息
     * @return 是否成功
     */
    @PostMapping
    public Result<Boolean> save(
            @RequestBody RouteOtdConfig config) {
        return Result.success(routeOtdConfigService.save(config));
    }

    /**
     * 更新OTD配置
     *
     * @param config OTD配置信息（必须包含ID）
     * @return 是否成功
     */
    @PutMapping
    public Result<Boolean> update(
            @RequestBody RouteOtdConfig config) {
        return Result.success(routeOtdConfigService.updateById(config));
    }

    /**
     * 删除OTD配置（软删除）
     *
     * @param id 配置ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @PathVariable Integer id) {
        RouteOtdConfig config = routeOtdConfigService.getById(id);
        if (config != null) {
            config.setIsActive(0);
            return Result.success(routeOtdConfigService.updateById(config));
        }
        return Result.success(false);
    }

    // ==================== 批量导出导入 ====================

    /**
     * 导出品牌的OTD配置模板
     */
    @GetMapping("/export/{brandId}")
    public void exportTemplate(
            @PathVariable Integer brandId,
            HttpServletResponse response) throws IOException {

        BrandDict brand = brandDictService.getById(brandId);
        String brandName = brand != null ? brand.getBrandName() : "未知品牌";
        String fileName = URLEncoder.encode(brandName + "_OTD配置模板.xlsx", "UTF-8")
                .replaceAll("\\+", "%20");

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + fileName + ";filename*=UTF-8''" + fileName);

        // 查询该品牌所有激活的线路
        List<RouteDict> routes = routeDictService.lambdaQuery()
                .eq(RouteDict::getBrandId, brandId)
                .eq(RouteDict::getIsActive, 1)
                .list();

        // 查询已有的OTD配置
        List<Integer> routeIds = routes.stream().map(RouteDict::getId).collect(Collectors.toList());
        Map<Integer, RouteOtdConfig> configMap = new HashMap<>();
        if (!routeIds.isEmpty()) {
            configMap = routeOtdConfigService.lambdaQuery()
                    .in(RouteOtdConfig::getRouteId, routeIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(RouteOtdConfig::getRouteId, c -> c));
        }

        // 查询港口名称映射
        Map<Integer, String> portNameMap = portDictService.list().stream()
                .collect(Collectors.toMap(PortDict::getId, PortDict::getPortName));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("OTD配置");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 表头
            String[] headers = {
                    "线路ID", "品牌", "出发地", "出发港", "目的港", "目的地",
                    "未出库_标准OTD", "集港在途_标准OTD", "待装船_标准OTD", "水运在途_标准OTD",
                    "待卸船_标准OTD", "待分拨_标准OTD", "分拨在途_标准OTD",
                    "未出库_预警", "集港在途_预警", "待装船_预警", "水运在途_预警",
                    "待卸船_预警", "待分拨_预警", "分拨在途_预警"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowIndex = 1;
            for (RouteDict route : routes) {
                Row row = sheet.createRow(rowIndex++);
                RouteOtdConfig config = configMap.get(route.getId());

                row.createCell(0).setCellValue(route.getId());
                row.createCell(1).setCellValue(brandName);
                row.createCell(2).setCellValue(route.getOriginCity() != null ? route.getOriginCity() : "");
                row.createCell(3).setCellValue(portNameMap.getOrDefault(route.getOriginPortId(), ""));
                row.createCell(4).setCellValue(portNameMap.getOrDefault(route.getDestPortId(), ""));
                row.createCell(5).setCellValue(route.getDestCity() != null ? route.getDestCity() : "");

                // OTD时效（已有配置则填充，否则留空）
                if (config != null && config.getNotDepartedOtd() != null) {
                    row.createCell(6).setCellValue(config.getNotDepartedOtd());
                } else {
                    row.createCell(6).setCellValue("");
                }

                if (config != null && config.getToPortOtd() != null) {
                    row.createCell(7).setCellValue(config.getToPortOtd());
                } else {
                    row.createCell(7).setCellValue("");
                }

                if (config != null && config.getAtPortWaitOtd() != null) {
                    row.createCell(8).setCellValue(config.getAtPortWaitOtd());
                } else {
                    row.createCell(8).setCellValue("");
                }

                if (config != null && config.getOnSeaOtd() != null) {
                    row.createCell(9).setCellValue(config.getOnSeaOtd());
                } else {
                    row.createCell(9).setCellValue("");
                }

                if (config != null && config.getAtDestWaitOtd() != null) {
                    row.createCell(10).setCellValue(config.getAtDestWaitOtd());
                } else {
                    row.createCell(10).setCellValue("");
                }

                if (config != null && config.getUnloadWaitDispatchOtd() != null) {
                    row.createCell(11).setCellValue(config.getUnloadWaitDispatchOtd());
                } else {
                    row.createCell(11).setCellValue("");
                }

                if (config != null && config.getDispatchingOtd() != null) {
                    row.createCell(12).setCellValue(config.getDispatchingOtd());
                } else {
                    row.createCell(12).setCellValue("");
                }

// 预警时效
                if (config != null && config.getNotDepartedWarn() != null) {
                    row.createCell(13).setCellValue(config.getNotDepartedWarn());
                } else {
                    row.createCell(13).setCellValue("");
                }

                if (config != null && config.getToPortWarn() != null) {
                    row.createCell(14).setCellValue(config.getToPortWarn());
                } else {
                    row.createCell(14).setCellValue("");
                }

                if (config != null && config.getAtPortWaitWarn() != null) {
                    row.createCell(15).setCellValue(config.getAtPortWaitWarn());
                } else {
                    row.createCell(15).setCellValue("");
                }

                if (config != null && config.getOnSeaWarn() != null) {
                    row.createCell(16).setCellValue(config.getOnSeaWarn());
                } else {
                    row.createCell(16).setCellValue("");
                }

                if (config != null && config.getAtDestWaitWarn() != null) {
                    row.createCell(17).setCellValue(config.getAtDestWaitWarn());
                } else {
                    row.createCell(17).setCellValue("");
                }

                if (config != null && config.getUnloadWaitDispatchWarn() != null) {
                    row.createCell(18).setCellValue(config.getUnloadWaitDispatchWarn());
                } else {
                    row.createCell(18).setCellValue("");
                }

                if (config != null && config.getDispatchingWarn() != null) {
                    row.createCell(19).setCellValue(config.getDispatchingWarn());
                } else {
                    row.createCell(19).setCellValue("");
                }
            }

            // 设置列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 12 * 256);
            }

            workbook.write(response.getOutputStream());
        }
    }

    /**
     * 预览导入数据
     */
    @PostMapping("/import/preview")
    public Result<List<OtdConfigImportDTO>> previewImport(
            @RequestParam("file") MultipartFile file) throws Exception {

        List<OtdConfigImportDTO> result = new ArrayList<>();

        ZipSecureFile.setMinInflateRatio(0.001);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                OtdConfigImportDTO dto = new OtdConfigImportDTO();

                // 读取线路ID（第一列）
                Cell routeIdCell = row.getCell(0);
                if (routeIdCell != null) {
                    dto.setRouteId((int) routeIdCell.getNumericCellValue());
                } else {
                    continue;
                }

                // 读取时效数据（支持小数）
                dto.setNotDepartedOtd(getDoubleCellValue(row.getCell(6)));
                dto.setToPortOtd(getDoubleCellValue(row.getCell(7)));
                dto.setAtPortWaitOtd(getDoubleCellValue(row.getCell(8)));
                dto.setOnSeaOtd(getDoubleCellValue(row.getCell(9)));
                dto.setAtDestWaitOtd(getDoubleCellValue(row.getCell(10)));
                dto.setUnloadWaitDispatchOtd(getDoubleCellValue(row.getCell(11)));
                dto.setDispatchingOtd(getDoubleCellValue(row.getCell(12)));

                dto.setNotDepartedWarn(getDoubleCellValue(row.getCell(13)));
                dto.setToPortWarn(getDoubleCellValue(row.getCell(14)));
                dto.setAtPortWaitWarn(getDoubleCellValue(row.getCell(15)));
                dto.setOnSeaWarn(getDoubleCellValue(row.getCell(16)));
                dto.setAtDestWaitWarn(getDoubleCellValue(row.getCell(17)));
                dto.setUnloadWaitDispatchWarn(getDoubleCellValue(row.getCell(18)));
                dto.setDispatchingWarn(getDoubleCellValue(row.getCell(19)));

                // 读取线路信息用于展示
                RouteDict route = routeDictService.getById(dto.getRouteId());
                if (route != null) {
                    BrandDict brand = brandDictService.getById(route.getBrandId());
                    dto.setBrandName(brand != null ? brand.getBrandName() : "");
                    dto.setOriginCity(route.getOriginCity());
                    dto.setDestCity(route.getDestCity());
                }

                result.add(dto);
            }
        }

        return Result.success(result);
    }

    /**
     * 批量导入OTD配置
     */
    @PostMapping("/import/batch")
    public Result<OtdConfigImportResultDTO> batchImport(
            @RequestBody List<OtdConfigImportDTO> importData) {

        OtdConfigImportResultDTO result = new OtdConfigImportResultDTO();

        for (OtdConfigImportDTO dto : importData) {
            try {
                if (dto.getRouteId() == null) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getFailDetails().add("线路ID为空");
                    continue;
                }

                // 查找已有配置
                RouteOtdConfig existing = routeOtdConfigService.lambdaQuery()
                        .eq(RouteOtdConfig::getRouteId, dto.getRouteId())
                        .one();

                RouteOtdConfig config = existing != null ? existing : new RouteOtdConfig();
                config.setRouteId(dto.getRouteId());
                config.setNotDepartedOtd(dto.getNotDepartedOtd());
                config.setToPortOtd(dto.getToPortOtd());
                config.setAtPortWaitOtd(dto.getAtPortWaitOtd());
                config.setOnSeaOtd(dto.getOnSeaOtd());
                config.setAtDestWaitOtd(dto.getAtDestWaitOtd());
                config.setUnloadWaitDispatchOtd(dto.getUnloadWaitDispatchOtd());
                config.setDispatchingOtd(dto.getDispatchingOtd());

                config.setNotDepartedWarn(dto.getNotDepartedWarn());
                config.setToPortWarn(dto.getToPortWarn());
                config.setAtPortWaitWarn(dto.getAtPortWaitWarn());
                config.setOnSeaWarn(dto.getOnSeaWarn());
                config.setAtDestWaitWarn(dto.getAtDestWaitWarn());
                config.setUnloadWaitDispatchWarn(dto.getUnloadWaitDispatchWarn());
                config.setDispatchingWarn(dto.getDispatchingWarn());
                config.setIsActive(1);

                routeOtdConfigService.saveOrUpdate(config);
                result.setSuccessCount(result.getSuccessCount() + 1);

            } catch (Exception e) {
                result.setFailCount(result.getFailCount() + 1);
                result.getFailDetails().add("线路ID " + dto.getRouteId() + " 导入失败: " + e.getMessage());
            }
        }

        return Result.success(result);
    }

    /**
     * 获取单元格Double值（支持小数时效）
     */
    private Double getDoubleCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String str = cell.getStringCellValue().trim();
                    return str.isEmpty() ? null : Double.parseDouble(str);
                case FORMULA:
                    switch (cell.getCachedFormulaResultType()) {
                        case NUMERIC:
                            return cell.getNumericCellValue();
                        case STRING:
                            String s = cell.getStringCellValue().trim();
                            return s.isEmpty() ? null : Double.parseDouble(s);
                        default:
                            return null;
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
