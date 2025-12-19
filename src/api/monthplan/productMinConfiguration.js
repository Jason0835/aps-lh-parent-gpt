import request, { downloadLink } from '@/utils/request'

// =
export function listProductMinConfiguration(query) {
  return request({
    url: '/monthplan/productMinConfiguration/list',
    method: 'post',
    data: query
  })
}
export function editProductMinConfiguration(query) {
  return request({
    url: '/monthplan/productMinConfiguration/save',
    method: 'post',
    data: query
  })
}
export function removeProductMinConfiguration(query) {
  return request({
    url: '/monthplan/productMinConfiguration/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/productMinConfiguration/export', query)
}
