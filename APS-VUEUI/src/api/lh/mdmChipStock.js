import request,{ downloadLink } from '@/utils/request'

export function listMdmChipStock(query) {
  return request({
    url: '/lh/lhChipStock/list',
    method: 'post',
    data: query
  })
}
export function removeMdmChipStock(ids) {
  return request({
    url: '/lh/lhChipStock/remove',
    method: 'post',
    params: { ids }
  })
}
export function editMdmChipStock(query) {
  return request({
    url: '/lh/lhChipStock/save',
    method: 'post',
    data: query
  })
}
export function getMachineList(query) {
  return request({
    url: '/lh/lhChipStock/getMachineList',
    method: 'post',
    params: query
  })
}

export function checkMdmChipStockUnique(query) {
  return request({
    url: '/lh/lhChipStock/checkUnique',
    method: 'post',
    data: query
  })
}

export function mergeMdmChipStock(query) {
  return request({
    url: '/lh/lhChipStock/mergeSave',
    method: 'post',
    data: query
  })
}
