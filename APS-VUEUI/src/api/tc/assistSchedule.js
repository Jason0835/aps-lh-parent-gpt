import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/tc/assistSchedule/list',
    method: 'post',
    data: query
  })
}
