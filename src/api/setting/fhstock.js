import request, { downloadLink } from '@/utils/request'

// =
export function listReturnedStock(query) {
  return request({
    url: '/setting/fhstock/list',
    method: 'post',
    data: query
  })
}
export function removeReturnedStock(query) {
  return request({
    url: '/setting/fhstock/remove',
    method: 'post',
    data: query
  })
}
export function saveReturnedStock(query) {
  return request({
    url: '/setting/fhstock/save',
    method: 'post',
    data: query
  })
}
export function checkGlueReturnedStockUnique(query) {
  return request({
    url: '/setting/fhstock/checkGlueReturnedStockUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/fhstock/checkComplete',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  return downloadLink('/setting/fhstock/export', query);
}
