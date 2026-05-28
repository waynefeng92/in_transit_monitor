package com.company.roro.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.roro.entity.OrderInfo;
import com.company.roro.service.OrderInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单管理接口
 *
 * 功能：管理运输订单，包括订单的创建、查询、更新等
 *
 * @author roro-team
 */
@Api(tags = "订单管理")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 分页查询订单
     *
     * @param current 当前页码
     * @param size 每页条数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询订单", notes = "返回分页的订单列表，按订单释放时间倒序排列")
    @GetMapping("/page")
    public Page<OrderInfo> page(
            @ApiParam(value = "当前页码", example = "1")
            @RequestParam(defaultValue = "1") Integer current,
            @ApiParam(value = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        return orderInfoService.lambdaQuery()
                .eq(OrderInfo::getIsActive, 1)
                .orderByDesc(OrderInfo::getOrderReleaseTime)
                .page(new Page<>(current, size));
    }

    /**
     * 根据ID查询订单详情
     *
     * @param id 订单ID
     * @return 订单信息
     */
    @ApiOperation(value = "根据ID查询订单", notes = "返回指定ID的订单详细信息")
    @GetMapping("/{id}")
    public OrderInfo getById(
            @ApiParam(value = "订单ID", required = true, example = "1")
            @PathVariable Integer id) {
        return orderInfoService.getById(id);
    }

    /**
     * 根据VIN查询订单
     *
     * @param vin 车架号
     * @return 该VIN下的所有订单
     */
    @ApiOperation(value = "根据VIN查询订单", notes = "返回指定车架号的所有订单（同一辆车可能有多个订单）")
    @GetMapping("/vin/{vin}")
    public List<OrderInfo> getByVin(
            @ApiParam(value = "车架号（17位）", required = true, example = "LSVAA4180E2123456")
            @PathVariable String vin) {
        return orderInfoService.lambdaQuery()
                .eq(OrderInfo::getVin, vin)
                .eq(OrderInfo::getIsActive, 1)
                .orderByDesc(OrderInfo::getOrderReleaseTime)
                .list();
    }

    /**
     * 新增订单
     *
     * @param orderInfo 订单信息
     * @return 是否成功
     */
    @ApiOperation(value = "新增订单", notes = "创建新的运输订单，同一VIN+订单释放时间的组合必须唯一")
    @PostMapping
    public boolean save(
            @ApiParam(value = "订单信息", required = true)
            @RequestBody OrderInfo orderInfo) {
        return orderInfoService.save(orderInfo);
    }

    /**
     * 更新订单信息
     *
     * @param orderInfo 订单信息（必须包含ID）
     * @return 是否成功
     */
    @ApiOperation(value = "更新订单", notes = "根据ID更新订单信息")
    @PutMapping
    public boolean update(
            @ApiParam(value = "订单信息（必须包含ID）", required = true)
            @RequestBody OrderInfo orderInfo) {
        return orderInfoService.updateById(orderInfo);
    }

    /**
     * 删除订单（软删除）
     *
     * @param id 订单ID
     * @return 是否成功
     */
    @ApiOperation(value = "删除订单", notes = "软删除，将 is_active 设置为 0")
    @DeleteMapping("/{id}")
    public boolean delete(
            @ApiParam(value = "订单ID", required = true, example = "1")
            @PathVariable Integer id) {
        OrderInfo order = orderInfoService.getById(id);
        if (order != null) {
            order.setIsActive(0);
            return orderInfoService.updateById(order);
        }
        return false;
    }
}