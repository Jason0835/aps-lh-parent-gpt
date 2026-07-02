import request, { downloadLink } from '@/utils/request'

/**
 * 查询钢丝圈机台维修计划列表
 * @param {Object} query 查询条件
 * @returns
 */
export function listMachineMaintenancePlan(query) {
  return request({
    url: '/gsq/machineMaintenancePlan/list',
    method: 'post',
    data: query
  })
}

/**
 * 保存钢丝圈机台维修计划（id为空新增，id不为空修改）
 * @param {Object} query 实体
 * @returns
 */
export function saveMachineMaintenancePlan(query) {
  return request({
    url: '/gsq/machineMaintenancePlan/save',
    method: 'post',
    data: query
  })
}

/**
 * 删除钢丝圈机台维修计划（逻辑删除）
 * @param {String|Array} ids 主键ID集合（逗号分隔字符串）
 * @returns
 */
export function removeMachineMaintenancePlan(ids) {
  return request({
    url: '/gsq/machineMaintenancePlan/delete/' + ids,
    method: 'post'
  })
}

/**
 * 导出钢丝圈机台维修计划
 * @param {Object} query 查询条件
 * @returns
 */
export function exportMachineMaintenancePlan(query) {
  return downloadLink('/gsq/machineMaintenancePlan/exportData/钢丝圈机台维修计划', query)
}
