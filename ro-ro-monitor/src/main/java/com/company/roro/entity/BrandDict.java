package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("brand_dict")
public class BrandDict {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String brandName;

    private String wmiCode;

    private Integer isActive;
}