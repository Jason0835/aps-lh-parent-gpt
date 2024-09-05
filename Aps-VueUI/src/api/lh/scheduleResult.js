import request,{ downloadLink } from '@/utils/request'

/**
 * 根据条件查询硫化排程结果列表
 * @param {*} query
 * @returns
 */
export function listScheduleResult(query) {
  return request({
    url: '/lh/scheduleResult/list',
    method: 'post',
    data: query
  })
}

/**
 * validateAdd
 * @param {*} query
 * @returns
 */
export function validateAdd(query) {
  return request({
    url: '/lh/scheduleResult/validateAdd',
    method: 'post',
    data: query
  })
}

/**
 * 修改或新增硫化排程结果
 * @param {*} query
 * @returns
 */
export function editScheduleResult(query) {
  return request({
    url: '/lh/scheduleResult/edit',
    method: 'post',
    data: query
  })
}

/**
 * 删除硫化排程结果（id不为空）
 * @param {Object} query
 * @param {String} query.ids 要删除的id字符串，用','分割
 * @returns
 */
export function removeScheduleResult(query) {
  return request({
    url: '/lh/scheduleResult/remove',
    method: 'post',
    data: query
  })
}

/**
 * lhValidateAutoPlan
 * @param {*} query
 * @returns
 */
export function lhValidateAutoPlan(query) {
  return request({
    url: '/lh/scheduleResult/lhValidateAutoPlan',
    method: 'post',
    data: query
  })
}

/**
 * 硫化自动排程
 * @param {*} query
 * @returns
 */
export function autoPlan(query) {
  return request({
    url: '/lh/scheduleResult/autoPlan',
    method: 'post',
    data: query
  })
}

/**
 * 转机台验证
 * @param {*} query
 * @returns
 */
export function validateChangeMachine(query) {
  return request({
    url: '/lh/scheduleResult/validateChangeMachine',
    method: 'post',
    data: query
  })
}

/**
 * 转机台
 * @param {*} query
 * @returns
 */
export function changeMachine(query) {
  return request({
    url: '/lh/scheduleResult/changeMachine',
    method: 'post',
    data: query
  })
}

/**
 * gantt
 * @param {*} query
 * @returns
 */
export function gantt(query) {
  return request({
    url: '/lh/scheduleResult/gantt',
    method: 'get',
    params: query
  })
}


/**
 * 获取甘特图
 * @param {*} query
 * @returns
 */
export function getGantData(query) {
  return request({
    url: '/lh/scheduleResult/getGantData',
    method: 'post',
    data: query
  })
}

/**
 * 调量
 * @param {*} query
 * @returns
 */
export function changeQty(query) {
  return request({
    url: '/lh/scheduleResult/changeQty',
    method: 'post',
    data: query
  })
}

/**
 * 调计划量
 * @param {Object} query
 * @param {String} query.id
 * @param {String} query.mmap
 * @returns
 */
export function changeQtyBuId(query) {
  return request({
    url: '/lh/scheduleResult/changeQty/'+ query.id,
    method: 'get',
    data: query.mmap
  })
}


/**
 * 发布排程
 * @param {*} query
 * @returns
 */
export function publishScheduleResult(query) {
  return request({
    url: '/lh/scheduleResult/publish',
    method: 'post',
    data: query
  })
}

/**
 * 导出硫化排程结果
 * @param {*} params
 * @returns
 */
export function exportScheduleResult(params) {
  return downloadLink("/lh/scheduleResult/export", params);
}

/**
 * 更改发布状态
 * @param {*} query
 * @returns
 */
export function changeReleaseStatus(query) {
  return request({
    url: '/lh/scheduleResult/changeReleaseStatus',
    method: 'post',
    data: query
  })
}
