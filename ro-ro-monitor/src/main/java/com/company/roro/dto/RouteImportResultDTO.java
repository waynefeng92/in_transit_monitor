package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("线路导入结果")
public class RouteImportResultDTO {

    @ApiModelProperty("成功数量")
    private int successCount;

    @ApiModelProperty("失败数量")
    private int failCount;

    @ApiModelProperty("跳过数量（已存在）")
    private int skipCount;

    @ApiModelProperty("失败详情")
    private List<String> failDetails = new ArrayList<>();
}