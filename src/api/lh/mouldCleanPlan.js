import request from '@/utils/request'

export function listMouldCleanPlan(query) {
  return request({
    url: '/lh/mouldCleanPlan/list',
    method: 'post',
    data: query
  })
}

export function removeMouldCleanPlan(ids) {
  return request({
    url: '/lh/mouldCleanPlan/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function editMouldCleanPlan(query) {
  return request({
    url: '/lh/mouldCleanPlan/save',
    method: 'post',
    data: query,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
}

export function getMachineList(query) {
  return request({
    url: '/lh/mouldCleanPlan/getMachineList',
    method: 'post',
    data: query
  })
}

export function syncFromWarn() {
  return request({
    url: '/lh/mouldCleanPlan/syncFromWarn',
    method: 'post'
  })
}
