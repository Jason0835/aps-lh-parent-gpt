import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/cd15/assistSchedule/list',
    method: 'post',
    data: query
  })
}
