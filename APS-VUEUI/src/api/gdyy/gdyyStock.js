import request, { downloadLink } from '@/utils/request'

export function listGdyyStock(query) {
  return request({
    url: '/gdyy/stock/list',
    method: 'post',
    data: query
  })
}

export function getGdyyStock(id) {
  return request({
    url: `/gdyy/stock/getInfo/${id}`,
    method: 'get'
  })
}

export function addGdyyStock(data) {
  return request({
    url: '/gdyy/stock/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateGdyyStock(data) {
  return request({
    url: '/gdyy/stock/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delGdyyStock(data) {
  return request({
    url: '/gdyy/stock/remove',
    method: 'post',
    data
  })
}

export function exportGdyyStock(query) {
  return downloadLink('/gdyy/stock/export', query)
}
