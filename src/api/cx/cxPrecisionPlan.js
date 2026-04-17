import request from '@/utils/request'

export function listCxPrecisionPlan(query) {
  return request({
    url: '/cx/cxPrecisionPlan/list',
    method: 'post',
    data: query
  })
}

export function saveCxPrecisionPlan(data) {
  return request({
    url: '/cx/cxPrecisionPlan/save',
    method: 'post',
    data
  })
}

export function removeCxPrecisionPlan(ids) {
  return request({
    url: '/cx/cxPrecisionPlan/remove',
    method: 'post',
    params: { ids }
  })
}

export function getCxPrecisionPlan(id) {
  return request({
    url: `/cx/cxPrecisionPlan/${id}`,
    method: 'get'
  })
}

export function checkCxPrecisionPlanUnique(data) {
  return request({
    url: '/cx/cxPrecisionPlan/checkUnique',
    method: 'post',
    data
  })
}

export function getMachineList(data) {
  return request({
    url: '/cx/cxPrecisionPlan/getMachineList',
    method: 'post',
    data
  })
}

export function listCxMachineInfo(data) {
  return request({
    url: '/cx/cxPrecisionPlan/getMachineList',
    method: 'post',
    data
  })
}

export function exportCxPrecisionPlan(query) {
  return request({
    url: '/cx/cxPrecisionPlan/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

export function syncFromMes(year) {
  return request({
    url: '/cx/cxPrecisionPlan/generateFromMes',
    method: 'post',
    params: { year }
  })
}

export function autoGeneratePlans(year) {
  return request({
    url: '/cx/cxPrecisionPlan/autoGenerateYearly',
    method: 'post',
    params: { year }
  })
}

export function checkWarning() {
  return request({
    url: '/cx/cxPrecisionPlan/checkWarning',
    method: 'post'
  })
}
