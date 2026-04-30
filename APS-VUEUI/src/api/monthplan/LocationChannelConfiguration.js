import request, { downloadLink } from '@/utils/request'

// =
export function listLocationChannelConfiguration(query) {
  return request({
    url: '/monthplan/LocationChannelConfiguration/list',
    method: 'post',
    data: query
  })
}
export function editLocationChannelConfiguration(query) {
  return request({
    url: '/monthplan/LocationChannelConfiguration/save',
    method: 'post',
    data: query
  })
}
export function removeLocationChannelConfiguration(query) {
  return request({
    url: '/monthplan/LocationChannelConfiguration/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/LocationChannelConfiguration/export', query)
}
