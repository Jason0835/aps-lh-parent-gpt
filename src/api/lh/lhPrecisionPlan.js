import request from '@/utils/request'

export function listLhPrecisionPlan(query) {
  return request({
    url: '/schedule/lhPrecisionPlan/list',
    method: 'post',
    data: query
  })
}

export function getLhPrecisionPlan(id) {
  return request({
    url: '/schedule/lhPrecisionPlan/' + id,
    method: 'get'
  })
}

export function saveLhPrecisionPlan(data) {
  return request({
    url: '/schedule/lhPrecisionPlan/save',
    method: 'post',
    data: data
  })
}

export function removeLhPrecisionPlan(ids) {
  return request({
    url: '/schedule/lhPrecisionPlan/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function syncFromMes() {
  return request({
    url: '/schedule/lhPrecisionPlan/generateFromMes',
    method: 'post'
  })
}

export function autoGeneratePlans(year) {
  return request({
    url: '/schedule/lhPrecisionPlan/autoGenerateYearly',
    method: 'post',
    params: { year: year }
  })
}

export function checkWarning() {
  return request({
    url: '/schedule/lhPrecisionPlan/checkWarning',
    method: 'post'
  })
}

export function exportLhPrecisionPlan(query) {
  return request({
    url: '/schedule/lhPrecisionPlan/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}
