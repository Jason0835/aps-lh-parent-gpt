import request, { downloadLink } from '@/utils/request'

// =
export function stockHedgingConfigurationList(query) {
  return request({
    url: '/monthplan/businessSortConfiguration/stockHedgingConfigurationList',
    method: 'post',
    data: query
  })
}
export function saveStockHedgingConfiguration(query) {
  return request({
    url: '/monthplan/businessSortConfiguration/saveStockHedgingConfiguration',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}
export function planOrderSortConfigurationList(query) {
  return request({
    url: '/monthplan/businessSortConfiguration/planOrderSortConfigurationList',
    method: 'post',
    data: query
  })
}
export function savePlanOrderConfiguration(query) {
  return request({
    url: '/monthplan/businessSortConfiguration/savePlanOrderConfiguration',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}
