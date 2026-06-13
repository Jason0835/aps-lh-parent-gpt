# 胎面排程重构详细设计说明

## 1. 文档说明

本文档用于指导胎面排程模块重构落地，统一业务目标、数据模型、自动排程流程、人工干预规则、MES 发布机制、班次建模、码值规范以及可解释性与问题排查设计。

本次设计采用“排程结果 + 结果解释 + 调度日志 + 基础资料”的模型。基础资料表名以用户提供的 `tm_schedule_v1.sql` 为准；排程核心表为重构新增表。

## 2. 设计原则

- 底层采用“六班一行”横向存储模型，一班次字段为 CLASS1~CLASS6 列族，不再使用纵向单班次模型。
- 不单独建设排程方案表，同一天多次排程默认先删除旧结果，再写入新 `batch_no` 作为当前结果。
- 未排任务仍写入 `T_TM_SCHEDULE_RESULT`，未排原因和规则证据写入 `T_TM_SCHEDULE_RESULT_EXPLAIN`。
- 任务解释信息统一承载计划量计算、库存依据、候选机台、规则命中、未排证据，不再拆多张诊断表。
- 基础资料字段如与用户 SQL 语义重叠，统一使用用户 SQL 中已有字段名。
- 新增业务启停字段统一使用 `enable_status`，字典为 `biz_yes_no`，`0` 否、`1` 是。
- `is_delete` 表示逻辑删除，`enable_status` 表示业务是否启用，两者不等价。

## 3. 核心业务表

### 3.1 `T_TM_SCHEDULE_RESULT`

作用：排程结果主表，采用“六班一行”横向模型。一条记录表示一个机台-胎面组合在当天的六个班次生产任务；已排和未排任务都存放在此表。

关键字段：

- `id`：主键ID
- `batch_no`：批次号
- `order_no`：工单号
- `schedule_date`：排程日期
- `machine_code`：机台编码，关联 `T_TM_MACHINE_INFO.machine_code`，未排任务允许为空
- `tread_code`：胎面编码
- `glue_code`：主胶料编码
- `whole_glue_code`：整条胶料组合编码
- `glue_seq`：胶料顺序
- `glue_group_code`：已删除，引擎通过 `glue_seq` 关联 `T_TM_GLUE_GROUP_ORDER` / `T_TM_GLUE_ORDER` 获取分组信息
- `mouth_plate_code`：口型板编码
- `class1_sequence`~`class6_sequence`：六班顺序
- `class1_plan_qty`~`class6_plan_qty`：六班计划量
- `class1_finish_qty`~`class6_finish_qty`：六班完成量
- `class1_analysis`~`class6_analysis`：六班原因分析
- `release_status`：发布状态，统一使用字典 `IS_RELEASE`（`0` 未发布，`1` 已发布，`2` 发布失败，`3` 发布中，`4` 超时失败，`5` 待发布）
- `data_source`：数据来源
- `tail_flag`：业务标识，使用 `biz_yes_no`
- 状态/解释类字段（`task_status`、`assign_status`、`unplanned_reason_code/desc`、`manual_locked_flag`、`sequence_lock_flag`、`force_change_flag`）已移至 `T_TM_SCHEDULE_RESULT_EXPLAIN`
- `origin_result_id`、`current_result_version`：已删除，无实际业务场景
- `sys_analysis`、`hand_analysis`：已删除，各班次原因分析由 `class{N}_analysis` 承载

### 3.3 `T_TM_SCHEDULE_RESULT_EXPLAIN`

作用：保存任务当前有效解释信息，用于回答“计划量如何得出、候选机台如何筛选、为什么未排上”。

关键字段：

- `result_id`：结果ID，关联 `T_TM_SCHEDULE_RESULT.id`
- `batch_no`：批次号
- `base_demand_qty`：基础需求量
- `loss_add_qty`：损耗补偿量
- `stock_deduct_qty`：库存抵扣量
- `last_shift_supply_qty`：上班覆盖量
- `month_surplus_deduct_qty`：月剩余抵扣量
- `tool_limit_adjust_qty`：工装约束调整量
- `min_start_adjust_qty`：最小起排补正量
- `tail_round_adjust_qty`：收尾取整补正量
- `capacity_adjust_qty`：产能均衡补正量
- `final_plan_qty`：最终计划量
- `calc_formula_desc`：计划量计算公式说明
- `stock_qty`、`plan_stock_qty`、`supply_hours`、`coverage_shift_count`：库存测算依据
- `rule_hit_json`：命中规则明细，记录参数编码、参数值、规则表来源、是否使用默认值
- `candidate_machine_json`：候选机台明细，记录候选机台、过滤原因、排序和评分
- `machine_select_reason`：最终选机说明
- `assign_status`、`unplanned_reason_code`、`unplanned_reason_desc`、`unplanned_evidence_json`：未排解释
- `task_status`：任务状态（已从 RESULT 移入）
- `manual_locked_flag`、`sequence_lock_flag`、`force_change_flag`：引擎行为约束标识（已从 RESULT 移入），均使用 `biz_yes_no`
- `sys_analysis`、`warning_msg`、`error_msg`：系统分析、告警和异常信息

说明：参数表不做版本号，任务解释表记录当前有效解释和本次最终诊断信息，不承担完整事件历史追溯。

## 4. 基础资料表

### 4.1 `T_TM_MACHINE_INFO`

作用：胎面机台基础资料。核心字段包括 `machine_code`、`machine_name`、`max_capacity`、`open_shift_code`、`machine_status`、`shift_code`。

说明：若 `open_flag = '0'`，自动排程不可在该日期该班次生成任务；

### 4.2 `T_TM_MACHINE_MAINTENANCE`

作用：机台维修/停机计划。用于自动排程时扣减机台可用时间或产能。

核心字段：

- `machine_code`：机台编码，关联 `T_TM_MACHINE_INFO.machine_code`
- `stop_start_time`：停机开始时间
- `stop_end_time`：停机结束时间（停机时长由后台计算，单位秒）

### 4.3 `T_TM_MACHINE_SPEED`

作用：维护机台生产速度，用于根据胎面和机台估算任务耗时。

核心字段：`machine_code`、`tread_code`、`product_speed`。

### 4.4 `T_TM_MOUTH_PLATE`

作用：维护口型板与机台关系。

核心字段：`mouth_plate_code`、`machine_code`、`plate_status`。

### 4.5 `T_TM_SPECIFY_MACHINE`

作用：维护胎面定点机台和禁排机台规则。

保留用户字段：

- `tread_code`
- `machine_code`
- `job_type`：作业类型，表示指定机台或禁排机台

- `priority`：同胎面多个规则时排序
- `enable_status`：是否启用，字典 `biz_yes_no`，`0` 否、`1` 是

自动排程读取条件：`is_delete = 0 AND enable_status = '1'`。

### 4.6 `T_TM_GLUE_MACHINE_REAL`

作用：维护胶料与可投机台关系，用于候选机台筛选。

保留用户字段：

- `glue_code`
- `base_glue_code`
- `machine_code`
- `machine_name`
- `shift_code`

补充字段：

- `priority`：胶料可投多机台时排序
- `allow_flag`：是否允许投产，字典 `biz_yes_no`
- `enable_status`：是否启用，字典 `biz_yes_no`

自动排程读取条件：`is_delete = 0 AND enable_status = '1'`；若 `allow_flag = '0'`，则作为禁止关系处理。

### 4.7 `T_TM_GLUE_GROUP_ORDER` 与 `T_TM_GLUE_ORDER`

作用：维护胶料组顺序和胶料顺序，用于同胶料连续生产、胶料优先级排序和机台任务链排序。

### 4.8 `T_TM_LOSS_SETTING`

作用：维护胎面损耗率。

主要字段：

- `tread_code`
- `machine_code`
- `loss_rate`

- `setting_level`：配置层级，用于区分规格级、机台级、默认级
- `priority`：同层级多条配置时排序
- `enable_status`：是否启用，字典 `biz_yes_no`

自动排程读取条件：`is_delete = 0 AND enable_status = '1'`。

### 4.9 `T_TM_CURL_ROLL`

作用：维护胎面卷曲长度。

主要字段：

- `tread_code`
- `curl_length`

### 4.10 `T_TM_PARAMS`

作用：维护胎面排程参数。

保留用户字段：

- `param_code`
- `param_name`
- `param_value`
- `default_value`
- `regular_expression`
- `error_tips`

