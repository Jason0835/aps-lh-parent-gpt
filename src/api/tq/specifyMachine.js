import request, { downloadLink } from '@/utils/request'

export function listSpecifyMachine(query) {
  return request({
    url: 'tq/specifyMachine/list',
    method: 'post',
    data: query
  })
}
export function addSpecifyMachine(query) {
  return request({
    url: 'tq/specifyMachine/save',
    method: 'post',
    data: query
  })
}
export function editSpecifyMachine(query) {
  return request({
    url: 'tq/specifyMachine/save',
    method: 'post',
    data: query
  })
}
export function removeSpecifyMachine(query) {
  return request({
    url: 'tq/specifyMachine/remove',
    method: 'post',
    data: query
  })
}
export function removeAllSpecifyMachine(query) {
  return request({
    url: 'tq/specifyMachine/removeAll',
    method: 'post',
    data: query
  })
}
export function exportData(query) {
  return downloadLink("/tq/specifyMachine/export", query);
}
