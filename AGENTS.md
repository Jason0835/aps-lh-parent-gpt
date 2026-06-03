# 项目编码规范
## 文件格式
utf-8 no bom
## 技术栈
- SpringCloud + Nacos + Gateway + Feign 请求链路：Vue → BootUI → Gateway → 后端微服务 所有服务注册至 Nacos，服务间通过 Feign 通信， 前端权限见BootUI
- 所有的@apixxx等和注释，必须使用中文。
- 如果要写plan,解释语言使用中文。
## 前端规范
- 前端开发规范见@APS-VUEUI/docs/前端规则.md

## 后端规范
- Date类的运算可以使用cn.hutool.core内的Date相关方法
- 有条件的话尽可能使用stream流来处理数据
- 导入业务数据importData见@docs/importdoc.md
- 遇到字段反显、导出补反显字段、非数据库字段需要反显时，必须先阅读并遵循@字段反显.md
- 使用LambdaQueryWrapper、LambdaUpdateWrapper
- 尽可能使用Lambda写法（例如：LambdaQueryWrapper、LambdaUpdateWrapper、LambdaQueryChainWrapper、LambdaUpdateChainWrapper），减少字符串字段名硬编码
- 框架已通过注解自动处理逻辑删除（isDelete字段），查询时不要手动拼接 `.and(w -> w.eq(::getIsDelete, ...).or().isNull(::getIsDelete))` 条件，直接使用简单的 LambdaQueryWrapper 即可，由框架自动过滤已删除数据
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
### 数据库相关：
- BaseEntity已经定义了 `id,createBy,createTime,updateBy,updateTime,isDelete,remark`，实体类不需要重复定义
- 非数据库字段必须添加 `@TableField(exist = false)`
- 不在数据库的字段要求反显时，参考@docs/字段反显.md
- 唯一性校验返回值：`UserConstants.NOT_UNIQUE = "1"` 不唯一，`UserConstants.UNIQUE = "0"` 唯一
- 如果要生成sql语，创建一个sql文件，放到@docs/sql

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
