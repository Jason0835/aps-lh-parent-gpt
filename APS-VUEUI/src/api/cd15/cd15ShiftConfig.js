import request, { downloadLink } from '@/utils/request'

export function listCd15ShiftConfig(query) {
  return request({
    url: '/cd15/cd15ShiftConfig/list',
    method: 'post',
    data: query
  })
}

export function getCd15ShiftConfig(id) {
  return request({
    url: `/cd15/cd15ShiftConfig/getInfo/${id}`,
    method: 'get'
  })
}

export function addCd15ShiftConfig(data) {
  return request({
    url: '/cd15/cd15ShiftConfig/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateCd15ShiftConfig(data) {
  return request({
    url: '/cd15/cd15ShiftConfig/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delCd15ShiftConfig(data) {
  return request({
    url: '/cd15/cd15ShiftConfig/remove',
    method: 'post',
    data
  })
}

export function exportCd15ShiftConfig(query) {
  return downloadLink('/cd15/cd15ShiftConfig/export', query)
}

export function changeCd15ShiftConfigStatus(data) {
  return request({
    url: '/cd15/cd15ShiftConfig/changeStatus',
    method: 'post',
    data
  })
}