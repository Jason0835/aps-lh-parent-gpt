import request, { downloadLink } from '@/utils/request'

// =
export function listRawWarningRecord(query) {
  return request({
    url: '/maindata/rawWarningRecord/list',
    method: 'post',
    data: query
  })
}
// =
export function materialRawWarningRecord(query) {
  return request({
    url: '/maindata/rawWarningRecord/execute-new-material-warning',
    method: 'post',
    data: query
  })
}
export function usageRawWarningRecord(query) {
  return request({
    url: '/maindata/rawWarningRecord/execute-usage-warning',
    method: 'post',
    data: query
  })
}

