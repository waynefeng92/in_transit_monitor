package com.company.roro.controller;

import com.company.roro.dto.OtdConfigImportDTO;
import com.company.roro.dto.OtdConfigImportResultDTO;
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
    public RouteOtdConfig getByRouteId(
            @PathVariable Integer routeId) {
        return routeOtdConfigService.lambdaQuery()
                .eq(RouteOtdConfig::getRouteId, routeId)
                .eq(RouteOtdConfig::getIsActive, 1)
                .one();
    }

    /**
     * 根据ID查询OTD配置
     *
     * @param id 配置ID
     * @return OTD配置信息
     */
    @GetMapping("/{id}")
    public RouteOtdConfig getById(
            @PathVariable Integer id) {
        return routeOtdConfigService.getById(id);
    }

    /**
     * 新增OTD配置
     *
     * @param config OTD配置信息
     * @return 是否成功
     */
    @PostMapping
    public boolean save(
            @RequestBody RouteOtdConfig config) {
        return routeOtdConfigService.save(config);
    }

    /**
     * 更新OTD配置
     *
     * @param config OTD配置信息（必须包含ID）
     * @return 是否成功
     */
    @PutMapping
    public boolean update(
            @RequestBody RouteOtdConfig config) {
        return routeOtdConfigService.updateById(config);
    }

    /**
     * 删除OTD配置（软删除）
     *
     * @param id 配置ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public boolean delete(
            @PathVariable Integer id) {
        RouteOtdConfig config = routeOtdConfigService.getById(id);
        if (config != null) {
            config.setIsActive(0);
            return routeOtdConfigService.updateById(config);
        }
        return false;
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
                Integer notDepartedOtd = config != null ? config.getNotDepartedOtd() : null;
                if (notDepartedOtd != null) {
                    row.createCell(6).setCellValue(notDepartedOtd);
                } else {
                    row.createCell(6).setCellValue("");
                }

                Integer toPortOtd = config != null ? config.getToPortOtd() : null;
                if (toPortOtd != null) {
                    row.createCell(7).setCellValue(toPortOtd);
                } else {
                    row.createCell(7).setCellValue("");
                }

                Integer atPortWaitOtd = config != null ? config.getAtPortWaitOtd() : null;
                if (atPortWaitOtd != null) {
                    row.createCell(8).setCellValue(atPortWaitOtd);
                } else {
                    row.createCell(8).setCellValue("");
                }

                Integer onSeaOtd = config != null ? config.getOnSeaOtd() : null;
                if (onSeaOtd != null) {
                    row.createCell(9).setCellValue(onSeaOtd);
                } else {
                    row.createCell(9).setCellValue("");
                }

                Integer atDestWaitOtd = config != null ? config.getAtDestWaitOtd() : null;
                if (atDestWaitOtd != null) {
                    row.createCell(10).setCellValue(atDestWaitOtd);
                } else {
                    row.createCell(10).setCellValue("");
                }

                Integer unloadWaitDispatchOtd = config != null ? config.getUnloadWaitDispatchOtd() : null;
                if (unloadWaitDispatchOtd != null) {
                    row.createCell(11).setCellValue(unloadWaitDispatchOtd);
                } else {
                    row.createCell(11).setCellValue("");
                }

                Integer dispatchingOtd = config != null ? config.getDispatchingOtd() : null;
                if (dispatchingOtd != null) {
                    row.createCell(12).setCellValue(dispatchingOtd);
                } else {
                    row.createCell(12).setCellValue("");
                }

// 预警时效
                Integer notDepartedWarn = config != null ? config.getNotDepartedWarn() : null;
                if (notDepartedWarn != null) {
                    row.createCell(13).setCellValue(notDepartedWarn);
                } else {
                    row.createCell(13).setCellValue("");
                }

                Integer toPortWarn = config != null ? config.getToPortWarn() : null;
                if (toPortWarn != null) {
                    row.createCell(14).setCellValue(toPortWarn);
                } else {
                    row.createCell(14).setCellValue("");
                }

                Integer atPortWaitWarn = config != null ? config.getAtPortWaitWarn() : null;
                if (atPortWaitWarn != null) {
                    row.createCell(15).setCellValue(atPortWaitWarn);
                } else {
                    row.createCell(15).setCellValue("");
                }

                Integer onSeaWarn = config != null ? config.getOnSeaWarn() : null;
                if (onSeaWarn != null) {
                    row.createCell(16).setCellValue(onSeaWarn);
                } else {
                    row.createCell(16).setCellValue("");
                }

                Integer atDestWaitWarn = config != null ? config.getAtDestWaitWarn() : null;
                if (atDestWaitWarn != null) {
                    row.createCell(17).setCellValue(atDestWaitWarn);
                } else {
                    row.createCell(17).setCellValue("");
                }

                Integer unloadWaitDispatchWarn = config != null ? config.getUnloadWaitDispatchWarn() : null;
                if (unloadWaitDispatchWarn != null) {
                    row.createCell(18).setCellValue(unloadWaitDispatchWarn);
                } else {
                    row.createCell(18).setCellValue("");
                }

                Integer dispatchingWarn = config != null ? config.getDispatchingWarn() : null;
                if (dispatchingWarn != null) {
                    row.createCell(19).setCellValue(dispatchingWarn);
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
    public List<OtdConfigImportDTO> previewImport(
            @RequestParam("file") MultipartFile file) throws Exception {

        List<OtdConfigImportDTO> result = new ArrayList<>();

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

                // 读取时效数据
                dto.setNotDepartedOtd(getIntCellValue(row.getCell(6)));
                dto.setToPortOtd(getIntCellValue(row.getCell(7)));
                dto.setAtPortWaitOtd(getIntCellValue(row.getCell(8)));
                dto.setOnSeaOtd(getIntCellValue(row.getCell(9)));
                dto.setAtDestWaitOtd(getIntCellValue(row.getCell(10)));
                dto.setUnloadWaitDispatchOtd(getIntCellValue(row.getCell(11)));
                dto.setDispatchingOtd(getIntCellValue(row.getCell(12)));

                dto.setNotDepartedWarn(getIntCellValue(row.getCell(13)));
                dto.setToPortWarn(getIntCellValue(row.getCell(14)));
                dto.setAtPortWaitWarn(getIntCellValue(row.getCell(15)));
                dto.setOnSeaWarn(getIntCellValue(row.getCell(16)));
                dto.setAtDestWaitWarn(getIntCellValue(row.getCell(17)));
                dto.setUnloadWaitDispatchWarn(getIntCellValue(row.getCell(18)));
                dto.setDispatchingWarn(getIntCellValue(row.getCell(19)));

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

        return result;
    }

    /**
     * 批量导入OTD配置
     */
    @PostMapping("/import/batch")
    public OtdConfigImportResultDTO batchImport(
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

        return result;
    }

    /**
     * 获取单元格整数值
     */
    private Integer getIntCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String str = cell.getStringCellValue().trim();
                    return str.isEmpty() ? null : Integer.parseInt(str);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}