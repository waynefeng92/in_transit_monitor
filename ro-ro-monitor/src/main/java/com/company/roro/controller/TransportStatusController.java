package com.company.roro.controller;

import com.company.roro.entity.TransportStatusDict;
import com.company.roro.service.TransportStatusDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 在途状态字典接口
 *
 * 功能：提供8种在途状态的查询
 *
 * 8种状态：
 * 1. NOT_DEPARTED           - 未出库
 * 2. TO_PORT                - 集港在途
 * 3. AT_PORT_WAIT_SHIP      - 已集港待装船
 * 4. ON_SEA                 - 水运在途
 * 5. AT_DEST_WAIT_UNLOAD    - 已到港待卸船
 * 6. UNLOADED_WAIT_DISPATCH - 已卸船待分拨
 * 7. DISPATCHING            - 分拨在途
 * 8. ARRIVED                - 已到达
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/transport-status")
public class TransportStatusController {

    @Autowired
    private TransportStatusDictService transportStatusDictService;

    /**
     * 查询所有在途状态
     *
     * @return 按 display_order 排序的状态列表
     */
    @GetMapping("/list")
    public List<TransportStatusDict> list() {
        return transportStatusDictService.lambdaQuery()
                .orderByAsc(TransportStatusDict::getDisplayOrder)
                .list();
    }
}