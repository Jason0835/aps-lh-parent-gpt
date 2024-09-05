import request from '@/utils/request'

/**
 * 成型定额设备管理列表
 * @param {Object} query
 * @returns
 */
export function lisQuotaMachine(query) {
  return request({
    url: 'cx/quota/list',
    method: 'post',
    data: query
  })
}
/**
 * 编辑
 * @param {Object} query
 * @returns
 */
export function editQuotaMachine(query) {
  return request({
    url: 'cx/quota/edit',
    method: 'post',
    data: query
  })
}
export function removeQuotaMachine(query) {
  return request({
    url: 'cx/quota/remove',
    method: 'post',
    data: query
  })
}
