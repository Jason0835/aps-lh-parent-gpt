import request, { downloadLink } from '@/utils/request'

// =
export function listLhflSafeStock(query) {
  return request({
    url: '/setting/lhflSafeStock/list',
    method: 'post',
    data: query
  })
}
export function removeLhflSafeStock(query) {
  return request({
    url: '/setting/lhflSafeStock/remove',
    method: 'post',
    data: query
  })
}
export function saveLhflSafeStock(query) {
  return request({
    url: '/setting/lhflSafeStock/save',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  return downloadLink('/setting/lhflSafeStock/export', query);
}
