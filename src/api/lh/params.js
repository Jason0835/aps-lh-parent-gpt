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
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function removeCuringParams(query) {
  return request({
    url: '/lh/lhParams/remove',
    method: 'post',
    data: query
  })
}


