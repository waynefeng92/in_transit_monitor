package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 上传批次实体类
 *
 * @author roro-team
 */
@Data
@TableName("upload_batch")
public class UploadBatch {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 批次号 */
    private String batchId;

    /** 上传文件名 */
    private String fileName;

    /** 上传人 */
    private String uploadUser;

    /** 本批次记录条数 */
    private Integer recordCount;

    /** 处理状态：PROCESSING(处理中)/SUCCESS(成功)/FAILED(失败) */
    private String status;

    /** 错误信息（失败时记录） */
    private String errorMessage;

    /** 上传时间 */
    private LocalDateTime uploadTime;
}