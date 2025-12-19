import request, { downloadLink } from '@/utils/request'

export function listMdmCalendar(query) {
  return request({
    url: '/lean/productioncalendar/list',
    method: 'post',
    data: query
  })
}
export function editMdmCalendar(query) {
  return request({
    url: '/lean/productioncalendar/edit',
    method: 'post',
    data: query
  })
}
export function removeMdmCalendar(query) {
  return request({
    url: '/lean/productioncalendar/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/lean/productioncalendar/export', query)
}
