import request from './request'

export const getSnapshots = (params) => {
  return request.get('/snapshots', { params })
}