- `param_group`：参数分组
- `value_type`：参数值类型
- `enable_status`：是否启用，字典 `biz_yes_no

自动排程读取条件：`is_delete = 0 AND enable_status = '1'`。

建议参数编码：

- `TM_MIN_START_QTY`：最小起排量
- `TM_TOOL_TOTAL_QTY`：工装数量或工装总量
- `TM_ADD_ROLL_QTY`：补卷数量
- `TM_TAIL_ROUND_RULE`：收尾取整规则
- `TM_NORMAL_ROUND_RULE`：常规取整规则
- `TM_LARGE_SMALL_THRESHOLD`：大小批量阈值
- `TM_STOCK_GUARD_SHIFT_COUNT`：库存保障班次数
- `TM_ROLLING_SHIFT_COUNT`：局部滚动重算班次数，默认 3
- `TM_MAX_LOOKAHEAD_SHIFT_COUNT`：需求前瞻班次数
- `DEMAND_QTY_CALCULATE_TYPE`：需求量计算类型，1=算法1,2=算法2

### 4.11 `T_TM_STOCK`

作用：维护胎面库存。自动排程通过 `stock_date + tread_code` 获取库存数量、不良数量和调整数量。

## 5. 班次建模

本次设计采用六班模型。班次编码、班次名称这类稳定码值由字典或码值说明维护；具体排程日期的开班状态、计划开始时间、计划结束时间统一由 `T_TM_SHIFT_CONFIG` 承载。

### 5.1 班次字典

作用：维护稳定班次码值和中文展示，例如 `MORNING` 早班、`MIDDLE` 中班、`NIGHT` 夜班。

说明：班次字典只负责码值和展示名称，不参与计算具体日期的实际时间窗。

### 5.2 `T_TM_SHIFT_CONFIG`

作用：维护某个日期的班次实例、开班情况和实际排程时间窗。六班横向模型中班次时间窗由 `T_TM_SHIFT_CONFIG` 统一管理，`T_TM_SCHEDULE_RESULT` 不再冗余存储班次起止时间。前端展示和排程计算时按 `factory_code + shift_code + shift_order` 关联日历获取实际时间窗。

关键字段：

- `factory_code`：工厂编码
- `shift_code`：班次编码，来源于班次字典
- `shift_name`：班次名称，可由字典初始化冗余，便于导出和页面展示
- `shift_order`：班次顺序，用于六班横向展示和任务链排序
- `plan_start_time`：计划开始时间，格式 `HH:mm:ss`
- `plan_end_time`：计划结束时间，格式 `HH:mm:ss`
- `cross_day_flag`：是否跨天，字典 `biz_yes_no`
- `open_flag`：是否开班，字典 `biz_yes_no`

说明：
- 跨天班次以 `plan_start_time` 和 `plan_end_time` 的实际时间为准。
- 唯一约束：`uk_tm_shift_config_factory_shift(factory_code, shift_code, shift_order, is_delete)`，同一工厂、同一班次编码、同一顺序只能存在一条未删除记录。

## 6. 自动排程引擎设计

自动排程拆成以下服务，禁止继续堆在单个大 `ServiceImpl` 中：

- `TmPlanBootstrapService`：生成 `batch_no`、`trace_id`，加载全局上下文。
- `TmInventoryPredictService`：读取 `T_TM_STOCK` 和损耗设置，计算预计库存和供应时长。
- `TmPlanCalcService`：计算计划量、预计划、收尾、小批量补卷等。
- `TmMachineAssignService`：基于机台、口型板、定点/禁排、胶料机台关系筛选候选机台。
- `TmCapacityBalanceService`：做产能均衡、中夜班移量、次日回拉和任务顺序计算。
- `TmSnapshotBuildService`：生成 `T_TM_SCHEDULE_RESULT_EXPLAIN`。
- `TmPersistService`：统一落结果和解释信息。

规则上下文来源：

- 机台：`T_TM_MACHINE_INFO`
- 检修/停机：`T_TM_MACHINE_MAINTENANCE`
- 生产速度：`T_TM_MACHINE_SPEED`
- 口型板：`T_TM_MOUTH_PLATE`
- 定点/禁排：`T_TM_SPECIFY_MACHINE`
- 胶料机台：`T_TM_GLUE_MACHINE_REAL`
- 胶料顺序：`T_TM_GLUE_GROUP_ORDER`、`T_TM_GLUE_ORDER`
- 损耗：`T_TM_LOSS_SETTING`
- 卷曲长度：`T_TM_CURL_ROLL`
- 参数：`T_TM_PARAMS`
- 库存：`T_TM_STOCK`
- 班次：班次字典、`T_TM_SHIFT_CONFIG`

## 7. 自动排程扩展设计

本章用于补充自动排程引擎的扩展方式。设计目标是在保证现有接口、配置键、服务名、表结构兼容的前提下，把可变业务规则从主流程中拆出来，避免后续把需求量算法、机台过滤、任务排序、局部重算、日志解释继续堆在单个 `ServiceImpl` 中。

### 7.1 通用与胎面专用边界

胎面排程和胎侧排程可能共享部分排程基础能力，但两者的业务规则、数据来源和落库对象不同。后续实现时按以下边界拆分：

- 通用排程能力：任务链、班次游标、参数快照、规则过滤接口、评分接口、策略注册接口、过程日志接口。这些能力不引用 `TmScheduleResult`、`TmTaskDraft`、胎面胶料、口型板等 TM 专用对象。
- 胎面专用能力：胎面需求量计算、胎面库存供应时长、胶料机台关系、口型板约束、卷曲长度、收尾取整、胎面结果解释和 `T_TM_SCHEDULE_RESULT` 落库。
- 胎侧专用能力：后续由胎侧模块实现相同通用接口，差异逻辑放在胎侧自己的实现类中，不通过修改胎面实现来兼容胎侧。

建议通用能力放在 `Aps-Common/aps-engine-common/src/main/java/com/zlt/aps/common/engine/schedule/`；胎面实现放在 `APS-Modules/aps-tm` 或 `Aps-Api/tm-api` 对应的 `com.zlt.aps.tm.engine` 包下。公共接口只依赖泛型任务对象和上下文对象，不反向依赖具体业务模块。

### 7.2 设计模式落点

- 模板方法：`AbsTmScheduleTemplate` 固定自动排程骨架，流程为初始化、库存预测、需求计算、任务排序、机台分配、产能均衡、解释构建、统一落库。子类或步骤 Service 只实现具体业务步骤，不改变主流程顺序。
- 策略模式：需求量算法、计划量算法、收尾取整、机台评分等按策略接口实现。算法1、算法2通过 `TM_ALGORITHM_SWITCH` 或详设最终确认的参数编码选择，后续新增算法只新增策略实现。
- 责任链：候选机台过滤按开班、机台状态、检修、口型、胶料、定点/禁排、产能顺序执行。每条规则返回结构化结果，包含是否通过、原因编码、原因描述和证据对象。
- 工厂/注册表：策略和规则由 Spring 自动注入后按编码注册，调用方根据参数编码或规则编码获取实现，避免在主流程中写大量 `if/else`。
- 门面模式：`TmScheduleOperationFacade` 统一承接插单、调量、转机台、删除等前端操作，内部编排校验、日志、任务链重排、发布状态回退和解释更新。
- 观察者：自动排程、插单、调量、转机台、删除、自动滚动、发布回执统一发布调度事件，日志、解释快照、后续通知等能力通过监听器扩展。

### 7.3 双向任务链设计

自动排程、人工插单、删除、转机台、调量和局部重算都需要频繁调整同一机台同一班次内的任务顺序。运行态任务链建议使用双向链表处理，最终结果仍落到 `T_TM_SCHEDULE_RESULT.class{N}_sequence`、`class{N}_plan_qty`、`class{N}_start_time`、`class{N}_end_time` 等横向字段。

核心结构：

- `ScheduleTaskNode<T>`：通用任务链节点。保存任务对象、前驱节点、后继节点、机台编码、排程日期、班次顺序、班次编码、任务顺序、计划量、预计开始时间、预计结束时间。`T` 为业务任务草稿对象，胎面使用 `TmTaskDraft`，胎侧后续使用自己的任务对象。
- `ScheduleTaskLinkedList<T>`：通用双向链表。负责追加任务、按位置插入任务、删除节点、前后移动节点、跨链转移节点、重新编号、顺序遍历、倒序遍历。所有会修改链表的方法必须返回结构化变更结果，说明影响节点、新顺序和是否需要后续重算。
- `MachineShiftTaskChain<T>`：机台班次任务链集合。按 `machineCode + scheduleDate + shiftOrder` 管理一条 `ScheduleTaskLinkedList<T>`，用于快速定位指定机台指定班次的链表。

典型操作：

- 自动排程：从待排优先队列取出任务，经过候选机台过滤和评分后，追加到目标 `machineCode + scheduleDate + shiftOrder` 链表尾部，并重算该链表顺序和预计时间。
- 插单：根据前端传入的插入位置定位节点，调用链表插入方法放到指定节点之后；若位置为空，则追加到链尾。插入后只重排当前节点之后的任务。
- 删除：从链表摘除目标节点，目标节点前驱和后继直接连接；删除后重排后续节点顺序。
- 转机台：先从原机台链表摘除节点，再插入目标机台链表指定位置或链尾；原机台链和目标机台链分别重算。
- 调量：更新节点计划量，若计划量变化导致预计结束时间或跨班归属变化，则从该节点开始触发局部重算。
- 局部重算：根据 `TM_ROLLING_SHIFT_COUNT` 或默认窗口，只重算影响起点之后的机台、班次和后续任务链，不全量重建全部排程结果。

### 7.4 算法与数据结构

排程算法分为“先排序待排规格，再过滤和评分机台，最后写入任务链”三层：

- 待排规格排序使用 `PriorityQueue<TmTaskDraft>` 或等价优先队列，比较器顺序为强紧急、库存紧急度、同在产胶料、胶料优先级、基部胶相似度、口型聚集、稳定兜底。稳定兜底按 `treadCode`、`machineCode` 等固定字段升序，保证相同输入重复运行结果一致。
- 候选机台先通过责任链过滤硬约束，再通过评分策略排序。硬约束包括不开班、停用、整班检修、口型不匹配、胶料不允许、禁排机台、产能不足。评分项包括剩余产能、同胶料、同口型、切换成本、稳定兜底。
- 运行态上下文使用 `Map<String, TmParamValue>` 保存参数快照，使用 `Map<String, MachineRuntimeState>` 保存机台剩余产能、链尾胶料、链尾口型、下一可开工时间，使用 `Map<MachineShiftKey, ScheduleTaskLinkedList<TmTaskDraft>>` 保存任务链。

### 7.5 类与方法说明要求

后续 Java 实现时，每个新增类和核心方法必须写中文注释，说明具体作用、参数传法、返回值、异常场景和是否修改任务链。

类说明至少包含：

- 类的业务作用。
- 适用模块，是通用排程能力还是胎面专用能力。
- 是否允许胎侧复用。
- 依赖的上下文、策略、规则或 Mapper。

方法说明至少包含：

- 方法作用。
- 参数如何传递，优先传上下文对象和业务草稿对象，避免散传大量字段。
- 返回值含义，规则和链表修改类方法必须返回结构化结果。
- 异常场景，例如参数缺失、策略未注册、链表节点不存在、规则冲突、数据不完整。
- 是否修改任务链、是否触发局部重算、是否写日志或解释信息。

### 7.6 排产过程日志设计

排程过程必须具备可排查性。建议新增通用接口 `ScheduleProcessLogger`，胎面实现为 `TmScheduleProcessLogger`。日志和解释信息使用同一个 `traceId` 串联。

日志字段必须包含：

- `batchNo`：排程批次号。
- `traceId`：本次排程追踪标识。
- `scheduleDate`：排程日期。
- `shiftCode` / `shiftOrder`：班次编码和班次顺序。
- `machineCode`：机台编码，未分配时允许为空。
- `treadCode`：胎面编码。
- `stepCode`：当前步骤编码。
- `inputSummary`：输入摘要，例如参数值、库存、候选数量。
- `outputSummary`：输出摘要，例如排序结果、过滤结果、评分结果、链表变更结果。

关键日志点：

- 参数加载：记录命中的参数编码、参数值、默认值来源。
- 库存预测：记录库存、需求量、覆盖班次数、供应小时数、库存不足时间。
- 待排排序：记录排序前后关键字段和最终优先级。
- 候选机台过滤：记录每台候选机台通过或过滤原因。
- 机台评分：记录评分项、总分、最终选择原因。
- 任务链变更：记录插入、删除、转机台、调量前后的节点顺序。
- 局部重算：记录影响范围、重算窗口、受影响任务数。
- 未排处理：记录未排原因编码、原因说明和证据摘要。
- 落库结果：记录写入结果数、解释数、未排数、异常数。

日志级别：

- `debug`：明细计算过程、排序明细、评分明细。
- `info`：关键步骤开始结束、汇总结果、落库结果。
- `warn`：未排、规则冲突、基础资料缺失但可继续处理的场景。
- `error`：系统异常、落库失败、策略缺失、关键基础数据缺失导致排程终止的场景。

日志不得打印敏感信息。过程日志用于排查排程逻辑是否正确，最终业务解释仍以 `T_TM_SCHEDULE_RESULT_EXPLAIN` 的 `rule_hit_json`、`candidate_machine_json`、`unplanned_evidence_json`、`machine_select_reason` 为准。

## 8. 人工操作设计

- 插单：页面可按多班录入，后端拆成任务级记录，写事件并局部重算。
- 调量：只允许当前班及未来班，计划量不得小于完成量，已发布成功任务修改后版本号加 1 并回退待发布。
- 转机台：默认必须通过规则校验；强制转机台需写风险事件。
- 删除：逻辑删除未成功发布任务，并重算后续任务链。
- 归并/合并班次：生成新任务或更新来源链，原任务按规则取消或拆分。
- 完成量导入：目标字段为 `T_TM_SCHEDULE_RESULT.class{N}_finish_qty`（按实际班次写入对应 CLASS 列），并记录事件。

## 9. 前端看板交互适配设计

### 9.1 设计目标

前端需要查询两天后的排程结果，并支持插单、调量、转机台等操作。底层采用 `T_TM_SCHEDULE_RESULT` 六班一行横向模型，一条记录直接包含 CLASS1~CLASS6 六个班次的计划量、完成量、顺序和原因分析，前端可直接按“机台 + 日期 + 班次”看板 VO。

核心原则：

- `T_TM_SCHEDULE_RESULT` 作为唯一排程结果主表，六班数据已在一行内。
- 看板 VO 直接映射横向字段，无需聚合转换。
- 前端不再自行拼两天后的横向班次数据。
- 前端操作只传 `result_id` 或业务化 DTO，由后端完成重排、重算和事件记录。

### 9.2 看板查询接口

接口：

```http
POST /tm/schedule/board
```

请求 DTO：`TmScheduleBoardQueryDto`

字段建议：

- `startDate`：开始日期，必填。
- `endDate`：结束日期，必填，默认可查今天到两天后。
- `machineCode`：机台编码，选填。
- `treadCode`：胎面编码，选填。
- `glueCode`：胶料编码，选填。
- `releaseStatus`：发布状态，选填。
- `assignStatus`：分配状态，选填。

返回 VO：`TmScheduleBoardVO`

- `dateColumns`：动态日期班次列，来源于 `T_TM_SHIFT_CONFIG`。
- `machineRows`：机台行。
- `cells`：每个机台、日期、班次下的任务集合。
- `unassignedTasks`：未排任务集合。
- `batchMap`：每个日期当前使用的 `batch_no`。
- `summary`：总计划量、未排数、发布成功数、异常数。

### 9.3 当前结果口径

默认查询不要求前端传 `batch_no`。同一天自动排程重跑时，旧结果会先删除，新批次直接覆盖成为当天唯一有效结果，因此查询直接按 `schedule_date` 读取未删除数据即可，不再额外查“最新有效批次”。

### 9.4 看板组装规则

后端组装流程：

1. 从 `T_TM_SHIFT_CONFIG` 读取日期范围内班次列，按 `schedule_date + shift_order` 排序。
2. 从 `T_TM_SCHEDULE_RESULT` 按 `schedule_date` 分组取最新 `batch_no`，形成 `batchMap`。
3. 从 `T_TM_SCHEDULE_RESULT` 读取这些批次下的任务。
4. `machine_code is null` 的任务放入 `unassignedTasks`（`assign_status` 已移至 EXPLAIN 表，通过 JOIN 获取）。
5. 已分配任务按 `machine_code + schedule_date` 分组为机台行，每行的 CLASS1~CLASS6 列直接映射到对应班次单元格。
6. 各单元格内任务按 `class{N}_sequence` 升序排列。

`dateColumns` 单项字段：`scheduleDate`、`shiftCode`、`shiftName`、`shiftOrder`、`planStartTime`、`planEndTime`、`openFlag`、`crossDayFlag`。

`machineRows` 单项字段：`machineId`、`machineCode`、`machineName`、`machineStatus`、`rowOrder`。

`tasks` 单项字段：`taskId`、`batchNo`、`class1Sequence`/`class2Sequence`/`class3Sequence`、`treadCode`、`glueCode`、`glueSeq`、`mouthPlateCode`、`class1PlanQty`/`class2PlanQty`/`class3PlanQty`、`class1FinishQty`/`class2FinishQty`/`class3FinishQty`、`taskStatus`、`releaseStatus`、`manualLockedFlag`、`assignStatus`、`unplannedReasonCode`、`unplannedReasonDesc`、`currentTaskVersion`。

### 9.5 前端操作适配

插单接口保持业务友好：

```http
POST /tm/schedule/insertTask
```

前端传日期、机台、胎面、胶料、口型板、各班计划量 Map、插入位置。后端写入一条 `T_TM_SCHEDULE_RESULT` 记录，各班次计划量分别写入 `class1_plan_qty`/`class2_plan_qty`/`class3_plan_qty`，重排对应 `class{N}_sequence`，写事件并重算后续任务链。

调量接口按任务粒度：

```http
POST /tm/schedule/changeQty
```

前端从看板任务中取 `taskId`，传 `newPlanQty`。后端校验 `newPlanQty >= finishQty`、发布状态和任务版本，必要时将发布状态回退为 `5`。

转机台接口支持单任务和批量任务：

```http
POST /tm/schedule/changeMachine
```

前端传 `taskIds`、目标机台、是否强制、原因。后端统一校验口型板、胶料、禁排和产能规则，成功后重排原机台与新机台任务链。

### 9.6 后端服务分层

新增 `TmScheduleBoardQueryService`：

- 读取班次日历。
- 读取日期范围内当前有效批次。
- 查询 `T_TM_SCHEDULE_RESULT`，横向字段直接映射为看板行列。
- 组装动态列、机台行、单元格、未排任务和汇总信息。

新增 `TmScheduleOperationFacade`：

- 承接前端插单、调量、转机台、删除等操作 DTO。
- 内部调用规则校验、任务链重排、事件记录、发布状态回退等服务。
- 屏蔽前端对排程结果表和任务链细节的感知。

### 9.7 查询性能说明

一期不建设物理看板缓存表，直接基于主表查询聚合。当前 SQL 已具备看板查询所需关键索引：

- `T_TM_SCHEDULE_RESULT.idx_tm_schedule_result_batch(batch_no, schedule_date, is_delete)`
- `T_TM_SHIFT_CONFIG.idx_tm_shift_config_date_order(schedule_date, shift_order, open_flag)`
- `T_TM_SCHEDULE_RESULT_EXPLAIN` 诊断查询索引（按 `result_id` / `trace_id` / `schedule_date` 实际实现补充）

若后续看板查询性能不足，再考虑新增读模型缓存表或 Redis 缓存。缓存只能作为查询加速，不作为排程主存储。

## 10. 统一码值设计

字段码值详细说明见根目录文档 [tm_schedule_field_code_notes.md](/D:/git/JY_APS_GITHUB/JY_APS/tm_schedule_field_code_notes.md)。

本模块重点码值：

- `enable_status`：是否启用，字典 `biz_yes_no`，`0` 否、`1` 是
- `release_status`：发布状态
- `data_source`：任务来源
- `generate_mode`：生成方式
- `result_status`：运行结果
- `event_type`：事件类型
- `param_group`：参数分组
- `value_type`：参数值类型
- `shift_code`：班次编码

可用规则统一查询条件：

```sql
is_delete = 0
AND enable_status = '1'
```

## 11. 可解释性与问题排查

问题排查以 `T_TM_SCHEDULE_RESULT` 和 `T_TM_SCHEDULE_RESULT_EXPLAIN` 为主：

- 计划量如何得出：查看计划量分量字段和 `calc_formula_desc`
- 为什么选某台机：查看 `candidate_machine_json` 和 `machine_select_reason`
- 为什么未排上：查看 `T_TM_SCHEDULE_RESULT_EXPLAIN` 的 `assign_status`、`unplanned_reason_code`、`unplanned_evidence_json`
- 命中了哪些参数和规则：查看 `rule_hit_json`
- 整次运行是否异常：查看 `T_TM_SCHEDULE_RESULT_EXPLAIN` 的 `result_status`、`error_msg`
- 人工是否干预：查看 `t_tm_dispatcher_log`

## 12. 事务与一致性设计

自动排程落库事务包含：

- 任务写入
- 任务解释写入
- 解释信息写入

人工操作事务包含：

- 任务变更
- 事件写入
- 任务链重算结果写入
- 发布状态回退

发布事务建议分阶段处理：

1. 创建发布批次和明细
2. 任务状态更新为 `RELEASING`
3. 调用 MES
4. 根据回执回写明细和任务状态

## 13. 测试设计

- 基础资料测试：参数、胶料机台、损耗设置、定点/禁排停用后不得参与自动排程。
- 看板查询测试：查询今天到两天后，返回多个 `schedule_date + shift_code` 动态列。
- 看板查询测试：不同日期能取各自最新有效 `batch_no`。
- 看板查询测试：未排任务进入 `unassignedTasks`，不混入机台单元格。
- 看板查询测试：`open_flag='0'` 的班次不可作为可排班次。
- 自动排程测试：覆盖多机台、多规格、库存不足、检修停机、定点冲突、禁排、胶料不匹配、外协、新规格。
- 人工调整测试：覆盖插单、调量、转机台、删除、归并、合并班次；操作后重新查询看板能看到最新结果。
- 发布测试：覆盖成功、失败、超时、重复回执和任务版本变更后的重发。
- 追溯测试：任一任务必须能查到计划量计算依据、命中规则、候选机台、未排原因、人工事件和发布记录。

## 14. 相关文件

- SQL 脚本：[tm_schedule_rebuild_mysql.sql](/D:/git/JY_APS_GITHUB/JY_APS/tm_schedule_rebuild_mysql.sql)
- 字段码值说明：[tm_schedule_field_code_notes.md](/D:/git/JY_APS_GITHUB/JY_APS/tm_schedule_field_code_notes.md)

## 15. 胎面排程功能逻辑说明

胎面排程管理页面的功能逻辑，作为页面功能级详细设计补充。本文档已有的数据模型、自动排程引擎拆分、人工操作、MES 发布和事务设计仍作为落地实现依据。

### 15.1 功能范围与页面入口

胎面排程管理页面用于查看、生成、维护和发布胎面排程结果。页面主要功能包括：

- 查询：按排程日期、胎面、胶料、口型板、发布状态、机台查询排程记录。
- 自动排程：按指定排程日期生成胎面排程批次。
- 插单：人工新增指定胎面、机台、班次和计划量的排程记录。
- 删除：逻辑删除未成功发布至 MES 的排程计划。
- 转机台：将选中排程记录从原机台调整到新机台。
- 调量：调整选中排程记录当前班次或未来班次的计划量。
- 排程发布：将符合状态条件的排程记录下发 MES。
- 导入、导出：用于排程结果数据的批量导入和导出。
- 自动滚动更新：每个班开始前同步库存并滚动修正后续计划。

列表展示字段包括发布状态、工单号、批次号、胎面、胶料、胶料序号、口型板、机台、月计划剩余量、库存，以及各班次的计划量、完成量、顺序、完成率和原因分析。

### 15.2 看板查询

查询采用后端看板适配层处理，输出结果直接用于前端“机台 + 日期 + 班次”看板展示。

```text
Step1 构造查询条件
Input:
  start_date, end_date, tread_code, glue_code, release_status,
  machine_code
