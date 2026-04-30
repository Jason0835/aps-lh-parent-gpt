import request, { downloadLink } from '@/utils/request'

// =
export function listRawWarningConfig(query) {
  return request({
    url: '/maindata/rawWarningConfig/list',
    method: 'post',
    data: query
  })
}
// =
export function saveRawWarningConfig(query) {
  return request({
    url: '/maindata/rawWarningConfig/save',
    method: 'post',
    data: query
  })
}
export function removeRawWarningConfig(query) {
  return request({
    url: '/maindata/rawWarningConfig/remove',
    method: 'post',
    data: query
  })
}
