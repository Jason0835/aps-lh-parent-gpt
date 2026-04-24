import request, { downloadLink } from '@/utils/request'

export function listLoss(query) {
  return request({
    url: '/tq/loss/list',
    method: 'post',
    data: query
  })
}

export function saveLoss(query) {
  return request({
    url: '/tq/loss/save',
    method: 'post',
    data: query
  })
}

export function removeLoss(ids) {
  return request({
    url: '/tq/loss/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportLoss(query) {
  return downloadLink("/tq/loss/export", query)
}