Process:
  组装 TmScheduleBoardQueryDto；
  日期范围默认支持今天到两天后；
  字符串条件统一 trim，空值不入过滤。
Output:
  BoardQueryCriteria

Step2 读取班次窗口
Load Data:
  T_TM_SHIFT_CONFIG
Where:
  schedule_date between BoardQueryCriteria.startDate and BoardQueryCriteria.endDate
  and is_delete=0
Fields:
  schedule_date, shift_code, shift_name, shift_order, plan_start_time,
  plan_end_time, open_flag, cross_day_flag
Process:
  生成 dateColumns，按 schedule_date + shift_order 排序；
  open_flag='0' 的班次可展示为停班列，但不可作为可排班次。
Output:
  DateColumns

Step3 读取当前有效批次
Load Data:
  T_TM_SCHEDULE_RESULT
Where:
  schedule_date between BoardQueryCriteria.startDate and BoardQueryCriteria.endDate
  and result_status in ('SUCCESS','PARTIAL_SUCCESS')
  and is_delete=0
Fields:
  schedule_date, batch_no, start_time, create_time
Process:
  每个 schedule_date 取 start_time 或 create_time 最大的一条作为当前有效批次；
  生成 batchMap。
Output:
  BatchMap(schedule_date -> batch_no)

Step4 读取任务主数据
Load Data:
  T_TM_SCHEDULE_RESULT
