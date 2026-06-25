import request, { downloadLink } from '@/utils/request'

export function listCd90MachineInfo(query) {
  return request({
    url: '/cd90/cd90MachineInfo/list',
    method: 'post',
    data: query
  })
}

export function getCd90MachineInfo(id) {
  return request({
    url: `/cd90/cd90MachineInfo/getInfo/${id}`,
    method: 'get'
  })
}

export function addCd90MachineInfo(data) {
  return request({
    url: '/cd90/cd90MachineInfo/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateCd90MachineInfo(data) {
  return request({
    url: '/cd90/cd90MachineInfo/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delCd90MachineInfo(data) {
  return request({
    url: '/cd90/cd90MachineInfo/remove',
    method: 'post',
    data
  })
}

export function exportCd90MachineInfo(query) {
  return downloadLink('/cd90/cd90MachineInfo/export', query)
}

export function getCd90MachineEnableOptions(query) {
  return request({
    url: '/cd90/cd90MachineInfo/enableOptions',
    method: 'post',
    data: query
  })
}

export function changeCd90MachineStatus(data) {
  return request({
    url: '/cd90/cd90MachineInfo/changeStatus',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}
