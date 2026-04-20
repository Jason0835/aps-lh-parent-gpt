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
  // 将逗号分隔的字符串转换为数组
  let idList = ids;
  if (typeof ids === 'string') {
    idList = ids.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
  }
  return request({
    url: '/cx/cxKeyProduct/remove',
    method: 'post',
    data: idList
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
