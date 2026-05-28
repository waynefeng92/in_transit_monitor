package com.company.roro.controller;

import com.company.roro.entity.PortDict;
import com.company.roro.service.PortDictService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@Api(tags = "港口管理")
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
    @ApiOperation(value = "查询所有港口", notes = "返回港口列表，可通过参数控制是否包含已禁用")
    @GetMapping("/list")
    public List<PortDict> list(
            @ApiParam(value = "是否包含已禁用", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        if (includeDisabled) {
            return portDictService.lambdaQuery()
                    .orderByAsc(PortDict::getId)
                    .list();
        } else {
            return portDictService.lambdaQuery()
                    .eq(PortDict::getIsActive, 1)
                    .orderByAsc(PortDict::getId)
                    .list();
        }
    }

    /**
     * 根据ID查询港口详情
     *
     * @param id 港口ID
     * @return 港口信息
     */
    @ApiOperation(value = "根据ID查询港口", notes = "返回指定ID的港口详细信息")
    @GetMapping("/{id}")
    public PortDict getById(
            @ApiParam(value = "港口ID", required = true, example = "1")
            @PathVariable Integer id) {
        return portDictService.getById(id);
    }

    /**
     * 新增港口
     *
     * @param portDict 港口信息
     * @return 是否成功
     */
    @ApiOperation(value = "新增港口", notes = "添加新的港口信息，港口名称不能重复")
    @PostMapping
    public boolean save(
            @ApiParam(value = "港口信息", required = true)
            @RequestBody PortDict portDict) {
        return portDictService.save(portDict);
    }

    /**
     * 更新港口信息
     *
     * @param portDict 港口信息（必须包含ID）
     * @return 是否成功
     */
    @ApiOperation(value = "更新港口", notes = "根据ID更新港口信息")
    @PutMapping
    public boolean update(
            @ApiParam(value = "港口信息（必须包含ID）", required = true)
            @RequestBody PortDict portDict) {
        return portDictService.updateById(portDict);
    }

    /**
     * 删除港口（软删除）
     *
     * @param id 港口ID
     * @return 是否成功
     */
    @ApiOperation(value = "删除港口", notes = "软删除，将 is_active 设置为 0")
    @DeleteMapping("/{id}")
    public boolean delete(
            @ApiParam(value = "港口ID", required = true, example = "1")
            @PathVariable Integer id) {
        PortDict port = portDictService.getById(id);
        if (port != null) {
            port.setIsActive(0);
            return portDictService.updateById(port);
        }
        return false;
    }
}