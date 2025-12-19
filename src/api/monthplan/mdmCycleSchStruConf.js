import request from '@/utils/request'
export function listCycleSchStruConf(query) {
  return request({
    url: '/monthplan/mdmCycleSchStruConf/list',
    method: 'post',
    data: query
  })
}
export function removeCycleSchStruConf(query) {
  return request({
    url: '/monthplan/mdmCycleSchStruConf/remove',
    method: 'post',
    data: query
  })
}
export function saveCycleSchStruConf(query) {
  return request({
    url: '/monthplan/mdmCycleSchStruConf/save',
    method: 'post',
    data: query
  })
}
export function getCycleSchStruConf(query) {
  return request({
    url: '/monthplan/mdmCycleSchStruConf/genMonthCycleSchStruConf',
    method: 'post',
    data: query
  })
}