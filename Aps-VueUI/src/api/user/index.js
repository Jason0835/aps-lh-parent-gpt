import request from "@/utils/request";

// Change System Language
export function changeLang(lang) {
  return request({
    url: "/vue/user/changeLang",
    method: "GET",
    params: {
      lang,
    },
  });
}

// Database Export
export function exportData() {
  return request({
    url: "/vue/user/export",
    method: "GET",
  });
}

// Asynchronous Database Export
export function exportAsync() {
  return request({
    url: "/vue/user/exportAsync",
    method: "GET",
  });
}

// Database Import
export function importData() {
  return request({
    url: "/vue/user/importData",
    method: "POST",
  });
}

// Get Database Export Template
export function getImportTemplate() {
  return request({
    url: "/vue/user/importTemplate",
    method: "GET",
  });
}

// Get Current User's Full Information
export function getLoginUser() {
  return request({
    url: "/vue/user/loginUser",
    method: "GET",
  });
}

// Refresh Current User's Online Status
export function refreshUser() {
  return request({
    url: "/vue/user/refresh",
    method: "POST",
  });
}

// Get Current User's Simplified Information
export function getSysUser() {
  return request({
    url: "/vue/user/sysUser",
    method: "GET",
  });
}

// Get Current User's Menus
export function getSysUserMenus() {
  return request({
    url: "/vue/user/sysUserMenus",
    method: "GET",
  });
}
