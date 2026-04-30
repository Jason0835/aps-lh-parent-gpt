import request from "@/utils/request";

export function listResult(query) {
  return request({
    url: "/lh/curingDailyCompletion/list",
    method: "post",
    data: query,
  });
}
