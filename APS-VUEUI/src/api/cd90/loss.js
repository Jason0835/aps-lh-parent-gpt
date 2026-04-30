import request from '@/utils/request'

// =
export function listLoss(query) {
  return request({
    url: '/cd90/loss/list',
    method: 'post',
    data: query
  })
}
export function editLoss(query) {
  return request({
    url: '/cd90/loss/edit',
    method: 'post',
    data: query
  })
}

export function removeLoss(query) {
  return request({
    url: '/cd90/loss/remove',
    method: 'post',
    data: query
  })
}


