/**
 * 跳转月计划「结构调整」页（路由名：MonthPlanStructureAdjust）。
 */
export function pushToMonthPlanStructureAdjust(router, query) {
  const isDup = (e) => e && e.name === "NavigationDuplicated";
  return router
    .push({ name: "MonthPlanStructureAdjust", query })
    .catch((e) => {
      if (isDup(e)) return Promise.resolve();
      return router.push({ name: "MonthPlanStructureAdjust", query });
    })
    .catch(() => {});
}

/** 同 push，使用 replace（如从结构调整流程返回清空 query 时） */
export function replaceMonthPlanStructureAdjust(router, query) {
  const isDup = (e) => e && e.name === "NavigationDuplicated";
  return router
    .replace({ name: "MonthPlanStructureAdjust", query })
    .catch((e) => {
      if (isDup(e)) return Promise.resolve();
      return router.replace({ name: "MonthPlanStructureAdjust", query });
    })
    .catch(() => {});
}
