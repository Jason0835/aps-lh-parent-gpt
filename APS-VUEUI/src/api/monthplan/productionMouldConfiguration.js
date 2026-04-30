import request, { downloadLink } from '@/utils/request'

// =
export function listProductionMouldConfiguration(query) {
  return request({
    url: '/monthplan/productionMouldConfiguration/list',
    method: 'post',
    data: query
  })
}
export function editProductionMouldConfiguration(query) {
  return request({
    url: '/monthplan/productionMouldConfiguration/save',
    method: 'post',
    data: query
  })
}
export function removeProductionMouldConfiguration(query) {
  return request({
    url: '/monthplan/productionMouldConfiguration/remove',
    method: 'post',
    data: query
  })
}
export function buildMouldingProduct(query) {
  return request({
    url: '/monthplan/productionMouldConfiguration/buildMouldingProduct',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function exportData(query) {
  downloadLink('/monthplan/productionMouldConfiguration/export', query)
}
