/**
 * 从接口异常、业务响应或普通字符串中提取可展示的错误信息。
 *
 * @param {Error|string|Object} error 原始异常或错误响应
 * @param {string} fallbackMessage 缺少后端信息时使用的兜底提示
 * @returns {string} 可供弹框展示的错误信息
 */
export function resolveErrorMessage(error, fallbackMessage) {
  if (!error) {
    return fallbackMessage
  }
  if (typeof error === 'string') {
    return error
  }
  return error.message ||
    error.msg ||
    (error.response && error.response.data && error.response.data.msg) ||
    fallbackMessage
}
