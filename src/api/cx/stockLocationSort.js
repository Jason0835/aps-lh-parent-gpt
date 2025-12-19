import request from '@/utils/request'

//
export function listStockLocationSort(query) {
  return request({
    url: '/cx/stockLocationSort/list',
    method: 'post',
    data: query
  })
}
export function editStockLocationSort(query) {
  return request({
    url: '/cx/stockLocationSort/edit',
    method: 'post',
    data: query
  })
}
export function removeStockLocationSort(query) {
  return request({
    url: '/cx/stockLocationSort/remove',
    method: 'post',
    data: query
  })
}

// export function getProductEmbryoVersions(query) {
//   return request({
//     url: '/cx/stockLocationSort/getProductEmbryoVersions',
//     method: 'post',
//     data: query
//   })
// }

