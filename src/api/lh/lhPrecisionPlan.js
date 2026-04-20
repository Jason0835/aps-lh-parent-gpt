import request from '@/utils/request'

export function listLhPrecisionPlan(query) {
  return request({
    url: '/lh/lhPrecisionPlan/list',
    method: 'post',
    data: query
  })
}

export function getLhPrecisionPlan(id) {
  return request({
    url: '/lh/lhPrecisionPlan/' + id,
    method: 'get'
  })
}

export function saveLhPrecisionPlan(data) {
  return request({
    url: '/lh/lhPrecisionPlan/save',
    method: 'post',
    data: data
  })
}

export function removeLhPrecisionPlan(ids) {
  return request({
    url: '/lh/lhPrecisionPlan/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function syncFromMes(year) {
  return request({
    url: '/lh/lhPrecisionPlan/generateFromMes',
    method: 'post',
    params: { year: year }
  })
}

export function autoGeneratePlans(year) {
  return request({
    url: '/lh/lhPrecisionPlan/autoGenerateYearly',
    method: 'post',
    params: { year: year }
  })
}

export function checkWarning() {
  return request({
    url: '/lh/lhPrecisionPlan/checkWarning',
    method: 'post'
  })
}

export function exportLhPrecisionPlan(query) {
  return request({
    url: '/lh/lhPrecisionPlan/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

export function checkLhPrecisionPlanUnique(data) {
  return request({
    url: '/lh/lhPrecisionPlan/checkUnique',
    method: 'post',
    data
  })
}

export function listLhMachineInfo(data) {
  return request({
    url: '/lh/lhPrecisionPlan/getMachineList',
    method: 'post',
    data
  })
}
