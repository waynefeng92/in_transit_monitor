package com.company.roro.controller;

import cn.hutool.core.util.StrUtil;
import com.company.roro.dto.*;
import com.company.roro.entity.BrandDict;
import com.company.roro.entity.PortDict;
import com.company.roro.entity.RouteDict;
import com.company.roro.service.BrandDictService;
import com.company.roro.service.PortDictService;
import com.company.roro.service.RouteDictService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 线路管理接口
 *
 * 功能：管理品牌运输线路，包括出发地、出发港、目的港、目的地等
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/route")
public class RouteController {

    @Autowired
    private RouteDictService routeDictService;

    @Autowired
    private BrandDictService brandDictService;

    @Autowired
    private PortDictService portDictService;

    /**
     * 查询所有激活的线路
     *
     * @param includeDisabled 是否包含已禁用的线路
     * @return 线路列表
     */
    @GetMapping("/list")
    public List<RouteDict> list(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        if (includeDisabled) {
            return routeDictService.lambdaQuery()
                    .orderByAsc(RouteDict::getId)
                    .list();
        } else {
            return routeDictService.lambdaQuery()
                    .eq(RouteDict::getIsActive, 1)
                    .orderByAsc(RouteDict::getId)
                    .list();
        }
    }

    /**
     * 根据品牌ID查询线路
     *
     * @param brandId 品牌ID
     * @return 该品牌下的所有激活线路
     */
    @GetMapping("/list/{brandId}")
    public List<RouteDict> listByBrand(
            @PathVariable Integer brandId) {
        return routeDictService.lambdaQuery()
                .eq(RouteDict::getBrandId, brandId)
                .eq(RouteDict::getIsActive, 1)
                .list();
    }

    /**
     * 根据ID查询线路详情
     *
     * @param id 线路ID
     * @return 线路信息
     */
    @GetMapping("/{id}")
    public RouteDict getById(
            @PathVariable Integer id) {
        return routeDictService.getById(id);
    }

    /**
     * 新增线路
     *
     * @param routeDict 线路信息
     * @return 是否成功
     */
    @PostMapping
    public boolean save(
            @RequestBody RouteDict routeDict) {
        return routeDictService.save(routeDict);
    }

    /**
     * 更新线路信息
     *
     * @param routeDict 线路信息（必须包含ID）
     * @return 是否成功
     */
    @PutMapping
    public boolean update(
            @RequestBody RouteDict routeDict) {
        return routeDictService.updateById(routeDict);
    }

