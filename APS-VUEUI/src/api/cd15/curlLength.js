import request, { downloadLink } from '@/utils/request'

export function listCd15CurlLength(query) {
  return request({
    url: '/cd15/curlLength/list',
    method: 'post',
    data: query
  })
}

export function getCd15CurlLength(id) {
  return request({
    url: `/cd15/curlLength/getInfo/${id}`,
    method: 'get'
  })
}

export function addCd15CurlLength(data) {
  return request({
    url: '/cd15/curlLength/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateCd15CurlLength(data) {
  return request({
    url: '/cd15/curlLength/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delCd15CurlLength(data) {
  return request({
    url: '/cd15/curlLength/remove',
    method: 'post',
    data
  })
}

export function exportCd15CurlLength(query) {
  return downloadLink('/cd15/curlLength/export', query)
}