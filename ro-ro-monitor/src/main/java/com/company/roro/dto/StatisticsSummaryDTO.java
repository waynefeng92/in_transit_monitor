package com.company.roro.dto;

import lombok.Data;

/**
 * 统计分析汇总 DTO
 */
@Data
public class StatisticsSummaryDTO {

    private Long totalArrivals;

    private Double avgEfficiency;

    private Double otdComplianceRate;

    private Long efficientCount;

    private Long normalCount;

    private Long delayedCount;
}
