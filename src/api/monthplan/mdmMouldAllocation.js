import request from '@/utils/request'

// =
export function listMdmMouldAllocation(query) {
  return request({
    url: '/monthplan/mdmMouldAllocation/list',
    method: 'post',
    data: query
  })
}
export function saveMdmMouldAllocation(query) {
  return request({
    url: '/monthplan/mdmMouldAllocation/save',
    method: 'post',
    data: query
  })
}
export function removeMdmMouldAllocation(query) {
  return request({
    url: '/monthplan/mdmMouldAllocation/remove',
    method: 'post',
    data: query
  })
}

