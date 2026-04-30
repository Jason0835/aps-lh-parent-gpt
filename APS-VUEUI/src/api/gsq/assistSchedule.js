import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/gsq/assistSchedule/list',
    method: 'post',
    data: query
  })
}
