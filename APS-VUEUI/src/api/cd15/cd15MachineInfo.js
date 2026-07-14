import request, { downloadLink } from '@/utils/request'

export function listCd15MachineInfo(query) {
  return request({
    url: '/cd15/cd15MachineInfo/list',
    method: 'post',
    data: query
  })
}

export function getCd15MachineInfo(id) {
  return request({
    url: `/cd15/cd15MachineInfo/getInfo/${id}`,
    method: 'get'
  })
}

export function addCd15MachineInfo(data) {
  return request({
    url: '/cd15/cd15MachineInfo/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateCd15MachineInfo(data) {
  return request({
    url: '/cd15/cd15MachineInfo/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delCd15MachineInfo(data) {
  return request({
    url: '/cd15/cd15MachineInfo/remove',
    method: 'post',
    data
  })
}

export function exportCd15MachineInfo(query) {
  return downloadLink('/cd15/cd15MachineInfo/export', query)
}

export function getCd15MachineEnableOptions(query) {
  return request({
    url: '/cd15/cd15MachineInfo/enableOptions',
    method: 'post',
    data: query
  })
}

export function changeCd15MachineStatus(data) {
  return request({
    url: '/cd15/cd15MachineInfo/changeStatus',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}
