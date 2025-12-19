import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/cd90/assistSchedule/list',
    method: 'post',
    data: query
  })
}
