package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verify all controller endpoints return Result<T> wrapper (code/message/data).
 *
 * Tests controllers that don't require complex MyBatis chain mocking.
 * Uses standalone MockMvc setup with mocked services via reflection.
 */
class ResultWrapperConsistencyTest {

    private void inject(Object controller, String fieldName, Object value) {
        try {
            var field = controller.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(controller, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject " + fieldName, e);
        }
    }

    @Test
    void monitorStatusList_returnsResultWrapper() throws Exception {
        var svc = org.mockito.Mockito.mock(MonitorStatusDictService.class);
        org.mockito.Mockito.when(svc.list()).thenReturn(java.util.Collections.emptyList());
        MonitorStatusController ctrl = new MonitorStatusController();
        inject(ctrl, "monitorStatusDictService", svc);
        MockMvcBuilders.standaloneSetup(ctrl).build()
                .perform(get("/api/monitor-status/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void locationAliasList_returnsResultWrapper() throws Exception {
        var svc = org.mockito.Mockito.mock(LocationAliasService.class);
        org.mockito.Mockito.when(svc.list()).thenReturn(java.util.Collections.emptyList());
        LocationAliasController ctrl = new LocationAliasController();
        inject(ctrl, "locationAliasService", svc);
        MockMvcBuilders.standaloneSetup(ctrl).build()
                .perform(get("/api/location-alias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void excelMappingStandardFields_returnsResultWrapper() throws Exception {
        var ctrl = new ExcelMappingController();
        inject(ctrl, "brandDictService", org.mockito.Mockito.mock(BrandDictService.class));
        MockMvcBuilders.standaloneSetup(ctrl).build()
                .perform(get("/api/excel-mapping/standard-fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void arrivedSummary_returnsResultWrapper() throws Exception {
        var arrivedSvc = org.mockito.Mockito.mock(ArrivedVehicleService.class);
        org.mockito.Mockito.when(arrivedSvc.calculateSummary(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.company.roro.dto.ArrivedSummaryDTO());
        ArrivedController ctrl = new ArrivedController();
        inject(ctrl, "arrivedVehicleService", arrivedSvc);
        MockMvcBuilders.standaloneSetup(ctrl).build()
                .perform(get("/api/arrived/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").isMap());
    }
}
