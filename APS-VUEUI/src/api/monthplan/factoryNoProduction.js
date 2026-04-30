import request, { downloadLink } from '@/utils/request'

// =
export function listFactoryNoProduction(query) {
  return request({
    url: '/monthplan/factoryNoProduction/list',
    method: 'post',
    data: query
  })
}
export function editFactoryNoProduction(query) {
  return request({
    url: '/monthplan/factoryNoProduction/edit',
    method: 'post',
    data: query
  })
}
export function removeFactoryNoProduction(query) {
  return request({
    url: '/monthplan/factoryNoProduction/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/factoryNoProduction/export', query)
}
