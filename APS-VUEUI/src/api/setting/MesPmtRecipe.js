import request, { downloadLink } from '@/utils/request'

export function listMesPmtRecipe(query) {
  return request({
    url: '/setting/MesPmtRecipe/list',
    method: 'post',
    data: query
  })
}

// =
export function selectMesPmtRecipeMachine(query) {
  return request({
    url: '/setting/MesPmtRecipe/selectMesPmtRecipeMachine',
    method: 'post',
    data: query
  })
}
export function selectMesPmtRecipeByParams(query) {
  return request({
    url: '/setting/MesPmtRecipe/selectMesPmtRecipeByParams',
    method: 'post',
    data: query
  })
}
export function syncMesPmtRecipe(query) {
  return request({
    url: '/setting/MesPmtRecipe/syncMesPmtRecipe',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  return downloadLink('/setting/MesPmtRecipe/export', query);
}
