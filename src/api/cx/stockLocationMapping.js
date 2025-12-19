import request from '@/utils/request'

//
export function listStockLocationMapping(query) {
  return request({
    url: '/cx/stockLocationMapping/list',
    method: 'post',
    data: query
  })
}
export function editStockLocationMapping(query) {
  return request({
    url: '/cx/stockLocationMapping/edit',
    method: 'post',
    data: query
  })
}
export function removeStockLocationMapping(query) {
  return request({
    url: '/cx/stockLocationMapping/remove',
    method: 'post',
    data: query
  })
}

// export function getProductEmbryoVersions(query) {
//   return request({
//     url: '/cx/stockLocationMapping/getProductEmbryoVersions',
//     method: 'post',
//     data: query
//   })
// }

