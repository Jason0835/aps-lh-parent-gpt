import request, {downloadLink} from '@/utils/request'
import moment from 'moment'
import i18n from '@/lang'

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
 * 硫化自动排程（/execute）
 * @param {Object} query
 * @param {string} query.scheduleDate 排程日期
 * @param {string} [query.factoryCode] 工厂编码
 * @returns {Promise<{ success?: boolean, message?: string, batchNo?: string, msg?: string }>}
 */
export function autoPlan(query) {
  return request({
    url: '/lh/lhScheduleResult/execute',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
/**
 * 硫化插单校验（/validateInsertOrder）
 * @param {Object} query
 * @param {string} query.scheduleDate 排程日期
 * @param {string} query.lhMachineCode 硫化机台编号
 * @param {string} query.productCode 物料编号
 * @returns
 */
export function validateInsertOrder(query) {
  return request({
    url: '/lh/lhScheduleResult/validateInsertOrder',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

/**
 * 获取SKU关联数据（硫化余量/胎胚库存/硫化班产/示方类型）
 * @param {Object} query
 * @param {string} query.factoryCode 工厂编码
 * @param {string} query.materialCode 物料编码
 * @param {string} query.scheduleDate 排程日期
 * @returns
 */
export function getSkuRelatedData(query) {
  return request({
    url: '/lh/lhScheduleResult/getSkuRelatedData',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

/**
 * 硫化插单（/insertOrder）
 * @param {Object} query
 * @param {string} query.scheduleDate 排程日期
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
 * 调量校验
 * @param {*} query
 * @returns
 */
export function validateAdjustQuantity(query) {
  return request({
    url: '/lh/lhScheduleResult/validateAdjustQuantity',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
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
 * @param {string} query.scheduleDate 排程日期
 * @param {string} [query.factoryCode] 工厂编码
 * @param {string} [query.ids] 选中的记录ID，多个用逗号分隔
 * @returns
 */
export function publishScheduleResult(query) {
  return request({
    url: '/lh/lhScheduleResult/publish',
    method: 'post',
    data: { scheduleDate: query.scheduleDate, factoryCode: query.factoryCode, ids: query.ids },
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function issueToMes(query) {
  return request({
    url: '/lh/lhScheduleResult/issueToMes',
    method: 'post',
    params: query
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
  const scheduleDate = params && params.scheduleDate ? moment(params.scheduleDate).format('YYYYMMDD') : moment().format('YYYYMMDD')
  const filenamePrefix = i18n.t('ui.data.column.lhScheduleResult.exportFileName')
  const filename = `${filenamePrefix}${scheduleDate}.xlsx`
  return downloadLink("/lh/lhScheduleResult/export", params, filename);
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

/**
 * 插单/排程相关：按工厂与排程日期等查询可用硫化机台
 * @param {Object} query
 * @param {string} query.scheduleDate 排程日期
 */
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

/**
 * 文字示方更新
 * @param {*} query
 * @returns
 */
export function generateTextMouldChangePlan(query) {
  return request({
    url: '/lh/lhScheduleResult/generateTextMouldChangePlan',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

/**
 * 计划更新
 * @param {*} query
 * @returns
 */
export function increaseMouldStartPlan(query) {
  return request({
    url: '/lh/lhScheduleResult/increaseMouldStartPlan',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}


