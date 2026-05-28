package com.company.roro.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("OTD配置导入结果")
public class OtdConfigImportResultDTO {

    @ApiModelProperty("成功数量")
    private int successCount;

    @ApiModelProperty("失败数量")
    private int failCount;

    @ApiModelProperty("失败详情")
    private List<String> failDetails = new ArrayList<>();
}