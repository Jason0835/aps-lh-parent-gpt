import request from '@/utils/request'

export function listScheduleResult(query) {
  return request({
    url: 'dj/djScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function removeScheduleResult(query) {
  return request({
    url: 'dj/djScheduleResult/remove',
    method: 'post',
    data: query
  })
}

//
export function validateAutoPlan(query) {
  return request({
    url: 'dj/djScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function autoPlan(query) {
  return request({
    url: 'dj/djScheduleResult/autoPlan',
    method: 'post',
    data: query
  })
}
export function balance(query) {
  return request({
    url: 'dj/djScheduleResult/balance',
    method: 'post',
    data: query
  })
}
// export function modelChange(query) {
//   return request({
//     url: 'dj/djScheduleResult/modelChange',
//     method: 'post',
//     data: query
//   })
// }
// export function modelAdjustPlan(query) {
//   return request({
//     url: 'dj/djScheduleResult/modelAdjustPlan',
//     method: 'post',
//     data: query
//   })
// }

// // 获取胎胚版本
// export function getProductEmbryoVersions(query) {
//   return request({
//     url: 'dj/djScheduleResult/getProductEmbryoVersions',
//     method: 'post',
//     data: query
//   })
// }
// export function getCxMachines(query) {
//   return request({
//     url: 'dj/djScheduleResult/getCxMachines',
//     method: 'post',
//     data: query
//   })
// }
// export function validateBeforeAdd(query) {
//   return request({
//     url: 'dj/djScheduleResult/validateBeforeAdd',
//     method: 'post',
//     data: query
//   })
// }
export function validateAdd(query) {
  return request({
    url: 'dj/djScheduleResult/validateAdd',
    method: 'post',
    data: query
  })
}

export function editScheduleResult(query) {
  return request({
    url: 'dj/djScheduleResult/edit',
    method: 'post',
    data: query
  })
}
export function batchChangeMachine(machineId, query) {
  return request({
    url: 'dj/djScheduleResult/batchChangeMachine/' + machineId,
    method: 'post',
    data: query
  })
}
export function chooseMachine(query) {
  return request({
    url: 'dj/djScheduleResult/chooseMachine',
    method: 'post',
    data: query
  })
}
export function mergeProduct(query) {
  return request({
    url: 'dj/djScheduleResult/mergeProduct',
    method: 'post',
    data: query
  })
}




// //
// export function modifyMoldsValidate(query) {
//   return request({
//     url: 'dj/djScheduleResult/modifyMoldsValidate',
//     method: 'post',
//     data: query
//   })
// }
// export function modifyMolds(query) {
//   return request({
//     url: 'dj/djScheduleResult/modifyMolds',
//     method: 'post',
//     data: query
//   })
// }


// export function validateChangeMachine(query) {
//   return request({
//     url: 'dj/djScheduleResult/validateChangeMachine',
//     method: 'post',
//     data: query
//   })
// }
export function publishValidate(query) {
  return request({
    url: 'dj/djScheduleResult/publishValidate',
    method: 'post',
    data: query
  })
}
export function publishScheduleResult(query) {
  return request({
    url: 'dj/djScheduleResult/publish',
    method: 'post',
    data: query
  })
}

// export function hasRecordValidate(query) {
//   return request({
//     url: 'dj/djScheduleResult/hasRecordValidate',
//     method: 'post',
//     data: query
//   })
// }

// export function modifyQty(query) {
//   return request({
//     url: `dj/djScheduleResult/modifyQty/${query}`,
//     method: 'post',
//     // data: query
//   })
// }

// export function manualClose(query) {
//   return request({
//     url: `dj/djScheduleResult/manualClose`,
//     method: 'post',
//     data: query
//   })
// }

// export function listFinished(query) {
//   return request({
//     url: `dj/djScheduleResult/finished/list`,
//     method: 'post',
//     data: query
//   })
// }
// export function producingIssue(query) {
//   return request({
//    url: `dj/djScheduleResult/producingIssue`,
//     method: 'post',
//     data: query
//   })
// }
// export function validateConstruction(query) {
//   return request({
//    url: `dj/djScheduleResult/validateConstruction`,
//     method: 'post',
//     data: query
//   })
// }
export function changeReleaseStatus(query) {
  return request({
   url: `dj/djScheduleResult/changeReleaseStatus`,
    method: 'post',
    data: query
  })
}

export function changeQty(query) {
  return request({
    url: 'dj/djScheduleResult/changeQty',
    method: 'post',
    data: query
  })
}

export function combinationMiddleAndNight(query) {
  return request({
   url: `dj/djScheduleResult/combinationMiddleAndNight`,
    method: 'post',
    data: query
  })
}

export function getSummaryVo(query) {
  return request({
   url: `dj/djScheduleResult/getSummaryVo`,
    method: 'post',
    data: query
  })
}

export function getWorkClass(query) {
  return request({
   url: `dj/djScheduleResult/getWorkClass`,
    method: 'get',
    params: query
  })
}

export function getPaddingDistList() {
  return request({
   url: `dj/djScheduleResult/getPaddingDistList`,
    method: 'get'
  })
}

export function getCurrentShift() {
  return request({
    url: `dj/djScheduleResult/getCurrentShift`,
    method: 'get'
  })
}
