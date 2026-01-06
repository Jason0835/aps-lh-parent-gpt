import request from '@/utils/request'

// =
export function listConsole(query) {
  return request({
    url: '/factory/console/list',
    method: 'post',
    data: query
  })
}
export function versionListConsole(query) {
  return request({
    url: '/factory/console/noSelectedVersionList',
    method: 'post',
    data: query
  })
}
export function versionConfirm(query) {
  return request({
    url: '/factory/console/confirmProductionRequireVersion',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function editConsole(query) {
  return request({
    url: '/factory/console/save',
    method: 'post',
    data: query
  })
}

export function removeConsole(query) {
  return request({
    url: '/factory/console/remove',
    method: 'post',
    data: query
  })
}
export function createSaleRequirePlan(query) {
  return request({
    url: '/factory/console/createSaleRequirePlan',
    method: 'post',
    data: query
  })
}
/**
 * 定稿
 */
export function finalized(query) {
  return request({
    url: '/factory/console/finalized',
    method: 'post',
    data: query
  })
}
/**
 * 排模具
 */
export function factoryMouldingProduction(query) {
  return request({
    url: '/factory/console/factoryMouldingProduction',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
/**
 *  生成
 * @param {*} query
 * @returns
 */
export function factoryWholeCourseProduction(query) {
  return request({
    url: '/factory/console/oneClickProductionProcess',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

/**
 *  初始化化
 * @param {*} query
 * @returns
 */
export function initFactoryProduction(query) {
  return request({
    url: '/factory/console/initFactoryProduction',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function deleteMonthPlanProductionVersion(query) {
  return request({
    url: '/factory/console/deleteMonthPlanProductionVersion',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function deleteMonthPlanRequire(query) {
  return request({
    url: '/factory/console/deleteMonthPlanRequire',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}


