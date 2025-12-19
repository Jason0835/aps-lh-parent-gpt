import request from "@/utils/request";

// 查询菜单列表
export function listMenu(query) {
  if (query?.params?.beginTime) {
    query.params = query.params.beginTime;
  } else if (query?.params?.beginTime) {
    query.params = query.params.endTime;
  } else if (query?.params?.beginTime && query?.params?.endTime) {
    query.params = query.params.beginTime + "-" + query.params.endTime;
  } else if (query?.params) {
    delete query.params;
  }

  return request({
    url: "/system/menu/list",
    method: "post",
    params: query,
  });
}

// 查询菜单详细
export function getMenu(menuId) {
  return request({
    url: "/system/menu/detail/" + menuId,
    method: "get",
  });
}

// 查询菜单下拉树结构
export function treeselect() {
  return request({
    url: "/system/menu/treeselect",
    method: "get",
  });
}

// 根据角色ID查询菜单下拉树结构
export function roleMenuTreeselect(roleId) {
  return request({
    url: "/system/menu/roleMenuTreeselect/" + roleId,
    method: "get",
  });
}

// 新增菜单
export function addMenu(data) {
  return request({
    url: "/system/menu/add",
    method: "post",
    data: data,
  });
}

// 修改菜单
export function updateMenu(data) {
  return request({
    url: "/system/menu/edit",
    method: "post",
    data: data,
  });
}

// 删除菜单
export function delMenu(menuId) {
  return request({
    url: "/system/menu/remove/" + menuId,
    method: "get",
  });
}
