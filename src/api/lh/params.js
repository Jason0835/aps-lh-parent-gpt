import request from '@/utils/request'

// =
export function listCuringParams(query) {
  return request({
    url: '/lh/lhParams/list',
    method: 'post',
    data: query
  })
}
export function editCuringParams(query) {
  return request({
    url: '/lh/lhParams/save',
    method: 'post',
    params: query
  })
}
export function removeCuringParams(ids) {
  return request({
    url: '/lh/lhParams/remove',
    method: 'post',
    params: { ids: ids }
  })
}


