import request, { downloadLink } from '@/utils/request'

// =
export function listStock(query) {
  return request({
    url: '/setting/stock/list',
    method: 'post',
    data: query
  })
}
export function removeStock(query) {
  return request({
    url: '/setting/stock/remove',
    method: 'post',
    data: query
  })
}
export function saveStock(query) {
  return request({
    url: '/setting/stock/save',
    method: 'post',
    data: query
  })
}
export function checkGlueStockUnique(query) {
  return request({
    url: '/setting/stock/checkGlueStockUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/stock/checkComplete',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  return downloadLink('/setting/stock/export', query);
}
