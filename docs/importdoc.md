# 导入开发规范

本文档用于约束业务数据 `importData` 的实现方式。新增或调整导入功能时，优先按本文档顺序检查，避免遗漏必填校验、Excel 内重复校验、业务校验、主数据校验、字段反显、唯一性校验和更新插入边界。

## 一、导入链路

标准导入链路如下：

```text
Vue 页面
  -> BootUI Controller
  -> Feign RemoteService
  -> 后端微服务 Controller
  -> ServiceImpl#importData(List<Entity> list, boolean updateSupport, Long importLogId)
```

各层职责：

| 层级 | 职责 |
| --- | --- |
| Vue | 配置上传地址，触发文件上传 |
| BootUI | 接收文件，解密文件，组装 `ImportContext`，调用 Feign |
| Feign | 透传 `ImportContext` 和 `updateSupport` 到后端微服务 |
| 微服务 Controller | 复用基类导入解析流程，不写业务校验 |
| ServiceImpl | 实现具体业务导入规则 |

## 二、前端配置

前端页面可参考同类结构配置 `uploadUrl`：

```vue
<import-upload
  uploadUrl="/lh/Xxxx/importData"
/>
```

注意事项：

- `uploadUrl` 应与 BootUI Controller 的导入地址一致。
- 如果页面存在下拉选择框，必须添加 `filterable` 属性。
- `factoryCode` 列表中必须放在第一列。
- `companyCode` 冗余字段不在任何界面显示。

## 三、BootUI 导入入口

BootUI Controller 负责接收前端文件、处理文件加密、组装 `ImportContext`，然后调用远程服务。

```java
@PostMapping("/importData")
@ResponseBody
@ApiOperation("数据导入")
@Override
public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
    byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

    ImportContext context = new ImportContext();
    context.setImportFilePath(this.importFilePath);
    context.setFunctionName(this.getFunctionName());
    context.setProcedureCode(this.getProcedureCode());
    context.setOriFileName(file.getOriginalFilename());
    context.setFileBytes(data);

    return iXxxRemoteService.importData(context, updateSupport);
}
```

注意事项：

- `@ApiOperation` 必须使用中文。
- `updateSupport` 应按前端传入值透传，不要固定写死，除非业务明确要求。
- BootUI 不写业务字段校验，业务校验统一放在微服务 `ServiceImpl`。

## 四、Feign 接口

Feign 接口负责透传导入上下文和是否更新已存在数据的标记。

```java
/**
 * 导入数据
 *
 * @param importContext 导入上下文
 * @param updateSupport 已存在记录是否更新
 * @return 导入结果
 */
@ApiOperation("导入数据")
@PostMapping("/xxx/importData/{updateSupport}")
AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport);
```

注意事项：

- Feign 的路径参数和微服务 Controller 保持一致。
- 如果路径中使用 `{updateSupport}`，Feign 方法参数使用 `@PathVariable("updateSupport")`。
- 注释和 `@ApiOperation` 必须使用中文。

## 五、微服务 Controller

微服务 Controller 只负责暴露导入接口并调用基类导入流程。文件解析、日志、模板处理等基础能力由基类处理，业务导入规则写在 `ServiceImpl`。

```java
/**
 * 导入数据
 *
 * @param importContext 导入上下文
 * @param updateSupport 已存在记录是否更新
 * @return 导入结果
 */
@Log(title = "ui.data.column.xxx.modelName", businessType = BusinessType.IMPORT)
@ApiOperation("导入数据")
@PostMapping("/importData/{updateSupport}")
@Override
public AjaxResult importData(@RequestBody ImportContext importContext,
                             @PathVariable("updateSupport") boolean updateSupport) throws Exception {
    return super.importData(importContext, updateSupport);
}
```

注意事项：

- Controller 继承结构必须符合项目规范：`Controller extends AbstractDocBizController<Entity>`。
- Controller 不直接实现逐行导入逻辑。
- `@Log` 的 `title` 使用多语言 key。

## 六、ServiceImpl 标准流程

`ServiceImpl#importData` 是导入业务规则的唯一实现位置。推荐整体流程如下：

