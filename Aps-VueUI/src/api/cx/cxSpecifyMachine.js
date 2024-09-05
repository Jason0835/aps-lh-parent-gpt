import request from '@/utils/request'

/**
 * 成型定点机台管理列表
 * @param {Object} query
 * @returns
 */
export function lisCxSpecifyMachine(query) {
  return request({
    url: 'cx/cxSpecifyMachine/list',
    method: 'post',
    data: query
  })
}
/**
 * 编辑
 * @param {Object} query
 * @returns
 */
export function editCxSpecifyMachine(query) {
  return request({
    url: 'cx/cxSpecifyMachine/edit',
    method: 'post',
    data: query
  })
}
