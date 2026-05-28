package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("route_dict")
public class RouteDict {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer brandId;

    private String originCity;

    private Integer originPortId;

    private Integer destPortId;

    private String destCity;

    private Integer isActive;
}