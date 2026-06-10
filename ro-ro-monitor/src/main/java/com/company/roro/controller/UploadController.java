package com.company.roro.controller;

import com.company.roro.dto.ExcelPreviewDTO;
import com.company.roro.dto.ExcelRowDTO;
import com.company.roro.entity.UploadBatch;
import com.company.roro.service.ExcelParseService;
import com.company.roro.service.TransitDataService;
import com.company.roro.service.UploadBatchService;
import com.company.roro.util.BatchIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 文件上传接口
 *
 * 功能：
 * 1. 预览 Excel 文件（获取 Sheet 列表和前5行数据）
 * 2. 上传在途 Excel 文件，异步解析并入库
 * 3. 查询上传历史
 *
 * 处理状态说明：
 * - PROCESSING：处理中
 * - SUCCESS：处理成功
 * - FAILED：处理失败
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private UploadBatchService uploadBatchService;

    @Autowired
    private ExcelParseService excelParseService;

    @Autowired
    private TransitDataService transitDataService;

    /**
     * Excel 处理专用线程池
     * 用于异步解析 Excel 文件，避免阻塞主线程
     */
    @Autowired
    @Qualifier("excelExecutor")
    private Executor excelExecutor;

    /**
     * 预览 Excel 文件
     *
     * 上传前先预览，让用户确认 Sheet 和数据格式是否正确
     *
     * @param file Excel 文件
     * @param brandId 品牌ID
     * @param sheetIndex Sheet索引（可选，不传则使用配置规则自动定位）
     * @return Sheet列表、表头、前5行预览数据
     */
    @PostMapping("/preview")
    public ExcelPreviewDTO previewExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam Integer brandId,
            @RequestParam(required = false) Integer sheetIndex) throws Exception {
        return excelParseService.previewExcel(file, brandId, sheetIndex);
    }

    /**
     * 上传在途 Excel 文件
     *
     * 流程：
     * 1. 生成批次号
     * 2. 记录上传批次（状态：PROCESSING）
     * 3. 将文件内容读入内存（避免异步线程中临时文件被清理）
     * 4. 异步解析 Excel 并处理数据入库
     * 5. 立即返回批次号
     * 6. 处理完成后更新批次状态（SUCCESS/FAILED）
     *
     * @param file Excel 文件
     * @param user 上传人（可选，默认 system）
     * @param brandId 品牌ID
     * @param sheetIndex 用户选择的 Sheet 索引（可选，不传则使用配置规则）
     * @return 批次号和处理结果
     */
    @PostMapping("/excel")
    public Map<String, Object> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "user", defaultValue = "system") String user,
            @RequestParam Integer brandId,
            @RequestParam(required = false) Integer sheetIndex) throws IOException {

        // 1. 生成批次号
        String batchId = BatchIdGenerator.generate();

        // 2. 记录上传批次（初始状态：处理中）
        UploadBatch batch = new UploadBatch();
        batch.setBatchId(batchId);
        batch.setFileName(file.getOriginalFilename());
        batch.setUploadUser(user);
        batch.setRecordCount(0);
        batch.setStatus("PROCESSING");
        uploadBatchService.save(batch);

        // 3. 将文件内容读取到内存（关键：避免异步线程中临时文件被清理）
        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // 4. 异步处理
        excelExecutor.execute(() -> {
            System.out.println("=== 异步任务开始执行，批次: " + batchId + " ===");
            try {
                // 使用内存中的字节数组创建新的 MultipartFile 对象
                MultipartFile asyncFile = new InMemoryMultipartFile(
                        originalFilename,
                        originalFilename,
                        contentType,
                        fileBytes
                );

                // 解析 Excel
                List<ExcelRowDTO> rows = excelParseService.parseExcel(asyncFile, brandId, sheetIndex);
                System.out.println("解析完成，共 " + (rows != null ? rows.size() : 0) + " 条数据");

                // 入库处理
                int successCount = transitDataService.processExcelData(rows, batchId);
                System.out.println("入库完成，成功 " + successCount + " 条");

                // 更新批次状态为成功
                UploadBatch updateBatch = new UploadBatch();
                updateBatch.setId(batch.getId());
                updateBatch.setRecordCount(successCount);
                updateBatch.setStatus("SUCCESS");
                uploadBatchService.updateById(updateBatch);

                System.out.println("=== 批次 " + batchId + " 处理完成，成功 " + successCount + " 条 ===");
            } catch (Exception e) {
                System.err.println("=== 批次 " + batchId + " 处理失败 ===");
                e.printStackTrace();

                // 更新批次状态为失败，记录错误信息
                UploadBatch updateBatch = new UploadBatch();
                updateBatch.setId(batch.getId());
                updateBatch.setStatus("FAILED");
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.length() > 500) {
                    errorMsg = errorMsg.substring(0, 500) + "...";
                }
                updateBatch.setErrorMessage(errorMsg);
                uploadBatchService.updateById(updateBatch);
            }
        });

        // 5. 立即返回批次号
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("batchId", batchId);
        result.put("fileName", originalFilename);
        result.put("message", "文件上传成功，正在后台处理中...");
        return result;
    }

    /**
     * 查询上传历史
     *
     * @return 上传批次列表，按上传时间倒序排列
     */
    @GetMapping("/history")
    public List<UploadBatch> history() {
        return uploadBatchService.lambdaQuery()
                .orderByDesc(UploadBatch::getUploadTime)
                .list();
    }

    /**
     * 根据批次号查询上传记录
     *
     * @param batchId 批次号
     * @return 批次信息，包含处理进度（recordCount）和状态（status）
     */
    @GetMapping("/batch/{batchId}")
    public UploadBatch getByBatchId(
            @PathVariable String batchId) {
        return uploadBatchService.lambdaQuery()
                .eq(UploadBatch::getBatchId, batchId)
                .one();
    }

    /**
     * 内存中的 MultipartFile 实现
     *
     * 用于在异步线程中读取文件内容，避免 Tomcat 临时文件被清理导致 FileNotFoundException
     */
    private static class InMemoryMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return bytes;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(bytes);
            }
        }
    }
}