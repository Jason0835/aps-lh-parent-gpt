import request from '@/utils/request'

//
export function listMonthStock(query) {
  return request({
    url: '/cx/monthStock/list',
    method: 'post',
    data: query
  })
}
export function editMonthStock(query) {
  return request({
    url: '/cx/monthStock/edit',
    method: 'post',
    data: query
  })
}
export function removeMonthStock(query) {
  return request({
    url: '/cx/monthStock/remove',
    method: 'post',
    data: query
  })
}

export function getProductEmbryoVersions(query) {
  return request({
    url: '/cx/monthStock/getProductEmbryoVersions',
    method: 'post',
    data: query
  })
}


