import request from '@/utils/request'

// 查询基础数据_地区列表
export function listCxScheduleResult(query) {
  return request({
    url: 'cx/cxScheduleStopInfo/list',
    method: 'post',
    data: query
  })
}
