import request from '@/utils/request'

export function listMonthPlanAdjustNotice(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/list',
    method: 'post',
    data: query,
  })
}

export function saveMonthPlanAdjustNotice(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/save',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function submitMonthPlanAdjustNotice(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/submit',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function executeAdjust(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/executeAdjust',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function cancelMonthPlanAdjustNotice(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/cancel',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function confirmAdjust(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/confirmAdjust',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function getOperatePlanList(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/getOperatePlanList',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function getAdjustDetail(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/getAdjustDetail',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function calculateAddQty(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/calculateAddQty',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function removeMonthPlanAdjustNotice(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/remove',
    method: 'post',
    data: query,
  })
}


export function getAdjustNoticeAdjustPlan(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/getAdjustNoticeAdjustPlan',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}


export function getStockInfo(query) {
  return request({
    url: '/factory/monthPlanAdjustNotice/getStockInfo',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}


