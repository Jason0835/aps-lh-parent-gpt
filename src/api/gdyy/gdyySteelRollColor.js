import request from '@/utils/request'

// =
export function listSteelRollColor(query) {
  return request({
    url: 'gdyy/gdyySteelRollColor/list',
    method: 'post',
    data: query
  })
}
export function editSteelRollColor(query) {
  return request({
    url: 'gdyy/gdyySteelRollColor/save',
    method: 'post',
    data: query
  })
}
export function removeSteelRollColor(query) {
  return request({
    url: 'gdyy/gdyySteelRollColor/remove',
    method: 'post',
    data: query
  })
}
export function checkRollCodeUnique(query) {
  return request({
    url: 'gdyy/gdyySteelRollColor/checkRollCodeUnique',
    method: 'post',
    data: query
  })
}


