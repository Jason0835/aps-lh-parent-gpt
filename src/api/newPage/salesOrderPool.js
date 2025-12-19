import request from '@/utils/request'
export function getSalesList(query) {
  return request({
    url: '/monthplan/SalesOrderPool/list',
    method: 'post',
    data: query,

  })
}
export function removeSales(query) {
  return request({
    url: '/monthplan/SalesOrderPool/remove',
    method: 'delete',
    data: query,

  })
}
export function getSCMData(query) {
  return request({
    url: '/monthplan/SalesOrderPool/getSCMData',
    method: 'post',
    data: query,

  })
}
export function getSCMDataCheck(query) {
  return request({
    url: '/monthplan/SalesOrderPool/checkSCMData',
    method: 'post',
    data: query,

  })
}
export function saveData(query) {
  return request({
    url: '/monthplan/SalesOrderPool/save',
    method: 'post',
    data: query,

  })
}
export function savePoData(query) {
  return request({
    url: '/monthplan/SalesOrderPool/editBySalCodePo',
    method: 'post',
    data: query,

  })
}