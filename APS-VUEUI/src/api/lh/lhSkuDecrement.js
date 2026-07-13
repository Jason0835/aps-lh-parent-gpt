import request from "@/utils/request";

export function listLhSkuDecrement(query) {
  return request({
    url: "/lh/lhSkuDecrement/list",
    method: "post",
    data: query,
  });
}

export function getLhSkuDecrementInfo(id) {
  return request({
    url: "/lh/lhSkuDecrement/getInfo/" + id,
    method: "get",
  });
}

export function confirmLhSkuDecrement(data) {
  return request({
    url: "/lh/lhSkuDecrement/confirm",
    method: "post",
    data,
    headers: {
      "Content-Type": "application/json;charset=UTF-8",
    },
  });
}

export function removeLhSkuDecrement(data) {
  return request({
    url: "/lh/lhSkuDecrement/remove",
    method: "post",
    data,
  });
}
