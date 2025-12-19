import request from '@/utils/request'

/**
 * 根据条件查询钢带压延注意事项
 * @param {Object} query
 * @returns
 */
export function listMattersAttention(query) {
  return request({
    url: '/gdyy/gdyyMattersAttention/list',
    method: 'post',
    data: query
  })
}
/**
 * 修改
 * @param {Object} query
 * @returns
 */
export function editMattersAttention(query) {
  return request({
    url: '/gdyy/gdyyMattersAttention/save',
    method: 'post',
    data: query
  })
}
export function removeMattersAttention(query) {
  return request({
    url: '/gdyy/gdyyMattersAttention/remove',
    method: 'post',
    data: query
  })
}
