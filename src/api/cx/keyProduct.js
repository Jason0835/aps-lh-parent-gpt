import request from '@/utils/request'

export function listMoldingParams(query) {
  return request({
    url: '/cx/cxKeyProduct/list',
    method: 'post',
    data: query
  })
}

export function getCxKeyProduct(id) {
  return request({
    url: '/cx/cxKeyProduct/' + id,
    method: 'get'
  })
}

export function editMoldingParams(data) {
  return request({
    url: '/cx/cxKeyProduct/save',
    method: 'post',
    data: data
  })
}

export function removeMoldingParams(ids) {
  return request({
    url: '/cx/cxKeyProduct/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportCxKeyProduct(query) {
  return request({
    url: '/cx/cxKeyProduct/export',
    method: 'get',
    params: query
  })
}

export function checkUniqueCxKeyProduct(data) {
  return request({
    url: '/cx/cxKeyProduct/checkUnique',
    method: 'post',
    data: data
  })
}