Where:
  is_delete=0
  and batch_no in BatchMap.values
  and schedule_date between BoardQueryCriteria.startDate and BoardQueryCriteria.endDate
  and (tread_code like %tread_code%)
  and (glue_code like %glue_code%)
  and (release_status = release_status)
  and (machine_code = machine_code)
Fields:
  id, schedule_date, batch_no, tread_code, glue_code, glue_seq,
  mouth_plate_code, machine_code,
  class1_plan_qty, class1_finish_qty, class1_sequence,
  class2_plan_qty, class2_finish_qty, class2_sequence,
  class3_plan_qty, class3_finish_qty, class3_sequence,
  class4_plan_qty, class4_finish_qty, class4_sequence,
  class5_plan_qty, class5_finish_qty, class5_sequence,
  class6_plan_qty, class6_finish_qty, class6_sequence,
  release_status
Output:
  TaskRows

Step5 关联补全
Load Data:
  T_TM_MACHINE_INFO, 班次/任务/发布状态字典
Process:
  按 machine_code 关联机台名称；
  将 release_status 转换为前端展示值。
Output:
  EnrichedRows

Step6 看板聚合
Process:
  machine_code 为空的任务进入 unassignedTasks（assign_status 通过 EXPLAIN 表 JOIN 获取）；
  已分配任务按 machine_code + schedule_date 分组为机台行，CLASS1~6 列直接映射到各班次单元格；
  各单元格内任务按 class{N}_sequence 升序排列；
  按机台生成 machineRows。
Output:
  dateColumns, machineRows, cells, unassignedTasks

Step7 汇总返回
Process:
  汇总 totalPlanQty、unassignedCount、releaseSuccessCount、errorCount；
  组装 TmScheduleBoardVO。
Output:
  TmScheduleBoardVO(dateColumns, machineRows, cells, unassignedTasks, batchMap, summary)
```

### 15.3 自动排程

自动排程以“成型 6 个班需求 + 胎面库存可供时长 + 胶料/口型/机台约束 + 机台任务链滚动”为主线，最终生成各机台各班次的排程结果。以下步骤按 `06-APS详细设计-胎面.xlsx` 中“胎面排程”的自动排程业务过程展开。

```text
Step1 打开自动排程页面
Input:
  operator
Process:
  点击“自动排程”按钮，弹出自动排程页面；
  排程日期默认带出服务器时间的下一天，允许按业务需要选择目标排程日期。
Output:
  AutoScheduleCmd(schedule_date, operator)

Step2 校验是否允许生成
Load Data:
  T_TM_SCHEDULE_RESULT
Process:
  检查所选排程日期是否已经存在下发过 MES 的计划；
  如果已有下发过 MES 的计划，则提示“当天已有生成计划，不可重复生成”，本次自动排程终止；
  如果允许生成，则生成本次 batch_no、trace_id，并准备覆盖当天未下发或未成功下发的旧结果。
