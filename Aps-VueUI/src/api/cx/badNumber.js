import request from '@/utils/request'

// 不良数
export function listBadNumber(query) {
  return request({
    url: '/cx/badNumber/list',
    method: 'post',
    data: query
  })
}


