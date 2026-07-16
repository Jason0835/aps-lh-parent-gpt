import request, { downloadLink } from '@/utils/request'

/** 查询钢丝圈排程结果列表 */
export function listScheduleResult(query) {
  return request({
    url: '/gsq/scheduleResult/list',
    method: 'post',
    data: query
  })
}

/** 保存钢丝圈排程结果（新增/修改） */
export function saveScheduleResult(query) {
  return request({
    url: '/gsq/scheduleResult/save',
    method: 'post',
    data: query
  })
}

/** 删除钢丝圈排程结果 */
export function removeScheduleResult(ids) {
  return request({
    url: '/gsq/scheduleResult/remove',
    method: 'post',
    params: { ids: ids }
  })
}

/** 导出钢丝圈排程结果 */
export function exportScheduleResult(query) {
  return downloadLink("/gsq/scheduleResult/export", query)
}

/** 导入钢丝圈排程结果 */
export function importScheduleResult(data) {
  return request({
    url: '/gsq/scheduleResult/importData',
    method: 'post',
    data: data
  })
}

/** 获取6班次日期列表（D日中班/D+1日夜早中/D+2日夜早） */
export function listScheduleShiftDates(query) {
  return request({
    url: '/gsq/scheduleResult/listScheduleShiftDates',
    method: 'post',
    data: query
  })
}

/** 自动排程 */
export function autoPlan(data) {
  return request({
    url: '/gsq/scheduleResult/autoPlan',
    method: 'post',
    data: data
  })
}

/** 插单 */
export function insertOrder(data) {
  return request({
    url: '/gsq/scheduleResult/insertOrder',
    method: 'post',
    data: data
  })
}

/** 插单前校验 */
export function validateInsertOrder(data) {
  return request({
    url: '/gsq/scheduleResult/validateInsertOrder',
    method: 'post',
    data: data
  })
}

/** 转机台 */
export function changeMachine(data) {
  return request({
    url: '/gsq/scheduleResult/changeMachine',
    method: 'post',
    data: data
  })
}

/** 转机台前校验 */
export function validateChangeMachine(data) {
  return request({
    url: '/gsq/scheduleResult/validateChangeMachine',
    method: 'post',
    data: data
  })
}

/** 调量前校验 */
export function validateChangeQty(data) {
  return request({
    url: '/gsq/scheduleResult/validateChangeQty',
    method: 'post',
    data: data
  })
}

/** 调量 */
export function changeQty(data) {
  return request({
    url: '/gsq/scheduleResult/changeQty',
    method: 'post',
    data: data
  })
}

/** 发布排程到MES（6班→3天拆分下发），传入选中记录ID列表 */
export function publishSchedule(data) {
  return request({
    url: '/gsq/scheduleResult/publish',
    method: 'post',
    data: { scheduleDate: data.scheduleDate, factoryCode: data.factoryCode, ids: data.ids },
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

/** 查询排程日期是否已发布 */
export function isPublish(data) {
  return request({
    url: '/gsq/scheduleResult/isPublish',
    method: 'post',
    data: data
  })
}

/** 唯一性校验（排程日期+钢丝圈代码+机台编号） */
export function checkUnique(data) {
  return request({
    url: '/gsq/scheduleResult/checkUnique',
    method: 'post',
    data: data
  })
}

/** 逻辑删除排程记录（已发布成功的计划不允许删除） */
export function logicDeleteScheduleResult(ids) {
  return request({
    url: '/gsq/scheduleResult/logicDelete',
    method: 'post',
    params: { ids: ids }
  })
}
