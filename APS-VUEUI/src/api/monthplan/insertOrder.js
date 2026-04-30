import request from '@/utils/request'
export function listSimulateResult(query) {
  return request({
    url: '/monthplan/simulatedResult/list',
    method: 'post',
    data: query
  })
}

export function createVmMonthPrediction(query) {
  return request({
    url: '/monthplan/simulatedResult/createVmMonthPrediction',
    method: 'post',
    data: query
  })
}