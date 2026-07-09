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
 * 保存钢丝圈损耗率（id为空新增，id不为空修改）
 * @param {Object} query 实体
 * @returns
 */
export function saveLossRate(query) {
  return request({
    url: '/gsq/lossRate/save',
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
    url: '/gsq/lossRate/delete/' + ids,
    method: 'post'
  })
}

/**
 * 校验钢丝圈损耗率唯一性
 * @param {Object} query 实体
 * @returns
 */
export function checkLossRateUnique(query) {
  return request({
    url: '/gsq/lossRate/checkUnique',
    method: 'post',
    data: query
  })
}

/**
 * 导出钢丝圈损耗率
 * @param {Object} query 查询条件
 * @returns
 */
export function exportLossRate(query) {
  return downloadLink('/gsq/lossRate/exportData/钢丝圈损耗率管理', query)
}
