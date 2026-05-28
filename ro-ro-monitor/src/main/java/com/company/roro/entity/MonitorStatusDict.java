package com.company.roro.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("monitor_status_dict")
public class MonitorStatusDict {

    @TableId
    private String statusCode;

    private String statusName;
}