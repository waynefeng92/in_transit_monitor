package com.company.roro.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.roro.dto.*;
import com.company.roro.entity.BrandDict;
import com.company.roro.entity.ExcelFieldMapping;
import com.company.roro.service.BrandDictService;
import com.company.roro.service.ExcelFieldMappingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel 字段映射配置接口
 *
 * 功能：运营人员可自助配置不同品牌的 Excel 表头映射规则
 *
 * @author roro-team
 */
@Api(tags = "Excel字段映射配置")
@RestController
@RequestMapping("/api/excel-mapping")
public class ExcelMappingController {

    @Autowired
    private ExcelFieldMappingService excelFieldMappingService;

    @Autowired
    private BrandDictService brandDictService;

    /**
     * 标准字段定义（用于前端下拉框）
     */
    private static final List<StandardFieldDTO> STANDARD_FIELDS = Arrays.asList(
            new StandardFieldDTO("vin", "车架号", "STRING", false),
            new StandardFieldDTO("brandName", "品牌名称", "STRING", false),
            new StandardFieldDTO("orderReleaseTime", "订单释放时间", "DATETIME", true),
            new StandardFieldDTO("originCity", "出发地", "STRING", false),
            new StandardFieldDTO("destCity", "目的地", "STRING", false),
            new StandardFieldDTO("departWarehouseTime", "出库时间", "DATETIME", true),
            new StandardFieldDTO("arrivePortTime", "集港到港时间", "DATETIME", true),
            new StandardFieldDTO("shipDepartTime", "船离始发港时间", "DATETIME", true),
            new StandardFieldDTO("shipArriveTime", "船到目的港时间", "DATETIME", true),
            new StandardFieldDTO("unloadFinishTime", "卸船完成时间", "DATETIME", true),
            new StandardFieldDTO("dispatchTime", "分拨时间", "DATETIME", true),
            new StandardFieldDTO("arriveShopTime", "到店时间", "DATETIME", true)
    );

    /**
     * 获取所有标准字段列表
     *
     * @return 标准字段列表
     */
    @ApiOperation(value = "获取标准字段列表", notes = "返回系统支持的所有标准字段，供前端下拉框选择")
    @GetMapping("/standard-fields")
    public List<StandardFieldDTO> getStandardFields() {
        return STANDARD_FIELDS;
    }

