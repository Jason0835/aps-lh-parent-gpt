import request from '@/utils/request'

export function listMouldCleanPlan(query) {
  return request({
    url: '/lh/mouldCleanPlan/list',
    method: 'post',
    data: query
  })
}

export function removeMouldCleanPlan(query) {
  return request({
    url: '/lh/mouldCleanPlan/remove',
    method: 'post',
    data: query
  })
}

export function editMouldCleanPlan(query) {
  return request({
    url: '/lh/mouldCleanPlan/save',
    method: 'post',
    data: query
  })
}

export function getMachineList(query) {
  return request({
    url: '/lh/mouldCleanPlan/getMachineList',
    method: 'post',
    params: query
  })
}
