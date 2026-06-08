import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/dj/assistSchedule/list',
    method: 'post',
    data: query
  })
}
