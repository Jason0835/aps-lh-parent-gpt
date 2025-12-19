import request from '@/utils/request'

// 不良数
export function listDispatcherLog(query) {
  return request({
    url: '/cd15/dispatcherLog/list',
    method: 'post',
    data: query
  })
}


