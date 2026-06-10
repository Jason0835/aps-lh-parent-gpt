import request, {downloadLink} from '@/utils/request'

/**
 * 查询胎面调度员操作日志列表
 * @param {Object} query
 * @returns
 */
export function listDispatcherLog(query) {
  return request({
    url: '/tm/tmDispatcherLog/list',
    method: 'post',
    data: query
  })
}

/**
 * 导出胎面调度员操作日志
 * @param {Object} query
 */
export function exportDispatcherLog(query) {
  return downloadLink("/tm/tmDispatcherLog/export", query);
}
