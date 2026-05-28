package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("port_dict")
public class PortDict {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String portName;

    private String portCode;

    private Integer isActive;
}