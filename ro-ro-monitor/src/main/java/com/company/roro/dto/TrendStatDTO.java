package com.company.roro.dto;

import lombok.Data;

/**
 * 趋势统计 DTO — 按周期聚合的到达效率趋势
 */
@Data
public class TrendStatDTO {

    private String period;

    private Integer arrivalCount;

    private Double avgEfficiency;

    private Integer efficientCount;

    private Integer normalCount;

    private Integer delayedCount;
}
