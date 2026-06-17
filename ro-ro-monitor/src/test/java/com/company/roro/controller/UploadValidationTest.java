package com.company.roro.controller;

import com.company.roro.dto.ExcelPreviewDTO;
import com.company.roro.service.ExcelParseService;
import com.company.roro.service.TransitDataService;
import com.company.roro.service.UploadBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UploadValidationTest {

    private MockMvc mockMvc;
    private UploadBatchService uploadBatchService;
    private ExcelParseService excelParseService;
    private TransitDataService transitDataService;
    private Executor executor;

    @BeforeEach
    void setUp() {
        UploadController controller = new UploadController();
        injectMocks(controller);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void injectMocks(UploadController controller) {
        try {
            uploadBatchService = mock(UploadBatchService.class);
            excelParseService = mock(ExcelParseService.class);
            transitDataService = mock(TransitDataService.class);
            executor = mock(Executor.class);

            var c = UploadController.class;
            var f1 = c.getDeclaredField("uploadBatchService");
            f1.setAccessible(true);
            f1.set(controller, uploadBatchService);

            var f2 = c.getDeclaredField("excelParseService");
            f2.setAccessible(true);
            f2.set(controller, excelParseService);

            var f3 = c.getDeclaredField("transitDataService");
            f3.setAccessible(true);
            f3.set(controller, transitDataService);

            var f4 = c.getDeclaredField("excelExecutor");
            f4.setAccessible(true);
            f4.set(controller, executor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }

    @Test
    void shouldAcceptXlsxFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes()
        );

        when(excelParseService.previewExcel(any(), anyInt(), anyInt()))
                .thenReturn(new ExcelPreviewDTO());

        mockMvc.perform(multipart("/api/upload/preview")
                        .file(file)
                        .param("brandId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectTextFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt",
                "text/plain",
                "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/preview")
                        .file(file)
                        .param("brandId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅支持 .xlsx 和 .xls 格式文件"));
    }

    @Test
    void shouldRejectNoFile() throws Exception {
        mockMvc.perform(multipart("/api/upload/preview")
                        .param("brandId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请选择要上传的文件"));
    }
}
