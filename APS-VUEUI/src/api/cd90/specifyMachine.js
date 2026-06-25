import request, { downloadLink } from '@/utils/request'

export function listSpecifyMachine(query) {
  return request({
    url: '/cd90/specifyMachine/list',
    method: 'post',
    data: query
  })
}

export function listTireFabricCodes() {
  return request({
    url: '/cd90/common/tireFabricCodes',
    method: 'post'
  })
}

export function listCordSpecs() {
  return request({
    url: '/cd90/common/cordSpecs',
    method: 'post'
  })
}

export function getSpecifyMachine(id) {
  return request({
    url: `/cd90/specifyMachine/getInfo/${id}`,
    method: 'get'
  })
}

export function addSpecifyMachine(data) {
  return request({
    url: '/cd90/specifyMachine/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function editSpecifyMachine(data) {
  return request({
    url: '/cd90/specifyMachine/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function checkUniqueSpecifyMachine(data) {
  return request({
    url: '/cd90/specifyMachine/checkUnique',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function removeSpecifyMachine(data) {
  return request({
    url: '/cd90/specifyMachine/remove',
    method: 'post',
    data
  })
}

export function removeAllSpecifyMachine(data) {
  return request({
    url: '/cd90/specifyMachine/removeAll',
    method: 'post',
    data
  })
}

export function exportSpecifyMachine(query) {
  return downloadLink('/cd90/specifyMachine/export', query)
}

export function importSpecifyMachine(data) {
  return request({
    url: '/cd90/specifyMachine/importData',
    method: 'post',
    data
  })
}
