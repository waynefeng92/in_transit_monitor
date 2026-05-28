package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer brandId;

    private String vin;

    private LocalDateTime orderReleaseTime;

    private String originCity;

    private String destCity;

    private Integer routeId;

    private Integer isActive;
}