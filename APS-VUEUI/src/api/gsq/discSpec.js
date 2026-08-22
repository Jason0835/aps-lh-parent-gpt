import request, { downloadLink } from '@/utils/request'

/**
 * 查询缠绕盘-规格关系列表
 * @param {Object} query 查询条件
 * @returns
 */
export function listDiscSpec(query) {
  return request({
    url: '/gsq/discSpec/list',
    method: 'post',
    data: query
  })
}

/**
 * 获取缠绕盘-规格关系详细信息
 * @param {Number} id 主键ID
 * @returns
 */
export function getDiscSpecInfo(id) {
  return request({
    url: '/gsq/discSpec/getInfo/' + id,
    method: 'get'
  })
}

/**
 * 保存缠绕盘-规格关系（id为空新增，id不为空修改）
 * @param {Object} query 实体
 * @returns
 */
export function saveDiscSpec(query) {
  return request({
    url: '/gsq/discSpec/save',
    method: 'post',
    data: query
  })
}

/**
 * 删除缠绕盘-规格关系（逻辑删除）
 * @param {String|Array} ids 主键ID集合（逗号分隔字符串）
 * @returns
 */
export function removeDiscSpec(ids) {
  return request({
    url: '/gsq/discSpec/remove',
    method: 'post',
    params: { ids }
  })
}

/**
 * 校验缠绕盘+钢丝圈规格组合唯一性
 * @param {Object} query 实体
 * @returns
 */
export function checkDiscSpecUnique(query) {
  return request({
    url: '/gsq/discSpec/checkUnique',
    method: 'post',
    data: query
  })
}

/**
 * 查询施工信息表全部钢丝圈选项（编码+名称，去重）
 * @returns
 */
export function listSteelRingOptions() {
  return request({
    url: '/gsq/discSpec/listSteelRingOptions',
    method: 'get'
  })
}

/**
 * 导出缠绕盘-规格关系
 * @param {Object} query 查询条件
 * @returns
 */
export function exportDiscSpec(query) {
  return downloadLink('/gsq/discSpec/export', query)
}
