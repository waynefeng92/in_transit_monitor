import request from './request'

export const getLocationAliasList = () => request.get('/location-alias')
export const saveLocationAlias = (data) => request.post('/location-alias', data)
export const updateLocationAlias = (data) => request.put('/location-alias', data)
export const deleteLocationAlias = (id) => request.delete(`/location-alias/${id}`)
