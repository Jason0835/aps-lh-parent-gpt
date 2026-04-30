import request from '@/utils/request'

export function listLhMachineOnlineInfo(query) {
  return request({
    url: '/lh/lhMachineOnlineInfo/list',
    method: 'post',
    data: query
  })
}

export function getLhMachineOnlineInfo(id) {
  return request({
    url: '/lh/lhMachineOnlineInfo/' + id,
    method: 'get'
  })
}

export function exportLhMachineOnlineInfo(query) {
  return request({
    url: '/lh/lhMachineOnlineInfo/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

