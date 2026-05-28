import request from './request'

// 获取标准字段列表
export function getStandardFields() {
  return request({
    url: '/excel-mapping/standard-fields',
    method: 'get'
  })
}

// 根据品牌查询配置
export function getMappingByBrand(brandId) {
  return request({
    url: `/excel-mapping/brand/${brandId}`,
    method: 'get'
  })
}

// 批量保存配置
export function batchSaveMapping(data) {
  return request({
    url: '/excel-mapping/batch',
    method: 'post',
    data
  })
}

// 复制配置
export function copyMapping(data) {
  return request({
    url: '/excel-mapping/copy',
    method: 'post',
    data
  })
}