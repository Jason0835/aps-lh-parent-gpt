import request from "@/utils/request";

export function listResult(query) {
  return request({
    url: "/lh/lhDayFinishQty/list",
    method: "post",
    data: query,
  });
}
