import request,{ downloadLink } from '@/utils/request'

/**
 * 根据条件查询帘布大卷与机台的映射表列表
 * @param {Object} query
 * @returns
 */
export function listMachineRollMapping(query) {
  return request({
    url: 'cd15/machineRollMapping/list',
    method: 'post',
    data: query
  })
}
/**
 * 修改帘布大卷与机台的映射表
 * @param {Object} query
 * @returns
 */
export function editMachineRollMapping(query) {
  return request({
    url: 'cd15/machineRollMapping/save',
    method: 'post',
    data: query
  })
}
export function removeMachineRollMapping(query) {
  return request({
    url: '/cd15/machineRollMapping/remove',
    method: 'post',
    data: query
  })
}
  