    /**
     * 分页查询映射配置
     *
     * @param current 当前页
     * @param size 每页条数
     * @param brandId 品牌ID（可选）
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询映射配置", notes = "支持按品牌筛选")
    @GetMapping("/list")
    public Page<ExcelMappingDTO> list(
            @ApiParam(value = "当前页码", example = "1")
            @RequestParam(defaultValue = "1") Integer current,
            @ApiParam(value = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") Integer size,
            @ApiParam(value = "品牌ID", required = false)
            @RequestParam(required = false) Integer brandId) {

        // 查询配置
        Page<ExcelFieldMapping> page = excelFieldMappingService.lambdaQuery()
                .eq(brandId != null, ExcelFieldMapping::getBrandId, brandId)
                .orderByAsc(ExcelFieldMapping::getSortOrder)
                .page(new Page<>(current, size));

        // 获取品牌名称映射
        Map<Integer, String> brandMap = brandDictService.list().stream()
                .collect(Collectors.toMap(BrandDict::getId, BrandDict::getBrandName));

        // 获取标准字段中文名映射
        Map<String, String> fieldNameMap = STANDARD_FIELDS.stream()
                .collect(Collectors.toMap(StandardFieldDTO::getFieldName, StandardFieldDTO::getFieldLabel));

        // 转换为 DTO
        Page<ExcelMappingDTO> dtoPage = new Page<>(current, size, page.getTotal());
        List<ExcelMappingDTO> dtoList = page.getRecords().stream().map(entity -> {
            ExcelMappingDTO dto = new ExcelMappingDTO();
            BeanUtils.copyProperties(entity, dto);
            dto.setBrandName(brandMap.getOrDefault(entity.getBrandId(), "默认规则"));
            dto.setStandardFieldName(fieldNameMap.getOrDefault(entity.getStandardField(), entity.getStandardField()));
            return dto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /**
     * 根据品牌ID查询配置
     *
     * @param brandId 品牌ID，传0或null表示查询默认规则
     * @return 该品牌的配置列表
     */
    @ApiOperation(value = "根据品牌查询配置", notes = "返回指定品牌的所有配置，brandId=0表示默认规则")
    @GetMapping("/brand/{brandId}")
    public List<ExcelMappingDTO> getByBrandId(
            @ApiParam(value = "品牌ID，0表示默认规则", required = true, example = "1")
            @PathVariable Integer brandId) {

        Integer queryBrandId = (brandId == 0) ? null : brandId;

        List<ExcelFieldMapping> list = excelFieldMappingService.lambdaQuery()
                .eq(queryBrandId != null, ExcelFieldMapping::getBrandId, queryBrandId)
                .isNull(queryBrandId == null, ExcelFieldMapping::getBrandId)
                .eq(ExcelFieldMapping::getIsActive, 1)
                .orderByAsc(ExcelFieldMapping::getSortOrder)
                .list();

        Map<String, String> fieldNameMap = STANDARD_FIELDS.stream()
                .collect(Collectors.toMap(StandardFieldDTO::getFieldName, StandardFieldDTO::getFieldLabel));

        return list.stream().map(entity -> {
            ExcelMappingDTO dto = new ExcelMappingDTO();
            BeanUtils.copyProperties(entity, dto);
            dto.setStandardFieldName(fieldNameMap.getOrDefault(entity.getStandardField(), entity.getStandardField()));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 新增配置
     *
     * @param dto 配置信息
     * @return 是否成功
     */
    @ApiOperation(value = "新增配置", notes = "添加一条字段映射规则")
    @PostMapping
    public boolean save(
            @ApiParam(value = "配置信息", required = true)
            @RequestBody ExcelMappingDTO dto) {
        ExcelFieldMapping entity = new ExcelFieldMapping();
        BeanUtils.copyProperties(dto, entity);
        return excelFieldMappingService.save(entity);
    }

    /**
     * 更新配置
     *
     * @param dto 配置信息（必须包含ID）
     * @return 是否成功
     */
    @ApiOperation(value = "更新配置", notes = "根据ID更新配置")
    @PutMapping
    public boolean update(
            @ApiParam(value = "配置信息（必须包含ID）", required = true)
            @RequestBody ExcelMappingDTO dto) {
        ExcelFieldMapping entity = new ExcelFieldMapping();
        BeanUtils.copyProperties(dto, entity);
        return excelFieldMappingService.updateById(entity);
    }

    /**
     * 批量保存配置
     *
     * 前端编辑完一个品牌的所有配置后，一次性提交保存
     *
     * @param request 批量保存请求
     * @return 是否成功
     */
    @ApiOperation(value = "批量保存配置", notes = "保存某品牌的所有配置，会先删除该品牌的旧配置再插入新配置")
    @PostMapping("/batch")
    public boolean batchSave(
            @ApiParam(value = "批量保存请求", required = true)
            @RequestBody BatchSaveMappingRequest request) {

        Integer brandId = request.getBrandId();
        List<ExcelMappingDTO> mappings = request.getMappings();

        if (mappings == null || mappings.isEmpty()) {
            return false;
        }

        // 删除该品牌的旧配置
        excelFieldMappingService.lambdaUpdate()
                .eq(ExcelFieldMapping::getBrandId, brandId)
                .remove();

        // 插入新配置
        List<ExcelFieldMapping> entities = mappings.stream().map(dto -> {
            ExcelFieldMapping entity = new ExcelFieldMapping();
            BeanUtils.copyProperties(dto, entity);
            entity.setId(null);  // 确保是新增
            entity.setBrandId(brandId);
            return entity;
        }).collect(Collectors.toList());

        return excelFieldMappingService.saveBatch(entities);
    }

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 是否成功
     */
    @ApiOperation(value = "删除配置", notes = "物理删除")
    @DeleteMapping("/{id}")
    public boolean delete(
            @ApiParam(value = "配置ID", required = true, example = "1")
            @PathVariable Integer id) {
        return excelFieldMappingService.removeById(id);
    }

    /**
     * 复制配置
     *
     * 将某个品牌的配置复制到另一个品牌，方便快速配置
     *
     * @param request 复制请求
     * @return 复制结果
     */
    @ApiOperation(value = "复制配置", notes = "将源品牌的配置复制到目标品牌")
    @PostMapping("/copy")
    public Map<String, Object> copyMapping(
            @ApiParam(value = "复制请求", required = true)
            @RequestBody CopyMappingRequest request) {

        Integer sourceBrandId = request.getSourceBrandId();
        Integer targetBrandId = request.getTargetBrandId();

        // 查询源品牌配置
        List<ExcelFieldMapping> sourceList = excelFieldMappingService.lambdaQuery()
                .eq(sourceBrandId != 0, ExcelFieldMapping::getBrandId, sourceBrandId)
                .isNull(sourceBrandId == 0, ExcelFieldMapping::getBrandId)
                .list();

        if (sourceList.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "源品牌没有配置");
            return result;
        }

        // 删除目标品牌的旧配置
        excelFieldMappingService.lambdaUpdate()
                .eq(ExcelFieldMapping::getBrandId, targetBrandId)
                .remove();

        // 复制配置
        List<ExcelFieldMapping> targetList = sourceList.stream().map(source -> {
            ExcelFieldMapping target = new ExcelFieldMapping();
            BeanUtils.copyProperties(source, target);
            target.setId(null);
            target.setBrandId(targetBrandId);
            return target;
        }).collect(Collectors.toList());

        boolean success = excelFieldMappingService.saveBatch(targetList);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "复制成功，共 " + targetList.size() + " 条配置" : "复制失败");
        result.put("count", targetList.size());
        return result;
    }

    /**
     * 启用/禁用配置
     *
     * @param id 配置ID
     * @param isActive 是否启用
     * @return 是否成功
     */
    @ApiOperation(value = "启用/禁用配置", notes = "切换配置的启用状态")
    @PutMapping("/{id}/status")
    public boolean updateStatus(
            @ApiParam(value = "配置ID", required = true)
            @PathVariable Integer id,
            @ApiParam(value = "是否启用：1启用，0禁用", required = true)
            @RequestParam Integer isActive) {

        ExcelFieldMapping entity = new ExcelFieldMapping();
        entity.setId(id);
        entity.setIsActive(isActive);
        return excelFieldMappingService.updateById(entity);
    }
}