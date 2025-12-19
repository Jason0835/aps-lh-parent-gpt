import request, { downloadLink } from '@/utils/request'

// =
export function listRawWeekUsage(query) {
  return request({
    url: '/maindata/rawWeekUsage/list',
    method: 'post',
    data: query
  })
}
// =
export function monthRawWeekUsage(query) {
  return request({
    url: '/maindata/rawWeekUsage/generate-by-month',
    method: 'post',
    data: query
  })
}

