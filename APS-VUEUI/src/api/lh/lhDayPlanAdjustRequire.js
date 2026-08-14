import request from "@/utils/request";

export function listLhDayPlanAdjustRequire(query) {
  return request({
    url: "/lh/lhDayPlanAdjustRequire/list",
    method: "post",
    data: query,
  });
}

export function saveLhDayPlanAdjustRequire(data) {
  return request({
    url: "/lh/lhDayPlanAdjustRequire/save",
    method: "post",
    data,
    headers: {
      "Content-Type": "application/json;charset=UTF-8",
    },
  });
}
