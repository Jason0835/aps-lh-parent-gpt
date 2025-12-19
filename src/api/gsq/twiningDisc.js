import request from '@/utils/request'

// =
export function listTwiningDisc(query) {
  return request({
    url: '/gsq/twiningDisc/list',
    method: 'post',
    data: query
  })
}
export function editTwiningDisc(query) {
  return request({
    url: '/gsq/twiningDisc/save',
    method: 'post',
    data: query
  })
}
export function removeTwiningDisc(query) {
  return request({
    url: '/gsq/twiningDisc/remove',
    method: 'post',
    data: query
  })
}
export function checkSerialNumberUnique(query) {
  return request({
    url: '/gsq/twiningDisc/checkSerialNumberUnique',
    method: 'post',
    data: query
  })
}


