import request from '@/utils/request'
export function mdmWorkCalendar(query) {
  return request({
    url: '/maindata/mdmWorkCalendar/list',
    method: 'post',
    data: query,
    // headers: {
    //   'Content-Type': 'application/json;charset=UTF-8'
    // },

  })
}
export function selectProcCodeList(query) {
  return request({
    url: '/maindata/mdmWorkCalendar/selectProcCodeList',
    method: 'post',
    data: query,

  })
}
export function genAnnualPlan(query) {
  return request({
    url: '/maindata/mdmWorkCalendar/genAnnualPlan',
    method: 'post',
    data: query,

  })
}
export function editAnnualPlan(query) {
  return request({
    url: '/maindata/mdmWorkCalendar/save',
    method: 'post',
    data: query,

  })
}