Output:
  RunCtx(batch_no, trace_id, schedule_date)

Step3 解析成型计划生成胎面需求
Load Data:
  成型近期 6 个班排产计划、胎胚 BOM、胎面标准长度、胎面对应胶料和口型信息
Process:
  根据胎胚 BOM 解析成型计划，获取需要供应的胎面规格列表；
  按 6 个班展开每个胎面规格的成型需求，形成后续排程计算的需求明细。
Output:
  TreadDemandList(tread_code, shift_code, construction_plan_qty, tread_standard_length, glue_code, mouth_plate_code)

Step4 计算胎面每班需求量
Load Data:
  TmParams(DEMAND_QTY_CALCULATE_TYPE)
Process:
  如果参数选择算法1，则胎面每班需求量 = 成型三班最大计划量 * 胎面标准长度；
  如果参数选择算法2，则胎面每班需求量 = 下个班成型计划量 * 胎面标准长度；
  计算每个胎面规格在 6 个班中的需求量。
Output:
  ShiftDemandQtyMap(tread_code, shift_code, demand_qty)

Step5 计算 14点胎面预计库存
Load Data:
  MES 6点胎面实际库存、早班胎面预计消耗量、早班胎面计划量
Process:
  每天早上 6 点从 MES 获取各胎面的实际库存；
  14点胎面预计库存 = 6点MES胎面库存 - 早班胎面预计消耗量 + 早班胎面计划量。
Output:
  StockForecastMap(tread_code, stock_14pm)

Step6 建立本班机台任务链
Process:
  设定本次计算起始班次为中班；
  读取当前已有排产任务，并按机台、班次、生产顺序排序；
  按“机台 + 班次”创建任务链，将同机台同班次任务按顺序加入链表；
  后续新增排程结果都追加到对应机台班次任务链末尾。
Output:
  MachineShiftTaskChain(machine_code, shift_code, task_list)

Step7 计算库存保证班数和库存供应成型时长
Process:
  库存保证班数 = 14点胎面预计库存 / 胎面当班需求量；
  从当前班开始顺次检查库存，每满足一个班的成型消耗，库存满足成型消耗的班数加 1；
  库存供应成型时长 = 库存满足成型消耗的班数 * 8小时 + （剩余库存成型计划开始时间 - 当班开始时间）；
  如果库存不够一个班的成型消耗，则加上本班最早使用该胎面的成型计划预计开始生产时间与本班开始时间的差值；
  同时计算库存不足时间，供后续判断最晚开始生产时间。
Output:
  SupplyCalcResult(tread_code, coverage_shift_count, supply_hours, stock_shortage_time)

Step8 排序待排产规格
Process:
  先按可供成型班次分组，并从早到晚排序；
  同一班次内按主胶料分组，以组内最早的库存供应成型时长作为胶料组排序依据；
  主胶料不同的规格继续比较基部胶，基部胶相同个数越多优先级越高；
  同一种胶料内按库存供应成型时长从小到大排序；
  同种预口型尽量安排在一起生产；
  如果库存即将不足，计算最晚开始生产时间 = 库存不足时间 - 工艺停放时长 - 计划量 * 工艺生产速度，排程预计开始时间不可晚于该时间点。
Output:
  SortedTreadQueue

Step9 选择下一个排产规格
Process:
  检查本机台本班任务链末尾任务，取出其胶料作为在产胶料；
  如果本班没有其他排程，则检查上一个班任务链最后一个规格的胶料；
  若待排规格中存在与在产胶料相同的规格，则取库存供应成型时长最小的规格作为下一个排产规格；
  若没有相同胶料，则计算各胶料用量和胶料当班可用库存，优先选择胶料用量不超过当班可用库存的胶料；
  胶料用量 = 使用同种胶料的规格需求量合计值 * 胶料用量；
  胶料当班可用库存 = 上个班预计库存 - 预计胶料消耗量 + 预计胶料生产量；
  如果多个胶料库存都满足，则按胶料优先级列表选择优先级最高的一组规格；
  如果胶料库存不足，则从待排产列表取库存供应成型时长最小的胶料组，并从组内取库存供应成型时长最小的规格。
Output:
  NextTreadSpec(tread_code, glue_code, mouth_plate_code)

Step10 计算需排产量
Process:
  需排产量 = （胎面每班需求量 * 库存最低保证班数 - 14点胎面预计库存）*（100% + 损耗率）；
  库存最低保证班数默认 3 个班，可通过参数配置；
  如果需排产量未达到最低起排量（默认 300 米，可配置），则本班不排该规格，滚动到下一班重新计算预计库存后再判断。
Output:
  NeedQtyResult(tread_code, shift_code, need_qty, min_start_check)

Step11 按可用工装限制计划量
Process:
  可用工装数量 = （工装总数 - 库存 / 工装卷曲米数）* 整车率；
  需排产量 = 最小值（可用工装数量 * 工装卷曲米数, 需排产量）；
  如果可用工装不足以支撑原始需排产量，则按工装上限截断计划量。
Output:
  ToolLimitedQty(tread_code, shift_code, plan_qty)

Step12 计算收尾和非收尾实际排产量
Process:
  根据月计划成型剩余量判断是否即将收尾；
  月计划剩余量小于等于需排产量时，该规格视为收尾规格；
  胎面的余量 = 成型的余量 * 胎面长度 *（100% + 损耗率）；
  非收尾规格实际排产 = 向上取整(需排产量 / 标准卷曲长度) * 标准卷曲长度；
  收尾规格实际排产 = 最小值(需排产量, 胎面的余量) * (100% + 损耗率)；
  非收尾规格需补够最低起排量并补成工装卷曲长度的整倍数，收尾规格严格按余量排产并补加损耗率。
Output:
  ActualPlanQty(tread_code, shift_code, actual_plan_qty, tail_flag)

Step13 处理停产收尾和开产阈值
Process:
  获取生产日历 3 天内的停产班次；
  停产班次前需要考虑停产收尾，停产收尾量为成型停产班次前的胎面需求量；
  停产收尾规格实际排产 = 成型停产日前的胎面需求量 * (100% + 损耗率)；
  获取生产日历开产班次；
  开班排产时按库存供应成型时长从小到大生产，只生产到参数阈值后切换到下一个库存供应成型时长未满阈值的规格；
  开产实际排产 = (需排产量 + 预计库存)/当班需求量 >= 阈值 ? 当班需求量 * 阈值 - 预计库存 : 需排产量。
Output:
  CalendarAdjustedPlanQty(tread_code, shift_code, actual_plan_qty)

Step14 筛选可用机台
Load Data:
  T_TM_MACHINE_INFO, T_TM_MACHINE_MAINTENANCE, T_TM_MOUTH_PLATE,
  T_TM_GLUE_MACHINE_REAL, T_TM_SPECIFY_MACHINE, T_TM_MACHINE_SPEED
Process:
  只选择状态为启用的机台；
  根据机台检修计划扣减检修时间段产能，若一整天检修，则该机台视为不可用；
  根据口型与机台绑定关系，筛选可生产该口型的机台；
  根据胶料与机台绑定关系，以主胶料为准筛选可投入该胶料的机台；
  根据定点机台关系选择胎面规格绑定机台，或排除设置了不可生产该胎面规格的机台。
Output:
  MachineCandidates(machine_code, available_capacity, filter_reason)

Step15 选择机台并扣减产能
Process:
  如果存在多个可用机台，则选择剩余产能较小且能够容纳本次计划量的机台，优先把一个机台排满；
  一个班最大可排 5500 米，可通过参数配置；
  班产需要结合机台检修计划扣减最大班产；
  总可用产能 = 满产产能 * （8 / (维修时长 + 各规格切换时长 + 各胶料切换时长)）；
  剩余产能 = 总可用产能 - 已排规格实际排产量；
  如果一个班排产量已经达到最大值，则将任务安排至下一个班。
Output:
  SelectedMachine(machine_code, remain_capacity)

Step16 生成排程结果
Persist:
  T_TM_SCHEDULE_RESULT
  T_TM_SCHEDULE_RESULT_EXPLAIN
Process:
  将排产规格追加到本机台本班任务链末尾；
  生成本条排程记录的胎面、胶料、胶料序号、口型板、机台、月计划剩余量、库存等展示字段；
  按班次写入计划量、顺序、完成量、完成率和原因分析字段；
  已分配任务写入 `T_TM_SCHEDULE_RESULT`，解释信息写入 `T_TM_SCHEDULE_RESULT_EXPLAIN`；
  解释信息需包含计划量计算过程、库存依据、规则命中、候选机台和最终选机原因。
Output:
  AssignedScheduleResult(result_id, machine_code, class{N}_plan_qty, class{N}_sequence)

Step17 滚动到下一班
Process:
  切换班次后，重新计算本班开始预计库存和库存保证班数；
  下一个班预计库存 = 上一个班预计库存 + 上一个班生产计划 - 上一个班成型需求量；
  创建本机台下一个班的任务链，并继承必要的上班次任务链信息；
  更新机台剩余产能、胎面库存预测和待排规格优先级。
Output:
  NextShiftCtx

