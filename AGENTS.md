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
- 所有 `if`、`else`、`for`、`while` 等控制语句必须使用大括号 `{}`，且左大括号不换行、右大括号独立一行。禁止单行写法，例如 `if (xxx) return yyy;` 必须写成：
  ```
  if (xxx) {
      return yyy;
  }
  ```
- **国际化规则**：所有返回给前端的信息（包括错误提示、校验失败提示等）必须使用 `I18nUtil.getMessage()` 抽取国际化 key，禁止硬编码中文/英文/越南语字符串直接返回前端。i18n key 统一以模块前缀命名（如 `ui.dj.*`），并同步更新 `apsui.properties`、`apsui_zh_CN.properties`、`apsui_en_US.properties`、`apsui_vi_VN.properties` 四个语言文件。
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

### 胎面部分调整
- 有涉及到胎面业务调整的部分，都需要同步更新到详设文档：docs/tm/tm_schedule_detailed_design.md
- 扩展 JSON 场景测试框架和断言，新增对应的测试场景