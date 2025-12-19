import request from '@/utils/request'

// =
export function listAssistSpec(query) {
  return request({
    url: '/tq/assistSpec/list',
    method: 'post',
    data: query
  })
}
export function editAssistSpec(query) {
  return request({
    url: '/tq/assistSpec/save',
    method: 'post',
    data: query
  })
}

export function removeAssistSpec(query) {
  return request({
    url: '/tq/assistSpec/remove',
    method: 'post',
    data: query
  })
}


