import request from '@/utils/request'
export function listMonCycleSchStruConf(query) {
  return request({
    url: '/monthplan/mdmMonCycleSchStruConf/list',
    method: 'post',
    data: query
  })
}
export function removeMonCycleSchStruConf(query) {
  return request({
    url: '/monthplan/mdmMonCycleSchStruConf/remove',
    method: 'post',
    data: query
  })
}
export function saveMonCycleSchStruConf(query) {
  return request({
    url: '/monthplan/mdmMonCycleSchStruConf/save',
    method: 'post',
    data: query
  })
}