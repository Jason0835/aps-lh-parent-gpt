import request, { downloadLink } from '@/utils/request'

export function listToolingCartCapacity(query) {
  return request({
    url: '/tq/toolingCartCapacity/list',
    method: 'post',
    data: query,
    headers: { repeatSubmit: false }
  })
}

export function saveToolingCartCapacity(query) {
  return request({
    url: '/tq/toolingCartCapacity/save',
    method: 'post',
    data: query
  })
}

export function removeToolingCartCapacity(ids) {
  return request({
    url: '/tq/toolingCartCapacity/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportToolingCartCapacity(query) {
  return downloadLink("/tq/toolingCartCapacity/export", query)
}
