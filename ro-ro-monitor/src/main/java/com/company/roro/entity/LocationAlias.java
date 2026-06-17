package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("location_alias")
public class LocationAlias {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String standardName;
    private String alias;
}
