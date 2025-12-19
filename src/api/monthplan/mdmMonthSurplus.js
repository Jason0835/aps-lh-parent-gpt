import request from '@/utils/request'
export function getMonthSurplusList(query) {
  return request({
    url: '/monthplan/mdmMonthSurplus/list',
    method: 'post',
    data: query
  })
}