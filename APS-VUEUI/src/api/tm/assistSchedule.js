import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/tm/assistSchedule/list',
    method: 'post',
    data: query
  })
}
