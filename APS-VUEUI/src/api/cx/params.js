import request from '@/utils/request'

// =
export function listMoldingParams(query) {
  return request({
    url: '/cx/cxParamConfig/list',
    method: 'post',
    data: query
  })
}
export function editMoldingParams(query) {
  return request({
    url: '/cx/cxParamConfig/save',
    method: 'post',
    data: query
  })
}
export function removeMoldingParams(query) {
  return request({
    url: '/cx/cxParamConfig/remove',
    method: 'post',
    data: query
  })
}


