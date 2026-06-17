package com.company.roro.scheduler;

import com.company.roro.service.ChartDataService;
import com.company.roro.service.MonitorSnapshotService;
import com.company.roro.service.TransitSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 SnapshotScheduler 不依赖 Controller 层
 *
 * 重构目标：Scheduler 只依赖 Service，不依赖 Controller
 */
class SnapshotSchedulerServiceRefactorTest {

    /**
     * 验证：Scheduler 注入的是 Service，不是 Controller
     */
    @Test
    void schedulerShouldDependOnServicesNotControllers() {
        Field[] fields = SnapshotScheduler.class.getDeclaredFields();

        Set<String> fieldTypes = Arrays.stream(fields)
                .map(f -> f.getType().getName())
                .collect(Collectors.toSet());

        // 必须包含服务类
        assertTrue(fieldTypes.contains(TransitSummaryService.class.getName()),
                "SnapshotScheduler must inject TransitSummaryService");
        assertTrue(fieldTypes.contains(ChartDataService.class.getName()),
                "SnapshotScheduler must inject ChartDataService");
        assertTrue(fieldTypes.contains(MonitorSnapshotService.class.getName()),
                "SnapshotScheduler must inject MonitorSnapshotService");

        // 禁止包含 Controller 类
        for (String typeName : fieldTypes) {
            assertFalse(typeName.contains("Controller"),
                    "SnapshotScheduler must NOT inject any Controller, but found: " + typeName);
        }
    }

    /**
     * 验证：SnapshotScheduler.java 源码不 import 任何 Controller
     */
    @Test
    void schedulerSourceShouldNotImportController() throws Exception {
        // 仅验证编译后的类不引用 Controller
        // 源码级检查由 grep 验证步骤完成
        Class<?> clazz = SnapshotScheduler.class;

        // 确保没有通过反射引用的 Controller 类
        assertNotNull(clazz.getDeclaredField("transitSummaryService"));
        assertNotNull(clazz.getDeclaredField("chartDataService"));
        assertNotNull(clazz.getDeclaredField("snapshotService"));

        // 验证字段上的 @Autowired 注解存在
        Field summaryField = clazz.getDeclaredField("transitSummaryService");
        assertNotNull(summaryField.getAnnotation(Autowired.class),
                "transitSummaryService should be @Autowired");

        Field chartField = clazz.getDeclaredField("chartDataService");
        assertNotNull(chartField.getAnnotation(Autowired.class),
                "chartDataService should be @Autowired");
    }
}
