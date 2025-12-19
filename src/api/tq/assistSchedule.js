import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/tq/assistSchedule/list',
    method: 'post',
    data: query
  })
}
