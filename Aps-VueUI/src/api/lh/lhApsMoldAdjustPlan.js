import request,{ downloadLink } from '@/utils/request'

/**
 * 根据条件查询硫化工序模具变动单APS列表
 * @param {*} query
 * @returns
 */
export function listApsMoldAdjustPlan(query) {
  return request({
    url: '/lh/lhApsMoldAdjustPlan/list',
    method: 'post',
    data: query
  })
}

/**
 * 新增硫化工序模具变动单APS
 * @param {Object} query
 * @param {string} query.planDate 计划日期
 * @param {string} query.lhMachineCode 硫化机台编号
 * @param {string} query.lhMachineName 硫化机台名称
 * @param {Array} query.apsMoldAdjustPlanList 模具计划记录数组
 * @returns
 */
export function addSubData(query) {
  return request({
    url: '/lh/lhApsMoldAdjustPlan/addSubData',
    method: 'post',
    data: query
  })
}

/**
 * 修改或新增硫化工序模具变动单APS
 * @param {*} query
 * @returns
 */
export function editApsMoldAdjustPlan(query) {
  return request({
    url: '/lh/lhApsMoldAdjustPlan/edit',
    method: 'post',
    data: query
  })
}

/**
 * 修改或新增硫化工序模具变动单APS
 * @param {*} query
 * @returns
 */
export function removeApsMoldAdjustPlan(query) {
  return request({
    url: '/lh/lhApsMoldAdjustPlan/remove',
    method: 'post',
    data: query
  })
}


/**
 * 发布排程
 * @param {*} query
 * @returns
 */
export function publishApsMoldAdjustPlan(query) {
  return request({
    url: '/lh/lhApsMoldAdjustPlan/publish',
    method: 'post',
    data: query
  })
}

/**
 * 导出主计划月度生产计划
 * @param {*} params
 * @returns
 */
export function exportApsMoldAdjustPlan(params) {
  return downloadLink("/lh/lhApsMoldAdjustPlan/export", params);
}


/**
 * 更改执行状态
 * @param {object} query
 * @param {string} query.isExecute 	是否执行，0：否，1：是
 * @returns
 */
export function changeExecute(query) {
  return request({
    url: '/lh/lhApsMoldAdjustPlan/changeExecute',
    method: 'post',
    data: query
  })
}

