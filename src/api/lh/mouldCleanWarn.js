import request from '@/utils/request'

export function listMouldCleanWarn(query) {
  return request({
    url: '/lh/mouldCleanWarn/list',
    method: 'post',
    data: query
  })
}

export function getMouldCleanWarn(id) {
  return request({
    url: '/lh/mouldCleanWarn/' + id,
    method: 'get'
  })
}

export function exportMouldCleanWarn(query) {
  return request({
    url: '/lh/mouldCleanWarn/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}
