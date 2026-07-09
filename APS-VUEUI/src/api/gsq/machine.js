import request,{ downloadLink } from '@/utils/request'

/**
 * 钢丝圈机台列表
 * @param {Object} query 查询条件
 * @returns 列表数据
 */
export function listMachine(query) {
  return request({
    url: '/gsq/machine/list',
    method: 'post',
    data: query
  })
}

/**
 * 获取所有启用的钢丝圈机台信息（status=0），供下拉框数据源使用
 * @returns 启用状态的机台列表
 */
export function listEnabledMachines() {
  return request({
    url: '/gsq/machine/listEnabledMachines',
    method: 'post'
  })
}

/**
 * 新增钢丝圈机台信息
 * @param {Object} query 机台信息
 * @returns 新增结果
 */
export function addMachine(query) {
  return request({
    url: '/gsq/machine/save',
    method: 'post',
    data: query
  })
}

/**
 * 修改钢丝圈机台信息
 * @param {Object} query 机台信息
 * @returns 修改结果
 */
export function editMachine(query) {
  return request({
    url: '/gsq/machine/save',
    method: 'post',
    data: query
  })
}

/**
 * 删除钢丝圈机台信息
 * @param {Object} query 包含ids的参数
 * @returns 删除结果
 */
export function removeMachine(query) {
  return request({
    url: '/gsq/machine/remove',
    method: 'post',
    params: query
  })
}

/**
 * 校验机台编号唯一性
 * @param {Object} query 机台信息
 * @returns 校验结果
 */
export function checkMachineCodeUnique(query) {
  return request({
    url: '/gsq/machine/checkUnique',
    method: 'post',
    data: query
  })
}

/**
 * 导出钢丝圈机台信息
 * @param {Object} query 查询条件
 * @returns 导出文件
 */
export function exportData(query) {
  return downloadLink("/gsq/machine/export", query);
}

/**
 * 获取机台详细信息
 * @param {Number} id 机台ID
 * @returns 机台详细信息
 */
export function getMachineInfo(id) {
  return request({
    url: '/gsq/machine/' + id,
    method: 'get'
  })
}

/**
 * 下载导入模板
 * @returns 模板文件
 */
export function importTemplate() {
  return downloadLink("/gsq/machine/importTemplate");
}
