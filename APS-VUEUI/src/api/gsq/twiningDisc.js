import request, { downloadLink } from '@/utils/request'

/**
 * 查询钢丝圈缠绕盘列表
 * @param {Object} query 查询条件
 * @returns
 */
export function listTwiningDisc(query) {
  return request({
    url: '/gsq/twiningDisc/list',
    method: 'post',
    data: query
  })
}

/**
 * 获取钢丝圈缠绕盘详情（含子表明细及钢丝圈名称反显）
 * @param {Number} id 主键ID
 * @returns
 */
export function getTwiningDiscInfo(id) {
  return request({
    url: '/gsq/twiningDisc/getInfo/' + id,
    method: 'get'
  })
}

/**
 * 保存钢丝圈缠绕盘（id为空新增，id不为空修改，单表保存）
 * @param {Object} query 实体
 * @returns
 */
export function saveTwiningDisc(query) {
  return request({
    url: '/gsq/twiningDisc/save',
    method: 'post',
    data: query
  })
}

/**
 * 删除钢丝圈缠绕盘（逻辑删除主表，按缠绕盘编码级联逻辑删除规格关系及机台关系）
 * @param {String|Array} ids 主键ID集合（逗号分隔字符串）
 * @returns
 */
export function removeTwiningDisc(ids) {
  return request({
    url: '/gsq/twiningDisc/remove',
    method: 'post',
    params: { ids }
  })
}

/**
 * 校验缠绕盘编码唯一性
 * @param {Object} query 实体
 * @returns
 */
export function checkTwiningDiscUnique(query) {
  return request({
    url: '/gsq/twiningDisc/checkUnique',
    method: 'post',
    data: query
  })
}

/**
 * 查询施工信息表全部钢丝圈选项（编码+名称，去重），供子表下拉选择使用
 * @returns
 */
export function listSteelRingOptions() {
  return request({
    url: '/gsq/twiningDisc/listSteelRingOptions',
    method: 'get'
  })
}

/**
 * 导出钢丝圈缠绕盘
 * @param {Object} query 查询条件
 * @returns
 */
export function exportTwiningDisc(query) {
  return downloadLink('/gsq/twiningDisc/export', query)
}
