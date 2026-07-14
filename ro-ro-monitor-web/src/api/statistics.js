import request from './request'

export function getStatisticsSummary(params) {
  return request.get('/statistics/summary', { params })
}

export function getTrend(params) {
  return request.get('/statistics/trend', { params })
}

export function getByRoute(params) {
  return request.get('/statistics/by-route', { params })
}
