import request, { downloadLink } from '@/utils/request'

/**
 * 查询胎圈备库班数配置列表
 * @param {Object} query 查询条件
 */
export function listStockShiftConfig(query) {
  return request({
    url: '/tq/stockShiftConfig/list',
    method: 'post',
    data: query
  })
}

/**
 * 保存胎圈备库班数配置（新增/编辑）
 * @param {Object} query 配置数据
 */
export function saveStockShiftConfig(query) {
  return request({
    url: '/tq/stockShiftConfig/save',
    method: 'post',
    data: query
  })
}

/**
 * 删除胎圈备库班数配置
 * @param {Object} query 包含ids的对象
 */
export function removeStockShiftConfig(query) {
  return request({
    url: '/tq/stockShiftConfig/remove',
    method: 'post',
    data: query
  })
}

/**
 * 校验唯一性
 * @param {Object} query 配置数据
 */
export function checkStockShiftConfigUnique(query) {
  return request({
    url: '/tq/stockShiftConfig/checkUnique',
    method: 'post',
    data: query
  })
}

/**
 * 校验配置规则交叉（确保新增/修改的规则不与现有规则有范围交叉）
 * @param {Object} query 配置数据
 */
export function checkStockShiftConfigRangeCross(query) {
  return request({
    url: '/tq/stockShiftConfig/checkRangeCross',
    method: 'post',
    data: query
  })
}

/**
 * 导出胎圈备库班数配置
 * @param {Object} query 查询条件
 */
export function exportStockShiftConfig(query) {
  return downloadLink("/tq/stockShiftConfig/export", query)
}
