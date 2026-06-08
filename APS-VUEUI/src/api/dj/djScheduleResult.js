import request from '@/utils/request'

export function listScheduleResult(query) {
  return request({
    url: 'dj/ncScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function removeScheduleResult(query) {
  return request({
    url: 'dj/ncScheduleResult/remove',
    method: 'post',
    data: query
  })
}

//
export function validateAutoPlan(query) {
  return request({
    url: 'dj/ncScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function autoPlan(query) {
  return request({
    url: 'dj/ncScheduleResult/autoPlan',
    method: 'post',
    data: query
  })
}
export function balance(query) {
  return request({
    url: 'dj/ncScheduleResult/balance',
    method: 'post',
    data: query
  })
}
// export function modelChange(query) {
//   return request({
//     url: 'dj/ncScheduleResult/modelChange',
//     method: 'post',
//     data: query
//   })
// }
// export function modelAdjustPlan(query) {
//   return request({
//     url: 'dj/ncScheduleResult/modelAdjustPlan',
//     method: 'post',
//     data: query
//   })
// }

// // 获取胎胚版本
// export function getProductEmbryoVersions(query) {
//   return request({
//     url: 'dj/ncScheduleResult/getProductEmbryoVersions',
//     method: 'post',
//     data: query
//   })
// }
// export function getCxMachines(query) {
//   return request({
//     url: 'dj/ncScheduleResult/getCxMachines',
//     method: 'post',
//     data: query
//   })
// }
// export function validateBeforeAdd(query) {
//   return request({
//     url: 'dj/ncScheduleResult/validateBeforeAdd',
//     method: 'post',
//     data: query
//   })
// }
export function validateAdd(query) {
  return request({
    url: 'dj/ncScheduleResult/validateAdd',
    method: 'post',
    data: query
  })
}

export function editScheduleResult(query) {
  return request({
    url: 'dj/ncScheduleResult/edit',
    method: 'post',
    data: query
  })
}
export function batchChangeMachine(machineId, query) {
  return request({
    url: 'dj/ncScheduleResult/batchChangeMachine/' + machineId,
    method: 'post',
    data: query
  })
}
export function chooseMachine(query) {
  return request({
    url: 'dj/ncScheduleResult/chooseMachine',
    method: 'post',
    data: query
  })
}
export function mergeProduct(query) {
  return request({
    url: 'dj/ncScheduleResult/mergeProduct',
    method: 'post',
    data: query
  })
}



// //
// export function modifyMoldsValidate(query) {
//   return request({
//     url: 'dj/ncScheduleResult/modifyMoldsValidate',
//     method: 'post',
//     data: query
//   })
// }
// export function modifyMolds(query) {
//   return request({
//     url: 'dj/ncScheduleResult/modifyMolds',
//     method: 'post',
//     data: query
//   })
// }


// export function validateChangeMachine(query) {
//   return request({
//     url: 'dj/ncScheduleResult/validateChangeMachine',
//     method: 'post',
//     data: query
//   })
// }
export function publishValidate(query) {
  return request({
    url: 'dj/ncScheduleResult/publishValidate',
    method: 'post',
    data: query
  })
}
export function publishScheduleResult(query) {
  return request({
    url: 'dj/ncScheduleResult/publish',
    method: 'post',
    data: query
  })
}

// export function hasRecordValidate(query) {
//   return request({
//     url: 'dj/ncScheduleResult/hasRecordValidate',
//     method: 'post',
//     data: query
//   })
// }

// export function modifyQty(query) {
//   return request({
//     url: `dj/ncScheduleResult/modifyQty/${query}`,
//     method: 'post',
//     // data: query
//   })
// }

// export function manualClose(query) {
//   return request({
//     url: `dj/ncScheduleResult/manualClose`,
//     method: 'post',
//     data: query
//   })
// }

// export function listFinished(query) {
//   return request({
//     url: `dj/ncScheduleResult/finished/list`,
//     method: 'post',
//     data: query
//   })
// }
// export function producingIssue(query) {
//   return request({
//    url: `dj/ncScheduleResult/producingIssue`,
//     method: 'post',
//     data: query
//   })
// }
// export function validateConstruction(query) {
//   return request({
//    url: `dj/ncScheduleResult/validateConstruction`,
//     method: 'post',
//     data: query
//   })
// }
export function changeReleaseStatus(query) {
  return request({
   url: `dj/ncScheduleResult/changeReleaseStatus`,
    method: 'post',
    data: query
  })
}

export function changeQty(query) {
  return request({
    url: 'dj/ncScheduleResult/changeQty',
    method: 'post',
    data: query
  })
}

export function combinationMiddleAndNight(query) {
  return request({
   url: `dj/ncScheduleResult/combinationMiddleAndNight`,
    method: 'post',
    data: query
  })
}

export function getSummaryVo(query) {
  return request({
   url: `dj/ncScheduleResult/getSummaryVo`,
    method: 'post',
    data: query
  })
}
