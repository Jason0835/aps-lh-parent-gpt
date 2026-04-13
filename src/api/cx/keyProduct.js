import request from '@/utils/request'

export function listCxKeyProduct(query) {
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

export function saveCxKeyProduct(data) {
  return request({
    url: '/cx/cxKeyProduct/save',
    method: 'post',
    data: data
  })
}

export function removeCxKeyProduct(ids) {
  return request({
    url: '/cx/cxKeyProduct/remove',
    method: 'delete',
    data: ids
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
