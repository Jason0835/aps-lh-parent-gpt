import request from '@/utils/request'

// =
export function listMaterial(query) {
  return request({
    url: '/setting/material/list',
    method: 'post',
    data: query
  })
}
export function removeMaterial(query) {
  return request({
    url: '/setting/material/remove',
    method: 'post',
    data: query
  })
}
export function saveMaterial(query) {
  return request({
    url: '/setting/material/save',
    method: 'post',
    data: query
  })
}
export function checkGlueMaterialUnique(query) {
  return request({
    url: '/setting/material/checkGlueMaterialUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/material/checkComplete',
    method: 'post',
    data: query
  })
}
