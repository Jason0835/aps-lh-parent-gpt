import request from '@/utils/request'

/**
 * 根据条件查询纤维压延外厂需求管理列表
 * @param {Object} query
 * @returns
 */
export function listAssistRequirement(query) {
  return request({
    url: '/xwyy/assistRequirement/list',
    method: 'post',
    data: query
  })
}
/**
 * 修改
 * @param {Object} query
 * @returns
 */
export function editAssistRequirement(query) {
  return request({
    url: '/xwyy/assistRequirement/edit',
    method: 'post',
    data: query
  })
}
export function removeAssistRequirement(query) {
  return request({
    url: '/xwyy/assistRequirement/remove',
    method: 'post',
    data: query
  })
}
