import CryptoJS from 'crypto-js'

// AES密钥，与后端配置一致
const AES_KEY = 'zlt_aps_pwd_key!'

/**
 * AES加密
 * @param {string} text 待加密的文本
 * @returns {string} 加密后的Base64字符串
 */
export function encrypt(text) {
  if (!text) return text

  // 将密钥转换为WordArray
  const key = CryptoJS.enc.Utf8.parse(AES_KEY)

  // 使用ECB模式，PKCS7填充（与Java的PKCS5Padding兼容）
  const encrypted = CryptoJS.AES.encrypt(text, key, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  })

  return encrypted.toString()
}

/**
 * AES解密
 * @param {string} encryptedText 待解密的Base64字符串
 * @returns {string} 解密后的文本
 */
export function decrypt(encryptedText) {
  if (!encryptedText) return encryptedText

  // 将密钥转换为WordArray
  const key = CryptoJS.enc.Utf8.parse(AES_KEY)

  // 解密
  const decrypted = CryptoJS.AES.decrypt(encryptedText, key, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  })

  return decrypted.toString(CryptoJS.enc.Utf8)
}

export default {
  encrypt,
  decrypt
}
