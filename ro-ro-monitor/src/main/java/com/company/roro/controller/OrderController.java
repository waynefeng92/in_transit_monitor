package com.company.roro.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.roro.entity.OrderInfo;
import com.company.roro.service.OrderInfoService;
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
    @GetMapping("/page")
    public Page<OrderInfo> page(
            @RequestParam(defaultValue = "1") Integer current,
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
    @GetMapping("/{id}")
    public OrderInfo getById(
            @PathVariable Integer id) {
        return orderInfoService.getById(id);
    }

    /**
     * 根据VIN查询订单
     *
     * @param vin 车架号
     * @return 该VIN下的所有订单
     */
    @GetMapping("/vin/{vin}")
    public List<OrderInfo> getByVin(
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
    @PostMapping
    public boolean save(
            @RequestBody OrderInfo orderInfo) {
        return orderInfoService.save(orderInfo);
    }

    /**
     * 更新订单信息
     *
     * @param orderInfo 订单信息（必须包含ID）
     * @return 是否成功
     */
    @PutMapping
    public boolean update(
            @RequestBody OrderInfo orderInfo) {
        return orderInfoService.updateById(orderInfo);
    }

    /**
     * 删除订单（软删除）
     *
     * @param id 订单ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public boolean delete(
            @PathVariable Integer id) {
        OrderInfo order = orderInfoService.getById(id);
        if (order != null) {
            order.setIsActive(0);
            return orderInfoService.updateById(order);
        }
        return false;
    }
}