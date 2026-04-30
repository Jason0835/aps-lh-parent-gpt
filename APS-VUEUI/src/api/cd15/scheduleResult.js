import request from '@/utils/request'

export function listScheduleResult(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function removeScheduleResult(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/remove',
    method: 'post',
    data: query
  })
}

//
export function validateAutoPlan(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function autoPlan(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/autoPlan',
    method: 'post',
    data: query
  })
}
export function balance(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/balance',
    method: 'post',
    data: query
  })
}
// export function modelChange(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/modelChange',
//     method: 'post',
//     data: query
//   })
// }
// export function modelAdjustPlan(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/modelAdjustPlan',
//     method: 'post',
//     data: query
//   })
// }

// // 获取胎胚版本
// export function getProductEmbryoVersions(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/getProductEmbryoVersions',
//     method: 'post',
//     data: query
//   })
// }
// export function getCxMachines(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/getCxMachines',
//     method: 'post',
//     data: query
//   })
// }
// export function validateBeforeAdd(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/validateBeforeAdd',
//     method: 'post',
//     data: query
//   })
// }
export function validateAdd(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/validateAdd',
    method: 'post',
    data: query
  })
}

export function editScheduleResult(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/edit',
    method: 'post',
    data: query
  })
}
export function batchChangeMachine(machineId, query) {
  return request({
    url: 'cd15/cd15ScheduleResult/batchChangeMachine/' + machineId,
    method: 'post',
    data: query
  })
}
export function chooseMachine(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/chooseMachine',
    method: 'post',
    data: query
  })
}
export function mergeProduct(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/mergeProduct',
    method: 'post',
    data: query
  })
}



// //
// export function modifyMoldsValidate(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/modifyMoldsValidate',
//     method: 'post',
//     data: query
//   })
// }
// export function modifyMolds(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/modifyMolds',
//     method: 'post',
//     data: query
//   })
// }


// export function validateChangeMachine(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/validateChangeMachine',
//     method: 'post',
//     data: query
//   })
// }
export function publishValidate(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/publishValidate',
    method: 'post',
    data: query
  })
}
export function publishScheduleResult(query) {
  return request({
    url: 'cd15/cd15ScheduleResult/publish',
    method: 'post',
    data: query
  })
}

// export function hasRecordValidate(query) {
//   return request({
//     url: 'cd15/cd15ScheduleResult/hasRecordValidate',
//     method: 'post',
//     data: query
//   })
// }

// export function modifyQty(query) {
//   return request({
//     url: `cd15/cd15ScheduleResult/modifyQty/${query}`,
//     method: 'post',
//     // data: query
//   })
// }

// export function manualClose(query) {
//   return request({
//     url: `cd15/cd15ScheduleResult/manualClose`,
//     method: 'post',
//     data: query
//   })
// }

// export function listFinished(query) {
//   return request({
//     url: `cd15/cd15ScheduleResult/finished/list`,
//     method: 'post',
//     data: query
//   })
// }
// export function producingIssue(query) {
//   return request({
//    url: `cd15/cd15ScheduleResult/producingIssue`,
//     method: 'post',
//     data: query
//   })
// }
// export function validateConstruction(query) {
//   return request({
//    url: `cd15/cd15ScheduleResult/validateConstruction`,
//     method: 'post',
//     data: query
//   })
// }
export function changeReleaseStatus(query) {
  return request({
   url: `cd15/cd15ScheduleResult/changeReleaseStatus`,
    method: 'post',
    data: query
  })
}
export function changeQty(query) {
  return request({
   url: `cd15/cd15ScheduleResult/changeQty`,
    method: 'post',
    data: query
  })
}
export function combinationMiddleAndNight(query) {
  return request({
   url: `cd15/cd15ScheduleResult/combinationMiddleAndNight`,
    method: 'post',
    data: query
  })
}

export function getSummaryVo(query) {
  return request({
   url: `cd15/cd15ScheduleResult/getSummaryVo`,
    method: 'post',
    data: query
  })
}
