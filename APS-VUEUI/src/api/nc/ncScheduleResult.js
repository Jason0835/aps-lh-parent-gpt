import request from '@/utils/request'

export function listScheduleResult(query) {
  return request({
    url: 'nc/ncScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function removeScheduleResult(query) {
  return request({
    url: 'nc/ncScheduleResult/remove',
    method: 'post',
    data: query
  })
}

//
export function validateAutoPlan(query) {
  return request({
    url: 'nc/ncScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function autoPlan(query) {
  return request({
    url: 'nc/ncScheduleResult/autoPlan',
    method: 'post',
    data: query
  })
}
export function balance(query) {
  return request({
    url: 'nc/ncScheduleResult/balance',
    method: 'post',
    data: query
  })
}
// export function modelChange(query) {
//   return request({
//     url: 'nc/ncScheduleResult/modelChange',
//     method: 'post',
//     data: query
//   })
// }
// export function modelAdjustPlan(query) {
//   return request({
//     url: 'nc/ncScheduleResult/modelAdjustPlan',
//     method: 'post',
//     data: query
//   })
// }

// // 获取胎胚版本
// export function getProductEmbryoVersions(query) {
//   return request({
//     url: 'nc/ncScheduleResult/getProductEmbryoVersions',
//     method: 'post',
//     data: query
//   })
// }
// export function getCxMachines(query) {
//   return request({
//     url: 'nc/ncScheduleResult/getCxMachines',
//     method: 'post',
//     data: query
//   })
// }
// export function validateBeforeAdd(query) {
//   return request({
//     url: 'nc/ncScheduleResult/validateBeforeAdd',
//     method: 'post',
//     data: query
//   })
// }
export function validateAdd(query) {
  return request({
    url: 'nc/ncScheduleResult/validateAdd',
    method: 'post',
    data: query
  })
}

// 新建插单（支持一次插入6个班，后端按班次跨天拆分多笔记录）
export function addScheduleResult(query) {
  return request({
    url: 'nc/ncScheduleResult/add',
    method: 'post',
    data: query
  })
}

export function editScheduleResult(query) {
  return request({
    url: 'nc/ncScheduleResult/edit',
    method: 'post',
    data: query
  })
}
export function batchChangeMachine(machineId, query) {
  return request({
    url: 'nc/ncScheduleResult/batchChangeMachine/' + machineId,
    method: 'post',
    data: query
  })
}
export function chooseMachine(query) {
  return request({
    url: 'nc/ncScheduleResult/chooseMachine',
    method: 'post',
    data: query
  })
}
export function mergeProduct(query) {
  return request({
    url: 'nc/ncScheduleResult/mergeProduct',
    method: 'post',
    data: query
  })
}



// //
// export function modifyMoldsValidate(query) {
//   return request({
//     url: 'nc/ncScheduleResult/modifyMoldsValidate',
//     method: 'post',
//     data: query
//   })
// }
// export function modifyMolds(query) {
//   return request({
//     url: 'nc/ncScheduleResult/modifyMolds',
//     method: 'post',
//     data: query
//   })
// }


// export function validateChangeMachine(query) {
//   return request({
//     url: 'nc/ncScheduleResult/validateChangeMachine',
//     method: 'post',
//     data: query
//   })
// }
export function publishValidate(query) {
  return request({
    url: 'nc/ncScheduleResult/publishValidate',
    method: 'post',
    data: query
  })
}
export function publishScheduleResult(query) {
  return request({
    url: 'nc/ncScheduleResult/publish',
    method: 'post',
    data: query
  })
}

// export function hasRecordValidate(query) {
//   return request({
//     url: 'nc/ncScheduleResult/hasRecordValidate',
//     method: 'post',
//     data: query
//   })
// }

// export function modifyQty(query) {
//   return request({
//     url: `nc/ncScheduleResult/modifyQty/${query}`,
//     method: 'post',
//     // data: query
//   })
// }

// export function manualClose(query) {
//   return request({
//     url: `nc/ncScheduleResult/manualClose`,
//     method: 'post',
//     data: query
//   })
// }

// export function listFinished(query) {
//   return request({
//     url: `nc/ncScheduleResult/finished/list`,
//     method: 'post',
//     data: query
//   })
// }
// export function producingIssue(query) {
//   return request({
//    url: `nc/ncScheduleResult/producingIssue`,
//     method: 'post',
//     data: query
//   })
// }
// export function validateConstruction(query) {
//   return request({
//    url: `nc/ncScheduleResult/validateConstruction`,
//     method: 'post',
//     data: query
//   })
// }
export function changeReleaseStatus(query) {
  return request({
   url: `nc/ncScheduleResult/changeReleaseStatus`,
    method: 'post',
    data: query
  })
}

export function changeQty(query) {
  return request({
    url: 'nc/ncScheduleResult/changeQty',
    method: 'post',
    data: query
  })
}

export function combinationMiddleAndNight(query) {
  return request({
   url: `nc/ncScheduleResult/combinationMiddleAndNight`,
    method: 'post',
    data: query
  })
}

export function getSummaryVo(query) {
  return request({
   url: `nc/ncScheduleResult/getSummaryVo`,
    method: 'post',
    data: query
  })
}

// 查询指定排产日的工作班次（班次标题）
export function getWorkClass(query) {
  return request({
   url: `nc/ncScheduleResult/getWorkClass`,
    method: 'get',
    params: query
  })
}

// 获取内衬下拉列表（去重，按内衬名称排序）
export function getPaddingDistList() {
  return request({
   url: `nc/ncScheduleResult/getPaddingDistList`,
    method: 'get'
  })
}

// 获取当前服务器时间对应的班次信息（用于插单弹窗）
export function getCurrentShift() {
  return request({
    url: `nc/ncScheduleResult/getCurrentShift`,
    method: 'get'
  })
}
