import request from '@/utils/request'

export function listMouldSleeve() {
  return request({
    url: '/lh/lhMachineInfo/mouldSleeve/list',
    method: 'get'
  })
}
