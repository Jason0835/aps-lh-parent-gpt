import request from '@/utils/request'

// 查询胎圈排程预警记录列表
export function listWarningRecord(query) {
  return request({
    url: '/tqWarningRecord/list',
    method: 'post',
    data: query
  })
}

// 获取胎圈排程预警记录详情
export function getWarningRecord(id) {
  return request({
    url: '/tqWarningRecord/' + id,
    method: 'get'
  })
}

// 处理胎圈排程预警记录
export function handleWarningRecord(data) {
  return request({
    url: '/tqWarningRecord/handleWarning',
    method: 'post',
    params: data
  })
}
