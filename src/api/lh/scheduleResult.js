import request,{ downloadLink } from '@/utils/request'

/**
 * 根据条件查询硫化排程结果列表
 * @param {*} query
 * @returns
 */
export function listScheduleResult(query) {
  return request({
    url: '/lh/lhScheduleResult/list',
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
    url: '/lh/lhScheduleResult/validateAdd',
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
    url: '/lh/lhScheduleResult/save',
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
    url: '/lh/lhScheduleResult/remove',
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
    url: '/lh/lhScheduleResult/lhValidateAutoPlan',
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
    url: '/lh/lhScheduleResult​/execute',
    method: 'post',
    data: query,
    // headers: {
    //   'Content-Type': 'application/json;charset=UTF-8'
    // },
  })
}
/**
 * 硫化自动排程
 * @param {*} query
 * @returns
 */
export function insertOrder(query) {
  return request({
    url: '/lh/lhScheduleResult/insertOrder',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

/**
 * 转机台验证
 * @param {*} query
 * @returns
 */
export function validateChangeMachine(query) {
  return request({
    url: '/lh/lhScheduleResult/validateChangeMachine',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

/**
 * 转机台
 * @param {*} query
 * @returns
 */
export function changeMachine(query) {
  return request({
    url: '/lh/lhScheduleResult/changeMachine',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

/**
 * gantt
 * @param {*} query
 * @returns
 */
export function gantt(query) {
  return request({
    url: '/lh/lhScheduleResult/gantt',
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
    url: '/lh/lhScheduleResult/getGantData',
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
    url: '/lh/lhScheduleResult/adjustQuantity',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
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
    url: '/lh/lhScheduleResult/changeQty/'+ query.id,
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
    url: '/lh/lhScheduleResult/publish',
    method: 'post',
    data: query
  })
}
/**
 * 根据规格号查询物料号List
 * @param {*} query
 * @returns
 */
export function selectListMdmProductConstruction(query) {
  return request({
    url: '/lh/lhScheduleResult/selectListMdmProductConstruction',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

/**
 * 导出硫化排程结果
 * @param {*} params
 * @returns
 */
export function exportScheduleResult(params) {
  return downloadLink("/lh/lhScheduleResult/export", params);
}
/**
 * 导出硫化排程结果
 * @param {*} params
 * @returns
 */
export function exportCombine(params) {
  return downloadLink("/lh/lhScheduleResult/exportCombine", params);
}

/**
 * 更改发布状态
 * @param {*} query
 * @returns
 */
export function changeReleaseStatus(query) {
  return request({
    url: '/lh/lhScheduleResult/changeReleaseStatus',
    method: 'post',
    data: query
  })
}
/**
 * 根据排程时间获取批次号
 * @param {*} query
 * @returns
 */
export function getBatchNo(query) {
  return request({
    url: '/lh/lhScheduleResult/getBatchNo',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function getScheduleMachineInfo(query) {
  return request({
    url: '/lh/lhScheduleResult/getScheduleMachineInfo',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}


/**
 * 获取日期
 * @param {*} query
 * @returns
 */
export function getScheduleDate(query) {
  return request({
    url: '/lh/lhScheduleResult/listScheduleShiftDates',
    method: 'post',
    data: query
  })
}

/**
 * 获取日期
 * @param {*} query
 * @returns
 */
export function adjustTextNo(query) {
  return request({
    url: '/lh/lhScheduleResult/adjustTextNo',
    method: 'post',
    data: query
  })
}