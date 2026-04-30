import request from '@/utils/request'

export function listMesLhScheduleResult(query) {
  return request({
    url: '/cxlh/mesLhScheduleResult/list',
    method: 'post',
    data: query
  })
}