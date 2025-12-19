import request, { downloadLink } from '@/utils/request'

//
export function listMouldingDayResult(query) {
  return request({
    url: '/monthplan/mouldingDayResult/list',
    method: 'post',
    data: query
  })
}

export function listProductionVersionList(query) {
  return request({
    url: '/monthplan/mouldingDayResult/productionVersionList',
    method: 'post',
    data: query
  })
}


export function changeSpecCode(query) {
  return request({
    url: '/monthplan/mouldingDayResult/changeSpecCode',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function statistics(query) {
  return request({
    url: '/monthplan/mouldingDayResult/statistics',
    method: 'post',
    data: query,
    // headers: {
    //   'Content-Type': 'application/json;charset=UTF-8'
    // },
  })
}