import request from '@/utils/request'

export function listScheduleResult(query) {
  return request({
    url: 'tc/tcScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function removeScheduleResult(query) {
  return request({
    url: 'tc/tcScheduleResult/remove',
    method: 'post',
    data: query
  })
}

//
export function validateAutoPlan(query) {
  return request({
    url: 'tc/tcScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function autoPlan(query) {
  return request({
    url: 'tc/tcScheduleResult/autoPlan',
    method: 'post',
    data: query
  })
}
export function balance(query) {
  return request({
    url: 'tc/tcScheduleResult/balance',
    method: 'post',
    data: query
  })
}
// export function modelChange(query) {
//   return request({
//     url: 'tc/tcScheduleResult/modelChange',
//     method: 'post',
//     data: query
//   })
// }
// export function modelAdjustPlan(query) {
//   return request({
//     url: 'tc/tcScheduleResult/modelAdjustPlan',
//     method: 'post',
//     data: query
//   })
// }

// // 获取胎胚版本
// export function getProductEmbryoVersions(query) {
//   return request({
//     url: 'tc/tcScheduleResult/getProductEmbryoVersions',
//     method: 'post',
//     data: query
//   })
// }
// export function getCxMachines(query) {
//   return request({
//     url: 'tc/tcScheduleResult/getCxMachines',
//     method: 'post',
//     data: query
//   })
// }
// export function validateBeforeAdd(query) {
//   return request({
//     url: 'tc/tcScheduleResult/validateBeforeAdd',
//     method: 'post',
//     data: query
//   })
// }
export function validateAdd(query) {
  return request({
    url: 'tc/tcScheduleResult/validateAdd',
    method: 'post',
    data: query
  })
}

export function editScheduleResult(query) {
  return request({
    url: 'tc/tcScheduleResult/edit',
    method: 'post',
    data: query
  })
}
export function batchChangeMachine(machineId, query) {
  return request({
    url: 'tc/tcScheduleResult/batchChangeMachine/' + machineId,
    method: 'post',
    data: query
  })
}
export function chooseMachine(query) {
  return request({
    url: 'tc/tcScheduleResult/chooseMachine',
    method: 'post',
    data: query
  })
}
export function mergeProduct(query) {
  return request({
    url: 'tc/tcScheduleResult/mergeProduct',
    method: 'post',
    data: query
  })
}



// //
// export function modifyMoldsValidate(query) {
//   return request({
//     url: 'tc/tcScheduleResult/modifyMoldsValidate',
//     method: 'post',
//     data: query
//   })
// }
// export function modifyMolds(query) {
//   return request({
//     url: 'tc/tcScheduleResult/modifyMolds',
//     method: 'post',
//     data: query
//   })
// }


// export function validateChangeMachine(query) {
//   return request({
//     url: 'tc/tcScheduleResult/validateChangeMachine',
//     method: 'post',
//     data: query
//   })
// }
export function publishValidate(query) {
  return request({
    url: 'tc/tcScheduleResult/publishValidate',
    method: 'post',
    data: query
  })
}
export function publishScheduleResult(query) {
  return request({
    url: 'tc/tcScheduleResult/publish',
    method: 'post',
    data: query
  })
}

// export function hasRecordValidate(query) {
//   return request({
//     url: 'tc/tcScheduleResult/hasRecordValidate',
//     method: 'post',
//     data: query
//   })
// }

// export function modifyQty(query) {
//   return request({
//     url: `tc/tcScheduleResult/modifyQty/${query}`,
//     method: 'post',
//     // data: query
//   })
// }

// export function manualClose(query) {
//   return request({
//     url: `tc/tcScheduleResult/manualClose`,
//     method: 'post',
//     data: query
//   })
// }

// export function listFinished(query) {
//   return request({
//     url: `tc/tcScheduleResult/finished/list`,
//     method: 'post',
//     data: query
//   })
// }
// export function producingIssue(query) {
//   return request({
//    url: `tc/tcScheduleResult/producingIssue`,
//     method: 'post',
//     data: query
//   })
// }
// export function validateConstruction(query) {
//   return request({
//    url: `tc/tcScheduleResult/validateConstruction`,
//     method: 'post',
//     data: query
//   })
// }
export function changeReleaseStatus(query) {
  return request({
   url: `tc/tcScheduleResult/changeReleaseStatus`,
    method: 'post',
    data: query
  })
}


export function changeQty(query) {
  return request({
    url: 'tc/tcScheduleResult/changeQty',
    method: 'post',
    data: query
  })
}

export function combinationMiddleAndNight(query) {
  return request({
   url: `tc/tcScheduleResult/combinationMiddleAndNight`,
    method: 'post',
    data: query
  })
}

export function getSummaryVo(query) {
  return request({
   url: `tc/tcScheduleResult/getSummaryVo`,
    method: 'post',
    data: query
  })
}