import request from './request'

// 获取车辆详情列表（下钻）
export function getVehicleDetails(params) {
  return request({
    url: '/chart/vehicle-details',
    method: 'get',
    params
  })
}
