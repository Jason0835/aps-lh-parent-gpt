/**
 * 跳转月计划结构内/结构调整页（组件路径：newPage/rollingCycle/index）。
 * BootUI 若仍使用历史路由名 MonthPlanStructureInnerAdjust，需将菜单组件改为上述路径，或保留该 name 与 rollingCycle 指向同一组件。
 */
export function pushToMonthPlanStructureAdjust(router, query) {
  const isDup = (e) => e && e.name === "NavigationDuplicated";
  return router
    .push({ name: "rollingCycle", query })
    .catch((e) => {
      if (isDup(e)) return Promise.resolve();
      return router.push({ name: "MonthPlanStructureInnerAdjust", query });
    })
    .catch(() => {});
}

/** 同 push，使用 replace（如从结构调整流程返回清空 query 时） */
export function replaceMonthPlanStructureAdjust(router, query) {
  const isDup = (e) => e && e.name === "NavigationDuplicated";
  return router
    .replace({ name: "rollingCycle", query })
    .catch((e) => {
      if (isDup(e)) return Promise.resolve();
      return router.replace({ name: "MonthPlanStructureInnerAdjust", query });
    })
    .catch(() => {});
}
