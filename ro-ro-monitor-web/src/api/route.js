import request from './request'

// 获取线路列表
export function getRouteList(includeDisabled = false) {
  return request({
    url: '/route/list',
    method: 'get',
    params: { includeDisabled }
  })
}

// 根据品牌查询线路
export function getRouteByBrand(brandId) {
  return request({
    url: `/route/list/${brandId}`,
    method: 'get'
  })
}

// 新增线路
export function saveRoute(data) {
  return request({
    url: '/route',
    method: 'post',
    data
  })
}

// 更新线路
export function updateRoute(data) {
  return request({
    url: '/route',
    method: 'put',
    data
  })
}

// 删除线路
export function deleteRoute(id) {
  return request({
    url: `/route/${id}`,
    method: 'delete'
  })
}

// 下载导入模板
export function downloadTemplate() {
  return request({
    url: '/route/template',
    method: 'get',
    responseType: 'blob'
  })
}

// 预览导入数据（带校验）
export function previewImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/route/import/preview',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 批量编辑
export function batchUpdateRoutes(data) {
  return request({
    url: '/route/batch',
    method: 'put',
    data
  })
}

// 批量导入
export function batchImport(data) {
  return request({
    url: '/route/import/batch',
    method: 'post',
    data
  })
}