package com.company.roro.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultDTO {

    /** 总记录数 */
    private int totalCount;

    /** 成功处理数 */
    private int successCount;

    /** 失败处理数 */
    private int failCount;

    /** 路线匹配成功数 */
    private int routeMatchedCount;

    /** 路线未匹配数 */
    private int routeUnmatchedCount;

    /** 失败详情（VIN + 原因） */
    private List<String> failDetails = new ArrayList<>();
}
