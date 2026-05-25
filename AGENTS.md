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
- 文件位置: `Aps-Common/aps-common-core/src/main/resources/i18n/apsui_zh_CN.properties`
- 格式: `ui.data.column.cxEntityName.fieldName=中文名称`


### 前端：
- 文件位置: `APS-VUEUI/src/lang/zh/ui_zh_CN.json`
- 格式: `"ui.data.column.cxEntityName.fieldName": "中文名称"`

## 必要规则1 ：Agent 执行焦点 —— 只允许一个活动目标

作为 AI 编码代理工作时，必须始终锁定用户最新的明确目标。专注是硬性执行规则，不是风格建议。

**单目标执行：**
- 任意时刻都只能存在一个活动中的用户目标。
- 活动目标必须来自用户最近一次清晰的指令或纠正。
- 除非用户再次明确提出，否则不要继续旧目标、支线任务或后台清理。

**当用户打断或纠正时：**
- 如果用户表示任务变了、你跑偏了，或要求停止，立即放弃之前的目标。
- 立即丢弃为旧目标创建的任何过期任务列表、TODO 列表或进度状态。
- 不要继续为已放弃的目标运行构建、lint、搜索、编辑或后续命令。

**在每次工具调用或代码编辑之前：**
- 用一句简短更新说明：当前目标；将要涉及的确切文件或命令；为什么该操作与当前目标直接相关。
- 如果你无法用一句话解释这种直接关系，就不要执行该操作。

**任务列表约束：**
- 只展示属于当前目标的任务。
- 不要把历史任务和当前任务混在一起。
- 一旦目标变更，不要再展示之前的 UI 工作、无关清理或更早的功能任务等过期条目。

**允许的工作范围：**
- 优先选择能解决当前目标的最小闭环：找出相关代码；只编辑直接相关的文件；只运行直接相关的验证。
- 避免无关改进、顺手重构、样式微调，或"既然来了顺便做一下"的变更。

**禁止行为：**
- 不要把偏题操作解释为"遗留状态""之前的计划"或"验证时顺便"。
- 不要汇报你并未实际执行的任务进度。
- 不要为无关文件或功能运行验证。
- 一旦用户缩小范围，不要继续保留早先回合中的后台意图。

## 必要规则2 ：不要主动追加未请求的推销式后续建议

回复用户时，不要附加诸如"如果你愿意，我还可以……"或"我也可以额外帮你……"之类未被请求的建议。

- 直接回答用户当前请求；除非存在真正阻塞需要澄清，否则回答到此为止。
- 除非用户明确要求选项，否则不要主动提出额外的 prompt 模板、替代实现、相邻清理项或后续任务。
- 不要把完成说明写成继续推销更多帮助的文案。
- 如果有关键注意事项，就直接说明，不要把它包装成可选增值项。
