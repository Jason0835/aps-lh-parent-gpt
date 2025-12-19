import request from '@/utils/request'

// =
export function scheduleMixAreaPermission(query) {
  return request({
    url: '/setting/service/scheduleMixAreaPermission',
    method: 'get',
    params: query
  })
}
