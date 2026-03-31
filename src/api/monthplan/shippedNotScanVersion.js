import request from '@/utils/request'

export function getList(query) {
  return request({
    url: '/monthplan/dpShippedNotScanVersion/list',
    method: 'post',
    data: query
  })
}

export function getVersionSelect(query) {
  return request({
    url: '/monthplan/dpShippedNotScanVersion/findMonthPlanVersion',
    method: 'post',
    data: query
  })
}

export function generateVersion(query) {
  return request({
    url: '/monthplan/dpShippedNotScanVersion/generate',
    method: 'post',
    data: query
  })
}
