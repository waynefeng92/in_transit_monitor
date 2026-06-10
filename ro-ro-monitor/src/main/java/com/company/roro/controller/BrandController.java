package com.company.roro.controller;

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
    public List<BrandDict> list(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        if (includeDisabled) {
            return brandDictService.lambdaQuery()
                    .orderByAsc(BrandDict::getId)
                    .list();
        } else {
            return brandDictService.lambdaQuery()
                    .eq(BrandDict::getIsActive, 1)
                    .orderByAsc(BrandDict::getId)
                    .list();
        }
    }

    /**
     * 根据ID查询品牌详情
     *
     * @param id 品牌ID
     * @return 品牌信息
     */
    @GetMapping("/{id}")
    public BrandDict getById(
            @PathVariable Integer id) {
        return brandDictService.getById(id);
    }

    /**
     * 新增品牌
     *
     * @param brandDict 品牌信息
     * @return 是否成功
     */
    @PostMapping
    public boolean save(
            @RequestBody BrandDict brandDict) {
        return brandDictService.save(brandDict);
    }

    /**
     * 更新品牌信息
     *
     * @param brandDict 品牌信息（必须包含ID）
     * @return 是否成功
     */
    @PutMapping
    public boolean update(
            @RequestBody BrandDict brandDict) {
        return brandDictService.updateById(brandDict);
    }

    /**
     * 删除品牌（软删除）
     *
     * @param id 品牌ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public boolean delete(
            @PathVariable Integer id) {
        BrandDict brand = brandDictService.getById(id);
        if (brand != null) {
            brand.setIsActive(0);
            return brandDictService.updateById(brand);
        }
        return false;
    }
}