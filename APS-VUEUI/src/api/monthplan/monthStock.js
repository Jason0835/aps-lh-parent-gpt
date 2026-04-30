import request, { downloadLink } from '@/utils/request'

//
export function listMonthStock(query) {
  return request({
    url: '/monthplan/monthStock/list',
    method: 'post',
    data: query
  })
}
export function editMonthStock(query) {
  return request({
    url: '/monthplan/monthStock/save',
    method: 'post',
    data: query
  })
}
export function crawMonthStock(query) {
  return request({
    url: '/monthplan/monthStock/craw',
    method: 'post',
    data: query
  })
}
export function removeMonthStock(query) {
  return request({
    url: '/monthplan/monthStock/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/monthStock/export', query)
}
