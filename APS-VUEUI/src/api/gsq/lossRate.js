import request, { downloadLink } from '@/utils/request'

/**
 * 查询钢丝圈损耗率列表
 * @param {Object} query 查询条件
 * @returns
 */
export function listLossRate(query) {
  return request({
    url: '/gsq/lossRate/list',
    method: 'post',
    data: query
  })
}

/**
 * 获取钢丝圈损耗率详情
 * @param {Number} id 主键ID
 * @returns
 */
export function getLossRate(id) {
  return request({
    url: '/gsq/lossRate/getInfo/' + id,
    method: 'get'
  })
}

/**
 * 新增钢丝圈损耗率
 * @param {Object} query 实体
 * @returns
 */
export function addLossRate(query) {
  return request({
    url: '/gsq/lossRate/add',
    method: 'post',
    data: query
  })
}

/**
 * 编辑钢丝圈损耗率
 * @param {Object} query 实体
 * @returns
 */
export function editLossRate(query) {
  return request({
    url: '/gsq/lossRate/edit',
    method: 'post',
    data: query
  })
}

/**
 * 删除钢丝圈损耗率（逻辑删除）
 * @param {String|Array} ids 主键ID集合（逗号分隔字符串）
 * @returns
 */
export function removeLossRate(ids) {
  return request({
    url: '/gsq/lossRate/remove',
    method: 'post',
    params: { ids: ids.toString() }
  })
}

/**
 * 导出钢丝圈损耗率
 * @param {Object} query 查询条件
 * @returns
 */
export function exportLossRate(query) {
  return downloadLink('/gsq/lossRate/export', query)
}
