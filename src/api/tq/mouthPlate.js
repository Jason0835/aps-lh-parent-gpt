import request, { downloadLink } from '@/utils/request'

export function listMouthPlate(query) {
  return request({
    url: '/tq/mouthPlate/list',
    method: 'post',
    data: query
  })
}

export function saveMouthPlate(query) {
  return request({
    url: '/tq/mouthPlate/save',
    method: 'post',
    data: query
  })
}

export function removeMouthPlate(ids) {
  return request({
    url: '/tq/mouthPlate/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function removeAllMouthPlate() {
  return request({
    url: '/tq/mouthPlate/removeAll',
    method: 'post'
  })
}

export function exportMouthPlate(query) {
  return downloadLink("/tq/mouthPlate/export", query)
}
