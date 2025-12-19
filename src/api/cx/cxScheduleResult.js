import request from '@/utils/request'

// 查询基础数据_地区列表
export function listCxScheduleResult(query) {
  return request({
    url: 'cx/cxScheduleResult/list',
    method: 'post',
    data: query
  })
}

//
export function validateAutoPlan(query) {
  return request({
    url: 'cx/cxScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function autoPlan(query) {
  return request({
    url: 'cx/cxScheduleResult/autoPlan',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },

  })
}
export function lhAutoPlan(query) {
  return request({
    url: 'cx/cxScheduleResult/lhAutoPlan',
    method: 'post',
    data: query
  })
}
export function modelChangeValidate(query) {
  return request({
    url: 'cx/cxScheduleResult/modelChangeValidate',
    method: 'post',
    data: query
  })
}
export function modelChange(query) {
  return request({
    url: 'cx/cxScheduleResult/modelChange',
    method: 'post',
    data: query
  })
}
export function modelAdjustPlan(query) {
  return request({
    url: 'cx/cxScheduleResult/modelAdjustPlan',
    method: 'post',
    data: query
  })
}

// 获取胎胚版本
export function getProductEmbryoVersions(query) {
  return request({
    url: 'cx/cxScheduleResult/getProductEmbryoVersions',
    method: 'post',
    data: query
  })
}
export function getCxMachines(query) {
  return request({
    url: 'cx/cxScheduleResult/getCxMachines',
    method: 'post',
    data: query
  })
}
export function validateBeforeAdd(query) {
  return request({
    url: 'cx/cxScheduleResult/validateBeforeAdd',
    method: 'post',
    data: query
  })
}
export function validateAdd(query) {
  return request({
    url: 'cx/cxScheduleResult/validateAdd',
    method: 'post',
    data: query
  })
}

export function cxScheduleResultEdit(query) {
  return request({
    url: 'cx/cxScheduleResult/edit',
    method: 'post',
    data: query
  })
}



//
export function modifyMoldsValidate(query) {
  return request({
    url: 'cx/cxScheduleResult/modifyMoldsValidate',
    method: 'post',
    data: query
  })
}
export function modifyMolds(query) {
  return request({
    url: 'cx/cxScheduleResult/modifyMolds',
    method: 'post',
    data: query
  })
}


export function validateChangeMachine(query) {
  return request({
    url: 'cx/cxScheduleResult/validateChangeMachine',
    method: 'post',
    data: query
  })
}
export function publishValidate(query) {
  return request({
    url: 'cx/cxScheduleResult/publishValidate',
    method: 'post',
    data: query
  })
}
export function publishScheduleResult(query) {
  return request({
    url: 'cx/cxScheduleResult/publish',
    method: 'post',
    data: query
  })
}

export function hasRecordValidate(query) {
  return request({
    url: 'cx/cxScheduleResult/hasRecordValidate',
    method: 'post',
    data: query
  })
}

export function modifyQty(query) {
  return request({
    url: `cx/cxScheduleResult/modifyQty/${query}`,
    method: 'post',
    // data: query
  })
}

export function manualClose(query) {
  return request({
    url: `cx/cxScheduleResult/manualClose`,
    method: 'post',
    data: query
  })
}

export function listFinished(query) {
  return request({
    url: `cx/cxScheduleResult/finished/list`,
    method: 'post',
    data: query
  })
}
export function producingIssue(query) {
  return request({
   url: `cx/cxScheduleResult/producingIssue`,
    method: 'post',
    data: query
  })
}
export function validateConstruction(query) {
  return request({
   url: `cx/cxScheduleResult/validateConstruction`,
    method: 'post',
    data: query
  })
}
export function changeReleaseStatus(query) {
  return request({
   url: `cx/cxScheduleResult/changeReleaseStatus`,
    method: 'post',
    data: query
  })
}
export function getInfoChangePlan(query) {
  return request({
   url: `cx/cxScheduleResult/changePlan/getInfo`,
    method: 'post',
    data: query
  })
}
export function validateChangeQty(query) {
  return request({
   url: `cx/cxScheduleResult/validateChangeQty`,
    method: 'post',
    data: query
  })
}

export function getInfoModifyQty(query) {
  return request({
   url: `cx/cxScheduleResult/modifyQty/getInfo`,
    method: 'post',
    data: query
  })
}

export function modifyStatus(query) {
  return request({
   url: `cx/cxScheduleResult/modifyStatus`,
    method: 'post',
    data: query
  })
}


export function getBomData(query) {
  return request({
   url: `cx/cxScheduleResult/getBomData`,
    method: 'post',
    data: query
  })
}

export function removeCxScheduleResult(query) {
  return request({
   url: `cx/cxScheduleResult/remove`,
    method: 'post',
    data: query
  })
}

export function changeMachine(query) {
  return request({
   url: `cx/cxScheduleResult/changeMachine`,
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function parseCxScheduleResult(query) {
  return request({
   url: `cx/cxScheduleResult/parseCxScheduleResult`,
    method: 'post',
    data: query,
    // headers: {
    //   'Content-Type': 'application/json;charset=UTF-8'
    // },
  })
}

