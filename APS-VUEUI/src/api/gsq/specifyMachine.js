import request, { downloadLink } from '@/utils/request'

/**
 * 查询钢丝圈定点机台列表
 * @param {Object} query 查询条件
 * @returns
 */
export function listSpecifyMachine(query) {
  return request({
    url: '/gsq/specifyMachine/list',
    method: 'post',
    data: query
  })
}

/**
 * 保存钢丝圈定点机台（id为空新增，id不为空修改）
 * @param {Object} query 实体
 * @returns
 */
export function saveSpecifyMachine(query) {
  return request({
    url: '/gsq/specifyMachine/save',
    method: 'post',
    data: query
  })
}

/**
 * 删除钢丝圈定点机台（逻辑删除）
 * @param {String|Array} ids 主键ID集合（逗号分隔字符串）
 * @returns
 */
export function removeSpecifyMachine(ids) {
  return request({
    url: '/gsq/specifyMachine/delete/' + ids,
    method: 'post'
  })
}

/**
 * 获取钢丝圈定点机台详细信息
 * @param {Number} id 主键ID
 * @returns
 */
export function getSpecifyMachine(id) {
  return request({
    url: '/gsq/specifyMachine/' + id,
    method: 'get'
  })
}

/**
 * 校验钢丝圈定点机台唯一性（钢丝圈代码+生产线）
 * @param {Object} query 实体
 * @returns
 */
export function checkSpecifyMachineUnique(query) {
  return request({
    url: '/gsq/specifyMachine/checkUnique',
    method: 'post',
    data: query
  })
}

/**
 * 导出钢丝圈定点机台
 * @param {Object} query 查询条件
 * @returns
 */
export function exportSpecifyMachine(query) {
  return downloadLink('/gsq/specifyMachine/exportData/钢丝圈定点机台', query)
}
