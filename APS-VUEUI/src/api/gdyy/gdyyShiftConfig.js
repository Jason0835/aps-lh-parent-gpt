import request, { downloadLink } from '@/utils/request'

export function listGdyyShiftConfig(query) {
  return request({
    url: '/gdyy/gdyyShiftConfig/list',
    method: 'post',
    data: query
  })
}

export function getGdyyShiftConfig(id) {
  return request({
    url: `/gdyy/gdyyShiftConfig/getInfo/${id}`,
    method: 'get'
  })
}

export function addGdyyShiftConfig(data) {
  return request({
    url: '/gdyy/gdyyShiftConfig/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateGdyyShiftConfig(data) {
  return request({
    url: '/gdyy/gdyyShiftConfig/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delGdyyShiftConfig(data) {
  return request({
    url: '/gdyy/gdyyShiftConfig/remove',
    method: 'post',
    data
  })
}

export function exportGdyyShiftConfig(query) {
  return downloadLink('/gdyy/gdyyShiftConfig/export', query)
}

export function changeGdyyShiftConfigStatus(data) {
  return request({
    url: '/gdyy/gdyyShiftConfig/changeStatus',
    method: 'post',
    data
  })
}
