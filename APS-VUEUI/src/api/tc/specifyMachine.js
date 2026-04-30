import request from '@/utils/request'

/**
 * 成型定点机台管理列表
 * @param {Object} query
 * @returns
 */
export function listSpecifyMachine(query) {
  return request({
    url: 'tc/specifyMachine/list',
    method: 'post',
    data: query
  })
}
/**
 * 编辑
 * @param {Object} query
 * @returns
 */
export function editSpecifyMachine(query) {
  return request({
    url: 'tc/specifyMachine/save',
    method: 'post',
    data: query
  })
}
export function removeSpecifyMachine(query) {
  return request({
    url: 'tc/specifyMachine/remove',
    method: 'post',
    data: query
  })
}
export function removeAllSpecifyMachine(query) {
  return request({
    url: 'tc/specifyMachine/removeAll',
    method: 'post',
    data: query
  })
}
