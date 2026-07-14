package com.company.roro.dto;

import lombok.Data;

/**
 * 维度分析 DTO — 按路线 / 按目的地城市 聚合统计
 */
@Data
public class DimensionStatDTO {

    private String name;

    private Long totalCount;

    private Double avgEfficiency;

    private Double otdComplianceRate;
}
