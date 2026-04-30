import request from '@/utils/request'

// =
export function listAssistSchedule(query) {
  return request({
    url: '/xwyy/assistSchedule/list',
    method: 'post',
    data: query
  })
}
