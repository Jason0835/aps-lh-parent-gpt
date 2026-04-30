import request, {downloadLink} from '@/utils/request'

// =
export function listSafeStock(query) {
  return request({
    url: '/setting/safeStock/list',
    method: 'post',
    data: query
  })
}
export function removeSafeStock(query) {
  return request({
    url: '/setting/safeStock/remove',
    method: 'post',
    data: query
  })
}
export function saveSafeStock(query) {
  return request({
    url: '/setting/safeStock/save',
    method: 'post',
    data: query
  })
}
export function checkGlueSafeStockUnique(query) {
  return request({
    url: '/setting/safeStock/checkGlueSafeStockUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/safeStock/checkComplete',
    method: 'post',
    data: query
  })
}
export function exportData(params) {
  return downloadLink("/setting/safeStock/export", params);
}

