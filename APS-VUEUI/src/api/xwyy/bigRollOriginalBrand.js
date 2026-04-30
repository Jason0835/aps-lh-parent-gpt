import request from '@/utils/request'

// =
export function listBigRollOriginalBrand(query) {
  return request({
    url: 'xwyy/bigRollOriginalBrand/list',
    method: 'post',
    data: query
  })
}
export function editBigRollOriginalBrand(query) {
  return request({
    url: 'xwyy/bigRollOriginalBrand/edit',
    method: 'post',
    data: query
  })
}
export function removeBigRollOriginalBrand(query) {
  return request({
    url: 'xwyy/bigRollOriginalBrand/remove',
    method: 'post',
    data: query
  })
}


