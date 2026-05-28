package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Excel 字段映射配置实体类
 *
 * 作用：适配不同客户/品牌的 Excel 表头格式
 */
@Data
@TableName("excel_field_mapping")
public class ExcelFieldMapping {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 品牌ID，NULL表示默认规则 */
    private Integer brandId;

    /** 标准字段名 */
    private String standardField;

    /** Excel表头名称，多个用逗号分隔 */
    private String excelColumnNames;

    /** 时间格式 */
    private String dateFormat;

    /** 是否必填：1必填，0可选 */
    private Integer isRequired;

    /** 默认值 */
    private String defaultValue;

    /** 排序 */
    private Integer sortOrder;

    /** 是否启用 */
    private Integer isActive;
}