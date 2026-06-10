import request from '@/utils/request'

// 不良数
export function listDispatcherLog(query) {
  return request({
    url: '/dj/dispatcherLog/list',
    method: 'post',
    data: query
  })
}


