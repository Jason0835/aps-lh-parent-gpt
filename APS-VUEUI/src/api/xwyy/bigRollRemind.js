import request from '@/utils/request'

// =
export function listBigRollRemind(query) {
  return request({
    url: 'xwyy/bigRollRemind/list',
    method: 'post',
    data: query
  })
}
export function editBigRollRemind(query) {
  return request({
    url: 'xwyy/bigRollRemind/edit',
    method: 'post',
    data: query
  })
}
export function removeBigRollRemind(query) {
  return request({
    url: 'xwyy/bigRollRemind/remove',
    method: 'post',
    data: query
  })
}


