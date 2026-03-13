import request from '@/utils/request'

// =
export function listMdmCapsuleChuck(query) {
  return request({
    url: '/monthplan/mdmCapsuleChuck/list',
    method: 'post',
    data: query
  })
}
export function saveMdmCapsuleChuck(query) {
  return request({
    url: '/monthplan/mdmCapsuleChuck/save',
    method: 'post',
    data: query
  })
}
export function removeMdmCapsuleChuck(query) {
  return request({
    url: '/monthplan/mdmCapsuleChuck/remove',
    method: 'post',
    data: query
  })
}

export function getTotal(query) {
  return request({
    url: '/monthplan/mdmCapsuleChuck/getSum',
    method: 'post',
    data: query
  })
}