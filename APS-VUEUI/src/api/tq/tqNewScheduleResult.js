import request, { downloadLink } from '@/utils/request'

export function listNewScheduleResult(query) {
  return request({
    url: '/tq/newScheduleResult/list',
    method: 'post',
    data: query
  })
}

export function saveNewScheduleResult(query) {
  return request({
    url: '/tq/newScheduleResult/save',
    method: 'post',
    data: query
  })
}

export function removeNewScheduleResult(ids) {
  return request({
    url: '/tq/newScheduleResult/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportNewScheduleResult(query) {
  return downloadLink("/tq/newScheduleResult/export", query)
}

export function importNewScheduleResult(data) {
  return request({
    url: '/tq/newScheduleResult/importData',
    method: 'post',
    data: data
  })
}

export function listScheduleShiftDates(query) {
  return request({
    url: '/tq/newScheduleResult/listScheduleShiftDates',
    method: 'post',
    data: query
  })
}

/** 自动排程 */
export function autoPlan(data) {
  return request({
    url: '/tq/newScheduleResult/autoPlan',
    method: 'post',
    data: data
  })
}

/** 插单 */
export function insertOrder(data) {
  return request({
    url: '/tq/newScheduleResult/insertOrder',
    method: 'post',
    data: data
  })
}

/** 插单前校验 */
export function validateInsertOrder(data) {
  return request({
    url: '/tq/newScheduleResult/validateInsertOrder',
    method: 'post',
    data: data
  })
}

/** 转机台 */
export function changeMachine(data) {
  return request({
    url: '/tq/newScheduleResult/changeMachine',
    method: 'post',
    data: data
  })
}

/** 转机台前校验 */
export function validateChangeMachine(data) {
  return request({
    url: '/tq/newScheduleResult/validateChangeMachine',
    method: 'post',
    data: data
  })
}

/** 调量前校验 */
export function validateChangeQty(data) {
  return request({
    url: '/tq/newScheduleResult/validateChangeQty',
    method: 'post',
    data: data
  })
}

/** 调量 */
export function changeQty(data) {
  return request({
    url: '/tq/newScheduleResult/changeQty',
    method: 'post',
    data: data
  })
}

/** 发布排程 */
export function publishSchedule(data) {
  return request({
    url: '/tq/newScheduleResult/publish',
    method: 'post',
    data: data
  })
}

/** 查询排程日期是否已发布 */
export function isPublish(data) {
  return request({
    url: '/tq/newScheduleResult/isPublish',
    method: 'post',
    data: data
  })
}

/** 唯一性校验 */
export function checkUnique(data) {
  return request({
    url: '/tq/newScheduleResult/checkUnique',
    method: 'post',
    data: data
  })
}

/** 逻辑删除排程记录 */
export function logicDeleteNewScheduleResult(ids) {
  return request({
    url: '/tq/newScheduleResult/logicDelete',
    method: 'post',
    data: ids
  })
}
