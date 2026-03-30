import request from '@/utils/request'
export function getMonthSurplusList(query) {
  return request({
    url: '/monthplan/mdmMonthSurplus/list',
    method: 'post',
    data: query
  })
}
export function getVersionSelect(query) {
  return request({
    url: '/monthplan/mdmMonthSurplus/listRequireVersions',
    method: 'post',
    data: query
  })
}
