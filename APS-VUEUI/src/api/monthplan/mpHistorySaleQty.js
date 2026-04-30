import request, { downloadLink } from '@/utils/request'

// =
export function listMpHistorySaleQty(query) {
  return request({
    url: '/monthplan/mpHistorySaleQty/list',
    method: 'post',
    data: query
  })
}
export function queryCalcStocking(query) {
  return request({
    url: '/monthplan/mpHistorySaleQty/queryCalcStocking',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mpHistorySaleQty/export', query)
}
