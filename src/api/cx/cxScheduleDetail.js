import request from "@/utils/request";

// 成型排程详情-根据查询条件分页查询
export function listCxScheduleDetailByQuery(query) {
  return request({
    url: "/cx/cxScheduleDetail/listByQuery",
    method: "post",
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  });
}

// 成型排程顺位-保存班次计划量等信息
export function updateCxScheduleDetailPlanQty(data) {
  return request({
    url: "/cx/cxScheduleDetail/updatePlanQty",
    method: "post",
    data,
    headers: {
      "Content-Type": "application/json;charset=UTF-8",
    },
  });
}

