import request from '@/utils/request'

// =
export function listAssistSpec(query) {
  return request({
    url: '/nc/assistSpec/list',
    method: 'post',
    data: query
  })
}
export function editAssistSpec(query) {
  return request({
    url: '/nc/assistSpec/save',
    method: 'post',
    data: query
  })
}

export function removeAssistSpec(query) {
  return request({
    url: '/nc/assistSpec/remove',
    method: 'post',
    data: query
  })
}


