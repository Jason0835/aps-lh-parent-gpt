import request from '@/utils/request'

//
export function listSapSpecMoldUse(query) {
  return request({
    url: '/cx/sapSpecMoldUse/list',
    method: 'post',
    data: query
  })
}
export function editSapSpecMoldUse(query) {
  return request({
    url: '/cx/sapSpecMoldUse/edit',
    method: 'post',
    data: query
  })
}
export function removeSapSpecMoldUse(query) {
  return request({
    url: '/cx/sapSpecMoldUse/remove',
    method: 'post',
    data: query
  })
}
export function getSpecDesc(query) {
  return request({
    url: '/cx/sapSpecMoldUse/getSpecDesc',
    method: 'post',
    data: query
  })
}


