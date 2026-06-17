package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.entity.PortDict;
import com.company.roro.service.PortDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 港口管理接口
 *
 * 功能：管理港口信息，包括港口名称、港口代码等
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/port")
public class PortController {

    @Autowired
    private PortDictService portDictService;

    /**
     * 查询所有激活的港口
     **
     * @param includeDisabled 是否包含已禁用的港口
     * @return 港口列表
     */
    @GetMapping("/list")
    public Result<List<PortDict>> list(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        if (includeDisabled) {
            return Result.success(portDictService.lambdaQuery()
                    .orderByAsc(PortDict::getId)
                    .list());
        } else {
            return Result.success(portDictService.lambdaQuery()
                    .eq(PortDict::getIsActive, 1)
                    .orderByAsc(PortDict::getId)
                    .list());
        }
    }

    /**
     * 根据ID查询港口详情
     *
     * @param id 港口ID
     * @return 港口信息
     */
    @GetMapping("/{id}")
    public Result<PortDict> getById(
            @PathVariable Integer id) {
        return Result.success(portDictService.getById(id));
    }

    /**
     * 新增港口
     *
     * @param portDict 港口信息
     * @return 是否成功
     */
    @PostMapping
    public Result<Boolean> save(
            @RequestBody PortDict portDict) {
        return Result.success(portDictService.save(portDict));
    }

    /**
     * 更新港口信息
     *
     * @param portDict 港口信息（必须包含ID）
     * @return 是否成功
     */
    @PutMapping
    public Result<Boolean> update(
            @RequestBody PortDict portDict) {
        return Result.success(portDictService.updateById(portDict));
    }

    /**
     * 删除港口（软删除）
     *
     * @param id 港口ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @PathVariable Integer id) {
        PortDict port = portDictService.getById(id);
        if (port != null) {
            port.setIsActive(0);
            return Result.success(portDictService.updateById(port));
        }
        return Result.success(false);
    }
}
