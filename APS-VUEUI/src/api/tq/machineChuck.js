import request, { downloadLink } from '@/utils/request'

export function listMachineChuck(query) {
  return request({
    url: '/tq/machineChuck/list',
    method: 'post',
    data: query
  })
}

export function saveMachineChuck(query) {
  return request({
    url: '/tq/machineChuck/save',
    method: 'post',
    data: query
  })
}

// 校验机台编码+寸口编码组合唯一性（返回 "1"-不唯一，"0"-唯一）
export function checkMachineChuckUnique(query) {
  return request({
    url: '/tq/machineChuck/checkUnique',
    method: 'post',
    data: query
  })
}

export function removeMachineChuck(ids) {
  return request({
    url: '/tq/machineChuck/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function removeAllMachineChuck() {
  return request({
    url: '/tq/machineChuck/removeAll',
    method: 'post'
  })
}

export function exportMachineChuck(query) {
  return downloadLink("/tq/machineChuck/export", query)
}
