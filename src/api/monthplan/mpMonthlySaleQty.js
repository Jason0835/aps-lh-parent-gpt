import request from '@/utils/request'

// =
export function listMpMonthlySaleQty(query) {
  return request({
    url: '/monthplan/mpMonthlySaleQty/list',
    method: 'post',
    data: query
  })
}

export function tabletMpMonthlySaleQty(query) {
  return request({
    url: '/monthplan/mpMonthlySaleQty/getShowTableTitleList',
    method: 'post',
    data: query
  })
}
