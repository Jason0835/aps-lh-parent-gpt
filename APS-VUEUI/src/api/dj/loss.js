import request from '@/utils/request'

// =
export function listLoss(query) {
  return request({
    url: '/dj/loss/list',
    method: 'post',
    data: query
  })
}
export function editLoss(query) {
  return request({
    url: '/dj/loss/edit',
    method: 'post',
    data: query
  })
}

export function removeLoss(query) {
  return request({
    url: '/dj/loss/remove',
    method: 'post',
    data: query
  })
}


