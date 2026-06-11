import request from '@/utils/request'

// 查询垫胶机台维修计划列表
export function listMachineMaintenance(query) {
  return request({
    url: '/dj/machineMaintenance/list',
    method: 'post',
    data: query
  })
}

// 查询垫胶机台维修计划详细
export function getMachineMaintenance(billId) {
  return request({
    url: '/dj/machineMaintenance/' + billId,
    method: 'get'
  })
}

// 新增垫胶机台维修计划
export function addMachineMaintenance(data) {
  return request({
    url: '/dj/machineMaintenance/save',
    method: 'post',
    data: data
  })
}

// 修改垫胶机台维修计划
export function updateMachineMaintenance(data) {
  return request({
    url: '/dj/machineMaintenance/save',
    method: 'post',
    data: data
  })
}

// 删除垫胶机台维修计划
export function delMachineMaintenance(ids) {
  return request({
    url: '/dj/machineMaintenance/remove',
    method: 'delete',
    data: ids
  })
}

// 导出垫胶机台维修计划
export function exportMachineMaintenance(query) {
  return request({
    url: '/dj/machineMaintenance/exportData/machineMaintenance',
    method: 'post',
    data: query,
    responseType: 'blob'
  })
}

// 导入垫胶机台维修计划
export function importMachineMaintenance(data, updateSupport) {
  return request({
    url: '/dj/machineMaintenance/importData?updateSupport=' + updateSupport,
    method: 'post',
    data: data
  })
}