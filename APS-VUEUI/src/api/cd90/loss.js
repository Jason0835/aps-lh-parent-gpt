import request, { downloadLink } from '@/utils/request'

export function listLossSetting(query) {
  return request({
    url: '/cd90/cd90LossSetting/list',
    method: 'post',
    data: query
  })
}

export function getLossSetting(id) {
  return request({
    url: `/cd90/cd90LossSetting/getInfo/${id}`,
    method: 'get'
  })
}

export function addLossSetting(data) {
  return request({
    url: '/cd90/cd90LossSetting/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateLossSetting(data) {
  return request({
    url: '/cd90/cd90LossSetting/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delLossSetting(data) {
  return request({
    url: '/cd90/cd90LossSetting/remove',
    method: 'post',
    data
  })
}

export function exportLossSetting(query) {
  return downloadLink('/cd90/cd90LossSetting/export', query)
}
