import request from "@/utils/request";

// 查询角色列表
export function listRole(query) {
  return request({
    url: "/system/role/list",
    method: "post",
    data: query,
  });
}

// 查询角色详细
export function getRole(roleId) {
  return request({
    url: "/system/role/detail/" + roleId,
    method: "get",
  });
}

// 新增角色
export function addRole(data) {
  return request({
    url: "/system/role/add",
    method: "post",
    data: data,
  });
}

// 修改角色
export function updateRole(data) {
  return request({
    url: "/system/role/edit",
    method: "post",
    data: data,
  });
}

// 角色数据权限
export function dataScope(data) {
  return request({
    url: "/system/role/authDataScope",
    method: "post",
    data: data,
  });
}

// 角色状态修改
export function changeRoleStatus(roleId, status) {
  const data = {
    roleId,
    status,
  };
  return request({
    url: "/system/role/changeStatus",
    method: "post",
    data: data,
  });
}

// 删除角色
export function delRole(roleId) {
  return request({
    url: "/system/role/remove",
    method: "post",
    data: {
      ids: roleId.join(','),
    },
  });
}

// 查询角色已授权用户列表
export function allocatedUserList(data) {
  return request({
    url: "/system/role/authUser/allocatedList",
    method: "post",
    data: data,
  });
}

// 查询角色未授权用户列表
export function unallocatedUserList(data) {
  return request({
    url: "/system/role/authUser/unallocatedList",
    method: "post",
    data: data,
  });
}

// 取消用户授权角色
export function authUserCancel(data) {
  return request({
    url: "/system/role/authUser/cancel",
    method: "post",
    data: data,
  });
}

// 批量取消用户授权角色
export function authUserCancelAll(data) {
  return request({
    url: "/system/role/authUser/cancelAll",
    method: "post",
    params: data,
  });
}

// 授权用户选择
export function authUserSelectAll(data) {
  return request({
    url: "/system/role/authUser/selectAll",
    method: "post",
    params: data,
  });
}

// 根据角色ID查询部门树结构
export function deptTreeSelect(roleId) {
  return request({
    url: "/system/role/deptTree/" + roleId,
    method: "get",
  });
}
