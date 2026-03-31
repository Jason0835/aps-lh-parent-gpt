import request from '@/utils/request'
//结构内记录
export function listInternalStructure(query) {
  return request({
    url: '/monthplan/mpAdjustStructureIn/list',
    method: 'post',
    data: query
  })
}
//结构内获取调整订单  结构外单选结构开始调整
export function getAdjustDetailList(query) {
  return request({
    url: '/monthplan/mpWeekRollAdjust/getAdjustDetailList',
    method: 'post',
    data: query
  })
}
//结构外记录
export function listOutsideStructure(query) {
  return request({
    url: '/monthplan/mpStructureAllocation/listAdjusts',
    method: 'post',
    data: query
  })
}
//确认调整结果
export function confirmAdjust(query) {
  return request({
    url: '/monthplan/mpWeekRollAdjust/confirmAdjust',
    method: 'post',
    data: query
  })
}

//新增结构
export function addAdjust(query) {
  return request({
    url: '/monthplan/mpStructureAllocation/save',
    method: 'post',
    data: query
  })
}

//自动调整
export function autoAdjust(query) {
  return request({
    url: '/monthplan/mpWeekRollAdjust/autoAdjust',
    method: 'post',
    data: query
  })
}


//结构内行内调整
export function saveAdjust(query) {
  return request({
    url: '/monthplan/mpAdjustStructureIn/save',
    method: 'post',
    data: query
  })
}
//结构外行删除
export function removeStructure(query) {
  return request({
    url: '/monthplan/mpStructureAllocation/remove',
    method: 'post',
    data: query
  })
}

//结构内删除
export function removeAdjust(query) {
  return request({
    url: '/monthplan/mpAdjustStructureIn/remove',
    method: 'post',
    data: query
  })
}


//结构内获取版本列表
export function versionAdjust(query) {
  return request({
    url: '/monthplan/mpAdjustStructureIn/getVersionList',
    method: 'post',
    data: query
  })
}
//结构外获取版本列表
export function versionStructure(query) {
  return request({
    // url: '/monthplan/mpStructureAllocation/getVersionList',
    url: '/monthplan/factoryMonthPlanFinalResult/getVersionList',
    method: 'post',
    data: query
  })
}


//结构外获取列表明细
export function getStructureDetail(query) {
  return request({
    url: '/monthplan/factoryMonthPlanFinalResult/listSkuScheduleItems',
    method: 'post',
    data: query
  })
}


//获取结果列表
export function listResult(query) {
  return request({
    url: '/monthplan/mpAdjustResult/list',
    method: 'post',
    data: query
  })
}

//获取结果版本列表
export function resultVersion(query) {
  return request({
    url: '/monthplan/mpAdjustResult/getVersionList',
    method: 'post',
    data: query
  })
}

//单选结构查询列表
export function listOutHistory(query) {
  return request({
    url: '/monthplan/mpAdjustStructureOut/list',
    method: 'post',
    data: query
  })
}

//单选结构查询列表修改
export function editOutHistory(query) {
  return request({
    url: '/monthplan/mpAdjustStructureOut/save',
    method: 'post',
    data: query
  })
}
//单选结构查询列表删除
export function removeOutHistory(query) {
  return request({
    url: '/monthplan/mpAdjustStructureOut/remove',
    method: 'post',
    data: query
  })
}

//单选结构版本查询列表
export function versionOutHistory(query) {
  return request({
    url: '/monthplan/mpAdjustStructureOut/getVersionList',
    method: 'post',
    data: query
  })
}

//结构外一个结构
export function outNextStructure(query) {
  return request({
    url: '/monthplan/mpStructureAllocation/getNextStructure',
    method: 'post',
    data: query
  })
}


//结构内结果修改
export function saveAdjustResult(query) {
  return request({
    url: '/monthplan/mpAdjustResult/save',
    method: 'post',
    data: query
  })
}

//统计调整结果
export function statisticsResult(query) {
  return request({
    url: '/monthplan/mpMonthPlanStatistics/list',
    method: 'post',
    data: query
  })
}

//结构外单选调整获取日期
export function outGetStayDay(query) {
  return request({
    url: '/monthplan/mpStructureAllocation/getPreviousStructure',
    method: 'post',
    data: query
  })
}


//调整日志
export function logList(query) {
  return request({
    url: '/monthplan/mpAdjustMaterialLog/list',
    method: 'post',
    data: query
  })
}


//调整日志版本列表
export function versionLog(query) {
  return request({
    // url: '/monthplan/mpStructureAllocation/getVersionList',
    url: '/monthplan/mpAdjustMaterialLog/getVersionList',
    method: 'post',
    data: query
  })
}