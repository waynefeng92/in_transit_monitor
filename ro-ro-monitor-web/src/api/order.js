import request from './request'

// 获取订单列表
export function getOrderList(params) {
  return request({
    url: '/order/list',
    method: 'get',
    params
  })
}

// 批量取消订单
export function batchCancelOrders(orderIds) {
  return request({
    url: '/order/batch-cancel',
    method: 'post',
    data: orderIds
  })
}

// 导出订单
export function exportOrders(params) {
  return request({
    url: '/order/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