Step18 完成 6 个班滚动计算
Process:
  重复步骤4到步骤17；
  按排程日期对应的 6 个班逐班计算，直到每个机台、每个班次的任务链都完成排产或确认无可排任务。
Output:
  SixShiftScheduleResult

Step19 记录未排和汇总结果
Persist:
  T_TM_SCHEDULE_RESULT
  T_TM_SCHEDULE_RESULT_EXPLAIN
Process:
  如果某个规格没有可用机台，或因最低起排量、工装、检修、口型、胶料、定点/禁排、产能等规则无法排产，则写入未排列表；
  未排任务仍写入 `T_TM_SCHEDULE_RESULT`，机台允许为空；
  未排原因、规则证据和候选机台过滤过程写入 `T_TM_SCHEDULE_RESULT_EXPLAIN`；
  汇总已排数量、未排数量、异常数量和本次 batch_no，返回自动排程结果。
Output:
  RunSummary(batch_no, assigned_count, unassigned_count, error_count)
```

### 15.4 插单

```text
Step1 校验输入
Input:
  schedule_date, tread_code, machine_code, shift_plan_qty_map, shift_order_map, reason_map
Process:
  至少一个班次 plan_qty>0；
  有计划量必须有顺序，有顺序必须有计划量；
  仅允许当前班及未来班次。
Output:
  InsertCmd

Step2 校验基础数据
Load Data:
  胎面主数据（现有主数据来源）、T_TM_MACHINE_INFO、T_TM_SHIFT_CONFIG
Process:
  校验 tread_code 存在、machine_code 可用、班次窗口有效；
  校验“第二个在产规格之后”约束。
Output:
  ValidatedInsert

Step3 写任务
Persist:
  T_TM_SCHEDULE_RESULT
Process:
  data_source='INSERT'，release_status='0'。
Output:
  InsertedResult(result_id_list)

Step4 写事件
Persist:
  t_tm_dispatcher_log(oper_type='1', ...)
Output:
  InsertEvent

Step5 触发滚动重算
Process:
  调用 14.10，影响范围从插单任务所在班次开始。
Output:
  RecalcSummary
```

### 15.5 删除

```text
Step1 查询目标任务
Load Data:
  T_TM_SCHEDULE_RESULT
Where:
  id=:result_id and is_delete=0
Output:
  TargetTask

Step2 删除校验
Process:
  已发布成功任务不可删除，仅允许 release_status in (0, 2, 5)。
Output:
  DeletableTask

Step3 逻辑删除
Persist:
  T_TM_SCHEDULE_RESULT set is_delete=1, update_time=now()
Output:
  DeletedTask

Step4 写事件
Persist:
  t_tm_dispatcher_log(...)
Output:
  DeleteEvent

Step5 触发滚动重算
Output:
  RecalcSummary
```

### 15.6 转机台

```text
Step1 查询任务与新机台
Load Data:
  T_TM_SCHEDULE_RESULT, T_TM_MACHINE_INFO
Output:
  TransferCmd

Step2 校验
Process:
  新旧机台不可相同；
  新机台需启用且满足口型/胶料/定点禁排约束。
Output:
  ValidatedTransfer

Step3 更新任务
Persist:
  T_TM_SCHEDULE_RESULT set machine_code, class{N}_sequence(待重排)
Output:
  TransferredTask

Step4 发布状态处理
Process:
  若原任务发布成功，release_status 回退为 5。
Persist:
  T_TM_SCHEDULE_RESULT
Output:
  VersionedTask

Step5 写事件与修改日志
Persist:
  t_tm_dispatcher_log(oper_type='0', ...)
Output:
  TransferEvent

Step6 双链重算
Process:
  分别对原机台链与新机台链调用 14.10。
Output:
  DualRecalcSummary
```

### 15.7 调量

```text
Step1 查询任务
Load Data:
  T_TM_SCHEDULE_RESULT
Fields:
  id, schedule_date, class1_plan_qty, class1_finish_qty, class2_plan_qty, class2_finish_qty, class3_plan_qty, class3_finish_qty, release_status
Output:
  AdjustCmd

Step2 校验
Process:
  仅允许当前班及未来班；
  new_plan_qty >= finish_qty。
Output:
  ValidatedAdjust

Step3 更新任务
Persist:
  T_TM_SCHEDULE_RESULT set class{N}_plan_qty=:new_plan_qty
Output:
  AdjustedTask

Step4 发布状态处理
Process:
  若已发布成功，release_status 回退为 5。
Output:
  VersionedTask

Step5 写事件
Persist:
  t_tm_dispatcher_log(oper_type='1', ...)
Output:
  AdjustEvent

Step6 触发重算
Output:
  RecalcSummary
```

### 15.8 排程发布

```text
Step1 过滤可发布任务
Load Data:
  T_TM_SCHEDULE_RESULT
Where:
  id in :selected_ids and is_delete=0
  and release_status in (0, 2, 5)
Output:
  PublishableTasks

Step2 空数据判断
Process:
  PublishableTasks 为空则返回业务提示，不继续发布。
Output:
  PublishReject or ContinueFlag

Step3 更新任务发布中
Persist:
  T_TM_SCHEDULE_RESULT set release_status=3
Output:
  ReleasingTasks

Step4 调用 MES
Process:
  按选中结果批量发送任务到 MES。
Output:
  MesResponse

Step5 回写发布结果
Persist:
  按 MES 返回结果回写 `T_TM_SCHEDULE_RESULT.release_status=1/2/4`
Output:
  PublishSummary(success_count, fail_count, timeout_count)
```

### 15.9 自动滚动更新

```text
Step1 识别触发窗口
Input:
  current_time, factory_code
Load Data:
  T_TM_SHIFT_CONFIG
Process:
  定位当前班次和下一班次窗口。
Output:
  RollingCtx(schedule_date, current_shift, next_shift)

Step2 同步库存并读取
Load Data:
  MES库存接口落地结果 + T_TM_STOCK
Where:
  stock_date=RollingCtx.schedule_date
Output:
  LatestStock

Step3 计算上修/下修
Load Data:
  T_CX_SCHEDULE_MAIN（成型计划）, T_TM_SCHEDULE_RESULT, T_TM_PARAMS
Process:
  若 预计库存 + 下班原计划 < 一个班需求量:
    new_plan = 一个班需求量 - 预计库存
  若 预计库存 + 下班原计划 > 3.5班阈值:
    new_plan = 3班需求量 - 预计库存
Output:
  AdjustPlanSet(result_id, old_plan, new_plan, reason)

Step4 更新任务
Persist:
  T_TM_SCHEDULE_RESULT(class{N}_plan_qty)
Output:
  AdjustedTasks

Step5 写事件日志
Persist:
  t_tm_dispatcher_log(...)
Output:
  RollingEvent

Step6 触发后续重算
Output:
  RecalcSummary
```

### 15.10 滚动更新后续排程

```text
Step1 锁定影响范围
Input:
  trigger_result_id, trigger_type(insert/delete/transfer/adjust/auto_rolling)
Process:
  计算影响起点：机台、班次、日期。
Output:
  ImpactScope

Step2 加载后续任务链
Load Data:
  T_TM_SCHEDULE_RESULT, T_TM_SHIFT_CONFIG
Where:
  machine_code in ImpactScope.machines
  and schedule_date/shift 在起点后续3个班次窗口内
  and is_delete=0
Fields:
  id, machine_id, class1_plan_qty, class1_finish_qty, class1_sequence, class2_plan_qty, class2_finish_qty, class2_sequence, class3_plan_qty, class3_finish_qty, class3_sequence, class4_plan_qty, class4_finish_qty, class4_sequence, class5_plan_qty, class5_finish_qty, class5_sequence, class6_plan_qty, class6_finish_qty, class6_sequence
Output:
  TaskChainBefore

Step3 重排顺序
Process:
  新增场景：后续节点 class{N}_sequence +1；
  删除场景：后续节点 class{N}_sequence -1；
  更新场景：保持顺序，重算时间。
Output:
  ReorderedChain

Step4 班次内产量重算
Load Data:
  T_TM_MACHINE_SPEED, T_TM_SHIFT_CONFIG
Process:
  基于 `schedule_date`、`machine_code`、`class1~class6_sequence`、`class1~class6_plan_qty`、
  `product_speed`、`plan_start_time`、`plan_end_time` 重算后续班次归属和计划量；
  当前版本只支撑顺序重排、班次内/跨班量重算，不精确追踪任务级开始结束时刻。
Output:
  RecalculatedChain

Step6 批量写回
Persist:
  T_TM_SCHEDULE_RESULT, T_TM_SCHEDULE_RESULT_EXPLAIN
Output:
  PersistResult

Step6 记录操作日志与当前解释
Persist:
  t_tm_dispatcher_log
  T_TM_SCHEDULE_RESULT_EXPLAIN
Output:
  RecalcSummary
