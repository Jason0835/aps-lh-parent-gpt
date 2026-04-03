import request from '@/utils/request'

// 查询成型精度计划列表
export function listCxPrecisionPlan(query) {
  return request({
    url: '/cx/cxPrecisionPlan/list',
    method: 'post',
    data: query
  })
}

// 新增/修改成型精度计划
export function saveCxPrecisionPlan(data) {
  return request({
    url: '/cx/cxPrecisionPlan/save',
    method: 'post',
    data: data
  })
}

// 删除成型精度计划
export function removeCxPrecisionPlan(ids) {
  return request({
    url: '/cx/cxPrecisionPlan/remove',
    method: 'post',
    data: ids
  })
}

// 获取成型精度计划详情
export function getCxPrecisionPlan(id) {
  return request({
    url: `/cx/cxPrecisionPlan/${id}`,
    method: 'get'
  })
}

// 校验唯一性
export function checkCxPrecisionPlanUnique(data) {
  return request({
    url: '/cx/cxPrecisionPlan/checkUnique',
    method: 'post',
    data: data
  })
}

// 获取成型机列表
export function getMachineList(data) {
  return request({
    url: '/cx/cxPrecisionPlan/getMachineList',
    method: 'post',
    data: data
  })
}

// 导出来型精度计划
export function exportCxPrecisionPlan(query, fileName) {
  return request({
    url: `/cx/cxPrecisionPlan/export`,
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}
