import request from '@/utils/request'

// 查询调度日志列表
export function listJobLog(data) {
  return request({
    url: '/monitor/jobLog/list',
    method: 'post',
    data: data
  })
}

// 删除调度日志
export function delJobLog(jobLogId) {
  return request({
    url: '/monitor/jobLog/remove',
    method: 'post',
    data: {
      ids: jobLogId.join(',')
    }
  })
}

// 清空调度日志
export function cleanJobLog() {
  return request({
    url: '/monitor/jobLog/clean',
    method: 'post'
  })
}
