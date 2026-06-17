package com.company.roro.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.roro.dto.Result;
import com.company.roro.entity.BrandDict;
import com.company.roro.entity.OrderInfo;
import com.company.roro.entity.TransportStatusDict;
import com.company.roro.entity.VehicleTransit;
import com.company.roro.service.BrandDictService;
import com.company.roro.service.OrderInfoService;
import com.company.roro.service.TransportStatusDictService;
import com.company.roro.service.VehicleTransitService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单管理接口
 *
 * 功能：管理运输订单，包括订单的创建、查询、更新、导出等
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private VehicleTransitService vehicleTransitService;

    @Autowired
    private BrandDictService brandDictService;

    @Autowired
    private TransportStatusDictService transportStatusDictService;

    /**
     * 分页查询订单
     *
     * @param current 当前页码
     * @param size 每页条数
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<OrderInfo>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(orderInfoService.lambdaQuery()
                .orderByDesc(OrderInfo::getOrderReleaseTime)
                .page(new Page<>(current, size)));
    }

    /**
     * 根据ID查询订单详情
     *
     * @param id 订单ID
     * @return 订单信息
     */
    @GetMapping("/{id}")
    public Result<OrderInfo> getById(
            @PathVariable Integer id) {
        return Result.success(orderInfoService.getById(id));
    }

    /**
     * 根据VIN查询订单
     *
     * @param vin 车架号
     * @return 该VIN下的所有订单
     */
    @GetMapping("/vin/{vin}")
    public Result<List<OrderInfo>> getByVin(
            @PathVariable String vin) {
        return Result.success(orderInfoService.lambdaQuery()
                .eq(OrderInfo::getVin, vin)
                .orderByDesc(OrderInfo::getOrderReleaseTime)
                .list());
    }

    /**
     * 新增订单
     *
     * @param orderInfo 订单信息
     * @return 是否成功
     */
    @PostMapping
    public Result<Boolean> save(
            @RequestBody OrderInfo orderInfo) {
        return Result.success(orderInfoService.save(orderInfo));
    }

    /**
     * 更新订单信息
     *
     * @param orderInfo 订单信息（必须包含ID）
     * @return 是否成功
     */
    @PutMapping
    public Result<Boolean> update(
            @RequestBody OrderInfo orderInfo) {
        return Result.success(orderInfoService.updateById(orderInfo));
    }

    /**
     * 删除订单（软删除）
     *
     * @param id 订单ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @PathVariable Integer id) {
        return Result.success(orderInfoService.removeById(id));
    }

    /**
     * 订单列表查询（带过滤器，关联在途状态、中文状态名称）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listOrders(
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) String vin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String transportStatus,
            @RequestParam(required = false) String monitorStatus,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        // 1. Build order query with filters
        LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
        if (brandId != null) orderQuery.eq(OrderInfo::getBrandId, brandId);
        if (vin != null && !vin.isEmpty()) orderQuery.like(OrderInfo::getVin, vin);
        if (startTime != null) orderQuery.ge(OrderInfo::getOrderReleaseTime, startTime);
        if (endTime != null) orderQuery.le(OrderInfo::getOrderReleaseTime, endTime);
        // 2. Get matching orders
        List<OrderInfo> orders = orderInfoService.list(orderQuery);
        if (orders.isEmpty()) return Result.success(emptyPage());

        List<Integer> orderIds = orders.stream().map(OrderInfo::getId).collect(Collectors.toList());

        // 3. Get vehicle_transit records for these orders
        LambdaQueryWrapper<VehicleTransit> transitQuery = new LambdaQueryWrapper<>();
        transitQuery.in(VehicleTransit::getOrderId, orderIds);
        if (transportStatus != null && !transportStatus.isEmpty()) {
            transitQuery.eq(VehicleTransit::getTransportStatus, transportStatus);
        }
        if (monitorStatus != null && !monitorStatus.isEmpty()) {
            transitQuery.eq(VehicleTransit::getMonitorStatus, monitorStatus);
        }
        List<VehicleTransit> transits = vehicleTransitService.list(transitQuery);
        Map<Integer, VehicleTransit> transitMap = transits.stream()
                .collect(Collectors.toMap(VehicleTransit::getOrderId, t -> t, (a, b) -> a));

        // 4. Get brand names and status Chinese names
        Map<Integer, String> brandMap = brandDictService.list().stream()
                .collect(Collectors.toMap(BrandDict::getId, BrandDict::getBrandName));
        Map<String, String> statusNameMap = transportStatusDictService.list().stream()
                .collect(Collectors.toMap(TransportStatusDict::getStatusCode, TransportStatusDict::getStatusName));

        // 5. Build result
        List<Map<String, Object>> records = new ArrayList<>();
        for (OrderInfo order : orders) {
            VehicleTransit transit = transitMap.get(order.getId());
            // Only include orders that have transit records matching filters
            if (transit == null && (transportStatus != null || monitorStatus != null)) continue;

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("orderId", order.getId());
            record.put("vin", order.getVin());
            record.put("brandName", brandMap.getOrDefault(order.getBrandId(), "未知"));
            record.put("orderReleaseTime", order.getOrderReleaseTime());
            record.put("originCity", order.getOriginCity());
            record.put("destCity", order.getDestCity());

            if (transit != null) {
                record.put("transportStatus", transit.getTransportStatus());
                record.put("transportStatusName", statusNameMap.getOrDefault(transit.getTransportStatus(), transit.getTransportStatus()));
                record.put("monitorStatus", transit.getMonitorStatus());
                record.put("batchId", transit.getBatchId());
            } else {
                record.put("transportStatus", null);
                record.put("transportStatusName", null);
                record.put("monitorStatus", null);
                record.put("batchId", null);
            }
            records.add(record);
        }

        // 6. Paginate
        int total = records.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageRecords = fromIndex < total ? records.subList(fromIndex, toIndex) : Collections.emptyList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageRecords);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    /**
     * 批量取消订单（软删除订单 + 移除在途记录）
     */
    @PostMapping("/batch-cancel")
    public Result<Map<String, Object>> batchCancel(@RequestBody List<Integer> orderIds) {
        int successCount = 0;
        for (Integer orderId : orderIds) {
            try {
                // Hard delete order
                orderInfoService.removeById(orderId);
                // Remove vehicle_transit records
                vehicleTransitService.lambdaUpdate()
                        .eq(VehicleTransit::getOrderId, orderId)
                        .remove();
                successCount++;
            } catch (Exception e) {
                // skip failed ones
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", successCount == orderIds.size());
        result.put("successCount", successCount);
        result.put("totalCount", orderIds.size());
        return Result.success(result);
    }

    /**
     * 导出订单到Excel（使用与/list相同的过滤条件，不分页）
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) String vin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String transportStatus,
            @RequestParam(required = false) String monitorStatus) throws IOException {

        // Build results using the same filtering logic as /list
        List<Map<String, Object>> records = buildOrderRecords(brandId, vin, startTime, endTime,
                transportStatus, monitorStatus, false);

        String fileName = URLEncoder.encode("订单数据.xlsx", "UTF-8").replaceAll("\\+", "%20");

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + fileName + ";filename*=UTF-8''" + fileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单数据");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Headers
            String[] headers = {"VIN", "品牌", "订单释放时间", "出发地", "目的地", "在途状态", "监控状态", "批次号"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIndex = 1;
            for (Map<String, Object> record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(objToStr(record.get("vin")));
                row.createCell(1).setCellValue(objToStr(record.get("brandName")));
                row.createCell(2).setCellValue(objToStr(record.get("orderReleaseTime")));
                row.createCell(3).setCellValue(objToStr(record.get("originCity")));
                row.createCell(4).setCellValue(objToStr(record.get("destCity")));
                row.createCell(5).setCellValue(objToStr(record.get("transportStatusName")));
                row.createCell(6).setCellValue(objToStr(record.get("monitorStatus")));
                row.createCell(7).setCellValue(objToStr(record.get("batchId")));
            }

            // Set column widths
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 16 * 256);
            }

            workbook.write(response.getOutputStream());
        }
    }

    /**
     * Shared filtering logic used by both /list and /export
     */
    private List<Map<String, Object>> buildOrderRecords(
            Integer brandId, String vin, LocalDateTime startTime, LocalDateTime endTime,
            String transportStatus, String monitorStatus, boolean returnNullTransits) {

        LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
        if (brandId != null) orderQuery.eq(OrderInfo::getBrandId, brandId);
        if (vin != null && !vin.isEmpty()) orderQuery.like(OrderInfo::getVin, vin);
        if (startTime != null) orderQuery.ge(OrderInfo::getOrderReleaseTime, startTime);
        if (endTime != null) orderQuery.le(OrderInfo::getOrderReleaseTime, endTime);
        List<OrderInfo> orders = orderInfoService.list(orderQuery);
        if (orders.isEmpty()) return Collections.emptyList();

        List<Integer> orderIds = orders.stream().map(OrderInfo::getId).collect(Collectors.toList());

        LambdaQueryWrapper<VehicleTransit> transitQuery = new LambdaQueryWrapper<>();
        transitQuery.in(VehicleTransit::getOrderId, orderIds);
        if (transportStatus != null && !transportStatus.isEmpty()) {
            transitQuery.eq(VehicleTransit::getTransportStatus, transportStatus);
        }
        if (monitorStatus != null && !monitorStatus.isEmpty()) {
            transitQuery.eq(VehicleTransit::getMonitorStatus, monitorStatus);
        }
        List<VehicleTransit> transits = vehicleTransitService.list(transitQuery);
        Map<Integer, VehicleTransit> transitMap = transits.stream()
                .collect(Collectors.toMap(VehicleTransit::getOrderId, t -> t, (a, b) -> a));

        Map<Integer, String> brandMap = brandDictService.list().stream()
                .collect(Collectors.toMap(BrandDict::getId, BrandDict::getBrandName));
        Map<String, String> statusNameMap = transportStatusDictService.list().stream()
                .collect(Collectors.toMap(TransportStatusDict::getStatusCode, TransportStatusDict::getStatusName));

        List<Map<String, Object>> records = new ArrayList<>();
        for (OrderInfo order : orders) {
            VehicleTransit transit = transitMap.get(order.getId());
            if (transit == null && !returnNullTransits
                    && (transportStatus != null || monitorStatus != null)) continue;

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("orderId", order.getId());
            record.put("vin", order.getVin());
            record.put("brandName", brandMap.getOrDefault(order.getBrandId(), "未知"));
            record.put("orderReleaseTime", order.getOrderReleaseTime());
            record.put("originCity", order.getOriginCity());
            record.put("destCity", order.getDestCity());

            if (transit != null) {
                record.put("transportStatus", transit.getTransportStatus());
                record.put("transportStatusName", statusNameMap.getOrDefault(transit.getTransportStatus(), transit.getTransportStatus()));
                record.put("monitorStatus", transit.getMonitorStatus());
                record.put("batchId", transit.getBatchId());
            } else {
                record.put("transportStatus", null);
                record.put("transportStatusName", null);
                record.put("monitorStatus", null);
                record.put("batchId", null);
            }
            records.add(record);
        }
        return records;
    }

    private String objToStr(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private Map<String, Object> emptyPage() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", Collections.emptyList());
        result.put("total", 0);
        result.put("page", 1);
        result.put("size", 20);
        return result;
    }
}
