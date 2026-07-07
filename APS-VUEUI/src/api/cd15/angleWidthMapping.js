import request, { downloadLink } from '@/utils/request'

export function listAngleWidthMapping(query) {
  return request({
    url: '/cd15/angleWidthMapping/list',
    method: 'post',
    data: query
  })
}

export function getAngleWidthMapping(id) {
  return request({
    url: `/cd15/angleWidthMapping/getInfo/${id}`,
    method: 'get'
  })
}

export function addAngleWidthMapping(data) {
  return request({
    url: '/cd15/angleWidthMapping/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateAngleWidthMapping(data) {
  return request({
    url: '/cd15/angleWidthMapping/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delAngleWidthMapping(data) {
  return request({
    url: '/cd15/angleWidthMapping/remove',
    method: 'post',
    data
  })
}

export function exportAngleWidthMapping(query) {
  return downloadLink('/cd15/angleWidthMapping/export', query)
}
