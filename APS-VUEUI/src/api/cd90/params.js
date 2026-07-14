import request, { downloadLink } from '@/utils/request'

export function listParams(query) {
  return request({ url: '/cd90/cd90Params/list', method: 'post', data: query })
}
export function getParams(id) {
  return request({ url: `/cd90/cd90Params/getInfo/${id}`, method: 'get' })
}
export function getCd90ParamValue(factoryCode, paramCode) {
  return request({ url: `/cd90/cd90Params/getParamValue/${factoryCode}/${paramCode}`, method: 'get' })
}
export function addParams(data) {
  return request({ url: '/cd90/cd90Params/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateParams(data) {
  return request({ url: '/cd90/cd90Params/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delParams(data) {
  return request({ url: '/cd90/cd90Params/remove', method: 'post', data })
}
export function exportParams(query) {
  return downloadLink('/cd90/cd90Params/export', query)
}
