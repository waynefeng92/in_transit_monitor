package com.company.roro.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.company.roro.dto.VehicleDetailDTO;
import com.company.roro.dto.VehicleDetailDTO.SegmentDetail;
import com.company.roro.entity.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VehicleDetailService 单元测试 — TDD 先行
 *
 * <p>测试固定 7 段运输详情计算逻辑：</p>
 * <ul>
 *   <li>段状态判定: NORMAL / WARN / OVERDUE / PENDING / N/A</li>
 *   <li>edge cases: 无在途记录、无 OTD 配置、VIN 不存在</li>
 *   <li>多订单取最新、在途使用当前时间</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class VehicleDetailServiceTest {

    @Mock
    private OrderInfoService orderInfoService;
    @Mock
    private VehicleTransitService vehicleTransitService;
    @Mock
    private RouteOtdConfigService routeOtdConfigService;
    @Mock
    private BrandDictService brandDictService;
    @Mock
    private RouteDictService routeDictService;
    @Mock
    private TransportStatusDictService transportStatusDictService;

    private VehicleDetailService service;

    /** 测试固定当前时间 */
    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 6, 26, 12, 0, 0);

    @BeforeEach
    void setUp() {
        VehicleDetailService realService = new VehicleDetailService(
                orderInfoService, vehicleTransitService, routeOtdConfigService,
                brandDictService, routeDictService, transportStatusDictService);
        service = spy(realService);
        lenient().doReturn(fixedNow).when(service).now();
    }

    // ==================== Test 1: 已完成车辆 — 全部 7 段 NORMAL ====================

    @Test
    void completedVehicle_all7SegmentsNormal() {
        String vin = "LSVAU2A36N2112345";

        // 构造 OTD 配置: 每段 OTD=50h, Warn=30h
        RouteOtdConfig otdConfig = buildOtdConfig(50.0, 30.0);

        // 构造时间: 所有实际耗时 < 30h → NORMAL
        // orderReleaseTime 最早, arriveShopTime 最晚
        LocalDateTime t0 = fixedNow.minusHours(100); // orderReleaseTime
        LocalDateTime t1 = fixedNow.minusHours(85);  // departWarehouseTime  (seg1: 15h)
        LocalDateTime t2 = fixedNow.minusHours(78);  // arrivePortTime      (seg2: 7h)
        LocalDateTime t3 = fixedNow.minusHours(60);  // shipDepartTime      (seg3: 18h)
        LocalDateTime t4 = fixedNow.minusHours(38);  // shipArriveTime      (seg4: 22h)
        LocalDateTime t5 = fixedNow.minusHours(28);  // unloadFinishTime    (seg5: 10h)
        LocalDateTime t6 = fixedNow.minusHours(18);  // dispatchTime        (seg6: 10h)
        LocalDateTime t7 = fixedNow.minusHours(5);   // arriveShopTime      (seg7: 13h)

        OrderInfo order = buildOrder(1, vin, "上海", "广州", 10, 100, t0);
        VehicleTransit transit = buildTransit(1, "ARRIVED", t1, t2, t3, t4, t5, t6, t7);
        BrandDict brand = buildBrand(100, "上汽名爵");
        RouteDict route = buildRoute(10, "上海", "广州");

        // Mock lambdaQuery chains
        mockOrderQuery(order);
        mockTransitQuery(transit);
        mockOtdConfigQuery(otdConfig);
        when(brandDictService.getById(100)).thenReturn(brand);
        when(routeDictService.getById(10)).thenReturn(route);
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        // Execute
        VehicleDetailDTO result = service.getVehicleDetail(vin);

        // Assert basic fields
        assertNotNull(result);
        assertEquals(vin, result.getVin());
        assertEquals("上汽名爵", result.getBrandName());
        assertEquals("上海", result.getOriginCity());
        assertEquals("广州", result.getDestCity());
        assertEquals("上海-广州", result.getRouteName());
        assertEquals("ARRIVED", result.getTransportStatus());
        assertEquals("已到达", result.getTransportStatusName());
        assertEquals(t0, result.getOrderReleaseTime());

        // Total: sum of 7 OTD fields = 50*7 = 350.0h
        assertEquals(350.0, result.getTotalStandardOtdHours(), 0.01);
        // Total actual: t0 → t7 = 95h
        assertEquals(95.0, result.getTotalActualHours(), 0.01);

        // Assert 7 segments — all NORMAL
        List<SegmentDetail> segments = result.getSegments();
        assertNotNull(segments);
        assertEquals(7, segments.size());

        assertSegment(segments.get(0), 1, "未出库", "NOT_DEPARTED", t0, t1, 50.0, 30.0, 15.0, "NORMAL");
        assertSegment(segments.get(1), 2, "集港在途", "TO_PORT", t1, t2, 50.0, 30.0, 7.0, "NORMAL");
        assertSegment(segments.get(2), 3, "已集港待装船", "AT_PORT_WAIT_SHIP", t2, t3, 50.0, 30.0, 18.0, "NORMAL");
        assertSegment(segments.get(3), 4, "水运在途", "ON_SEA", t3, t4, 50.0, 30.0, 22.0, "NORMAL");
        assertSegment(segments.get(4), 5, "已到港待卸船", "AT_DEST_WAIT_UNLOAD", t4, t5, 50.0, 30.0, 10.0, "NORMAL");
        assertSegment(segments.get(5), 6, "已卸船待分拨", "UNLOADED_WAIT_DISPATCH", t5, t6, 50.0, 30.0, 10.0, "NORMAL");
        assertSegment(segments.get(6), 7, "分拨在途", "DISPATCHING", t6, t7, 50.0, 30.0, 13.0, "NORMAL");
    }

    // ==================== Test 2: 在途车辆 — 末尾段使用 now() ====================

    @Test
    void inTransitVehicle_lastSegmentUsesNow() {
        String vin = "LSVAU2A36N2999999";

        RouteOtdConfig otdConfig = buildOtdConfig(40.0, 25.0);

        // dispatchTime 和 arriveShopTime 为 null → seg6 使用 now(), seg7 PENDING
        // seg1-5 已完成，时间控制在 25h 内 → NORMAL
        LocalDateTime t0 = fixedNow.minusHours(120); // orderReleaseTime
        LocalDateTime t1 = fixedNow.minusHours(100); // departWarehouseTime  (seg1: 20h)
        LocalDateTime t2 = fixedNow.minusHours(88);  // arrivePortTime      (seg2: 12h)
        LocalDateTime t3 = fixedNow.minusHours(68);  // shipDepartTime      (seg3: 20h)
        LocalDateTime t4 = fixedNow.minusHours(44);  // shipArriveTime      (seg4: 24h)
        LocalDateTime t5 = fixedNow.minusHours(24);  // unloadFinishTime    (seg5: 20h)
        // t6 = dispatchTime = null
        // t7 = arriveShopTime = null

        OrderInfo order = buildOrder(1, vin, "上海", "广州", 10, 100, t0);
        VehicleTransit transit = buildTransit(1, "UNLOADED_WAIT_DISPATCH", t1, t2, t3, t4, t5, null, null);
        BrandDict brand = buildBrand(100, "上汽名爵");

        mockOrderQuery(order);
        mockTransitQuery(transit);
        mockOtdConfigQuery(otdConfig);
        when(brandDictService.getById(100)).thenReturn(brand);
        when(routeDictService.getById(10)).thenReturn(null); // 无线路
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNotNull(result);
        assertEquals(7, result.getSegments().size());

        // seg1-5: NORMAL (within 25h warn)
        assertSegment(result.getSegments().get(0), 1, "未出库", "NOT_DEPARTED", t0, t1, 40.0, 25.0, 20.0, "NORMAL");
        assertSegment(result.getSegments().get(1), 2, "集港在途", "TO_PORT", t1, t2, 40.0, 25.0, 12.0, "NORMAL");
        assertSegment(result.getSegments().get(2), 3, "已集港待装船", "AT_PORT_WAIT_SHIP", t2, t3, 40.0, 25.0, 20.0, "NORMAL");
        assertSegment(result.getSegments().get(3), 4, "水运在途", "ON_SEA", t3, t4, 40.0, 25.0, 24.0, "NORMAL");
        assertSegment(result.getSegments().get(4), 5, "已到港待卸船", "AT_DEST_WAIT_UNLOAD", t4, t5, 40.0, 25.0, 20.0, "NORMAL");

        // seg6: startTime=t5, endTime=null → uses now(), seg6 duration = t5→now = 24h
        SegmentDetail seg6 = result.getSegments().get(5);
        assertEquals(6, seg6.getSegmentIndex());
        assertEquals("UNLOADED_WAIT_DISPATCH", seg6.getStatusCode());
        assertEquals(t5, seg6.getStartTime());
        assertNull(seg6.getEndTime());
        assertEquals(40.0, seg6.getStandardOtdHours(), 0.01);
        assertEquals(25.0, seg6.getWarnThresholdHours(), 0.01);
        // actualDuration = now - t5 = 24h, 24 <= 25 → NORMAL
        assertEquals(24.0, seg6.getActualDurationHours(), 0.01);
        assertEquals("NORMAL", seg6.getStatus());

        // seg7: startTime=null, endTime=null → PENDING
        SegmentDetail seg7 = result.getSegments().get(6);
        assertEquals(7, seg7.getSegmentIndex());
        assertEquals("DISPATCHING", seg7.getStatusCode());
        assertNull(seg7.getStartTime());
        assertNull(seg7.getEndTime());
        assertEquals(40.0, seg7.getStandardOtdHours(), 0.01);
        assertEquals(25.0, seg7.getWarnThresholdHours(), 0.01);
        assertNull(seg7.getActualDurationHours());
        assertEquals("PENDING", seg7.getStatus());

        // Total actual uses now() since arriveShopTime is null
        assertEquals(120.0, result.getTotalActualHours(), 0.01);

        // Verify now() was called at least once (for the in-progress segment)
        verify(service, atLeastOnce()).now();
    }

    // ==================== Test 3: 无在途记录 ====================

    @Test
    void noTransitRecord_returnsDtoWithEmptySegments() {
        String vin = "LSVAU2A36N2888888";
        LocalDateTime t0 = fixedNow.minusHours(50);

        OrderInfo order = buildOrder(1, vin, "北京", "深圳", 20, 200, t0);
        BrandDict brand = buildBrand(200, "比亚迪");

        mockOrderQuery(order);
        // VehicleTransit 查询返回 null
        mockTransitQuery(null);
        when(brandDictService.getById(200)).thenReturn(brand);
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNotNull(result);
        assertEquals(vin, result.getVin());
        assertEquals("比亚迪", result.getBrandName());
        assertEquals("北京", result.getOriginCity());
        assertEquals("深圳", result.getDestCity());
        assertEquals("无在途记录", result.getTransportStatus());
        assertEquals("无在途记录", result.getTransportStatusName());
        assertEquals(t0, result.getOrderReleaseTime());
        assertNull(result.getTotalStandardOtdHours());
        assertNull(result.getTotalActualHours());

        // Segments 为空列表
        assertNotNull(result.getSegments());
        assertTrue(result.getSegments().isEmpty());
    }

    // ==================== Test 4: 无 OTD 配置 — 全部 N/A ====================

    @Test
    void noOtdConfig_allSegmentsNA() {
        String vin = "LSVAU2A36N2777777";

        LocalDateTime t0 = fixedNow.minusHours(80);
        LocalDateTime t1 = fixedNow.minusHours(70);
        LocalDateTime t2 = fixedNow.minusHours(65);
        LocalDateTime t3 = fixedNow.minusHours(45);
        LocalDateTime t4 = fixedNow.minusHours(25);
        LocalDateTime t5 = fixedNow.minusHours(15);
        LocalDateTime t6 = fixedNow.minusHours(8);
        LocalDateTime t7 = fixedNow.minusHours(2);

        OrderInfo order = buildOrder(1, vin, "成都", "重庆", 30, 300, t0);
        VehicleTransit transit = buildTransit(1, "ARRIVED", t1, t2, t3, t4, t5, t6, t7);
        BrandDict brand = buildBrand(300, "长安汽车");

        mockOrderQuery(order);
        mockTransitQuery(transit);
        mockOtdConfigQuery(null); // 无 OTD 配置
        when(brandDictService.getById(300)).thenReturn(brand);
        when(routeDictService.getById(30)).thenReturn(null);
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNotNull(result);
        assertEquals(7, result.getSegments().size());
        assertNull(result.getTotalStandardOtdHours()); // 无配置 → null

        // 全部段 status="N/A", standardOtdHours=null, warnThresholdHours=null
        for (SegmentDetail seg : result.getSegments()) {
            assertEquals("N/A", seg.getStatus());
            assertNull(seg.getStandardOtdHours());
            assertNull(seg.getWarnThresholdHours());
            assertNotNull(seg.getActualDurationHours()); // 有 start/end → 有 actual
        }
    }

    // ==================== Test 5: 边界测试 — 刚好超过 OTD → OVERDUE ====================

    @Test
    void overdueAtBoundary() {
        String vin = "LSVAU2A36N2666666";

        RouteOtdConfig otdConfig = buildOtdConfig(10.0, 5.0);

        // seg1: duration=11h, OTD=10h → 11 > 10 → OVERDUE
        // seg2: duration=6h, OTD=10h, Warn=5h → 6 > 5 but 6 <= 10 → WARN
        LocalDateTime t0 = fixedNow.minusHours(20);
        LocalDateTime t1 = fixedNow.minusHours(9);   // seg1: 11h
        LocalDateTime t2 = fixedNow.minusHours(3);   // seg2: 6h
        // seg3-7: no timestamps → PENDING

        OrderInfo order = buildOrder(1, vin, "上海", "南京", 40, 400, t0);
        VehicleTransit transit = new VehicleTransit();
        transit.setOrderId(1);
        transit.setTransportStatus("TO_PORT");
        transit.setDepartWarehouseTime(t1);
        transit.setArrivePortTime(t2);
        // shipDepartTime onwards = null

        BrandDict brand = buildBrand(400, "上汽大众");

        mockOrderQuery(order);
        mockTransitQuery(transit);
        mockOtdConfigQuery(otdConfig);
        when(brandDictService.getById(400)).thenReturn(brand);
        when(routeDictService.getById(40)).thenReturn(null);
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNotNull(result);
        List<SegmentDetail> segments = result.getSegments();
        assertEquals(7, segments.size());

        // seg1: 11h > 10h OTD → OVERDUE
        assertEquals("OVERDUE", segments.get(0).getStatus());
        assertEquals(11.0, segments.get(0).getActualDurationHours(), 0.01);

        // seg2: 6h > 5h warn and 6h <= 10h OTD → WARN
        assertEquals("WARN", segments.get(1).getStatus());
        assertEquals(6.0, segments.get(1).getActualDurationHours(), 0.01);

        // seg3: arrivePortTime=t2 is set as startTime, shipDepartTime is null
        // → IN-PROGRESS, uses now(). Duration = now() - t2 = 3h, 3 <= 5 warn → NORMAL
        SegmentDetail seg3 = segments.get(2);
        assertEquals("NORMAL", seg3.getStatus());
        assertEquals(t2, seg3.getStartTime());
        assertNull(seg3.getEndTime());
        assertEquals(3.0, seg3.getActualDurationHours(), 0.01);

        // seg4-7: startTime and endTime both null → PENDING
        for (int i = 3; i < 7; i++) {
            assertEquals("PENDING", segments.get(i).getStatus());
            assertNull(segments.get(i).getActualDurationHours());
        }
    }

    // ==================== Test 6: VIN 不存在 — 返回 null ====================

    @Test
    void vinNotFound_returnsNull() {
        String vin = "NONEXISTENT_VIN";

        // Mock 空列表
        mockEmptyOrderQuery();

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNull(result);
        // 不应查询其他服务
        verify(vehicleTransitService, never()).lambdaQuery();
        verify(brandDictService, never()).getById(anyInt());
    }

    // ==================== Test 7: 多个订单 — 取最新 ====================

    @Test
    void multipleOrders_takesLatest() {
        String vin = "LSVAU2A36N2555555";

        LocalDateTime earlierTime = fixedNow.minusDays(10);
        LocalDateTime laterTime = fixedNow.minusDays(2);

        // 较早的订单
        OrderInfo earlierOrder = buildOrder(100, vin, "旧城市A", "旧城市B", 50, 500, earlierTime);
        // 较新的订单
        OrderInfo laterOrder = buildOrder(200, vin, "上海", "广州", 60, 600, laterTime);

        VehicleTransit transit = buildTransit(200, "ARRIVED",
                laterTime.plusHours(10), laterTime.plusHours(15), laterTime.plusHours(35),
                laterTime.plusHours(55), laterTime.plusHours(65), laterTime.plusHours(75),
                laterTime.plusHours(85));
        BrandDict brand = buildBrand(600, "上汽通用");
        RouteDict route = buildRoute(60, "上海", "广州");
        RouteOtdConfig otdConfig = buildOtdConfig(50.0, 30.0);

        // Mock: lambdaQuery returns list with both orders, ordered by releaseTime desc
        LambdaQueryChainWrapper<OrderInfo> orderQuery = mockChainWrapper();
        when(orderInfoService.lambdaQuery()).thenReturn(orderQuery);
        when(orderQuery.eq(any(), any())).thenReturn(orderQuery);
        when(orderQuery.orderByDesc(any(SFunction.class))).thenReturn(orderQuery);
        when(orderQuery.list()).thenReturn(Arrays.asList(laterOrder, earlierOrder));

        mockTransitQuery(transit);
        mockOtdConfigQuery(otdConfig);
        when(brandDictService.getById(600)).thenReturn(brand);
        when(routeDictService.getById(60)).thenReturn(route);
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNotNull(result);
        // 应使用 latest 订单的数据
        assertEquals("上海", result.getOriginCity());
        assertEquals("广州", result.getDestCity());
        assertEquals("上海-广州", result.getRouteName());
        assertEquals(laterTime, result.getOrderReleaseTime());
    }

    // ==================== Test 8: WARN (未逾期) ====================

    @Test
    void warnButNotOverdue() {
        String vin = "LSVAU2A36N2444444";

        RouteOtdConfig otdConfig = buildOtdConfig(50.0, 20.0);

        // seg1: duration=30h, OTD=50h, Warn=20h → 30 > 20 and 30 <= 50 → WARN
        LocalDateTime t0 = fixedNow.minusHours(30);
        LocalDateTime t1 = fixedNow; // seg1: 30h

        OrderInfo order = buildOrder(1, vin, "上海", "杭州", 70, 700, t0);
        VehicleTransit transit = new VehicleTransit();
        transit.setOrderId(1);
        transit.setTransportStatus("TO_PORT");
        transit.setDepartWarehouseTime(t1);
        // rest null

        BrandDict brand = buildBrand(700, "吉利汽车");

        mockOrderQuery(order);
        mockTransitQuery(transit);
        mockOtdConfigQuery(otdConfig);
        when(brandDictService.getById(700)).thenReturn(brand);
        when(routeDictService.getById(70)).thenReturn(null);
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNotNull(result);
        List<SegmentDetail> segments = result.getSegments();

        // seg1: WARN (30h > 20h warn, 30h <= 50h OTD)
        SegmentDetail seg1 = segments.get(0);
        assertEquals("WARN", seg1.getStatus());
        assertEquals(30.0, seg1.getActualDurationHours(), 0.01);
        assertEquals(50.0, seg1.getStandardOtdHours(), 0.01);
        assertEquals(20.0, seg1.getWarnThresholdHours(), 0.01);

        // seg2: in-progress — departWarehouseTime set but arrivePortTime null
        // departure at t1=fixedNow, now=fixedNow → 0h elapsed ≤ 20h warn → NORMAL
        SegmentDetail seg2 = segments.get(1);
        assertEquals("NORMAL", seg2.getStatus());
        assertEquals(0.0, seg2.getActualDurationHours(), 0.01);
        assertEquals("集港在途", seg2.getSegmentName());

        // seg3-7: startTime and endTime both null → PENDING
        for (int i = 2; i < 7; i++) {
            assertEquals("PENDING", segments.get(i).getStatus());
        }
    }

    // ==================== Test 9: 隐式完成 — 段 N 有开始时间，则前面段隐式标记完成 ====================

    @Test
    void implicitCompletion_beforeStartedSegment() {
        String vin = "LRWYGCEK3TC767299";

        RouteOtdConfig otdConfig = buildOtdConfig(50.0, 30.0);

        // 模拟特斯拉车辆，无前端港口到达数据：
        // seg1: startTime=orderReleaseTime, endTime=departWarehouseTime(null)
        // seg2: startTime=null, endTime=null
        // seg3: startTime=null, endTime=shipDepartTime
        // seg4: startTime=shipDepartTime, endTime=null → 真正进行中
        LocalDateTime t0 = fixedNow.minusHours(50); // orderReleaseTime
        LocalDateTime shipDepartTime = fixedNow.minusHours(20); // seg3 end, seg4 start

        OrderInfo order = buildOrder(1, vin, "上海", "广州", 10, 100, t0);
        VehicleTransit transit = new VehicleTransit();
        transit.setOrderId(1);
        transit.setTransportStatus("ON_SEA");
        transit.setShipDepartTime(shipDepartTime);
        // departWarehouseTime, arrivePortTime, shipArriveTime = null

        BrandDict brand = buildBrand(100, "特斯拉");

        mockOrderQuery(order);
        mockTransitQuery(transit);
        mockOtdConfigQuery(otdConfig);
        when(brandDictService.getById(100)).thenReturn(brand);
        when(routeDictService.getById(10)).thenReturn(null);
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNotNull(result);
        List<SegmentDetail> segments = result.getSegments();
        assertEquals(7, segments.size());

        // seg1: startTime=t0, endTime=null → 原为 IN-PROGRESS，隐式完成变为 NORMAL
        SegmentDetail seg1 = segments.get(0);
        assertEquals(1, seg1.getSegmentIndex());
        assertEquals("NOT_DEPARTED", seg1.getStatusCode());
        assertEquals(t0, seg1.getStartTime());
        assertNull(seg1.getEndTime());
        assertNull(seg1.getActualDurationHours());
        assertEquals("NORMAL", seg1.getStatus());

        // seg2: startTime=null, endTime=null → 原为 PENDING，隐式完成变为 NORMAL
        SegmentDetail seg2 = segments.get(1);
        assertEquals(2, seg2.getSegmentIndex());
        assertEquals("TO_PORT", seg2.getStatusCode());
        assertNull(seg2.getStartTime());
        assertNull(seg2.getEndTime());
        assertNull(seg2.getActualDurationHours());
        assertEquals("NORMAL", seg2.getStatus());

        // seg3: startTime=null, endTime=shipDepartTime → 已有逻辑处理为 NORMAL（不变）
        SegmentDetail seg3 = segments.get(2);
        assertEquals(3, seg3.getSegmentIndex());
        assertEquals("AT_PORT_WAIT_SHIP", seg3.getStatusCode());
        assertNull(seg3.getStartTime());
        assertEquals(shipDepartTime, seg3.getEndTime());
        assertNull(seg3.getActualDurationHours());
        assertEquals("NORMAL", seg3.getStatus());

        // seg4: startTime=shipDepartTime, endTime=null → 真正进行中，不受影响（maxStartedIdx=3，i<3 不包含）
        SegmentDetail seg4 = segments.get(3);
        assertEquals(4, seg4.getSegmentIndex());
        assertEquals("ON_SEA", seg4.getStatusCode());
        assertEquals(shipDepartTime, seg4.getStartTime());
        assertNull(seg4.getEndTime());
        assertNotNull(seg4.getActualDurationHours()); // 使用 now()
        assertEquals("NORMAL", seg4.getStatus()); // 20h <= 30h warn

        // seg5-7: startTime=null, endTime=null → PENDING，不受影响
        for (int i = 4; i < 7; i++) {
            assertEquals("PENDING", segments.get(i).getStatus());
            assertNull(segments.get(i).getActualDurationHours());
        }
    }

    // ==================== Test 10: 隐式完成 — 运输状态传播 ====================

    @Test
    void implicitCompletion_fromTransportStatus() {
        String vin = "LRWYGCEK0TC766241";

        RouteOtdConfig otdConfig = buildOtdConfig(50.0, 30.0);

        // 模拟车辆：transportStatus="TO_PORT" 但只有 orderReleaseTime 有值
        // seg1: start=orderReleaseTime, end=null
        // seg2: start=null, end=null（无出库时间）
        // seg3-7: all null
        // 旧的 timestamp 逻辑：maxStartedIdx=0 → i < 0 → 无隐式完成
        // 新的状态逻辑：currentIdx=1 → completionBoundary=max(0,1)=1 → seg0 标记为已完成
        LocalDateTime t0 = fixedNow.minusHours(50);

        OrderInfo order = buildOrder(1, vin, "上海", "广州", 10, 100, t0);
        VehicleTransit transit = new VehicleTransit();
        transit.setOrderId(1);
        transit.setTransportStatus("TO_PORT");
        // departWarehouseTime and all later timestamps are null
        transit.setDepartWarehouseTime(null);
        transit.setArrivePortTime(null);

        BrandDict brand = buildBrand(100, "测试品牌");

        mockOrderQuery(order);
        mockTransitQuery(transit);
        mockOtdConfigQuery(otdConfig);
        when(brandDictService.getById(100)).thenReturn(brand);
        when(routeDictService.getById(10)).thenReturn(null);
        when(transportStatusDictService.list()).thenReturn(buildStatusDictList());

        VehicleDetailDTO result = service.getVehicleDetail(vin);

        assertNotNull(result);
        List<SegmentDetail> segments = result.getSegments();
        assertEquals(7, segments.size());

        // seg1（index 0）: 不应是 IN-PROGRESS，而是 NORMAL（transportStatus 隐式完成）
        SegmentDetail seg1 = segments.get(0);
        assertEquals(1, seg1.getSegmentIndex());
        assertEquals("NOT_DEPARTED", seg1.getStatusCode());
        assertEquals(t0, seg1.getStartTime());
        assertNull(seg1.getEndTime());
        assertNull(seg1.getActualDurationHours());
        assertEquals("NORMAL", seg1.getStatus());

        // seg2（index 1）: TO_PORT — 当前段，无开始时间 → PENDING
        SegmentDetail seg2 = segments.get(1);
        assertEquals(2, seg2.getSegmentIndex());
        assertEquals("TO_PORT", seg2.getStatusCode());
        assertNull(seg2.getStartTime());
        assertNull(seg2.getEndTime());
        assertNull(seg2.getActualDurationHours());
        assertEquals("PENDING", seg2.getStatus());

        // seg3-7: 未开始 → PENDING
        for (int i = 2; i < 7; i++) {
            assertEquals("PENDING", segments.get(i).getStatus());
            assertNull(segments.get(i).getActualDurationHours());
        }
    }

    // ==================== Helper Methods ====================

    /** 构造标准 RouteOtdConfig，所有 7 段统一设置 */
    private RouteOtdConfig buildOtdConfig(double otd, double warn) {
        RouteOtdConfig config = new RouteOtdConfig();
        config.setRouteId(10);
        config.setIsActive(1);
        config.setNotDepartedOtd(otd);
        config.setNotDepartedWarn(warn);
        config.setToPortOtd(otd);
        config.setToPortWarn(warn);
        config.setAtPortWaitOtd(otd);
        config.setAtPortWaitWarn(warn);
        config.setOnSeaOtd(otd);
        config.setOnSeaWarn(warn);
        config.setAtDestWaitOtd(otd);
        config.setAtDestWaitWarn(warn);
        config.setUnloadWaitDispatchOtd(otd);
        config.setUnloadWaitDispatchWarn(warn);
        config.setDispatchingOtd(otd);
        config.setDispatchingWarn(warn);
        return config;
    }

    /** 构造 OrderInfo */
    private OrderInfo buildOrder(int id, String vin, String originCity, String destCity,
                                  int routeId, int brandId, LocalDateTime orderReleaseTime) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setVin(vin);
        order.setOriginCity(originCity);
        order.setDestCity(destCity);
        order.setRouteId(routeId);
        order.setBrandId(brandId);
        order.setOrderReleaseTime(orderReleaseTime);
        return order;
    }

    /** 构造完整的 VehicleTransit (所有 7 段时间) */
    private VehicleTransit buildTransit(int orderId, String transportStatus,
                                         LocalDateTime departWarehouseTime,
                                         LocalDateTime arrivePortTime,
                                         LocalDateTime shipDepartTime,
                                         LocalDateTime shipArriveTime,
                                         LocalDateTime unloadFinishTime,
                                         LocalDateTime dispatchTime,
                                         LocalDateTime arriveShopTime) {
        VehicleTransit transit = new VehicleTransit();
        transit.setOrderId(orderId);
        transit.setTransportStatus(transportStatus);
        transit.setDepartWarehouseTime(departWarehouseTime);
        transit.setArrivePortTime(arrivePortTime);
        transit.setShipDepartTime(shipDepartTime);
        transit.setShipArriveTime(shipArriveTime);
        transit.setUnloadFinishTime(unloadFinishTime);
        transit.setDispatchTime(dispatchTime);
        transit.setArriveShopTime(arriveShopTime);
        return transit;
    }

    /** 构造 BrandDict */
    private BrandDict buildBrand(int id, String brandName) {
        BrandDict brand = new BrandDict();
        brand.setId(id);
        brand.setBrandName(brandName);
        return brand;
    }

    /** 构造 RouteDict */
    private RouteDict buildRoute(int id, String originCity, String destCity) {
        RouteDict route = new RouteDict();
        route.setId(id);
        route.setOriginCity(originCity);
        route.setDestCity(destCity);
        return route;
    }

    /** 构造 TransportStatusDict 列表 (所有 8 种状态) */
    private List<TransportStatusDict> buildStatusDictList() {
        return Arrays.asList(
                createStatusDict("NOT_DEPARTED", "未出库"),
                createStatusDict("TO_PORT", "集港在途"),
                createStatusDict("AT_PORT_WAIT_SHIP", "已集港待装船"),
                createStatusDict("ON_SEA", "水运在途"),
                createStatusDict("AT_DEST_WAIT_UNLOAD", "已到港待卸船"),
                createStatusDict("UNLOADED_WAIT_DISPATCH", "已卸船待分拨"),
                createStatusDict("DISPATCHING", "分拨在途"),
                createStatusDict("ARRIVED", "已到达")
        );
    }

    private TransportStatusDict createStatusDict(String statusCode, String statusName) {
        TransportStatusDict dict = new TransportStatusDict();
        dict.setStatusCode(statusCode);
        dict.setStatusName(statusName);
        return dict;
    }

    // ==================== Mock Setup Helpers ====================

    @SuppressWarnings("unchecked")
    private <T> LambdaQueryChainWrapper<T> mockChainWrapper() {
        return mock(LambdaQueryChainWrapper.class);
    }

    /** Mock orderInfoService.lambdaQuery() → returns given order list */
    @SuppressWarnings("unchecked")
    private void mockOrderQuery(OrderInfo... orders) {
        LambdaQueryChainWrapper<OrderInfo> query = mockChainWrapper();
        when(orderInfoService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.orderByDesc(any(SFunction.class))).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(orders));
    }

    /** Mock orderInfoService.lambdaQuery() → empty list */
    @SuppressWarnings("unchecked")
    private void mockEmptyOrderQuery() {
        LambdaQueryChainWrapper<OrderInfo> query = mockChainWrapper();
        when(orderInfoService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.orderByDesc(any(SFunction.class))).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());
    }

    /** Mock vehicleTransitService.lambdaQuery() → returns given transit (or null) */
    @SuppressWarnings("unchecked")
    private void mockTransitQuery(VehicleTransit transit) {
        LambdaQueryChainWrapper<VehicleTransit> query = mockChainWrapper();
        when(vehicleTransitService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.one()).thenReturn(transit);
    }

    /** Mock routeOtdConfigService.lambdaQuery() → returns given config (or null) */
    @SuppressWarnings("unchecked")
    private void mockOtdConfigQuery(RouteOtdConfig config) {
        LambdaQueryChainWrapper<RouteOtdConfig> query = mockChainWrapper();
        when(routeOtdConfigService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.one()).thenReturn(config);
    }

    // ==================== Segment Assertion Helper ====================

    private void assertSegment(SegmentDetail seg, int expectedIndex, String expectedName,
                                String expectedStatusCode, LocalDateTime expectedStart,
                                LocalDateTime expectedEnd, Double expectedOtd,
                                Double expectedWarn, Double expectedDuration,
                                String expectedStatus) {
        assertEquals(expectedIndex, seg.getSegmentIndex());
        assertEquals(expectedName, seg.getSegmentName());
        assertEquals(expectedStatusCode, seg.getStatusCode());
        assertEquals(expectedStart, seg.getStartTime());
        assertEquals(expectedEnd, seg.getEndTime());
        if (expectedOtd == null) {
            assertNull(seg.getStandardOtdHours());
        } else {
            assertEquals(expectedOtd, seg.getStandardOtdHours(), 0.01);
        }
        if (expectedWarn == null) {
            assertNull(seg.getWarnThresholdHours());
        } else {
            assertEquals(expectedWarn, seg.getWarnThresholdHours(), 0.01);
        }
        if (expectedDuration == null) {
            assertNull(seg.getActualDurationHours());
        } else {
            assertEquals(expectedDuration, seg.getActualDurationHours(), 0.01);
        }
        assertEquals(expectedStatus, seg.getStatus());
    }
}
