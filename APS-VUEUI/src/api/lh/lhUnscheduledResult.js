import request from '@/utils/request'

// 不良数
export function listLhUnscheduledResult(query) {
  return request({
    url: '/lh/lhUnscheduledResult/list',
    method: 'post',
    data: query
  })
}


