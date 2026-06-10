import request, { downloadLink } from '@/utils/request'

export function listCd90ShiftConfig(query) {
  return request({
    url: '/cd90/cd90ShiftConfig/list',
    method: 'post',
    data: query
  })
}

export function getCd90ShiftConfig(id) {
  return request({
    url: `/cd90/cd90ShiftConfig/getInfo/${id}`,
    method: 'get'
  })
}

export function addCd90ShiftConfig(data) {
  return request({
    url: '/cd90/cd90ShiftConfig/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateCd90ShiftConfig(data) {
  return request({
    url: '/cd90/cd90ShiftConfig/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delCd90ShiftConfig(data) {
  return request({
    url: '/cd90/cd90ShiftConfig/remove',
    method: 'post',
    data
  })
}

export function exportCd90ShiftConfig(query) {
  return downloadLink('/cd90/cd90ShiftConfig/export', query)
}

export function changeCd90ShiftConfigStatus(data) {
  return request({
    url: '/cd90/cd90ShiftConfig/changeStatus',
    method: 'post',
    data
  })
}