import request, { downloadLink } from '@/utils/request'

/**
 * 查询钢丝圈库存列表
 * @param {Object} query 查询条件
 * @returns
 */
export function listSteelRingStock(query) {
  return request({
    url: '/gsq/steelRingStock/list',
    method: 'post',
    data: query
  })
}

/**
 * 获取钢丝圈库存详情
 * @param {Number} id 主键ID
 * @returns
 */
export function getSteelRingStock(id) {
  return request({
    url: '/gsq/steelRingStock/getInfo/' + id,
    method: 'get'
  })
}

/**
 * 新增钢丝圈库存
 * @param {Object} query 实体
 * @returns
 */
export function addSteelRingStock(query) {
  return request({
    url: '/gsq/steelRingStock/add',
    method: 'post',
    data: query
  })
}

/**
 * 编辑钢丝圈库存
 * @param {Object} query 实体
 * @returns
 */
export function editSteelRingStock(query) {
  return request({
    url: '/gsq/steelRingStock/edit',
    method: 'post',
    data: query
  })
}

/**
 * 删除钢丝圈库存（逻辑删除）
 * @param {String|Array} ids 主键ID集合（逗号分隔字符串）
 * @returns
 */
export function removeSteelRingStock(ids) {
  return request({
    url: '/gsq/steelRingStock/remove',
    method: 'post',
    params: { ids: ids.toString() }
  })
}

/**
 * 导出钢丝圈库存
 * @param {Object} query 查询条件
 * @returns
 */
export function exportSteelRingStock(query) {
  return downloadLink('/gsq/steelRingStock/export', query)
}
