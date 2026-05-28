package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Excel 解析配置实体类
 *
 * 用于配置每个品牌的 Excel 解析规则（Sheet定位、表头行号等）
 */
@Data
@TableName("excel_parse_config")
public class ExcelParseConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 品牌ID，NULL表示默认规则 */
    private Integer brandId;

    /** Sheet定位方式：NAME/INDEX/AUTO */
    private String sheetLocateType;

    /** Sheet名称（当locate_type=NAME时使用） */
    private String sheetName;

    /** Sheet索引（当locate_type=INDEX时使用，0表示第一个） */
    private Integer sheetIndex;

    /** 表头所在行号（0表示第一行） */
    private Integer headerRowIndex;

    /** 数据起始行号（1表示第二行） */
    private Integer dataStartRowIndex;

    /** 是否启用 */
    private Integer isActive;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}