```

### 15.11 待确认问题

以下问题仍需业务或技术口径最终确认：

- 覆盖旧批次的实现方式：旧批次按“逻辑失效”处理还是按“is_delete=1”处理，需要固定一种实现，避免统计口径不一致。
- `总可用产能 = 满产产能 * （8 / (维修时长 + 各规格切换时长 + 各胶料切换时长)）` 的单位换算尚未完全闭环，需要统一“小时/米/班产”维度后再编码。
- “插单只能加到第二个在产规格之后”的“第二个在产规格”判定范围仍待定（当前机台任务链内，或当前班全局）。
- 试验胶版本仍依赖外部纸质流程，系统内无版本化库存字段，当前仅能记录“外部确认结果”。
- 导入、导出的模板字段、校验规则和失败回执格式未在本章展开，当前按现有代码行为执行，后续建议补专章说明。

## 16. 代码实现步骤

本章用于指导后续 Java 代码按步骤实现。实现时仍遵循 Controller 只做参数接收和结果返回、业务逻辑放在 Service、优先使用 LambdaQueryWrapper/LambdaUpdateWrapper、不手动追加逻辑删除条件、中文注释完整说明类和方法作用的规范。

### 16.1 建立通用排程接口包

建议位置：`Aps-Common/aps-engine-common/src/main/java/com/zlt/aps/common/engine/schedule/`。

目标：沉淀胎面和胎侧后续可共用的任务链、规则、评分、日志和策略工厂接口。

建议类与方法：

- `IScheduleTaskNode<T>`：通用任务节点读取接口。用于屏蔽具体节点实现，让任务链服务能读取任务、机台、日期、班次、顺序、计划量和预计时间。
  - `T getTask()`：获取节点承载的业务任务对象。参数无，返回泛型任务对象；节点未绑定任务时应抛出业务异常或返回空对象，由实现类统一约定。
  - `String getMachineCode()`：获取节点所属机台编码。参数无，返回机台编码；未分配任务允许返回空。
  - `Integer getShiftOrder()`：获取班次顺序。参数无，返回六班横向模型中的班次序号。
  - `Integer getSequence()`：获取当前链内顺序。参数无，返回任务顺序。

- `IScheduleTaskLinkedList<T>`：通用双向链表操作接口。用于插入、删除、转移和重排任务节点。
  - `ScheduleChainChangeResult<T> append(ScheduleTaskNode<T> node, ScheduleOperationContext context)`：将节点追加到链尾。`node` 传待追加节点，`context` 传操作人、原因、traceId；返回链表变更结果；节点已在其他链表中时抛出业务异常；会修改任务链并打印任务链变更日志。
  - `ScheduleChainChangeResult<T> insertAfter(ScheduleTaskNode<T> anchorNode, ScheduleTaskNode<T> newNode, ScheduleOperationContext context)`：将新节点插入指定节点之后。`anchorNode` 为空时按链尾追加处理；返回受影响节点和新顺序；锚点不在当前链表时抛出业务异常；会修改任务链。
  - `ScheduleChainChangeResult<T> remove(ScheduleTaskNode<T> node, ScheduleOperationContext context)`：从链表摘除节点。`node` 传目标节点；返回删除节点、后续受影响节点和顺序变化；节点不存在时抛出业务异常；会修改任务链。
  - `ScheduleChainChangeResult<T> transferTo(ScheduleTaskNode<T> node, IScheduleTaskLinkedList<T> targetList, ScheduleTaskNode<T> targetAnchor, ScheduleOperationContext context)`：跨链转移节点。`targetList` 传目标链，`targetAnchor` 传目标插入位置；返回原链和目标链的影响结果；会同时修改两条任务链。
  - `ScheduleChainChangeResult<T> resequence(ScheduleOperationContext context)`：从头节点开始重新编号。参数传操作上下文；返回全部顺序变化节点；只修改顺序字段，不改变节点前后关系。

- `IScheduleRule<T, C>`：通用规则过滤接口。用于机台过滤、任务可排校验等规则扩展。
  - `String getRuleCode()`：返回规则编码，用于日志和解释信息。
  - `ScheduleRuleResult evaluate(T target, C context)`：执行规则。`target` 传被校验对象，`context` 传排程上下文；返回是否通过、原因编码、原因描述和证据对象；规则异常时抛出业务异常，不允许吞异常。

- `IScheduleScoreStrategy<T, C>`：通用评分策略接口。用于候选机台、候选任务排序。
  - `String getStrategyCode()`：返回评分策略编码。
  - `ScheduleScoreResult score(T target, C context)`：对目标对象评分。`target` 传候选对象，`context` 传上下文；返回评分项、总分和说明；关键输入缺失时抛出业务异常。

- `IScheduleProcessLogger<C>`：通用排程过程日志接口。
  - `void logStepStart(C context, String stepCode, String inputSummary)`：记录步骤开始。`context` 传排程上下文，`stepCode` 传步骤编码，`inputSummary` 传输入摘要；无返回值。
  - `void logStepEnd(C context, String stepCode, String outputSummary)`：记录步骤结束。`outputSummary` 传输出摘要；无返回值。
  - `void logRuleResult(C context, String ruleCode, ScheduleRuleResult result)`：记录规则结果。规则冲突或过滤使用 `warn`，正常通过使用 `debug`。
  - `void logChainChange(C context, ScheduleChainChangeResult<?> result)`：记录任务链变更。用于插单、删除、转机台、调量和局部重算。

验证点：通用包编译通过；所有通用接口不引用 `TmScheduleResult`、`TmTaskDraft`、胎面胶料、口型板等 TM 专用类型。

### 16.2 实现双向链表基础结构

目标：提供排程运行态任务链结构，供自动排程和人工调整共用。

建议类与方法：

- `ScheduleTaskNode<T>`：双向链表节点实现类。字段包括任务对象、前驱节点、后继节点、机台编码、排程日期、班次编码、班次顺序、任务顺序、计划量、预计开始时间、预计结束时间。
  - 构造方法传入业务任务对象和基础定位信息，不在构造方法中查询数据库。
  - `linkAfter(ScheduleTaskNode<T> previousNode)`：将当前节点连接到指定节点之后。`previousNode` 传前驱节点；无返回值；只处理指针关系，不重排顺序。
  - `unlink()`：摘除当前节点。无参数；无返回值；摘除后当前节点前驱和后继置空。

- `ScheduleTaskLinkedList<T>`：双向链表实现类。
  - `append`、`insertAfter`、`remove`、`transferTo`、`resequence` 按 16.1 接口实现。
  - `List<ScheduleTaskNode<T>> toList()`：按头到尾返回节点列表。参数无；返回当前链表快照；不允许调用方修改内部指针。
  - `ScheduleTaskNode<T> findByTaskId(String taskId)`：按任务标识查找节点。`taskId` 传业务任务ID；返回节点，不存在返回空或抛出业务异常，由实现类注释明确。

- `MachineShiftTaskChain<T>`：机台班次链表管理类。
  - `ScheduleTaskLinkedList<T> getOrCreate(String machineCode, LocalDate scheduleDate, Integer shiftOrder)`：获取或创建指定机台班次链表。参数分别传机台、日期、班次顺序；返回链表。
  - `ScheduleTaskLinkedList<T> get(String machineCode, LocalDate scheduleDate, Integer shiftOrder)`：只读取已存在链表。不存在时返回空。
  - `List<ScheduleTaskLinkedList<T>> listAffectedChains(ImpactScope scope)`：按影响范围返回需要重算的链表。`scope` 传影响起点、机台集合、班次窗口；返回链表列表。

验证点：覆盖追加、头部插入、中间插入、尾部插入、删除头节点、删除中间节点、删除尾节点、跨链转移、重新编号。

### 16.3 建立 TM 引擎领域对象

目标：让自动排程步骤之间传运行态对象，不直接传数据库实体。

建议位置：`APS-Modules/aps-tm/src/main/java/com/zlt/aps/tm/engine/domain/`；若需要跨服务契约再放入 `Aps-Api/tm-api`。

建议类与方法：

- `TmScheduleContext`：胎面排程上下文。承载 `batchNo`、`traceId`、`scheduleDate`、操作人、参数快照、班次列表、库存预测、机台运行态、待排队列和任务链。
  - `TmParamValue getParam(String paramCode)`：按参数编码读取本次快照。`paramCode` 传参数编码；返回参数值对象；参数不存在时返回默认值并记录规则命中，关键参数无默认值时抛出业务异常。
  - `ScheduleTaskLinkedList<TmTaskDraft> getTaskChain(String machineCode, Integer shiftOrder)`：获取机台班次任务链。参数传机台编码和班次顺序；返回链表。

- `TmTaskDraft`：胎面待排任务草稿。承载胎面编码、胶料、基部胶、口型板、需求量、计划量、库存覆盖、供应时长、强紧急时间、收尾标识、未排原因。
  - `String getBusinessKey()`：返回稳定业务键，用于排序兜底和日志。
  - `boolean isUnassigned()`：判断是否未分配机台。

- `TmMachineCandidate`：候选机台对象。承载机台编码、剩余产能、可用时间、链尾胶料、链尾口型、过滤状态、过滤原因、评分结果。
  - `void markFiltered(String reasonCode, String reasonDesc, Object evidence)`：标记候选机台被过滤。参数传原因编码、中文说明和证据；无返回值；会影响候选结果。
  - `void applyScore(TmMachineScoreResult scoreResult)`：写入评分结果。参数传评分结果；无返回值。

- `TmRuleTrace`：规则命中证据对象。用于汇总参数命中、规则过滤、评分和未排证据。
  - `void addRuleHit(String ruleCode, String result, Object evidence)`：追加规则证据。参数传规则编码、结果和证据；无返回值。
  - `String toExplainJson()`：转换为解释 JSON 文本。参数无；返回可写入 `rule_hit_json` 或 `unplanned_evidence_json` 的文本。

验证点：领域对象不继承数据库实体；字段能覆盖解释表需要的 `rule_hit_json`、`candidate_machine_json`、`unplanned_evidence_json`。

### 16.4 实现自动排程模板流程

目标：固定主流程，允许每一步独立替换和测试。

建议类与方法：

- `AbsTmScheduleTemplate`：胎面排程模板抽象类。
  - `TmAutoScheduleResponseVo execute(TmScheduleContext context)`：模板方法。`context` 必须包含排程日期和操作人；返回自动排程响应；任何步骤异常记录 `error` 日志并继续交由上层事务处理。
  - `protected abstract void doBootstrap(TmScheduleContext context)`：初始化批次、追踪号、参数和基础资料。
  - `protected abstract void doInventoryPredict(TmScheduleContext context)`：预测库存和供应时长。
  - `protected abstract void doDemandAndPlanCalc(TmScheduleContext context)`：计算需求量和计划量。
  - `protected abstract void doTaskSort(TmScheduleContext context)`：建立待排优先队列。
  - `protected abstract void doMachineAssign(TmScheduleContext context)`：过滤候选机台并插入任务链。
  - `protected abstract void doSnapshotAndPersist(TmScheduleContext context)`：生成解释并统一落库。

- `TmScheduleTemplateImpl`：胎面模板实现类。
  - 通过注入各步骤 Service 完成具体实现。
  - 每个步骤开始调用 `TmScheduleProcessLogger.logStepStart`，结束调用 `logStepEnd`。

验证点：模板只负责编排，不直接写复杂业务规则；每步可单独 mock 测试。

### 16.5 实现策略与规则链

目标：把高频变化规则做成可替换实现。

建议类与方法：

- `ITmDemandQtyStrategy`：胎面需求量算法策略。
  - `String getAlgorithmCode()`：返回算法编码，例如 `1`、`2`。
  - `TmDemandQtyResult calculate(TmDemandQtyInput input, TmScheduleContext context)`：计算需求量。`input` 传成型计划、胎面长度、班次；`context` 传参数和日志上下文；返回每班需求量。

- `ITmPlanQtyStrategy`：胎面计划量算法策略。
  - `TmPlanQtyResult calculate(TmTaskDraft draft, TmScheduleContext context)`：计算计划量。`draft` 传待排任务；返回基础需求、库存抵扣、损耗、工装限制、收尾补正、最终计划量。

- `ITmMachineFilterRule`：胎面候选机台过滤规则。
  - `ScheduleRuleResult evaluate(TmMachineCandidate candidate, TmMachineRuleContext context)`：执行单条规则。`candidate` 传候选机台；`context` 传任务、班次、基础资料；返回通过或过滤结果。

- `ITmMachineScoreStrategy`：胎面机台评分策略。
  - `TmMachineScoreResult score(TmMachineCandidate candidate, TmMachineRuleContext context)`：对候选机台评分。返回评分项和总分。

- `ITmTaskSortStrategy`：待排任务排序策略。
  - `Comparator<TmTaskDraft> buildComparator(TmScheduleContext context)`：返回排序比较器。参数传上下文；返回比较器，用于优先队列。

验证点：算法1/算法2能按参数切换；新增过滤规则不改主流程；过滤结果能进入解释 JSON。

### 16.6 实现 TM 任务链排程服务

目标：统一处理自动排程和人工操作对任务链的修改。

建议类与方法：

- `TmTaskChainScheduleService`：胎面任务链排程服务。
  - `ScheduleChainChangeResult<TmTaskDraft> appendAutoTask(TmTaskDraft task, TmMachineCandidate machine, TmScheduleContext context)`：自动排程追加任务。`task` 传待排任务，`machine` 传选中机台，`context` 传上下文；返回链表变更结果；会修改任务链并打印日志。
  - `ScheduleChainChangeResult<TmTaskDraft> insertManualTask(TmTaskDraft task, TmInsertPosition position, TmScheduleContext context)`：人工插单。`position` 传目标机台、班次和锚点任务；返回影响节点；会触发局部重算。
  - `ScheduleChainChangeResult<TmTaskDraft> removeTask(String taskId, TmScheduleContext context)`：删除任务。`taskId` 传任务ID；返回删除和重排结果；目标任务不存在或已发布成功不可删时抛出业务异常。
  - `ScheduleChainChangeResult<TmTaskDraft> transferMachine(String taskId, String targetMachineCode, TmTransferPosition position, TmScheduleContext context)`：转机台。参数传任务ID、目标机台和目标位置；返回原链与目标链变更结果；会分别重算两条链。
  - `ScheduleChainChangeResult<TmTaskDraft> changeQty(String taskId, BigDecimal newPlanQty, Integer shiftOrder, TmScheduleContext context)`：调量。参数传任务ID、新计划量和班次；返回影响结果；新计划量小于完成量时抛出业务异常。

验证点：插单、删除、转机台、调量只影响必要链表；发布成功任务状态回退逻辑由操作门面统一处理。

### 16.7 实现过程日志与解释快照

目标：让一次排程能通过 `batchNo + traceId` 追溯“为什么这么排”。

建议类与方法：

- `TmScheduleProcessLogger`：胎面过程日志实现。
  - `logStepStart`、`logStepEnd`、`logRuleResult`、`logChainChange` 按 16.1 接口实现。
  - `void logUnplanned(TmTaskDraft task, TmRuleTrace trace, TmScheduleContext context)`：记录未排任务。`task` 传未排任务，`trace` 传证据，`context` 传上下文；使用 `warn` 级别。
  - `void logPersistSummary(TmScheduleContext context, TmPersistResult result)`：记录落库汇总。参数传上下文和落库结果；使用 `info` 级别。

- `TmSnapshotBuildService`：解释快照构建服务。
  - `TmSnapshotBuildResult buildTaskExplain(TmTaskDraft task, TmScheduleContext context)`：构建单任务解释。返回计划量分量、规则命中、候选机台、未排证据。
  - `String buildCandidateMachineJson(List<TmMachineCandidate> candidates)`：生成候选机台 JSON。参数传候选列表；返回 JSON 文本。
  - `String buildRuleHitJson(TmRuleTrace trace)`：生成规则命中 JSON。参数传规则证据；返回 JSON 文本。

验证点：日志中能看到参数加载、库存预测、排序、过滤、评分、任务链变更、未排、局部重算和落库；解释表能看到同一 `traceId` 的规则证据。

### 16.8 实现落库服务

目标：统一把运行态任务链结果转换为数据库结果和解释信息。

建议类与方法：

- `TmPersistService`：胎面排程落库服务。
  - `TmPersistResult persist(TmScheduleContext context)`：统一落库。`context` 传完整排程上下文；返回写入结果数、解释数、未排数和异常数；方法由上层编排控制事务。
  - `List<TmScheduleResult> convertChainToResult(ScheduleTaskLinkedList<TmTaskDraft> chain, TmScheduleContext context)`：将链表转换为结果实体。参数传任务链和上下文；返回结果实体列表。
  - `TmScheduleResultExplain convertExplain(TmTaskDraft task, TmSnapshotBuildResult snapshot)`：转换解释实体。参数传任务和解释快照；返回解释实体。
  - `void persistUnplanned(TmTaskDraft task, TmSnapshotBuildResult snapshot, TmScheduleContext context)`：写入未排任务。未排任务仍写入 `T_TM_SCHEDULE_RESULT`，机台为空，原因写解释表。

落库规则：

- 按链表顺序写入 `class{N}_sequence`、`class{N}_plan_qty`、`class{N}_start_time`、`class{N}_end_time`。
- 未排任务写入 `T_TM_SCHEDULE_RESULT`，`machine_code` 允许为空。
- 解释信息写入 `T_TM_SCHEDULE_RESULT_EXPLAIN`。
- 事务边界放在自动排程入口或操作门面，不在策略类、规则类中开启事务。

验证点：同一批次结果和解释数量一致；未排任务可通过结果表和解释表追溯；落库失败不吞异常。

### 16.9 补充测试与验证

实现顺序建议先测通用结构，再测 TM 业务规则：

1. 通用双向链表单元测试：追加、插入、删除、转移、重新编号、遍历。
2. TM 排序策略单元测试：强紧急优先于同胶料，库存紧急度排序正确，稳定兜底可重复。
3. 机台过滤规则链单元测试：开班、机台状态、检修、口型、胶料、定点/禁排、产能。
4. 机台评分测试：剩余产能可容纳且最小者优先，同胶料和同口型加分生效。
5. 人工操作链表测试：插单、删除、调量、转机台后顺序重算正确。
6. 解释与日志测试：同一 `batchNo + traceId` 能串起参数、库存、排序、过滤、评分、落库全过程。
7. 编译验证：实现代码后优先执行 `mvn -pl APS-Modules/aps-tm -am -DskipTests compile`；若改动 API 层，补跑 `mvn -pl Aps-Api/tm-api -am -DskipTests compile`。

验收标准：

- 不修改 Feign 契约、服务名、接口路径和配置键。
- 通用接口不依赖胎面专用类。
- 胎面专用实现能覆盖详设中的自动排程、人工操作、局部重算、解释和日志要求。
- 所有新增类和方法有中文注释，说明作用、参数、返回值、异常和代码意图。
