import request from '@/utils/request'

// =
export function listLoss(query) {
  return request({
    url: '/gsq/loss/list',
    method: 'post',
    data: query
  })
}
export function editLoss(query) {
  return request({
    url: '/gsq/loss/edit',
    method: 'post',
    data: query
  })
}

export function removeLoss(query) {
  return request({
    url: '/gsq/loss/remove',
    method: 'post',
    data: query
  })
}


