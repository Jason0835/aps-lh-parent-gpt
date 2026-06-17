import request, { downloadLink } from '@/utils/request'

export function listSpecifyMachine(query) {
  return request({
    url: '/cd15/specifyMachine/list',
    method: 'post',
    data: query
  })
}

export function getSpecifyMachine(id) {
  return request({
    url: `/cd15/specifyMachine/getInfo/${id}`,
    method: 'get'
  })
}

export function addSpecifyMachine(data) {
  return request({
    url: '/cd15/specifyMachine/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function editSpecifyMachine(data) {
  return request({
    url: '/cd15/specifyMachine/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function checkUniqueSpecifyMachine(data) {
  return request({
    url: '/cd15/specifyMachine/checkUnique',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function removeSpecifyMachine(data) {
  return request({
    url: '/cd15/specifyMachine/remove',
    method: 'post',
    data
  })
}

export function removeAllSpecifyMachine(data) {
  return request({
    url: '/cd15/specifyMachine/removeAll',
    method: 'post',
    data
  })
}

export function exportSpecifyMachine(query) {
  return downloadLink('/cd15/specifyMachine/export', query)
}

export function importSpecifyMachine(data) {
  return request({
    url: '/cd15/specifyMachine/importData',
    method: 'post',
    data
  })
}