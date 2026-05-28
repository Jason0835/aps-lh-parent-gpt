import request from "@/utils/request";
import { encrypt as aesEncrypt } from "@/utils/aes";

// 登录方法
export function login(username, password, code, uuid) {
  // const form = new FormData();
  // form.set("username", username);
  // form.set("password", password);
  // form.set("rememberMe", false);
  // form.set("lang", "zh_CN");

  // 对密码进行AES加密
  const encryptedPassword = aesEncrypt(password);

  return request({
    url: "/login",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", // Content
    },
    method: "post",
    data: {
      username,
      password: encryptedPassword,
      rememberMe: false,
      lang: localStorage.getItem("language") || "zh_CN" ,
    },
  });
}

// 注册方法
export function register(data) {
  return request({
    url: "/auth/register",
    headers: {
      isToken: false,
    },
    method: "post",
    data: data,
  });
}

// 刷新方法
export function refreshToken() {
  return request({
    url: "/auth/refresh",
    method: "post",
  });
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: "/system/user/getInfo",
    method: "get",
  });
}

// 退出方法
export function logout() {
  return request({
    url: "/logout",
    method: "post",
  });
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: "/code",
    headers: {
      isToken: false,
    },
    method: "get",
    timeout: 20000,
  });
}
