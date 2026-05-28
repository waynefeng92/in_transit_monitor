package com.company.roro.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.roro.dto.ExcelRowDTO;
import com.company.roro.config.MonitorConfig;
import com.company.roro.entity.*;
import com.company.roro.service.*;
import com.company.roro.util.StatusCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在途数据处理服务实现
 */
@Service
@RequiredArgsConstructor
public class TransitDataServiceImpl implements TransitDataService {

    private final BrandDictService brandDictService;
    private final OrderInfoService orderInfoService;
    private final RouteDictService routeDictService;
    private final RouteOtdConfigService routeOtdConfigService;
    private final VehicleTransitService vehicleTransitService;
    private final MonitorConfig monitorConfig;

    @Override
    @Transactional
    public int processExcelData(List<ExcelRowDTO> rows, String batchId) {

        ExcelRowDTO firstRow = rows.get(0);
        System.out.println("=== 第一条数据 ===");
        System.out.println("VIN: " + firstRow.getVin());
        System.out.println("到店时间: " + firstRow.getArriveShopTime());

        int successCount = 0;

        for (ExcelRowDTO row : rows) {
            try {
                // 1. 处理品牌
                BrandDict brand = getOrCreateBrand(row.getBrandName());
                if (brand == null) {
                    System.err.println("品牌不存在且无法创建: " + row.getBrandName());
                    continue;
                }

                // 2. 处理订单
                OrderInfo order = getOrCreateOrder(brand.getId(), row);
                if (order == null) {
                    System.err.println("订单处理失败: VIN=" + row.getVin());
                    continue;
                }

                // 3. 匹配线路
                RouteDict route = matchRoute(brand.getId(), row.getOriginCity(), row.getDestCity());

                // 4. 获取 OTD 配置
                RouteOtdConfig otdConfig = null;
                if (route != null) {
                    otdConfig = routeOtdConfigService.lambdaQuery()
                            .eq(RouteOtdConfig::getRouteId, route.getId())
                            .eq(RouteOtdConfig::getIsActive, 1)
                            .one();
                    order.setRouteId(route.getId());
                    orderInfoService.updateById(order);
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
                System.err.println("处理行数据失败: VIN=" + row.getVin() + ", 错误: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return successCount;
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
    private OrderInfo getOrCreateOrder(Integer brandId, ExcelRowDTO row) {
        if (row.getVin() == null || row.getOrderReleaseTime() == null) {
            return null;
        }

        // 根据 VIN + 订单释放时间 查找
        OrderInfo order = orderInfoService.lambdaQuery()
                .eq(OrderInfo::getVin, row.getVin())
                .eq(OrderInfo::getOrderReleaseTime, row.getOrderReleaseTime())
                .one();

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
            // 存在则更新出发地/目的地（如果 Excel 中有新值）
            boolean needUpdate = false;
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
     * Upsert 在途记录
     */
    private void upsertVehicleTransit(VehicleTransit transit) {
        System.out.println("准备入库，orderId: " + transit.getOrderId());
        System.out.println("  arriveShopTime: " + transit.getArriveShopTime());

        VehicleTransit existing = vehicleTransitService.lambdaQuery()
                .eq(VehicleTransit::getOrderId, transit.getOrderId())
                .one();

        if (existing != null) {
            transit.setId(existing.getId());
            vehicleTransitService.updateById(transit);
            System.out.println("  更新成功");
        } else {
            vehicleTransitService.save(transit);
            System.out.println("  新增成功，ID: " + transit.getId());
        }
    }
}
