# CX Precision Plan UI Align-to-LH Plan

Goal: Update `jy_aps_ui` 成型精度计划页面（列表/查询/编辑）以适配后端 `T_CX_PRECISION_PLAN` 新结构，并按需求展示字段与查询规则；精度类型只读由后端返回；周期按精度类型映射（15/60）。

Files:
- Modify: `src/views/cx/cxPrecisionPlan/index.vue`
- Modify: `src/views/cx/cxPrecisionPlan/components/infoDialog.vue`

Steps:
1. index.vue
   - 搜索条件改为：机台(精确)、计划日期(默认当天、精确)、实际日期(精确)。
   - columns 改为：机台、精度类型、计划日期、实际日期、周期(前端映射15/60)、到期日(剩余天数daysToDue)、数据源(0=Mes,1=系统)、备注。
   - 查询参数改为：`planDateStart/planDateEnd`、`actualDateStart/actualDateEnd`（不再使用 planDateBegin/End）。
2. infoDialog.vue
   - 表单字段改为：分厂、机台、精度类型(只读)、计划日期、实际日期、周期(只读)、到期日(只读daysToDue)、数据源(只读)、备注。
   - 删除旧字段：班次/开始结束时间/预计小时/上次精度/到期日(Date)/等。
   - 根据计划日期变化实时计算 daysToDue；周期按精度类型映射。

Note:
- “周期映射”实现：优先从精度类型字典 label 中提取 15/60；若提取不到，默认空。
