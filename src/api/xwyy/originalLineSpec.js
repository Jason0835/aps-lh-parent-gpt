import request from '@/utils/request'

// =
export function listOriginalLineSpec(query) {
  return request({
    url: 'xwyy/originalLineSpec/list',
    method: 'post',
    data: query
  })
}
export function editOriginalLineSpec(query) {
  return request({
    url: 'xwyy/originalLineSpec/save',
    method: 'post',
    data: query
  })
}
export function removeOriginalLineSpec(query) {
  return request({
    url: 'xwyy/originalLineSpec/remove',
    method: 'post',
    data: query
  })
}


