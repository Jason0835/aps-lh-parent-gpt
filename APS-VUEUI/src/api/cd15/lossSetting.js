import request, { downloadLink } from '@/utils/request'

export function listLossSetting(query) {
  return request({
    url: '/cd15/cd15LossSetting/list',
    method: 'post',
    data: query
  })
}

export function getLossSetting(id) {
  return request({
    url: `/cd15/cd15LossSetting/getInfo/${id}`,
    method: 'get'
  })
}

export function addLossSetting(data) {
  return request({
    url: '/cd15/cd15LossSetting/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateLossSetting(data) {
  return request({
    url: '/cd15/cd15LossSetting/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delLossSetting(data) {
  return request({
    url: '/cd15/cd15LossSetting/remove',
    method: 'post',
    data
  })
}

export function exportLossSetting(query) {
  return downloadLink('/cd15/cd15LossSetting/export', query)
}
