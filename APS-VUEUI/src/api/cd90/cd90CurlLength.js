import request, { downloadLink } from '@/utils/request'

export function listCd90CurlLength(query) {
  return request({
    url: '/cd90/cd90CurlLength/list',
    method: 'post',
    data: query
  })
}

export function getCd90CurlLength(id) {
  return request({
    url: `/cd90/cd90CurlLength/getInfo/${id}`,
    method: 'get'
  })
}

export function addCd90CurlLength(data) {
  return request({
    url: '/cd90/cd90CurlLength/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateCd90CurlLength(data) {
  return request({
    url: '/cd90/cd90CurlLength/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delCd90CurlLength(data) {
  return request({
    url: '/cd90/cd90CurlLength/remove',
    method: 'post',
    data
  })
}

export function exportCd90CurlLength(query) {
  return downloadLink('/cd90/cd90CurlLength/export', query)
}
