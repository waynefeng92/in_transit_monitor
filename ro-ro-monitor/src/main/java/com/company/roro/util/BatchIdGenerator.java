package com.company.roro.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 批次号生成工具类
 *
 * 作用：
 * 1. 每次上传 Excel 时生成唯一批次号
 * 2. 用于数据溯源，知道某条记录来自哪次上传
 *
 * 格式说明：
 * BATCH_20260412_143025_a1b2c3
 *   │      │        │      │
 *   │      │        │      └── UUID前6位（保证唯一性）
 *   │      │        └── 时分秒
 *   │      └── 年月日
 *   └── 固定前缀
 */
public class BatchIdGenerator {

    /** 日期时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 生成上传批次号
     *
     * @return 格式为 BATCH_20260412_143025_a1b2c3 的批次号
     */
    public static String generate() {
        // 获取当前时间戳
        String timestamp = LocalDateTime.now().format(FORMATTER);

        // 获取 UUID 前6位作为随机后缀，保证并发上传时不重复
        String uuid = UUID.randomUUID().toString().substring(0, 6);

        return "BATCH_" + timestamp + "_" + uuid;
    }
}