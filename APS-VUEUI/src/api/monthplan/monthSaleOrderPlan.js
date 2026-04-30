import request, { downloadLink } from '@/utils/request'

export function listMonthSaleOrderPlan(query) {
  return request({
    url: '/demand/monthSaleOrderPlan/list',
    method: 'post',
    data: query
  })
}
export function editMonthSaleOrderPlan(query) {
  return request({
    url: '/demand/monthSaleOrderPlan/edit',
    method: 'post',
    data: query
  })
}
export function removeMonthSaleOrderPlan(query) {
  return request({
    url: '/demand/monthSaleOrderPlan/remove',
    method: 'post',
    data: query
  })
}

export function syncInSaleOrder(query) {
  return request({
    url: '/demand/monthSaleOrderPlan/syncInSaleOrder',
    method: 'post',
    data: query
  })
}
export function syncOutSaleOrder(query) {
  return request({
    url: '/demand/monthSaleOrderPlan/syncOutSaleOrder',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/demand/monthSaleOrderPlan/export', query)
}
