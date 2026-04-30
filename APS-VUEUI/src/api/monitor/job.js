import request from '@/utils/request'

// 查询定时任务调度列表
export function listJob(data) {
  return request({
    url: '/monitor/job/list',
    method: 'post',
    data: data
  })
}

// 查询定时任务调度详细
export function getJob(jobId) {
  return request({
    url: '/monitor/job/details/' + jobId,
    method: 'get'
  })
}

// 新增定时任务调度
export function addJob(data) {
  return request({
    url: '/monitor/job/add',
    method: 'post',
    data: data
  })
}

// 修改定时任务调度
export function updateJob(data) {
  return request({
    url: '/monitor/job/edit',
    method: 'post',
    data: data
  })
}

// 删除定时任务调度
export function delJob(jobId) {
  return request({
    url: '/monitor/job/remove',
    method: 'post',
    data: {
      ids: jobId.join(',')
    }
  })
}

// 任务状态修改
export function changeJobStatus(jobId, status) {
  const data = {
    jobId,
    status
  }
  return request({
    url: '/monitor/job/changeStatus',
    method: 'post',
    data: data
  })
}


// 定时任务立即执行一次
export function runJob(jobId, jobGroup) {
  const data = {
    jobId,
    jobGroup
  }
  return request({
    url: '/monitor/job/run',
    method: 'post',
    data: data
  })
}

// 校验cron表达式是否符合格式
export function validatorCronExpressionIsValid(value) {
  return request({
    url: '/monitor/job/vue/checkCronExpressionIsValid',
    method: 'post',
    data: {
      cronExpression: value
    }
  })
}
