import request from '@/utils/request'

// =
export function listGlueDecomposePlan(query) {
  return request({
    url: '/schedule/glueDecomposePlan/list',
    method: 'post',
    data: query
  })
}
export function saveGlueDecomposePlan(query) {
  return request({
    url: '/schedule/glueDecomposePlan/save',
    method: 'post',
    data: query
  })
}

export function removeGlueDecomposePlan(query) {
  return request({
    url: '/schedule/glueDecomposePlan/remove',
    method: 'post',
    data: query
  })
}
export function checkPlanDateAndMixAreaExist(query) {
  return request({
    url: '/schedule/glueDecomposePlan/checkPlanDateAndMixAreaExist',
    method: 'post',
    data: query
  })
}
export function decompositionPlan(query) {
  return request({
    url: '/schedule/glueDecomposePlan/decompositionPlan',
    method: 'post',
    data: query
  })
}
export function listGlueSpanSend(query) {
  return request({
    url: '/schedule/glueDecomposePlan/listGlueSpanSend',
    method: 'post',
    data: query
  })
}
export function sendGlueSpan(query) {
  return request({
    url: '/schedule/glueDecomposePlan/sendGlueSpan',
    method: 'post',
    data: query
  })
}
export function listGlueSpanReceive(query) {
  return request({
    url: '/schedule/glueDecomposePlan/listGlueSpanReceive',
    method: 'post',
    data: query
  })
}
export function receiveGlueSpanReceive(query) {
  return request({
    url: '/schedule/glueDecomposePlan/receiveGlueSpanReceive',
    method: 'post',
    data: query
  })
}
export function deleteGlueSpanSend(query) {
  return request({
    url: '/schedule/glueDecomposePlan/deleteGlueSpanSend',
    method: 'post',
    data: query
  })
}


