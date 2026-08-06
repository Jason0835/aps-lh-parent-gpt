import request from '@/utils/request'

// 查询内衬机台维修计划列表
export function listMachineMaintenance(query) {
  return request({
    url: '/nc/machineMaintenance/list',
    method: 'post',
    data: query
  })
}

// 查询内衬机台维修计划详细
export function getMachineMaintenance(billId) {
  return request({
    url: '/nc/machineMaintenance/' + billId,
    method: 'get'
  })
}

// 新增内衬机台维修计划
export function addMachineMaintenance(data) {
  return request({
    url: '/nc/machineMaintenance/save',
    method: 'post',
    data: data
  })
}

// 修改内衬机台维修计划
export function updateMachineMaintenance(data) {
  return request({
    url: '/nc/machineMaintenance/save',
    method: 'post',
    data: data
  })
}

// 删除内衬机台维修计划
export function delMachineMaintenance(ids) {
  return request({
    url: '/nc/machineMaintenance/remove',
    method: 'delete',
    data: ids
  })
}

// 导出内衬机台维修计划
export function exportMachineMaintenance(query) {
  return request({
    url: '/nc/machineMaintenance/exportData/machineMaintenance',
    method: 'post',
    data: query,
    responseType: 'blob'
  })
}

// 导入内衬机台维修计划
export function importMachineMaintenance(data, updateSupport) {
  return request({
    url: '/nc/machineMaintenance/importData?updateSupport=' + updateSupport,
    method: 'post',
    data: data
  })
}
