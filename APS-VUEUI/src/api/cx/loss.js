import request from '@/utils/request'

// =
export function listLoss(query) {
  return request({
    url: '/cx/loss/list',
    method: 'post',
    data: query
  })
}
export function editLoss(query) {
  return request({
    url: '/cx/loss/edit',
    method: 'post',
    data: query
  })
}

export function removeLoss(query) {
  return request({
    url: '/cx/loss/remove',
    method: 'post',
    data: query
  })
}


