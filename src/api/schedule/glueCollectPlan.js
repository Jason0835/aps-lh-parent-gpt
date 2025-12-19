import request from '@/utils/request'

// =
export function listGlueCollectPlan(query) {
  return request({
    url: '/schedule/glueCollectPlan/list',
    method: 'post',
    data: query
  })
}
export function editGlueCollectPlan(query) {
  return request({
    url: '/schedule/glueCollectPlan/save',
    method: 'post',
    data: query
  })
}

export function removeGlueCollectPlan(query) {
  return request({
    url: '/schedule/glueCollectPlan/remove',
    method: 'post',
    data: query
  })
}
export function summaryPlan(query) {
  return request({
    url: '/schedule/glueCollectPlan/summaryPlan',
    method: 'post',
    data: query
  })
}
export function chooseMachine(query) {
  return request({
    url: '/schedule/glueCollectPlan/chooseMachine',
    method: 'post',
    data: query
  })
}
export function checkPlanDateExist(query) {
  return request({
    url: '/schedule/glueCollectPlan/checkPlanDateExist',
    method: 'post',
    data: query
  })
}



