# APS项目技术栈

**技术栈：SpringCloud + Nacos + Gateway + Feign  
**请求链路**：Vue → BootUI → Gateway → 后端微服务  
- 所有服务注册至 Nacos
- 服务间通过 Feign 通信
- 前端权限见BootUI

# APS项目开发规范总结

## 命名规范

### 后端命名规则：
- **实体**: `CxEntityName.java` 位于 `aps-cx-lh-api/src/main/java/com/zlt/aps/cx/entity/`
- **远程服务接口**: `xxxRemoteService.java` 
- **业务控制器**: `xxxController.java` 
- **服务接口**: `IxxxService.java`
- **服务实现**: `xxxImpl.java` 
- **UI控制器**: `xxxUIController.java` 

### 前端命名规则：
- **API接口**: `xxEntityName.js` 
- **主页面**: `index.vue`
- **新增/编辑弹窗**: `infoDialog.vue` 位于 `/components/`

## 继承关系

必须遵循的继承结构：
```
Controller  extends AbstractDocBizController<Entity>
Service    extends IDocService<Entity>
ServiceImpl extends AbstractDocService<Entity> implements ICxEntityNameService
UIController extends BaseUIController<Entity>
```

## 业务规则要求

### 数据库相关：
- BaseEntity已经定义了 `id,createBy,createTime,updateBy,updateTime,isDelete,remark`，实体类不需要重复定义
- 非数据库字段必须添加 `@TableField(exist = false)`
- 唯一性校验返回值：`UserConstants.NOT_UNIQUE = "1"` 不唯一，`UserConstants.UNIQUE = "0"` 唯一

### Excel导入导出：
- 所有导出字段必须添加 `@Excel` 注解
- `@Excel` 的 name 属性使用多语言key：`@Excel(name = "ui.data.column.cxEntityName.fieldName")`
- 字典字段需要指定 `dictType`: `@Excel(name = "...", dictType = "dict_type_code")`

### 搜索规则：
- 所有数据库字段都需要实现查询功能
- 编号/名称类字段使用模糊查询 (`like`)
- 日期时间使用区间查询，前端使用 `daterange`，后端使用 `between`
- 前端搜索条件按照需求放置，factoryCode一般放在最前面

### 前端要求：
- **所有下拉选择框必须添加 `filterable` 属性**支持可输入筛选
- factoryCode字典反显使用 `selectDictLabel(this.dict.type.biz_factory_name, value)`
- factoryCode列表中必须放在第一列
- companyCode冗余字段不在任何界面显示
- 多语言key格式：`ui.data.column.cxEntityName.fieldName`

### 唯一性规则：
- 新增、编辑、导入都需要校验唯一性
- 唯一性校验由service层实现 `checkUnique()` 方法
- 编辑时需要排除当前记录自身

### 导入功能：
- 导入弹窗必须支持"是否更新已经存在的数据"选择框，设置 `:updateSupport="true"`
- 需要校验关联数据（如成型机）是否存在
- 按照用户选择决定是跳过还是更新已存在数据

## 路径定位


## 多语言

### 后端：
- 文件位置: `jy_aps_admin/Aps-Common/aps-common-core/src/main/resources/i18n/apsui_zh_CN.properties`
- 格式: `ui.data.column.cxEntityName.fieldName=中文名称`

### 前端：
- 文件位置: `jy_aps_ui/src/lang/zh/ui_zh_CN.json`
- 格式: `"ui.data.column.cxEntityName.fieldName": "中文名称"`

## 本次xxx计划遵循规则验证
- ✅ 命名规范正确，所有文件遵循cx模块命名模式
- ✅ 继承关系正确：`AbstractDocBizController`, `IDocService`, `AbstractDocService`
- ✅ 添加了 `@Excel` 注解且name使用多语言key
- ✅ 所有下拉框添加了 `filterable`
- ✅ factoryCode放在列表第一列并正确字典反显
- ✅ companyCode不在界面显示
- ✅ 实现了唯一性校验（成型机台+精度类型+计划日期）
- ✅ 导入支持更新选项
- ✅ 日期使用区间查询，机台名称使用模糊查询
- ✅ 前后端多语言配置完整
