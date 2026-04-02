import request,{ downloadLink } from '@/utils/request'

export function listMdmChipStock(query) {
  return request({
    url: '/lh/mdmChipStock/list',
    method: 'post',
    data: query
  })
}
export function removeMdmChipStock(ids) {
  return request({
    url: '/lh/mdmChipStock/remove',
    method: 'post',
    params: { ids }
  })
}
export function editMdmChipStock(query) {
  return request({
    url: '/lh/mdmChipStock/save',
    method: 'post',
    data: query
  })
}
export function getMachineList(query) {
  return request({
    url: '/lh/mdmChipStock/getMachineList',
    method: 'post',
    params: query
  })
}

export function checkMdmChipStockUnique(query) {
  return request({
    url: '/lh/mdmChipStock/checkUnique',
    method: 'post',
    data: query
  })
}

export function mergeMdmChipStock(query) {
  return request({
    url: '/lh/mdmChipStock/mergeSave',
    method: 'post',
    data: query
  })
}
