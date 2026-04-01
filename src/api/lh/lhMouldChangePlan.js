import request,{ downloadLink } from '@/utils/request'

export function listLhMouldChangePlan(query) {
  return request({
    url: '/lh/lhMouldChangePlan/list',
    method: 'post',
    data: query
  })
}
export function removeLhMouldChangePlan(ids) {
  return request({
    url: '/lh/lhMouldChangePlan/remove',
    method: 'post',
    params: { ids }
  })
}
export function editLhMouldChangePlan(query) {
  return request({
    url: '/lh/lhMouldChangePlan/save',
    method: 'post',
    data: query
  })
}
export function getMachineList(query) {
  return request({
    url: '/lh/lhMouldChangePlan/getMachineList',
    method: 'post',
    params: query
  })
}

export function getMaterialList(query) {
  return request({
    url: '/lh/lhMouldChangePlan/getMaterialList',
    method: 'post',
    params: query
  })
}
