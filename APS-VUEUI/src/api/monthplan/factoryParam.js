import request, { downloadLink } from '@/utils/request'

// =
export function listFactoryParam(query) {
  return request({
    url: '/monthplan/factoryParam/list',
    method: 'post',
    data: query
  })
}
export function editFactoryParam(query) {
  return request({
    url: '/monthplan/factoryParam/edit',
    method: 'post',
    data: query
  })
}
export function removeFactoryParam(query) {
  return request({
    url: '/monthplan/factoryParam/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/factoryParam/export', query)
}
