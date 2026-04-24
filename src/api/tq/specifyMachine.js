import request, { downloadLink } from '@/utils/request'

export function listSpecifyMachine(query) {
  return request({
    url: '/tq/specifyMachine/list',
    method: 'post',
    data: query
  })
}

export function saveSpecifyMachine(query) {
  return request({
    url: '/tq/specifyMachine/save',
    method: 'post',
    data: query
  })
}

export function removeSpecifyMachine(ids) {
  return request({
    url: '/tq/specifyMachine/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function removeAllSpecifyMachine() {
  return request({
    url: '/tq/specifyMachine/removeAll',
    method: 'post'
  })
}

export function exportSpecifyMachine(query) {
  return downloadLink("/tq/specifyMachine/export", query)
}
