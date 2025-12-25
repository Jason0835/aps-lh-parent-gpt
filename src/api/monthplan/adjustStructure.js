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
    url: '/monthplan/mpAdjustStructureOut/list',
    method: 'post',
    data: query
  })
}