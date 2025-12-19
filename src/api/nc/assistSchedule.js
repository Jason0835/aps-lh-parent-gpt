import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/nc/assistSchedule/list',
    method: 'post',
    data: query
  })
}
