package com.company.roro.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.roro.dto.ExcelRowDTO;
import com.company.roro.dto.ImportResultDTO;
import com.company.roro.config.MonitorConfig;
import com.company.roro.entity.*;
import com.company.roro.service.*;
import com.company.roro.util.StatusCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在途数据处理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransitDataServiceImpl implements TransitDataService {

    private final BrandDictService brandDictService;
    private final OrderInfoService orderInfoService;
    private final RouteDictService routeDictService;
    private final RouteOtdConfigService routeOtdConfigService;
    private final VehicleTransitService vehicleTransitService;
    private final MonitorConfig monitorConfig;
    private final LocationAliasService locationAliasService;

    private static final int MAX_RETRIES = 3;

    @Override
    @Transactional
    public ImportResultDTO processExcelData(List<ExcelRowDTO> rows, String batchId) {

        ImportResultDTO result = new ImportResultDTO();
        result.setTotalCount(rows.size());

        ExcelRowDTO firstRow = rows.get(0);
        log.info("=== 第一条数据 ===");
        log.info("VIN: {}", firstRow.getVin());
        log.info("到店时间: {}", firstRow.getArriveShopTime());

        int successCount = 0;
        int routeMatched = 0;
        int routeUnmatched = 0;
        List<String> failDetails = new ArrayList<>();

        // Preload brand cache to avoid per-row DB queries
        Map<String, BrandDict> brandCache = brandDictService.list().stream()
            .collect(Collectors.toMap(BrandDict::getBrandName, b -> b, (a, b) -> a));
        log.info("预加载品牌: {} 条", brandCache.size());

        // Preload route cache: "品牌ID:出发地:目的地" -> RouteDict
        Map<String, RouteDict> routeCache = routeDictService.list().stream()
            .collect(Collectors.toMap(
                r -> r.getBrandId() + ":" + (r.getOriginCity() != null ? r.getOriginCity() : "") + ":" + (r.getDestCity() != null ? r.getDestCity() : ""),
                r -> r,
                (a, b) -> a
            ));
        log.info("预加载线路: {} 条", routeCache.size());

        // Preload OTD config cache: routeId -> RouteOtdConfig
        Map<Integer, RouteOtdConfig> otdConfigCache = routeOtdConfigService.lambdaQuery()
            .eq(RouteOtdConfig::getIsActive, 1)
            .list().stream()
            .collect(Collectors.toMap(RouteOtdConfig::getRouteId, c -> c, (a, b) -> a));
        log.info("预加载OTD配置: {} 条", otdConfigCache.size());

        // Preload location alias cache: alias -> standard_name
        Map<String, String> aliasMap = locationAliasService.list().stream()
            .collect(Collectors.toMap(LocationAlias::getAlias, LocationAlias::getStandardName, (a, b) -> a));
        log.info("预加载地点别名: {} 条", aliasMap.size());

        // 预加载订单缓存（避免逐行查库）
        List<String> vins = rows.stream().map(ExcelRowDTO::getVin).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, OrderInfo> orderCache = orderInfoService.lambdaQuery()
            .in(OrderInfo::getVin, vins)
            .list().stream()
            .collect(Collectors.toMap(OrderInfo::getVin, o -> o, (a, b) -> a));
        log.info("预加载订单: {} 条", orderCache.size());

        for (ExcelRowDTO row : rows) {
            try {
                // 1. 处理品牌 (cached lookup)
                BrandDict brand = brandCache.get(row.getBrandName());
                if (brand == null && !StrUtil.isBlank(row.getBrandName())) {
                    brand = new BrandDict();
                    brand.setBrandName(row.getBrandName());
                    brand.setIsActive(1);
                    brandDictService.save(brand);
                    brandCache.put(row.getBrandName(), brand);
                }
                if (brand == null) {
                    log.error("品牌不存在且无法创建: {}", row.getBrandName());
                    continue;
                }

                // 2. 处理订单
                OrderInfo order = getOrCreateOrder(brand.getId(), row, orderCache);
                if (order == null) {
                    log.error("订单处理失败: VIN={}", row.getVin());
                    continue;
                }

                // 3. 匹配线路 (cached lookup)
                // Normalize location names through alias map
                String normalizedOriginCity = row.getOriginCity();
                String normalizedDestCity = row.getDestCity();
                if (normalizedOriginCity != null) {
                    normalizedOriginCity = aliasMap.getOrDefault(normalizedOriginCity, normalizedOriginCity);
                }
                if (normalizedDestCity != null) {
                    normalizedDestCity = aliasMap.getOrDefault(normalizedDestCity, normalizedDestCity);
                }
                String routeKey = brand.getId() + ":" + (normalizedOriginCity != null ? normalizedOriginCity : "") + ":" + (normalizedDestCity != null ? normalizedDestCity : "");
                RouteDict route = routeCache.get(routeKey);

                if (route != null) {
                    routeMatched++;
                } else {
                    routeUnmatched++;
                    String vin = row.getVin() != null ? row.getVin() : "未知VIN";
                    String destCity = normalizedDestCity != null ? normalizedDestCity : "未知城市";
                    failDetails.add("VIN=" + vin + " 城市=" + destCity + " 出发地=" + (normalizedOriginCity != null ? normalizedOriginCity : "") + " 线路未匹配");
                }

                // 4. 获取 OTD 配置 (cached lookup)
                RouteOtdConfig otdConfig = null;
                if (route != null) {
                    otdConfig = otdConfigCache.get(route.getId());
                    // Only update order when routeId actually changes
                    if (!route.getId().equals(order.getRouteId())) {
                        order.setRouteId(route.getId());
                        orderInfoService.updateById(order);
                    }
                }

                // 5. 构建在途记录
                VehicleTransit transit = buildVehicleTransit(order.getId(), row, batchId);

                // 6. 计算在途状态
                String transportStatus = StatusCalculator.calculateTransportStatus(transit);
                transit.setTransportStatus(transportStatus);

                // 7. 计算监控状态
                String monitorStatus = StatusCalculator.calculateMonitorStatus(
                        transit,
                        otdConfig,
                        order.getOrderReleaseTime()
                );
                transit.setMonitorStatus(monitorStatus);

                // 7b. 计算整段监控状态
                String overallMonitorStatus = StatusCalculator.calculateOverallMonitorStatus(
                        transit,
                        otdConfig,
                        order.getOrderReleaseTime(),
                        LocalDateTime.now(),
                        monitorConfig.getOverallWarnRatio()
                );
                transit.setOverallMonitorStatus(overallMonitorStatus);

                // 7c. 计算三段监控状态
                String sectionMonitorStatus = StatusCalculator.calculateSectionMonitorStatus(
                        transit, otdConfig, order.getOrderReleaseTime(), LocalDateTime.now(), monitorConfig.getOverallWarnRatio()
                );
                transit.setSectionMonitorStatus(sectionMonitorStatus);

                // 8. Upsert 到 vehicle_transit 表
                upsertVehicleTransit(transit);

                successCount++;

            } catch (Exception e) {
                log.error("处理行数据失败: VIN={}, 错误: {}", row.getVin(), e.getMessage(), e);
                failDetails.add("VIN=" + (row.getVin() != null ? row.getVin() : "?") + " 处理失败: " + e.getMessage());
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(rows.size() - successCount);
        result.setRouteMatchedCount(routeMatched);
        result.setRouteUnmatchedCount(routeUnmatched);
        result.setFailDetails(failDetails);

        return result;
    }

    /**
     * 获取或创建品牌
     */
    private BrandDict getOrCreateBrand(String brandName) {
        if (StrUtil.isBlank(brandName)) {
            return null;
        }

        // 先按名称查找
        BrandDict brand = brandDictService.lambdaQuery()
                .eq(BrandDict::getBrandName, brandName)
                .one();

        if (brand == null) {
            // 不存在则创建
            brand = new BrandDict();
            brand.setBrandName(brandName);
            brand.setIsActive(1);
            brandDictService.save(brand);
        }

        return brand;
    }

    /**
     * 获取或创建订单
     */
    private OrderInfo getOrCreateOrder(Integer brandId, ExcelRowDTO row, Map<String, OrderInfo> orderCache) {
        if (row.getVin() == null) {
            return null;
        }

        // 优先从缓存获取
        OrderInfo order = orderCache.get(row.getVin());
        if (order == null) {
            // 缓存未命中，回退查库
            order = orderInfoService.lambdaQuery()
                    .eq(OrderInfo::getVin, row.getVin())
                    .one();
        }

        if (order == null) {
            // 不存在则创建
            order = new OrderInfo();
            order.setBrandId(brandId);
            order.setVin(row.getVin());
            order.setOrderReleaseTime(row.getOrderReleaseTime());
            order.setOriginCity(row.getOriginCity());
            order.setDestCity(row.getDestCity());
            order.setIsActive(1);
            orderInfoService.save(order);
        } else {
            // 存在则更新出发地/目的地/订单释放时间（如果 Excel 中有新值）
            boolean needUpdate = false;
            if (row.getOrderReleaseTime() != null && !row.getOrderReleaseTime().equals(order.getOrderReleaseTime())) {
                order.setOrderReleaseTime(row.getOrderReleaseTime());
                needUpdate = true;
            }
            if (StrUtil.isNotBlank(row.getOriginCity()) && !row.getOriginCity().equals(order.getOriginCity())) {
                order.setOriginCity(row.getOriginCity());
                needUpdate = true;
            }
            if (StrUtil.isNotBlank(row.getDestCity()) && !row.getDestCity().equals(order.getDestCity())) {
                order.setDestCity(row.getDestCity());
                needUpdate = true;
            }
            if (needUpdate) {
                orderInfoService.updateById(order);
            }
        }

        return order;
    }

    /**
     * 匹配线路
     */
    private RouteDict matchRoute(Integer brandId, String originCity, String destCity) {
        if (brandId == null || StrUtil.isBlank(originCity) || StrUtil.isBlank(destCity)) {
            return null;
        }

        // 根据品牌、出发地、目的地匹配
        return routeDictService.lambdaQuery()
                .eq(RouteDict::getBrandId, brandId)
                .eq(RouteDict::getOriginCity, originCity)
                .eq(RouteDict::getDestCity, destCity)
                .eq(RouteDict::getIsActive, 1)
                .one();
    }

    /**
     * 构建在途记录对象
     */
    private VehicleTransit buildVehicleTransit(Integer orderId, ExcelRowDTO row, String batchId) {
        VehicleTransit transit = new VehicleTransit();
        transit.setOrderId(orderId);
        transit.setDepartWarehouseTime(row.getDepartWarehouseTime());
        transit.setArrivePortTime(row.getArrivePortTime());
        transit.setShipDepartTime(row.getShipDepartTime());
        transit.setShipArriveTime(row.getShipArriveTime());
        transit.setUnloadFinishTime(row.getUnloadFinishTime());
        transit.setDispatchTime(row.getDispatchTime());
        transit.setArriveShopTime(row.getArriveShopTime());
        transit.setBatchId(batchId);
        transit.setDataSource("EXCEL");
        transit.setUpdatedAt(LocalDateTime.now());
        return transit;
    }

    /**
     * Upsert 在途记录（带乐观锁冲突重试）
     */
    private void upsertVehicleTransit(VehicleTransit transit) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                boolean success = tryUpsertVehicleTransit(transit);
                if (success) {
                    return;
                }
                // updateById 返回 false 表示乐观锁冲突（version 不匹配）
                if (attempt == MAX_RETRIES) {
                    log.error("更新车辆状态失败，乐观锁冲突已达最大重试次数，orderId={}", transit.getOrderId());
                    throw new RuntimeException("更新车辆状态失败，数据已被其他操作修改，orderId=" + transit.getOrderId());
                }
                log.warn("乐观锁冲突，第{}次重试，orderId={}", attempt, transit.getOrderId());
            } catch (DuplicateKeyException e) {
                if (attempt == MAX_RETRIES) {
                    log.error("保存车辆状态失败，唯一键冲突已达最大重试次数，orderId={}", transit.getOrderId());
                    throw new RuntimeException("保存车辆状态失败，数据冲突，orderId=" + transit.getOrderId(), e);
                }
                log.warn("唯一键冲突，第{}次重试，orderId={}", attempt, transit.getOrderId());
            }
        }
    }

    /**
     * 单次尝试 Upsert 在途记录
     * @return true 表示成功，false 表示乐观锁冲突需要重试
     */
    private boolean tryUpsertVehicleTransit(VehicleTransit transit) {
        log.info("准备入库，orderId: {}", transit.getOrderId());
        log.info("  arriveShopTime: {}", transit.getArriveShopTime());

        VehicleTransit existing = vehicleTransitService.lambdaQuery()
                .eq(VehicleTransit::getOrderId, transit.getOrderId())
                .one();

        if (existing != null) {
            if (transit.getDepartWarehouseTime() != null) {
                existing.setDepartWarehouseTime(transit.getDepartWarehouseTime());
            }
            if (transit.getArrivePortTime() != null) {
                existing.setArrivePortTime(transit.getArrivePortTime());
            }
            if (transit.getShipDepartTime() != null) {
                existing.setShipDepartTime(transit.getShipDepartTime());
            }
            if (transit.getShipArriveTime() != null) {
                existing.setShipArriveTime(transit.getShipArriveTime());
            }
            if (transit.getUnloadFinishTime() != null) {
                existing.setUnloadFinishTime(transit.getUnloadFinishTime());
            }
            if (transit.getDispatchTime() != null) {
                existing.setDispatchTime(transit.getDispatchTime());
            }
            if (transit.getArriveShopTime() != null) {
                existing.setArriveShopTime(transit.getArriveShopTime());
            }
            existing.setBatchId(transit.getBatchId());
            existing.setDataSource("EXCEL");
            existing.setUpdatedAt(LocalDateTime.now());

            // 重新计算在途状态（时间字段已更新）
            String newStatus = StatusCalculator.calculateTransportStatus(existing);
            existing.setTransportStatus(newStatus);

            // 复制已计算好的监控状态（在 processExcelData 中已根据完整上下文计算）
            existing.setMonitorStatus(transit.getMonitorStatus());
            existing.setOverallMonitorStatus(transit.getOverallMonitorStatus());
            existing.setSectionMonitorStatus(transit.getSectionMonitorStatus());

            boolean updated = vehicleTransitService.updateById(existing);
            if (updated) {
                log.info("  合并更新成功");
            }
            return updated;
        } else {
            vehicleTransitService.save(transit);
            log.info("  新增成功，ID: {}", transit.getId());
            return true;
        }
    }

    /**
     * 定时刷新监控状态 — 每 5 分钟重算所有在途车辆的三个监控状态字段
     */
    @Scheduled(cron = "${monitor.refresh-cron:0 */5 * * * ?}")
    @Transactional
    public void refreshMonitorStatus() {
        log.info("开始定时刷新监控状态");
        long start = System.currentTimeMillis();

        LocalDateTime now = LocalDateTime.now();
        double warnRatio = monitorConfig.getOverallWarnRatio();

        // 1. 查询所有在途车辆（排除已到达）
        List<VehicleTransit> transitList = vehicleTransitService.lambdaQuery()
                .ne(VehicleTransit::getTransportStatus, "ARRIVED")
                .list();

        if (transitList.isEmpty()) {
            log.info("无在途车辆需要刷新");
            return;
        }

        // 2. 批量加载关联数据
        List<Integer> orderIds = transitList.stream()
                .map(VehicleTransit::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, OrderInfo> orderMap = orderInfoService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getId, o -> o));

        List<Integer> routeIds = orderMap.values().stream()
                .map(OrderInfo::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, RouteOtdConfig> otdConfigMap = routeOtdConfigService.lambdaQuery()
                .in(RouteOtdConfig::getRouteId, routeIds)
                .eq(RouteOtdConfig::getIsActive, 1)
                .list().stream()
                .collect(Collectors.toMap(RouteOtdConfig::getRouteId, c -> c, (a, b) -> a));

        // 3. 逐车重算
        int successCount = 0;
        int failCount = 0;

        for (VehicleTransit transit : transitList) {
            try {
                OrderInfo order = orderMap.get(transit.getOrderId());
                if (order == null) continue;

                RouteOtdConfig otdConfig = otdConfigMap.get(order.getRouteId());

                // 重算三个监控状态
                String monitorStatus = StatusCalculator.calculateMonitorStatus(
                        transit, otdConfig, order.getOrderReleaseTime());
                String overallMonitorStatus = StatusCalculator.calculateOverallMonitorStatus(
                        transit, otdConfig, order.getOrderReleaseTime(), now, warnRatio);
                String sectionMonitorStatus = StatusCalculator.calculateSectionMonitorStatus(
                        transit, otdConfig, order.getOrderReleaseTime(), now, warnRatio);

                // 只在值发生变化时更新，减少 DB 写入
                boolean changed = false;
                if (!Objects.equals(transit.getMonitorStatus(), monitorStatus)) {
                    transit.setMonitorStatus(monitorStatus);
                    changed = true;
                }
                if (!Objects.equals(transit.getOverallMonitorStatus(), overallMonitorStatus)) {
                    transit.setOverallMonitorStatus(overallMonitorStatus);
                    changed = true;
                }
                if (!Objects.equals(transit.getSectionMonitorStatus(), sectionMonitorStatus)) {
                    transit.setSectionMonitorStatus(sectionMonitorStatus);
                    changed = true;
                }

                if (changed) {
                    transit.setUpdatedAt(LocalDateTime.now());
                    vehicleTransitService.updateById(transit);
                }
                successCount++;

            } catch (Exception e) {
                log.error("刷新监控状态失败, orderId={}, vin={}",
                        transit.getOrderId(),
                        orderMap.get(transit.getOrderId()) != null ? orderMap.get(transit.getOrderId()).getVin() : "?",
                        e);
                failCount++;
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("监控状态刷新完成: 成功={}, 失败={}, 耗时={}ms", successCount, failCount, elapsed);
    }
}
