import request from '@/utils/request'

// =
export function listMdmMoldingMachine(query) {
  return request({
    url: '/monthplan/mdmMoldingMachine/list',
    method: 'post',
    data: query
  })
}
export function saveMdmMoldingMachine(query) {
  return request({
    url: '/monthplan/mdmMoldingMachine/save',
    method: 'post',
    data: query
  })
}
export function removeMdmMoldingMachine(query) {
  return request({
    url: '/monthplan/mdmMoldingMachine/remove',
    method: 'post',
    data: query
  })
}