```java
/**
 * 导入数据
 *
 * @param list 导入数据
 * @param updateSupport 已存在记录是否更新
 * @param importLogId 导入日志 ID
 * @return 导入结果
 */
@Override
public AjaxResult importData(List<Xxx> list, boolean updateSupport, Long importLogId) {
    int successNum = 0;
    int failureNum = 0;
    StringBuilder successMsg = new StringBuilder();
    StringBuilder failureMsg = new StringBuilder();

    // 第一轮：注解必填校验、Excel 内重复校验
    ImportExcelValidatedUtils.validated(list);
    ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,this.getCheckUniqueFields().toArray(new String[0]));

    // 过滤第一轮已经失败的数据，后续只处理可继续校验的数据
    List<Xxx> canCheckList = list.stream()
            .filter(Xxx::getIsCan)
            .collect(Collectors.toList());

    // 第二轮前：批量预取主数据、字典、已存在数据等
    // Map<String, MdmXxx> xxxMap = ...

    for (Xxx item : canCheckList) {
        boolean isCan = true;
        StringBuilder rowMsg = new StringBuilder();

        // 第二轮：业务字段校验、关联主数据校验、字段反显
        // 如果有错误，只修改 isCan 和 rowMsg，不要立即进入插入或更新逻辑

        if (!isCan) {
            failureNum++;
            // 记录失败信息
            continue;
        }

        // 唯一性校验、updateSupport 判断、更新或插入
        successNum++;
    }

    return AjaxResult.success();
}
```

实际项目中应按已有同类模块的返回消息、导入日志、失败行标记方式实现，不要只照搬示例的返回值。

## 七、两轮校验规则

导入校验必须分两轮执行。

### 第一轮：基础格式校验

第一轮只做基础校验：

- `ImportExcelValidatedUtils.validated(...)`：执行实体字段上 `@ImportExcelValidated(required = true)` 等注解校验。
- `ImportExcelValidatedUtils.validatedRepeat(...)`：执行 Excel 内重复校验。

第一轮规则：

- 第一轮失败的行必须直接标记为不可继续处理。
- 第一轮失败的行后续不再参与业务校验。
- 已通过 `@ImportExcelValidated(required = true)` 声明的必填字段，第二轮业务校验阶段不要重复写“判空后立即失败”的逻辑。
- 如果必填字段还有额外业务规则，例如编码格式、长度范围、关联主数据存在性，则第二轮可以继续校验这些额外规则。

### 第二轮：业务校验

第二轮只处理第一轮通过的数据，主要包括：

- 字段编码是否合法。
- 关联主数据是否存在，例如工厂、组织、部门、机台、物料、规格等。
- 字典值是否存在或是否需要反显。
- 唯一性规则是否满足。
- 更新已存在数据时是否允许覆盖。
- 非数据库字段是否需要反显。

第二轮规则：

- 每行使用 `isCan` 收集所有业务字段问题。
- 不要遇到第一个业务错误就立刻 `continue`，应尽量收集当前行所有业务错误。
- 当前行存在业务错误时，统一 `failureNum++` 后 `continue`。
- 当前行失败后，不允许继续进入 `checkUnique(...)`、更新已存在数据、插入数据等后续逻辑。

## 八、批量预取规则

导入校验需要验证主数据或已存在数据时，必须批量预取，禁止在逐行循环中每行查询一次数据库。

常见需要批量预取的数据：

- 工厂、组织、部门。
- 机台、产线、库位、物料、规格、模具等业务主数据。
- 字典数据。
- 已存在的业务数据，用于唯一性校验和 `updateSupport` 更新判断。

推荐做法：

1. 从导入列表中提取对应字段值。
2. 过滤空值并去重。
3. 按 1000 条一批查询数据库。
4. 将查询结果组装成 `Map`。
5. 逐行校验时只从 `Map` 中匹配。

示例：

```java
List<String> machineCodeList = canCheckList.stream()
        .map(Xxx::getMachineCode)
        .filter(StringUtils::isNotBlank)
        .distinct()
        .collect(Collectors.toList());

Map<String, MdmMachine> machineMap = CollUtil.split(machineCodeList, 1000).stream()
        .flatMap(batch -> mdmMachineService.list(new LambdaQueryWrapper<MdmMachine>()
                .in(MdmMachine::getMachineCode, batch)).stream())
        .collect(Collectors.toMap(MdmMachine::getMachineCode, Function.identity(), (oldValue, newValue) -> oldValue));
```

