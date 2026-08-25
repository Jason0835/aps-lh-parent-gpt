# 项目编码规范
## 文件格式
utf-8 no bom
## 技术栈
- SpringCloud + Nacos + Gateway + Feign 请求链路：Vue → BootUI → Gateway → 后端微服务 所有服务注册至 Nacos，服务间通过 Feign 通信， 前端权限见BootUI
- 所有的@apixxx等和注释，必须使用中文。
- 如果要写plan,解释语言使用中文。
## 文档规范
- 文档编写规范见@docs/文档规范.md

## 前端规范
- 前端开发规范见@APS-VUEUI/docs/前端规则.md

## 后端规范
- Date类的运算可以使用cn.hutool.core内的Date相关方法
- 有条件的话尽可能使用stream流来处理数据
- 导入业务数据importData见@docs/importdoc.md
- 遇到字段反显、导出补反显字段、非数据库字段需要反显时，必须先阅读并遵循@字段反显.md
- 使用LambdaQueryWrapper、LambdaUpdateWrapper
- 尽可能使用Lambda写法（例如：LambdaQueryWrapper、LambdaUpdateWrapper、LambdaQueryChainWrapper、LambdaUpdateChainWrapper），减少字符串字段名硬编码
- 强制规则：框架已通过注解自动处理逻辑删除（`isDelete`/`delFlag` 字段），生成或修改查询代码时，除非用户特别说明，禁止手动追加逻辑删除条件。不要写 `wrapper.eq(Entity::getIsDelete, ApsConstant.DEL_FLAG_NORMAL)`、`wrapper.eq(Entity::getDelFlag, ApsConstant.DEL_FLAG_NORMAL)`、`.eq(BaseEntity::getIsDelete, 0)` 或 `.and(w -> w.eq(::getIsDelete, ...).or().isNull(::getIsDelete))` 这类条件；直接使用业务查询条件，由框架自动过滤已删除数据。
- 批量新增数据统一使用 `baseDao.saveBatch()` 方法保存，不要编写自定义的批量 insert SQL 到 mapper.xml
- 数值类型转 BigDecimal 统一使用 `BigDecimalUtils.valueOf()` 方法（空值自动返回 0），避免手写 `value != null ? BigDecimal.valueOf(value) : null` 或 `value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO`
- 调用类内部的私有方法时统一在调用前加 `this.` 前缀，例如 `this.loadCxSchedule(factoryCode, scheduleDate)`
- 所有 `if`、`else`、`for`、`while` 等控制语句必须使用大括号 `{}`，且左大括号不换行、右大括号独立一行。禁止单行写法
- 编写或重构 Java 代码时，对于集合的过滤、映射、收集等操作，优先使用 Stream API 替代传统 for 循环，使代码更简洁易读。例如：
```java
// 反例：for 循环
List<String> result = new ArrayList<>();
for (MdmConstructionInfo info : list) {
    String code = info.getCordSpec();
    if (code != null && !code.trim().isEmpty()) {
        result.add(code.trim());
    }
}

// 正例：Stream API
List<String> result = list.stream()
        .map(MdmConstructionInfo::getCordSpec)
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .distinct()
        .collect(Collectors.toList());
```
- **动态字段访问**：实体类中批量读写类似命名规则的字段（如 class1PlanQty ~ class6PlanQty、class1Sequence ~ class6Sequence、class1Analysis ~ class6Analysis 等）时，禁止使用逐个字段的 switch/case 或 if-else 硬编码获取/设置值，统一通过实体的 `getFieldValueByFieldName(String)` / `setFieldValueByFieldName(String, Object)` 方法配合字段名模板常量（如 `String.format("class%dPlanQty", index)`）动态访问。
- **国际化规则**：所有返回给前端的信息（包括错误提示、校验失败提示等）必须使用 `I18nUtil.getMessage()` 抽取国际化 key，禁止硬编码中文/英文/越南语字符串直接返回前端。i18n key 统一以模块前缀命名（如 `ui.dj.*`），并同步更新 `apsui.properties`、`apsui_zh_CN.properties`、`apsui_en_US.properties`、`apsui_vi_VN.properties` 四个语言文件。
  - **占位符规范**：properties 文件中使用 `{0}`、`{1}`、`{2}` 等格式作为占位符（`java.text.MessageFormat` 风格），代码中必须使用 `MessageFormat.format()` 进行参数替换，禁止使用 `String.format()`。
