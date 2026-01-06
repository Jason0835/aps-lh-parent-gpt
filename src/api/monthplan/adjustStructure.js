import request from '@/utils/request'
//结构内记录
export function listInternalStructure(query) {
  return request({
    url: '/monthplan/mpAdjustStructureIn/list',
    method: 'post',
    data: query
  })
}
//结构内获取调整订单  结构外单选结构调整
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
    url: '/monthplan/mpStructureAllocation/list',
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