注意事项：

- 查询条件优先使用 `LambdaQueryWrapper`、`LambdaUpdateWrapper` 等 Lambda 写法。
- `Map` 的 key 必须和逐行校验使用的字段保持一致。
- 如果存在多字段唯一定位，key 可使用业务上稳定的组合 key。
- 批量预取应在逐行循环前完成。

## 九、唯一性、更新和插入

唯一性校验返回值遵循项目常量：

```java
UserConstants.NOT_UNIQUE = "1"; // 不唯一
UserConstants.UNIQUE = "0";     // 唯一
```

处理顺序：

1. 当前行基础校验通过。
2. 当前行业务校验通过。
3. 执行唯一性校验或从已存在数据 `Map` 中判断是否存在。
4. 如果数据已存在：
   - `updateSupport = true` 时执行更新。
   - `updateSupport = false` 时记录失败。
5. 如果数据不存在，执行新增。

注意事项：

- 业务校验失败的数据不能进入唯一性、更新或插入逻辑。
- 更新时只更新业务允许覆盖的字段。
- 新增和更新都应补齐必要的反显字段、审计字段和业务默认值。

## 十、字段反显要求

遇到以下场景，必须先阅读并遵循 `docs/字段反显.md`：

- 导入时需要根据编码反显名称。
- 导出时需要补充反显字段。
- 非数据库字段需要展示或导出。
- 字典字段需要显示标签。

常见规则：

- `factoryCode` 字典反显使用 `selectDictLabel(this.dict.type.biz_factory_name, value)`。
- 非数据库字段必须添加 `@TableField(exist = false)`。
- 导出字段必须添加 `@Excel` 注解。
- `@Excel` 的 `name` 属性使用多语言 key，例如 `@Excel(name = "ui.data.column.cxEntityName.fieldName")`。
- 字典字段需要指定 `dictType`，例如 `@Excel(name = "...", dictType = "dict_type_code")`。

## 十一、多语言和注释

多语言 key 格式：

```text
ui.data.column.cxEntityName.fieldName
```

后端多语言文件：

```text
jy_aps_admin/Aps-Common/aps-common-core/src/main/resources/i18n/apsui_zh_CN.properties
```

前端多语言文件：

```text
jy_aps_ui/src/lang/zh/ui_zh_CN.json
```

注释要求：

- `@ApiOperation`、接口注释、字段注释必须使用中文。
- 实体类字段需要加注释。
- 导入逻辑中复杂业务校验建议添加中文注释，说明为什么这样校验。

## 十二、实现检查清单

新增或修改导入功能完成后，按以下清单自查：

- [ ] Vue 页面已配置正确的 `uploadUrl`。
- [ ] BootUI 已组装 `ImportContext` 并透传 `updateSupport`。
- [ ] Feign 路径和微服务 Controller 路径一致。
- [ ] 微服务 Controller 只调用 `super.importData(...)`。
- [ ] ServiceImpl 实现了 `importData(List<Entity> list, boolean updateSupport, Long importLogId)`。
- [ ] 第一轮先执行 `ImportExcelValidatedUtils.validated(...)`。
- [ ] 第一轮再执行 `ImportExcelValidatedUtils.validatedRepeat(...)`。
- [ ] 第一轮失败的行没有参与第二轮业务校验。
- [ ] 第二轮业务校验使用 `isCan` 收集当前行所有业务错误。
- [ ] 第二轮失败的行统一 `failureNum++` 后 `continue`。
- [ ] 业务失败行没有进入唯一性校验、更新或插入逻辑。
- [ ] 主数据、字典、已存在数据已批量预取。
- [ ] 循环内没有逐行查询数据库。
- [ ] 查询和更新优先使用 Lambda Wrapper。
- [ ] 字段反显已按 `docs/字段反显.md` 处理。
- [ ] 导出字段已添加 `@Excel` 注解和多语言 key。
- [ ] 注释、`@ApiOperation`、多语言文案均为中文。
