import request from '@/utils/request'

// 不良数
export function listDispatcherLog(query) {
  return request({
    url: '/nc/dispatcherLog/list',
    method: 'post',
    data: query
  })
}


