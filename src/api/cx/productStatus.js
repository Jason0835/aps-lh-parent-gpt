import request from '@/utils/request'

export function listProductStatus(query) {
  return request({
    url: '/cx/productStatus/list',
    method: 'post',
    data: query
  })
}
export function editProductStatus(query) {
  return request({
    url: '/cx/productStatus/edit',
    method: 'post',
    data: query
  })
}
export function modifyQtyProductStatus(query) {
  return request({
    url: '/cx/productStatus/modifyQty',
    method: 'post',
    data: query
  })
}
export function getInfoModifyQty(query) {
  return request({
    url: '/cx/productStatus/modifyQty/getInfo',
    method: 'post',
    data: query
  })
}
export function validateProduction(query) {
  return request({
    url: '/cx/productStatus/validateProduction',
    method: 'post',
    data: query
  })
}
export function markUnProduct(query) {
  return request({
    url: '/cx/productStatus/markUnProduct',
    method: 'post',
    data: query
  })
}

