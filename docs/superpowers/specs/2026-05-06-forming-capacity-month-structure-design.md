# 成型产能最近12个月结构列设计

## 背景

`APS-VUEUI/src/views/newPage/formingCapacity/index.vue` 当前复用成型机台档案分页接口
`/monthplan/mdmMoldingMachine/list` 展示机台列表。需求是在列表中增加最近 12 个历史月份列，不包含当前月，每列展示该机台在对应月份关联到的结构名称。

例如当前月份为 2026 年 5 月时，前端列标题依次展示：

`26-04, 26-03, 26-02, 26-01, 25-12, 25-11, 25-10, 25-09, 25-08, 25-07, 25-06, 25-05`

## 目标

在保持现有分页接口和机台列表查询条件不变的前提下，为当前页每台成型机补充最近 12 个历史月份的结构名称数据。

同一机台、同一年月下存在多个结构时，按 `STRUCTURE_NAME` 去重后使用英文逗号 `,` 拼接展示。

## 数据口径

结构名称来源：

- 读取 `t_mp_moulding_day_result.MAIN_MATERIAL_DESC`
- 关联 `t_mdm_sku_structure_ref.MAIN_MATERIAL_DESC`
- 取 `t_mdm_sku_structure_ref.STRUCTURE_NAME`

版本过滤：

- `t_mp_moulding_day_result.PRODUCTION_VERSION = t_mp_proc_version.PRODUCTION_VERSION`
- `t_mp_proc_version.IS_FINAL = '1'`
- `t_mp_moulding_day_result.IS_DELETE = 0`
- `t_mp_proc_version.IS_DELETE = 0`
- `t_mdm_sku_structure_ref.IS_DELETE = 0`

年月过滤：

- 以后端当前日期为基准，计算当前月之前的 12 个月。
- 查询使用 `t_mp_moulding_day_result.YEAR` 和 `t_mp_moulding_day_result.MONTH`。
- 列标题使用 `YY-MM`，数据 key 使用完整年月，避免跨年混淆，例如 `2026-04`。

机台匹配：

- `t_mp_moulding_day_result.CX_MACHINE_CODE` 可能包含多个机台编码，多个编码以英文逗号分隔。
- 后端批量查询当前页相关月份的结果后，在 Java 侧拆分 `CX_MACHINE_CODE`，将同一条结果展开到多个机台编码上。
- 拆分时去掉首尾空格，忽略空编码。

## 后端设计

继续保留 BootUI 层现有入口：

```java
public TableDataInfo list(MdmMoldingMachine mdmMoldingMachine) {
    return iMdmMoldingMachineService.list(mdmMoldingMachine);
}
```

服务端分页逻辑保持现有行为。远程服务返回 `TableDataInfo` 后，只对当前页 `rows` 做数据补充，避免影响分页总数和查询条件。

在 `MdmMoldingMachine` 增加非数据库字段：

```java
/**
 * 最近12个月结构名称，key为yyyy-MM，value为去重后逗号拼接的结构名称。
 */
@TableField(exist = false)
private Map<String, String> monthStructureNameMap;
```

新增 mapper 查询，返回字段建议包含：

- `year`
- `month`
- `cxMachineCode`
- `structureName`

SQL 查询只负责取出基础明细或初步去重结果，Java 侧负责：

- 拆分 `CX_MACHINE_CODE`
- 过滤当前页机台编码
- 按 `机台编码 + yyyy-MM` 分组
- 对 `STRUCTURE_NAME` 去重
- 使用英文逗号拼接
- 回填到每个 `MdmMoldingMachine.monthStructureNameMap`

## 前端设计

`formingCapacity/index.vue` 的 `columns()` 中继续保留现有机台基础列，并在操作列之前插入 12 个动态月份列。

动态列生成规则：

- 从前端当前日期计算最近 12 个历史月份，不含当前月。
- `label` 为 `YY-MM`，例如 `26-04`。
- `prop` 指向后端完整年月 key，例如 `monthStructureNameMap.2026-04`。
- 建议设置 `minWidth`，避免结构名称较多时列宽过窄。

前端只负责展示，不在前端做结构名称聚合。

## 错误与边界处理

- 当前页无机台数据时，不额外查询月份结构数据。
- 某机台某月份没有匹配结构时，对应单元格为空。
- `CX_MACHINE_CODE` 为空时忽略。
- `STRUCTURE_NAME` 为空时不参与拼接。
- 同一结构名称重复出现时只展示一次。
- 跨年月份使用完整年月作为数据 key，避免 `26-01` 和其它年份混淆。

## 验证范围

后端验证：

- 编译 `APS-Modules/aps-mp`。
- 编译 `Aps-BootUI`。
- 如已有相关测试条件，补充或运行当前页机台回填逻辑的单元测试。

前端验证：

- 构建前端。
- 检查 `formingCapacity/index.vue` 页面列顺序和标题。
- 确认下拉查询框仍保留 `filterable` 属性要求；本次不新增下拉框。

## 不包含范围

- 不修改机台基础分页条件。
- 不修改导入逻辑。
- 不默认修改导出逻辑；如需要导出最近 12 个月结构列，可作为后续需求单独补充。
