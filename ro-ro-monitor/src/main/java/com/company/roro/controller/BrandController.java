package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.entity.BrandDict;
import com.company.roro.service.BrandDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 品牌管理接口
 *
 * 功能：管理车辆品牌信息，包括品牌名称、WMI代码等
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/brand")
public class BrandController {

    @Autowired
    private BrandDictService brandDictService;

    /**
     * 查询所有品牌
     *
     * @param includeDisabled 是否包含已禁用的品牌
     * @return 品牌列表
     */
    @GetMapping("/list")
    public Result<List<BrandDict>> list(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        if (includeDisabled) {
            return Result.success(brandDictService.lambdaQuery()
                    .orderByAsc(BrandDict::getId)
                    .list());
        } else {
            return Result.success(brandDictService.lambdaQuery()
                    .eq(BrandDict::getIsActive, 1)
                    .orderByAsc(BrandDict::getId)
                    .list());
        }
    }

    /**
     * 根据ID查询品牌详情
     *
     * @param id 品牌ID
     * @return 品牌信息
     */
    @GetMapping("/{id}")
    public Result<BrandDict> getById(
            @PathVariable Integer id) {
        return Result.success(brandDictService.getById(id));
    }

    /**
     * 新增品牌
     *
     * @param brandDict 品牌信息
     * @return 是否成功
     */
    @PostMapping
    public Result<Boolean> save(
            @RequestBody BrandDict brandDict) {
        return Result.success(brandDictService.save(brandDict));
    }

    /**
     * 更新品牌信息
     *
     * @param brandDict 品牌信息（必须包含ID）
     * @return 是否成功
     */
    @PutMapping
    public Result<Boolean> update(
            @RequestBody BrandDict brandDict) {
        return Result.success(brandDictService.updateById(brandDict));
    }

    /**
     * 删除品牌（软删除）
     *
     * @param id 品牌ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @PathVariable Integer id) {
        BrandDict brand = brandDictService.getById(id);
        if (brand != null) {
            brand.setIsActive(0);
            return Result.success(brandDictService.updateById(brand));
        }
        return Result.success(false);
    }
}