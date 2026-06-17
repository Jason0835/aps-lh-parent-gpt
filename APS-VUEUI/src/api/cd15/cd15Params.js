import request, { downloadLink } from '@/utils/request'

export function listCd15Params(query) {
  return request({
    url: '/cd15/cd15Params/list',
    method: 'post',
    data: query
  })
}

export function getCd15Params(id) {
  return request({
    url: `/cd15/cd15Params/getInfo/${id}`,
    method: 'get'
  })
}

export function addCd15Params(data) {
  return request({
    url: '/cd15/cd15Params/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateCd15Params(data) {
  return request({
    url: '/cd15/cd15Params/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delCd15Params(data) {
  return request({
    url: '/cd15/cd15Params/remove',
    method: 'post',
    data
  })
}

export function exportCd15Params(query) {
  return downloadLink('/cd15/cd15Params/export', query)
}