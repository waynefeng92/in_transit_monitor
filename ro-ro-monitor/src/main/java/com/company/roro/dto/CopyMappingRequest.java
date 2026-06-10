package com.company.roro.dto;

import lombok.Data;

/**
 * 复制配置请求
 */
@Data
public class CopyMappingRequest {

    private Integer sourceBrandId;

    private Integer targetBrandId;
}