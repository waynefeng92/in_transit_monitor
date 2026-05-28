import request from './request'

// 获取品牌列表
export function getBrandList(includeDisabled = false) {
  return request({
    url: '/brand/list',
    method: 'get',
    params: { includeDisabled }
  })
}

// 新增品牌
export function saveBrand(data) {
  return request({
    url: '/brand',
    method: 'post',
    data
  })
}

// 更新品牌
export function updateBrand(data) {
  return request({
    url: '/brand',
    method: 'put',
    data
  })
}

// 删除品牌（软删除）
export function deleteBrand(id) {
  return request({
    url: `/brand/${id}`,
    method: 'delete'
  })
}