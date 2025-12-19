import request from '@/utils/request'

// =
export function listMdmWorkWearInfo(query) {
  return request({
    url: '/monthplan/mdmWorkWearInfo/list',
    method: 'post',
    data: query
  })
}
export function saveMdmWorkWearInfo(query) {
  return request({
    url: '/monthplan/mdmWorkWearInfo/save',
    method: 'post',
    data: query
  })
}
export function removeMdmWorkWearInfo(query) {
  return request({
    url: '/monthplan/mdmWorkWearInfo/remove',
    method: 'post',
    data: query
  })
}
