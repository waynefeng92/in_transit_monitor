package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.dto.VehicleDetailDTO;
import com.company.roro.service.VehicleDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 车辆详情接口
 *
 * 对外提供单个车辆的运输详情查询，包含 7 段运输状态详情。
 *
 * @author roro-team
 */
@RestController
@RequestMapping("/api/vehicle-detail")
public class VehicleDetailController {

    @Autowired
    private VehicleDetailService vehicleDetailService;

    /**
     * 根据 VIN 查询车辆运输详情
     *
     * @param vin 车架号（17 位）
     * @return 车辆详情 DTO，VIN 不存在时返回 404 错误
     */
    @GetMapping("/{vin}")
    public Result<VehicleDetailDTO> detail(@PathVariable String vin) {
        VehicleDetailDTO dto = vehicleDetailService.getVehicleDetail(vin);
        if (dto == null) {
            return Result.error(404, "未找到车辆: " + vin);
        }
        return Result.success(dto);
    }
}
