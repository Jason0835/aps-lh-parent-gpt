import request from '@/utils/request'

// =
export function createOrderForecast(query) {
  return request({
    url: '/monthplan/productionPrediction/createMonthPrediction',
    method: 'post',
    data: query
  })
}
export function listOrderForecast(query) {
  return request({
    url: '/monthplan/productionPrediction/list',
    method: 'post',
    data: query
  })
}

export function getOrderForecastVersion(query) {
  return request({
    url: '/monthplan/productionPrediction/findPredictionVersion',
    method: 'post',
    data: query
  })
}

