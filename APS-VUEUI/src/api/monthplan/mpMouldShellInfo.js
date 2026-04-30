import request, { downloadLink } from '@/utils/request'
export function listMpMouldShellInfo(query) {
  return request({
    url: '/monthplan/mpMouldShellInfo/list',
    method: 'post',
    data: query
  })
}
export function editMpMouldShellInfo(query) {
  return request({
    url: '/monthplan/mpMouldShellInfo/save',
    method: 'post',
    data: query
  })
}
export function removeMpMouldShellInfo(query) {
  return request({
    url: '/monthplan/mpMouldShellInfo/remove',
    method: 'post',
    data: query
  })
}