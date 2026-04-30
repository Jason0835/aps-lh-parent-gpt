import request, { downloadLink } from '@/utils/request'

export function listTooling(query) {
  return request({
    url: '/tq/tooling/list',
    method: 'post',
    data: query,
    headers: { repeatSubmit: false }
  })
}

export function listAllTooling() {
  return request({
    url: '/tq/tooling/listAllTooling',
    method: 'post'
  })
}

export function saveTooling(query) {
  return request({
    url: '/tq/tooling/save',
    method: 'post',
    data: query
  })
}

export function removeTooling(ids) {
  return request({
    url: '/tq/tooling/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportTooling(query) {
  return downloadLink("/tq/tooling/export", query)
}
