import request from '@/utils/request'

// 胎侧参数设置 API
export function listParams(query) {
  return request({
    url: '/tc/tcParams/list',
    method: 'post',
    data: query
  })
}
export function saveParams(data) {
  return request({
    url: '/tc/tcParams/save',
    method: 'post',
    data: data
  })
}
export function removeParams(query) {
  return request({
    url: '/tc/tcParams/remove',
    method: 'post',
    data: query
  })
}
export function getInfo(id) {
  return request({
    url: '/tc/tcParams/' + id,
    method: 'get'
  })
}
