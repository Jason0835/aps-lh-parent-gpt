import request, { downloadLink } from '@/utils/request'

export function listScheduleResult(query) {
  return request({
    url: '/tq/scheduleResult/list',
    method: 'post',
    data: query
  })
}

export function saveScheduleResult(query) {
  return request({
    url: '/tq/scheduleResult/save',
    method: 'post',
    data: query
  })
}

export function removeScheduleResult(ids) {
  return request({
    url: '/tq/scheduleResult/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportScheduleResult(query) {
  return downloadLink("/tq/scheduleResult/export", query)
}

export function importScheduleResult(data) {
  return request({
    url: '/tq/scheduleResult/importData',
    method: 'post',
    data: data
  })
}

export function listScheduleShiftDates(query) {
  return request({
    url: '/tq/scheduleResult/listScheduleShiftDates',
    method: 'post',
    data: query
  })
}

/** 自动排程 */
export function autoPlan(data) {
  return request({
    url: '/tq/scheduleResult/autoPlan',
    method: 'post',
    data: data
  })
}

/** 插单 */
export function insertOrder(data) {
  return request({
    url: '/tq/scheduleResult/insertOrder',
    method: 'post',
    data: data
  })
}

/** 插单前校验 */
export function validateInsertOrder(data) {
  return request({
    url: '/tq/scheduleResult/validateInsertOrder',
    method: 'post',
    data: data
  })
}

/** 转机台 */
export function changeMachine(data) {
  return request({
    url: '/tq/scheduleResult/changeMachine',
    method: 'post',
    data: data
  })
}

/** 转机台前校验 */
export function validateChangeMachine(data) {
  return request({
    url: '/tq/scheduleResult/validateChangeMachine',
    method: 'post',
    data: data
  })
}

/** 获取转机台候选机台列表（按规格过滤） */
export function listCandidateMachines(id) {
  return request({
    url: '/tq/scheduleResult/listCandidateMachines/' + id,
    method: 'post'
  })
}

/** 调量前校验 */
export function validateChangeQty(data) {
  return request({
    url: '/tq/scheduleResult/validateChangeQty',
    method: 'post',
    data: data
  })
}

/** 调量 */
export function changeQty(data) {
  return request({
    url: '/tq/scheduleResult/changeQty',
    method: 'post',
    data: data
  })
}

/** 发布排程 */
export function publishSchedule(data) {
  return request({
    url: '/tq/scheduleResult/publish',
    method: 'post',
    data: data
  })
}

/** 查询排程日期是否已发布 */
export function isPublish(data) {
  return request({
    url: '/tq/scheduleResult/isPublish',
    method: 'post',
    data: data
  })
}

/** 唯一性校验 */
export function checkUnique(data) {
  return request({
    url: '/tq/scheduleResult/checkUnique',
    method: 'post',
    data: data
  })
}

/** 逻辑删除排程记录 */
export function logicDeleteScheduleResult(ids) {
  return request({
    url: '/tq/scheduleResult/logicDelete',
    method: 'post',
    params: { ids: ids }
  })
}

// ==================== 新人工操作接口（走任务链路径，支持锚点插入、resequence 重排、批量操作） ====================

/** 人工插单（新接口，支持锚点插入） */
export function insertTask(data) {
  return request({
    url: '/tq/scheduleResult/insertTask',
    method: 'post',
    data: data
  })
}

/** 批量转机台（支持锚点、目标班次） */
export function batchChangeMachine(data) {
  return request({
    url: '/tq/scheduleResult/batchChangeMachine',
    method: 'post',
    data: data
  })
}

/** 批量调量 */
export function batchChangeQty(data) {
  return request({
    url: '/tq/scheduleResult/batchChangeQty',
    method: 'post',
    data: data
  })
}

/** 批量删除（删除后 resequence 重排） */
export function batchDelete(ids) {
  return request({
    url: '/tq/scheduleResult/batchDelete',
    method: 'post',
    data: ids
  })
}
