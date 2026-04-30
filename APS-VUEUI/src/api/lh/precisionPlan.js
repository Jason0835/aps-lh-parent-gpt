import request from "@/utils/request";

export function listPrecisionPlan(query) {
  return request({
    url: "/schedule/lhPrecisionPlan/list",
    method: "post",
    data: query,
  });
}

export function getPrecisionPlan(id) {
  return request({
    url: "/schedule/lhPrecisionPlan/" + id,
    method: "get",
  });
}

export function savePrecisionPlan(data) {
  return request({
    url: "/schedule/lhPrecisionPlan/save",
    method: "post",
    data: data,
  });
}

export function removePrecisionPlan(ids) {
  return request({
    url: "/schedule/lhPrecisionPlan/remove",
    method: "delete",
    data: ids,
  });
}

export function generateFromMes(year) {
  return request({
    url: "/schedule/lhPrecisionPlan/generateFromMes",
    method: "post",
    params: { year: year },
  });
}

export function getYearList() {
  return request({
    url: "/schedule/lhPrecisionPlan/getYearList",
    method: "get",
  });
}
