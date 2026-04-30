import request, { downloadLink } from '@/utils/request'

export function listMesPmtRecipeWeight(query) {
  return request({
    url: '/setting/MesPmtRecipeWeight/list',
    method: 'post',
    data: query
  })
}
