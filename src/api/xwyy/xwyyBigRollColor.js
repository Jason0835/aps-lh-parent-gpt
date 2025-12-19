import request from '@/utils/request'

// =
export function listBigRollColor(query) {
  return request({
    url: 'xwyy/xwyyBigRollColor/list',
    method: 'post',
    data: query
  })
}
export function editBigRollColor(query) {
  return request({
    url: 'xwyy/xwyyBigRollColor/save',
    method: 'post',
    data: query
  })
}
export function removeBigRollColor(query) {
  return request({
    url: 'xwyy/xwyyBigRollColor/remove',
    method: 'post',
    data: query
  })
}


export function checkRollCodeUnique(query) {
  return request({
    url: 'xwyy/xwyyBigRollColor/checkRollCodeUnique',
    method: 'post',
    data: query
  })
}


