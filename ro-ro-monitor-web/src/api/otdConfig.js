import request from './request'

// 根据线路ID获取OTD配置
export function getOtdConfigByRoute(routeId) {
  return request({
    url: `/otd-config/route/${routeId}`,
    method: 'get'
  })
}

// 新增OTD配置
export function saveOtdConfig(data) {
  return request({
    url: '/otd-config',
    method: 'post',
    data
  })
}

// 更新OTD配置
export function updateOtdConfig(data) {
  return request({
    url: '/otd-config',
    method: 'put',
    data
  })
}

// 导出品牌OTD配置模板
export function exportOtdTemplate(brandId) {
  return request({
    url: `/otd-config/export/${brandId}`,
    method: 'get',
    responseType: 'blob'
  })
}

// 预览导入数据
export function previewOtdImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/otd-config/import/preview',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 批量导入
export function batchImportOtd(data) {
  return request({
    url: '/otd-config/import/batch',
    method: 'post',
    data
  })
}