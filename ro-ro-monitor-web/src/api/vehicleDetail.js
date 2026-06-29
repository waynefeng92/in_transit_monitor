import request from './request'

export function getVehicleDetail(vin) {
  return request({
    url: `/vehicle-detail/${vin}`,
    method: 'get'
  })
}
