import request from '@/utils/request'

// =
export function listType(query) {
  return request({
    url: '/setting/type/list',
    method: 'post',
    data: query
  })
}
export function removeType(query) {
  return request({
    url: '/setting/type/remove',
    method: 'post',
    data: query
  })
}
export function saveType(query) {
  return request({
    url: '/setting/type/save',
    method: 'post',
    data: query
  })
}
export function checkRecipeTypeUnique(query) {
  return request({
    url: '/setting/type/checkRecipeTypeUnique',
    method: 'post',
    data: query
  })
}