### 注释规范
- 优先重要,主要逻辑方法需加注释
- 注释用中文，尽可能的详细
- 实体类的字段需要加注释
- 方法需要加注释，方法参数需要加注释
- 方法内部逻辑需要加注释

### 必须遵循的继承结构：
```
Controller  extends AbstractDocBizController<Entity>
Service    extends IDocService<Entity>
ServiceImpl extends AbstractDocService<Entity> implements ICxEntityNameService
UIController extends BaseUIController<Entity>
```
### bootui 后端规范：
- 禁止使用单字母缩写作为变量名或方法参数名（如 `q`、`e`、`w`），必须使用有业务含义的完整命名（如 `queryVO`、`entity`、`wrapper`）。

### 数据库相关：
- BaseEntity已经定义了 `id,createBy,createTime,updateBy,updateTime,isDelete,remark`，实体类不需要重复定义
- 非数据库字段必须添加 `@TableField(exist = false)`
- 不在数据库的字段要求反显时，参考@docs/字段反显.md
- 唯一性校验返回值：`UserConstants.NOT_UNIQUE = "1"` 不唯一，`UserConstants.UNIQUE = "0"` 唯一
- 如果要生成sql语句，创建一个sql文件，放到@docs/sql
- SQL 文件规范：
  - 使用 MySQL 语法，不使用 Oracle 语法（如 `NVARCHAR2` → `VARCHAR`，`COMMENT ON COLUMN` → 行内 `COMMENT`）
  - 表名、字段名统一使用反引号（`` ` ``）包裹
  - 文件名格式：`yyyyMMdd_描述.sql`
  - 文件内包含表名说明和日期头注释

### Excel导入导出：
- 所有导出字段必须添加 `@Excel` 注解
- `@Excel` 的 name 属性使用多语言key：`@Excel(name = "ui.data.column.cxEntityName.fieldName")`
- 字典字段需要指定 `dictType`: `@Excel(name = "...", dictType = "dict_type_code")`
## 多语言

### 后端：
- 文件位置: `Aps-Common/aps-common-core/src/main/resources/i18n/`
- 语言文件:
  - `apsui.properties` (默认)
  - `apsui_zh_CN.properties` (中文简体)
  - `apsui_en_US.properties` (英文)
  - `apsui_vi_VN.properties` (越南语)
- 格式: `ui.data.column.cxEntityName.fieldName=中文名称`
- 新增多语言时，需同步更新以上所有语言文件

### 前端：
- Vue 多语言: `APS-VUEUI/src/lang/`
  - 中文: `zh/ui_zh_CN.json`
  - 英文: `en/ui_en_US.json`
  - 越南语: `vi/ui_vi_VN.json`
- 格式: `"ui.construction.carcassRawMaterialList": "胎胚原材料清单"`

### Windows PowerShell 命令规范
- 不直接拼接复杂 PowerShell 命令。
- 涉及 `rg`、`mvn`、多参数、正则、引号、逗号、管道符时，优先调用 `scripts/` 下的固定脚本。
- `rg` 残留检查优先使用 `--fixed-strings`，复杂正则必须拆成多条简单检查。
- Maven 多模块测试必须使用：
  `mvn -pl <module> -am test -DfailIfNoTests=false`
- 如果当前模块依赖 `tm-api`、`api`、`common` 等上游模块，必须带 `-am`。
- 仓库存在无测试模块时，必须带 `-DfailIfNoTests=false`。
- 命令失败时，先判断是 shell 参数解析问题、Maven 模块依赖问题，还是代码编译/测试问题，不要直接认定代码失败。
- **代码修改后无需执行 Maven 编译验证**，由用户自行处理编译问题。

### 胎面部分调整
- 有涉及到胎面业务调整的部分，都需要同步更新到详设文档：docs/tm/tm_schedule_detailed_design.md
- 扩展 JSON 场景测试框架和断言，新增对应的测试场景
- 成型排程与施工信息（T_CX_SCHEDULE_RESULT ↔ T_MDM_CONSTRUCTION_INFO）的版本匹配由参数 `TM_VERSION_MATCH_MODE` 控制：`RECIPE`（默认）按 CD90 式 `(EMBRYO_CODE, CLASSn_RECIPE_NO)` 逐班解析（`TmAutoScheduleDataLoadService.loadFormingDemandTasksByRecipe`，跳过示方书为空的班次）；`BOM` 模式（`loadFormingDemandTasksByBom`）同一胎胚按 `TREAD_CODE` 分组择一取施工版本（LATERAL + ROW_NUMBER，优先 `BOM_DATA_VERSION` 匹配，否则取最新有效记录），避免"仅按胎胚关联致多版本变体满量重复展开"及"严格版本等值 join 在 `BOM_DATA_VERSION` 为空时归 0"两种极端。库存预测 `TmEngineInventoryPredictMapper.selectFirstShiftDemandRows` 与主流程 `selectFormingDemandRows` 同口径择一；`selectFirstShiftDemandRowsByRecipe` 为 RECIPE 模式。多胎面共用同一胎胚时各胎面独立成任务，`BOM_DATA_VERSION` 为空无法精确归属工单到胎面时建议补全该字段。
- 自动排程真实库测试数据制作规范见 @docs/tm/testdata/胎面自动排程测试数据制作规范.md（造数据前必读：`DESCRIBE` 核对表 NOT NULL 无默认字段如 `T_MDM_CONSTRUCTION_INFO.MES_MATERIAL_CODE`、设 `TM_VERSION_MATCH_MODE=BOM`、为测试工厂配 `T_TM_SHIFT_CONFIG`、`TM_FORMING_SHIFT_OFFSET` 与成型需求所在 CLASS 配套、前日链尾放 class6 行级 glue/base/mouth）。现有 `docs/tm/testdata/tm_auto_plan_scene_data.sql` 在本库版本已不可直接运行（缺上述字段/表/参数），勿照抄。

### 胎侧部分调整
- 胎侧排程详设文档：docs/tc/tc_schedule_detailed_design.md，基于胎面详设方法论与 `07-APS详细设计-胎侧.xlsx` 整理；胎侧与胎面共享通用排程引擎 `Aps-Common/aps-engine-common`，差异逻辑放在 `APS-Modules/aps-tc` / `Aps-Api/tc-api` 的 `com.zlt.aps.tc.engine` 包下，不通过修改胎面实现兼容胎侧。
- 有涉及到胎侧业务调整的部分，都需要同步更新到详设文档：`docs/tc/tc_schedule_detailed_design.md`,非业务部分不可新增到详设文档，避免文档臃肿
- 胎侧独有业务点（与胎面差异，落地和调整时需保留）：整车率使用胎侧参数 `TC_VEHICLE_RATE`（胎面使用独立的 `TM_VEHICLE_RATE`，均默认1）、单班最大可排量取 `T_TC_MACHINE_INFO.MAX_CAPACITY`（无效时固定回退5500米，不读取统一容量参数）、库存最低保证班数 `TC_MIN_STOCK_CLASS`（默认3班，胎面为1班）、胎侧+垫胶共用机台 `T_TC_DJ_SHARED_MACHINE` 班次错开约束、机台选择"优先排满一台"口径。
- 参数与表名统一 `TC_` / `T_TC_*` 前缀，班次映射、版本匹配 `TC_VERSION_MATCH_MODE`、成型偏移 `TC_FORMING_SHIFT_OFFSET` 等与胎面 `TM_*` 对齐。

## 数据库查询与排查规范

- 排查、分析硫化排程问题时，数据库证据一律优先使用 db-mcp 插件工具：`query`（只读查询）、`list_tables`（列表）、`describe_table`（表结构）、`list_databases`（库清单）、`list_connections`（列出连接）、`use_connection`（切换连接）、`execute`（写操作，谨慎使用）；不要手写 shell 的 mysql 命令或临时 JDBC helper。
- 当前会话没有 db-mcp 工具时，先挂载插件再继续查询：在输入框引用 `[@db-mcp](plugin://db-mcp@personal)`，或打开 Sources → Use plugins 勾选 db-mcp；仅当 db-mcp 不可用或无法覆盖（如跨库多语句、特殊 mysql 参数）时才回退到 mysql CLI。
- 默认连接为 `local`（本机 127.0.0.1:3307/apslh）；需要连其它库时用 `use_connection` 或工具里的 `connection` 参数指定。
- 查询不熟悉的表前先用 `describe_table` 确认真实字段；排查证据链统一联合结果表、未排表、换模表和过程日志，按批次 + 物料 + 产品状态窄查再扩大。