    /**
     * 删除线路（软删除）
     *
     * @param id 线路ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public boolean delete(
            @PathVariable Integer id) {
        RouteDict route = routeDictService.getById(id);
        if (route != null) {
            route.setIsActive(0);
            return routeDictService.updateById(route);
        }
        return false;
    }

    // ==================== 批量导入 ====================

    /**
     * 下载线路导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String fileName = "线路批量导入模板.xlsx";
        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
                .replaceAll("\\+", "%20");

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + encodedFileName + ";filename*=UTF-8''" + encodedFileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("线路导入模板");

            // 创建表头样式（加粗）
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"品牌", "出发地", "出发港", "目的港", "目的地"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 添加示例数据
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("上汽大众");
            exampleRow.createCell(1).setCellValue("上海");
            exampleRow.createCell(2).setCellValue("上海港");
            exampleRow.createCell(3).setCellValue("大连港");
            exampleRow.createCell(4).setCellValue("大连");

            Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("特斯拉");
            exampleRow2.createCell(1).setCellValue("上海");
            exampleRow2.createCell(2).setCellValue("上海港");
            exampleRow2.createCell(3).setCellValue("天津港");
            exampleRow2.createCell(4).setCellValue("天津");

            // 关键：先写入数据，再自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 额外设置一下最小列宽，避免太窄
            for (int i = 0; i < headers.length; i++) {
                int columnWidth = sheet.getColumnWidth(i);
                if (columnWidth < 3000) {  // 最小宽度约 12 个字符
                    sheet.setColumnWidth(i, 3000);
                }
            }

            workbook.write(response.getOutputStream());
        }
    }

    /**
     * 预览导入数据（带校验）
     */
    @PostMapping("/import/preview")
    public RouteImportPreviewDTO previewImport(
            @RequestParam("file") MultipartFile file) throws Exception {

        RouteImportPreviewDTO preview = new RouteImportPreviewDTO();
        List<RouteImportRowDTO> rows = new ArrayList<>();

        // 预加载所有品牌和港口，用于快速校验
        Map<String, BrandDict> brandMap = brandDictService.list().stream()
                .filter(b -> b.getIsActive() == 1)
                .collect(Collectors.toMap(BrandDict::getBrandName, b -> b, (a, b) -> a));
        Map<String, PortDict> portMap = portDictService.list().stream()
                .filter(p -> p.getIsActive() == 1)
                .collect(Collectors.toMap(PortDict::getPortName, p -> p, (a, b) -> a));

        Set<String> missingBrands = new HashSet<>();
        Set<String> missingPorts = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                RouteImportRowDTO dto = new RouteImportRowDTO();
                dto.setRowNum(i + 1);

                String brandName = getCellStringValue(row.getCell(0));
                String originCity = getCellStringValue(row.getCell(1));
                String originPortName = getCellStringValue(row.getCell(2));
                String destPortName = getCellStringValue(row.getCell(3));
                String destCity = getCellStringValue(row.getCell(4));

                // 跳过空行
                if (StrUtil.isBlank(brandName) && StrUtil.isBlank(originCity)) {
                    continue;
                }

                dto.setBrandName(brandName);
                dto.setOriginCity(originCity);
                dto.setOriginPortName(originPortName);
                dto.setDestPortName(destPortName);
                dto.setDestCity(destCity);

                // 校验品牌
                if (StrUtil.isNotBlank(brandName)) {
                    dto.setBrandExists(brandMap.containsKey(brandName));
                    if (!dto.isBrandExists()) {
                        missingBrands.add(brandName);
                    }
                }

                // 校验出发港
                if (StrUtil.isNotBlank(originPortName)) {
                    dto.setOriginPortExists(portMap.containsKey(originPortName));
                    if (!dto.isOriginPortExists()) {
                        missingPorts.add(originPortName);
                    }
                }

                // 校验目的港
                if (StrUtil.isNotBlank(destPortName)) {
                    dto.setDestPortExists(portMap.containsKey(destPortName));
                    if (!dto.isDestPortExists()) {
                        missingPorts.add(destPortName);
                    }
                }

                // 是否可导入（必填字段都存在且校验通过）
                boolean hasRequired = StrUtil.isNotBlank(brandName)
                        && StrUtil.isNotBlank(originCity)
                        && StrUtil.isNotBlank(originPortName)
                        && StrUtil.isNotBlank(destPortName)
                        && StrUtil.isNotBlank(destCity);
                dto.setCanImport(hasRequired && dto.isBrandExists()
                        && dto.isOriginPortExists() && dto.isDestPortExists());

                rows.add(dto);
            }
        }

        preview.setRows(rows);
        preview.setMissingBrands(new ArrayList<>(missingBrands));
        preview.setMissingPorts(new ArrayList<>(missingPorts));

        long canImportCount = rows.stream().filter(RouteImportRowDTO::isCanImport).count();
        preview.setCanImportCount((int) canImportCount);
        preview.setCannotImportCount(rows.size() - (int) canImportCount);

        return preview;
    }

    /**
     * 批量导入线路
     */
    @PostMapping("/import/batch")
    public RouteImportResultDTO batchImport(
            @RequestBody List<RouteImportRequestDTO> importData) {

        RouteImportResultDTO result = new RouteImportResultDTO();

        // 预加载品牌和港口
        Map<String, BrandDict> brandMap = brandDictService.list().stream()
                .filter(b -> b.getIsActive() == 1)
                .collect(Collectors.toMap(BrandDict::getBrandName, b -> b, (a, b) -> a));
        Map<String, PortDict> portMap = portDictService.list().stream()
                .filter(p -> p.getIsActive() == 1)
                .collect(Collectors.toMap(PortDict::getPortName, p -> p, (a, b) -> a));

        for (RouteImportRequestDTO dto : importData) {
            try {
                // 查找品牌
                BrandDict brand = brandMap.get(dto.getBrandName());
                if (brand == null) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getFailDetails().add("品牌不存在: " + dto.getBrandName());
                    continue;
                }

                // 查找出发港
                PortDict originPort = portMap.get(dto.getOriginPortName());
                if (originPort == null) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getFailDetails().add("出发港不存在: " + dto.getOriginPortName());
                    continue;
                }

                // 查找目的港
                PortDict destPort = portMap.get(dto.getDestPortName());
                if (destPort == null) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getFailDetails().add("目的港不存在: " + dto.getDestPortName());
                    continue;
                }

                // 检查是否已存在相同线路
                RouteDict existing = routeDictService.lambdaQuery()
                        .eq(RouteDict::getBrandId, brand.getId())
                        .eq(RouteDict::getOriginCity, dto.getOriginCity())
                        .eq(RouteDict::getOriginPortId, originPort.getId())
                        .eq(RouteDict::getDestPortId, destPort.getId())
                        .eq(RouteDict::getDestCity, dto.getDestCity())
                        .one();

                if (existing != null) {
                    result.setSkipCount(result.getSkipCount() + 1);
                    continue;
                }

                // 创建新线路
                RouteDict route = new RouteDict();
                route.setBrandId(brand.getId());
                route.setOriginCity(dto.getOriginCity());
                route.setOriginPortId(originPort.getId());
                route.setDestPortId(destPort.getId());
                route.setDestCity(dto.getDestCity());
                route.setIsActive(1);
                routeDictService.save(route);

                result.setSuccessCount(result.getSuccessCount() + 1);

            } catch (Exception e) {
                result.setFailCount(result.getFailCount() + 1);
                result.getFailDetails().add("导入失败: " + dto.getBrandName() + " - " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
}