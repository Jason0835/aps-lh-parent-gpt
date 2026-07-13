import request from '@/utils/request'

// =
export function listLoss(query) {
  return request({
    url: '/nc/lossSetting/list',
    method: 'post',
    data: query
  })
}
export function editLoss(query) {
  return request({
    url: '/nc/lossSetting/save',
    method: 'post',
    data: query
  })
}

export function removeLoss(query) {
  return request({
    url: '/nc/lossSetting/remove',
    method: 'post',
    data: query
  })
}


