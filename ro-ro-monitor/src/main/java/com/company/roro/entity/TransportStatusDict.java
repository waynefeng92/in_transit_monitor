package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("transport_status_dict")
public class TransportStatusDict {

    @TableId
    private String statusCode;

    private String statusName;

    private Integer displayOrder;
}