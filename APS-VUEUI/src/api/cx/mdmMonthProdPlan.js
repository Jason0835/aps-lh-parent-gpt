import request,{ downloadLink } from '@/utils/request'

/**
 * 月度生产计划列表
 * @param {*} query
 * @returns
 */
export function listMonthProdPlan(query) {
  return request({
    url: 'cx/mdmMonthProdPlan/list',
    method: 'post',
    data: query
  })
}

/**
 * 下发主计划
 * @param {*} query
 * @returns
 */
export function issuePlan(query) {
  return request({
    url: 'cx/mdmMonthProdPlan/issuePlan',
    method: 'post',
    data: query
  })
}

/**
 * 导出主计划月度生产计划
 * @param {*} params
 * @returns
 */
export function exportMonthProdPlan(params) {
  return downloadLink("/cx/mdmMonthProdPlan/export", params);
}

/**
 * 预计超欠产导出
 * @param {*} params
 * @returns
 */
export function exportExpectedExcessArrears(params) {
  return downloadLink("/cx/mdmMonthProdPlan/expectedExport", params);
}

/**
 * 超欠产导出
 * @param {*} params
 * @returns
 */
export function exportExcessArrears(params) {
  return downloadLink("/cx/mdmMonthProdPlan/overProdExport", params);
}

/**
 * 修改施工版本
 * @param {object} query
 * @param {number} query.id ID
 * @param {string} query.monthPlanApsVersion 生产排程记录主计划版本号
 * @param {string} query.embryoCode 成型胎胚代码
 * @param {string} query.bomDataVersion 施工版本
 * @returns
 */
export function changeBomDataVersion(query) {
  return request({
    url: '/cx/mdmMonthProdPlan/changeBomDataVersion',
    method: 'post',
    data: query
  })
}

/**
 * 更新预计超欠产
 * @param {object} query
 * @param {number} query.id ID
 * @param {string} query.expectedExcessArrears 预计超欠产
 * @returns
 */
export function updateExpectedExcessArrears(query) {
  return request({
    url: '/cx/mdmMonthProdPlan/updateExpectedExcessArrears',
    method: 'post',
    data: query
  })
}

/**
 * 获取月计划甘特图数据
 * @param {*} query
 * @returns
 */
export function getGantData(query) {
  return request({
    url: '/cx/mdmMonthProdPlan/getGantData',
    method: 'post',
    data: query
  })
}
