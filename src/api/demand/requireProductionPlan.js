import request from '@/utils/request'

export function listRequireProductionPlan(query) {
  return request({
    url: 'demand/requireProductionPlan/list',
    method: 'post',
    data: query
  })
}
export function removeRequireProductionPlan(query) {
  return request({
    url: 'demand/requireProductionPlan/remove',
    method: 'post',
    data: query
  })
}
export function saveRequireProductionPlan(query) {
  return request({
    url: 'demand/requireProductionPlan/save',
    method: 'post',
    data: query
  })
}
export function getVersionList(query) {
  return request({
    url: 'factory/console/versionList',
    method: 'post',
    data: query
  })
}
