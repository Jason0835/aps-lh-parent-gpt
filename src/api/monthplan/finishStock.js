import request from '@/utils/request'
export function getListMes(query) {
  return request({
    // url: '/monthplan/mdmFinishStock/list4Mes',
    url: '/monthplan/mdmProductStock/list',
    method: 'post',
    data: query
  })
}

export function getFinishList(query) {
  return request({
    url: '/monthplan/mdmFinishStock/list',
    method: 'post',
    data: query
  })
}

export function genOverDueSkuByStock(query) {
  return request({
    url: '/monthplan/mdmProductStock/genOverDueSkuByStock',
    method: 'post',
    data: query
  })
}