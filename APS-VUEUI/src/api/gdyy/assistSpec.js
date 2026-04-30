import request from '@/utils/request'

// =
export function listAssistSpec(query) {
  return request({
    url: '/gdyy/assistSpec/list',
    method: 'post',
    data: query
  })
}
export function editAssistSpec(query) {
  return request({
    url: '/gdyy/assistSpec/save',
    method: 'post',
    data: query
  })
}

export function removeAssistSpec(query) {
  return request({
    url: '/gdyy/assistSpec/remove',
    method: 'post',
    data: query
  })
}


