import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/gdyy/assistSchedule/list',
    method: 'post',
    data: query
  })
}
