import request, { downloadLink } from '@/utils/request'

export function listStorageLaneLimit(query) {
  return request({
    url: '/cd15/cd15StorageLaneLimit/list',
    method: 'post',
    data: query
  })
}

export function getStorageLaneLimit(id) {
  return request({
    url: `/cd15/cd15StorageLaneLimit/getInfo/${id}`,
    method: 'get'
  })
}

export function addStorageLaneLimit(data) {
  return request({
    url: '/cd15/cd15StorageLaneLimit/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateStorageLaneLimit(data) {
  return request({
    url: '/cd15/cd15StorageLaneLimit/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delStorageLaneLimit(data) {
  return request({
    url: '/cd15/cd15StorageLaneLimit/remove',
    method: 'post',
    data
  })
}

export function exportStorageLaneLimit(query) {
  return downloadLink('/cd15/cd15StorageLaneLimit/export', query)
}

export function listSteelStripCodes() {
  return request({
    url: '/cd15/common/steelStripCodes',
    method: 'post'
  })
}