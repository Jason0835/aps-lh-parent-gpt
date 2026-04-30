import request from '@/utils/request'

export function listTmScheduleResult(query) {
  return request({
    url: 'tm/tmScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function removeTmScheduleResult(query) {
  return request({
    url: 'tm/tmScheduleResult/remove',
    method: 'post',
    data: query
  })
}

//
export function validateAutoPlan(query) {
  return request({
    url: 'tm/tmScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function autoPlan(query) {
  return request({
    url: 'tm/tmScheduleResult/autoPlan',
    method: 'post',
    data: query
  })
}
export function balance(query) {
  return request({
    url: 'tm/tmScheduleResult/balance',
    method: 'post',
    data: query
  })
}
export function combinationMiddleAndNight(query) {
  return request({
    url: 'tm/tmScheduleResult/combinationMiddleAndNight',
    method: 'post',
    data: query
  })
}
// export function modelAdjustPlan(query) {
//   return request({
//     url: 'tm/tmScheduleResult/modelAdjustPlan',
//     method: 'post',
//     data: query
//   })
// }

// // 获取胎胚版本
// export function getProductEmbryoVersions(query) {
//   return request({
//     url: 'tm/tmScheduleResult/getProductEmbryoVersions',
//     method: 'post',
//     data: query
//   })
// }
// export function getCxMachines(query) {
//   return request({
//     url: 'tm/tmScheduleResult/getCxMachines',
//     method: 'post',
//     data: query
//   })
// }
// export function validateBeforeAdd(query) {
//   return request({
//     url: 'tm/tmScheduleResult/validateBeforeAdd',
//     method: 'post',
//     data: query
//   })
// }
export function validateAdd(query) {
  return request({
    url: 'tm/tmScheduleResult/validateAdd',
    method: 'post',
    data: query
  })
}

export function editScheduleResult(query) {
  return request({
    url: 'tm/tmScheduleResult/edit',
    method: 'post',
    data: query
  })
}

export function batchChangeMachine(machineId, query) {
  return request({
    url: 'tm/tmScheduleResult/batchChangeMachine/' + machineId,
    method: 'post',
    data: query
  })
}
export function chooseMachine(query) {
  return request({
    url: 'tm/tmScheduleResult/chooseMachine',
    method: 'post',
    data: query
  })
}
export function mergeProduct(query) {
  return request({
    url: 'tm/tmScheduleResult/mergeProduct',
    method: 'post',
    data: query
  })
}
export function changeQty(query) {
  return request({
    url: 'tm/tmScheduleResult/changeQty',
    method: 'post',
    data: query
  })
}



// //
// export function modifyMoldsValidate(query) {
//   return request({
//     url: 'tm/tmScheduleResult/modifyMoldsValidate',
//     method: 'post',
//     data: query
//   })
// }
// export function modifyMolds(query) {
//   return request({
//     url: 'tm/tmScheduleResult/modifyMolds',
//     method: 'post',
//     data: query
//   })
// }


// export function validateChangeMachine(query) {
//   return request({
//     url: 'tm/tmScheduleResult/validateChangeMachine',
//     method: 'post',
//     data: query
//   })
// }
export function publishValidate(query) {
  return request({
    url: 'tm/tmScheduleResult/publishValidate',
    method: 'post',
    data: query
  })
}
export function publishScheduleResult(query) {
  return request({
    url: 'tm/tmScheduleResult/publish',
    method: 'post',
    data: query
  })
}

// export function hasRecordValidate(query) {
//   return request({
//     url: 'tm/tmScheduleResult/hasRecordValidate',
//     method: 'post',
//     data: query
//   })
// }

// export function modifyQty(query) {
//   return request({
//     url: `tm/tmScheduleResult/modifyQty/${query}`,
//     method: 'post',
//     // data: query
//   })
// }

// export function manualClose(query) {
//   return request({
//     url: `tm/tmScheduleResult/manualClose`,
//     method: 'post',
//     data: query
//   })
// }

// export function listFinished(query) {
//   return request({
//     url: `tm/tmScheduleResult/finished/list`,
//     method: 'post',
//     data: query
//   })
// }
// export function producingIssue(query) {
//   return request({
//    url: `tm/tmScheduleResult/producingIssue`,
//     method: 'post',
//     data: query
//   })
// }
// export function validateConstruction(query) {
//   return request({
//    url: `tm/tmScheduleResult/validateConstruction`,
//     method: 'post',
//     data: query
//   })
// }
export function changeReleaseStatus(query) {
  return request({
   url: `tm/tmScheduleResult/changeReleaseStatus`,
    method: 'post',
    data: query
  })
}

export function getSummaryVo(query) {
  return request({
   url: `tm/tmScheduleResult/getSummaryVo`,
    method: 'post',
    data: query
  })
}



