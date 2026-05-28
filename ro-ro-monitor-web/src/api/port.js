import request from './request'

// 获取港口列表
export function getPortList(includeDisabled = false) {
  return request({
    url: '/port/list',
    method: 'get',
    params: { includeDisabled }
  })
}

// 新增港口
export function savePort(data) {
  return request({
    url: '/port',
    method: 'post',
    data
  })
}

// 更新港口
export function updatePort(data) {
  return request({
    url: '/port',
    method: 'put',
    data
  })
}

// 删除港口（软删除）
export function deletePort(id) {
  return request({
    url: `/port/${id}`,
    method: 'delete'
  })
}