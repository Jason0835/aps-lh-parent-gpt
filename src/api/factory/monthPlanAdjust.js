import request from '@/utils/request'

export function adjustFactoryMonthPlan(query) {
  return request({
    url: '/factory/monthPlanAdjust/adjustFactoryMonthPlan',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function getAdjustControlInfo(query) {
  return request({
    url: '/factory/monthPlanAdjust/getAdjustControlInfo',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}


