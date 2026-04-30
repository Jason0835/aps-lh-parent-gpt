import request from "@/utils/request";
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询用户列表
export function listUser(query) {
  // if (query?.params?.beginTime) {
  //   query.params = query.params.beginTime;
  // } else if (query?.params?.beginTime) {
  //   query.params = query.params.endTime;
  // } else if (query?.params?.beginTime && query?.params?.endTime) {
  //   query.params = query.params.beginTime + "-" + query.params.endTime;
  // } else if (query?.params) {
  //   delete query.params;
  // }
  return request({
    url: "/system/user/list",
    method: "post",
    data: query,
  });
}

// 查询用户详细
export function getUser(userId) {
  return request({
    url: "/system/user/detail/" + parseStrEmpty(userId),
    method: "get",
  });
}

// 新增用户
export function addUser(data) {
  // delete data.params;
  //TODO object array form 传参
  // delete data.dept;
  // delete data.roles;
  // delete data.roleIds;
  // delete data.postIds;
  return request({
    url: "/system/user/add",
    method: "post",
    data: data,
  });
}

// 修改用户
export function updateUser(data) {
  return request({
    url: "/system/user/edit",
    method: "post",
    data: data,
  });
}

// 删除用户
export function delUser(userId) {
  return request({
    url: "/system/user/remove/",
    method: "post",
    data: {
      ids: userId.join(","),
    },
  });
}

// 用户密码重置
export function resetUserPwd(userId, password) {
  const data = {
    userId,
    password,
  };
  return request({
    url: "/system/user/resetPwd",
    method: "post",
    data: data,
  });
}

// 用户状态修改
export function changeUserStatus(userId, status) {
  const data = {
    userId,
    status,
  };
  return request({
    url: "/system/user/changeStatus",
    method: "post",
    data: data,
  });
}

// 查询用户个人信息
export function getUserProfile() {
  return request({
    url: "/system/user/profile/detail",
    method: "get",
  });
}

// 修改用户个人信息
export function updateUserProfile(data) {
  return request({
    url: "/system/user/profile/update",
    method: "post",
    data: data,
  });
}

// 用户密码重置
export function updateUserPwd(oldPassword, newPassword) {
  const data = {
    oldPassword,
    newPassword,
  };
  return request({
    url: "/system/user/profile/resetPwd",
    method: "post",
    params: data,
  });
}

// 用户头像上传
export function uploadAvatar(data) {
  return request({
    url: "/system/user/profile/updateAvatar",
    method: "post",
    data: {
      avatarfile: data
    },
  });
}

// 查询授权角色
export function getAuthRole(userId) {
  return request({
    url: "/system/user/authRole/vue/" + userId,
    method: "get",
  });
}

// 保存授权角色
export function updateAuthRole(data) {
  return request({
    url: "/system/user/authRole/insertAuthRole",
    method: "post",
    data,
  });
}

// 查询部门下拉树结构
export function deptTreeSelect() {
  return request({
    url: "/system/dept/deptTree",
    method: "get",
  })
}
