import request from '@/utils/request'

export function listLhChipStock(query) {
  return request({
    url: '/lh/lhChipStock/list',
    method: 'post',
    data: query
  })
}

export function removeLhChipStock(ids) {
  return request({
    url: '/lh/lhChipStock/remove',
    method: 'post',
    params: { ids }
  })
}

export function editLhChipStock(query) {
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

export function checkLhChipStockUnique(query) {
  return request({
    url: '/lh/lhChipStock/checkUnique',
    method: 'post',
    data: query
  })
}

export function mergeLhChipStock(query) {
  return request({
    url: '/lh/lhChipStock/mergeSave',
    method: 'post',
    data: query
  })
}
