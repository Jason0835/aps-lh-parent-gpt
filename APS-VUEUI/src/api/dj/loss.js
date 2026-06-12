import request from '@/utils/request'

// =
export function listLoss(query) {
  return request({
    url: 'dj/lossSetting/list',
    method: 'post',
    data: query
  })
}
export function editLoss(query) {
  return request({
    url: 'dj/lossSetting/edit',
    method: 'post',
    data: query
  })
}

export function removeLoss(query) {
  return request({
    url: 'dj/lossSetting/remove',
    method: 'post',
    data: query
  })
}


