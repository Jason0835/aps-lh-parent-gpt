import request, { downloadLink } from '@/utils/request'

export function listMustFinishPlan(query) {
  return request({
    url: '/maindata/mustFinishPlan/list',
    method: 'post',
    data: query
  })
}
export function editMustFinishPlan(query) {
  return request({
    url: '/maindata/mustFinishPlan/edit',
    method: 'post',
    data: query
  })
}
export function removeMustFinishPlan(query) {
  return request({
    url: '/maindata/mustFinishPlan/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/maindata/mustFinishPlan/export', query)
}
