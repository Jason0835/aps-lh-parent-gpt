# APS 成型排程系统 - 项目规范

## 项目概览

金宇轮胎APS系统-成型排程模块，基于 Spring Boot 2.7.18 开发，实现成型车间的智能排程功能。

### 核心功能
- **排程执行**: 根据硫化需求和库存情况自动生成成型排程
- **重排程**: 支持手动触发重新排程
- **动态调整**: 根据实时情况调整排程结果
- **节假日排程**: 特殊处理节假日和节前排程
- **试制排程**: 支持试制任务的排程管理

### 技术栈
- Java 1.8 (OpenJDK 1.8.0_411)
- Spring Boot 2.7.18
- MyBatis Plus 3.5.3.1
- MySQL 8.0.33
- Maven 3.8.7

## 目录结构

```
src/main/java/com/zlt/aps/
├── cx/                                    # 成型排程模块
│   ├── config/                            # 配置类
│   │   ├── MybatisPlusConfig.java
│   │   └── OpenApiConfig.java
│   ├── controller/                        # 控制器层
│   │   ├── ScheduleMainController.java
│   │   └── ScheduleDetailController.java
│   ├── entity/                            # 实体类
│   │   ├── config/                        # 配置相关实体
│   │   │   ├── CxKeyProduct.java
│   │   │   ├── CxParamConfig.java
│   │   │   ├── CxShiftConfig.java
│   │   │   └── CxStructurePriority.java
│   │   └── schedule/                      # 排程相关实体
│   │       ├── CxScheduleDetail.java
│   │       ├── CxScheduleResult.java
│   │       ├── CxTrialPlan.java
│   │       └── LhScheduleResult.java
│   ├── enums/                             # 枚举类
│   │   └── DayVulcanizationModeEnum.java
│   ├── mapper/                            # Mapper 接口
│   ├── service/                           # 服务层
│   │   ├── engine/                        # 核心算法引擎
│   │   │   ├── BalancingService.java      # DFS均衡分配
│   │   │   ├── ContinueTaskProcessor.java # 续作任务处理
│   │   │   ├── CoreScheduleAlgorithmService.java # 算法接口
│   │   │   ├── NewTaskProcessor.java      # 新增任务处理
│   │   │   ├── ProductionCalculator.java  # 产量计算工具
│   │   │   ├── ScheduleDayTypeHelper.java # 班次类型判定
│   │   │   ├── ShiftScheduleService.java  # 班次精排
│   │   │   ├── TaskGroupService.java      # 任务分组与属性计算
│   │   │   └── TrialTaskProcessor.java    # 试制任务处理
│   │   └── impl/                          # 服务实现
│   │       ├── CoreScheduleAlgorithmServiceImpl.java
│   │       ├── ScheduleServiceImpl.java   # 排程主流程编排
│   │       ├── HolidayScheduleServiceImpl.java
│   │       └── validation/               # 数据校验策略（策略模式）
│   │           ├── ValidationStrategy.java           # 校验策略接口
│   │           ├── BaseValidationStrategy.java       # 校验基础类
│   │           ├── ValidationItem.java               # 校验项枚举
│   │           ├── ScheduleDataValidationResult.java # 校验结果
│   │           ├── ScheduleDataValidator.java        # 校验器（自动注册）
│   │           ├── ShiftConfigValidationStrategy.java
│   │           ├── MoldingMachineValidationStrategy.java
│   │           ├── LhScheduleResultValidationStrategy.java
│   │           ├── MaterialValidationStrategy.java
│   │           ├── MaterialLhCapacityValidationStrategy.java
│   │           └── ParamConfigValidationStrategy.java
│   └── vo/                                # 值对象
│       ├── MonthPlanProductLhCapacityVo.java
│       ├── ScheduleContextVo.java
│       └── ...
└── mp/api/domain/entity/                  # 主数据实体类
    ├── MdmMaterialInfo.java
    ├── MdmMoldingMachine.java
    ├── MdmMonthSurplus.java
    ├── MdmStructureLhRatio.java
    ├── MdmWorkCalendar.java
    ├── MpCxCapacityConfiguration.java
    └── ...
```

## 构建和测试命令

### 编译
```bash
mvn compile -q
```

### 运行测试
```bash
mvn test
```

### 打包
```bash
mvn package -DskipTests
```

### 运行应用（启动后访问 http://localhost:5000/api）
```bash
mvn spring-boot:run -q
```

## 代码风格指南

### 包命名规范
- `com.zlt.aps.cx` - 成型排程模块核心包
- `com.zlt.aps.mp` - 主数据相关包

### 类命名规范
- 实体类: `Cx` 前缀表示成型模块，`Mdm` 前缀表示主数据
- Mapper: 实体类名 + `Mapper`
- Service: 实体类名 + `Service`
- Controller: 模块名 + `Controller`
- Processor: 任务处理器（如 `ContinueTaskProcessor`、`NewTaskProcessor`、`TrialTaskProcessor`）
- Strategy: 校验/策略模式实现（如 `MaterialValidationStrategy`）

### 注释规范
- 类和公共方法必须添加 Javadoc 注释
- 使用 `@author` 标注作者
- 使用 `@param` 和 `@return` 说明参数和返回值

## 参数治理体系

### 参数编码规范
成型排程参数统一采用 `SYS04` 编码体系：
- `SYS04` = 成型模块标识
- 第6-7位 = 分组码
- 第8-10位 = 流水码

分组码定义：
| 分组码 | 含义 |
|-------|------|
| `01` | 基础参数 |
| `02` | 约束参数 |
| `03` | 硫化时间参数 |
| `04` | 机台参数 |
| `05` | 收尾参数 |
| `06` | 立库参数 |
| `07` | 均衡分配参数 |

完整参数编码清单：
| 编码 | 参数名称 | 默认值 | 说明 |
|------|---------|-------|------|
| `SYS04010001` | 日硫化量计算模式 | `2` | 1=MES, 2=标准, 3=APS |
| `SYS04010002` | 胶种类型编码 | `T101,T133,T601` | 逗号分隔的胶种编码列表 |
| `SYS04020001` | 损耗率 | `0.02` | 用于计算实际产能 |
| `SYS04020002` | 机台种类上限 | `4` | 单个机台最多生产的胎胚种类数 |
| `SYS04020003` | 机台默认最大硫化机数 | `10` | 配比缺失时单台最多生产的硫化机数 |
| `SYS04030001` | 硫化停锅时间 | - | 优先从T_LH_PARAMS读取 |
| `SYS04030002` | 硫化开模时间 | - | 优先从T_LH_PARAMS读取 |
| `SYS04030003` | 预留消化时长 | `1` | 硫化消化时间（小时） |
| `SYS04030004` | 精度计划提前天数 | `3` | 精度计划扣减天数 |
| `SYS04040001` | 机台最大胎胚种类数 | `4` | 新格式：H15,3;H14,5 |
| `SYS04050001` | 收尾舍弃阈值 | `2` | 非主销余量≤此值舍弃 |
| `SYS04050002` | 紧急收尾成型余量阈值 | `400` | 成型余量<此值标记紧急 |
| `SYS04050003` | 近期收尾天数阈值 | `10` | 距收尾天数≤此值标记为近期收尾 |
| `SYS04050004` | 紧急收尾天数阈值 | `3` | 距收尾天数≤此值标记为紧急收尾 |
| `SYS04060001` | 成型胎胚立库库容 | `5000` | 立库可容纳胎胚数量（条数），0=不启用管控 |
| `SYS04060002` | 立库库容使用比例阈值 | `0.9` | 所有胎胚班后预计库存总和≥库容×此值时封顶产量 |
| `SYS04070001` | 均衡种类差额阈值 | `1` | DFS均衡时机台间胎胚种类数最大允许差额 |
| `SYS04070002` | 均衡负荷差额阈值 | `3` | DFS均衡时机台间硫化机台数最大允许差额 |
| `SYS04070003` | 强制保留历史任务 | `N` | Y=续作不换胎胚, N=正常均衡 |

### 参数来源优先级
成型排程不再独立维护其他模块已有参数，参数加载遵循以下优先级：

1. **源头参数表（优先）**
   - 硫化停锅/开模时间 → `T_LH_PARAMS`（硫化参数表）
     - `SYS0310007` = 硫化停锅时间
     - `SYS0310006` = 硫化开模时间
   - 日硫化量计算模式 → `T_MP_FACTORY_PARAM`（工厂月计划参数表）
     - `SYS0202002` = 日硫化量计算模式（MES/标准/APS）
   - 试制量试SKU上限、周日是否允许排产 → `T_MP_FACTORY_PARAM`
     - `SYS0206003` = 试制量试SKU上限
     - `SYS0206005` = 试制量试周日允许排产标志

2. **成型参数表（兜底）**
   - `T_CX_PARAM_CONFIG` 中对应 `SYS04` 编码的参数

3. **代码硬编码（最后兜底）**
   - 当上述来源均无值时采用代码默认值

### 机台最大胎胚种类数参数格式
参数值格式：`H15,3;H14,5`（分号分隔，逗号分隔前缀和数值）
- 解析为 `Map<String, Integer>`，key=机台编码前缀，value=该机台前缀对应的最大胎胚种类数
- 机台匹配时按Map遍历，找到第一个前缀匹配的条目
- 未匹配到任何前缀时使用默认值4

## 核心算法说明

### 排程执行流程

1. **构建排程上下文** (`ScheduleServiceImpl.buildScheduleContext`)
   - 加载机台信息、物料信息、库存数据
   - 加载硫化需求任务、在机信息
   - 加载配置参数和约束条件（按参数治理体系优先级加载）
   - 构建物料日硫化产能映射（按参数模式计算）
   - 加载试制计划数据
   - 初始化 `machineMaxEmbryoTypes` Map、`maxTrialSkuPerDay`、`trialAllowedOnSunday`

2. **数据完整性校验** (`ScheduleDataValidator.validate`)
   - 校验班次配置、成型机台、物料信息、硫化排程结果
   - 校验物料日硫化产能（按参数模式校验对应字段是否有值）
   - 校验参数配置完整性
   - 校验失败阻断排程

3. **执行核心算法** (`CoreScheduleAlgorithmService.executeSchedule`)
   - 按天循环（共8个班次，约3天）
   - 每天流程：
     - **任务分组** (`TaskGroupService`): 收尾/紧急/续作/试制/新增分类，三层优先级排序
     - **续作处理** (`ContinueTaskProcessor`): 保底预留+均衡
     - **新增处理** (`NewTaskProcessor`): 均衡分配+结构分配
     - **试制处理** (`TrialTaskProcessor`): 约束验证+排产
     - **班次均衡** (`ShiftScheduleService`): 按试制/停产/开产/收尾/普通类型排产
   - 精度计划联动硫化扣量（`applyPrecisionHourDeduction`）

4. **保存排程结果**
   - 保存到 `T_CX_SCHEDULE_RESULT` 主表
   - 保存到 `T_CX_SCHEDULE_DETAIL` 子表

### 任务分组与属性计算 (TaskGroupService)

任务分组是排程核心环节（S5.2阶段），负责将所有硫化需求按业务规则分组并计算任务属性。

#### 三层优先级排序体系

任务按以下规则计算优先级分值，分值高者优先排产：

**第一层（基础分层，千位级）**：
| 条件 | 优先级分值 |
|------|-----------|
| 补充计划任务 | 10000 |
| 有计划量 + 3天内紧急收尾 | 9000 |
| 有计划量 + 10天内近期收尾 | 8000 |
| 有计划量 + 正常 | 7000 |
| 无计划量 + 3天内紧急 | 6000 |
| 无计划量 + 10天内近期 | 5000 |
| 无计划量 + 正常 | 4000 |

**第二层（任务类型加成）**：
- 试制/量试任务：+1500
- 续作任务：+800

**第三层（同层内排序）**：
- 库存少的优先：`-min(库存量, 499)`（库存越少分值越高）

排序逻辑：总分 = 第一层基础分 + 第二层加成 + 第三层库存调整

#### 零净需求暂存与第二轮分配

- **零净需求判定**：库存已覆盖需求 或 补充计划任务，净需求量 <= 0
- **暂存处理**：零净需求任务不立即分配，进入 `zeroNetDemandTasks` 暂存列表
- **第二轮轮询分配**：第一轮正常任务分配完成后，按结构轮询方式逐车分配暂存任务
  - 每次从暂存列表中按优先级取任务，分配1车（当前班次产能允许前提下）
  - 一车一车补充，而非一次性全部分配
  - 补充计划任务按"缺多少补多少"逻辑下量

#### 结构推荐机台总产能管控

每个结构有推荐的成型机台列表，管控两个维度：
1. **硫化机数上限**：推荐机台总数 <= 结构硫化机台数 + 上限余量（参数控制）
2. **累计耗时上限**：推荐机台总产能对应的累计生产耗时 <= 班次总时长 × 机台数 × 比例阈值

当推荐机台产能不足以覆盖需求时，扩展候选机台范围（非推荐机台也纳入）。

#### 成型胎胚立库库容双维度管控

立库库容管控在计划量确定后（S5.2.6.2）执行，两个维度取较严格的限制：

**维度一（空间）— 库容上限封顶**：
- 计算所有胎胚的预计班后库存总和：`Σ(当前库存 + 累计成型产出 - 累计硫化消耗)`
- 若总和 >= `立库库容 × 比例阈值`，封顶本任务产量使总和不超过库容上限
- 封顶后向下取整车；不够一车则产量归零舍弃

**维度二（时间）— 可供硫化6小时封顶**：
- 计算本胎胚预计班后库存可供硫化时长：`projectedStock × singleTireMoldSeconds / totalMoldQty / 3600`
- 若时长 > 6h，封顶产量使库存仅够6小时硫化
- 同样取整车，不够一车归零

**动态累计追踪**：
- `shiftFormingOutputMap`：逐任务累计本班次成型产出（封顶后取整车值）
- `shiftVulcanizingConsumptionMap`：逐任务累计本班次硫化消耗
- 每个任务处理后都会更新预计班后库存，后续任务看到的是最新值

**注意**：维度一比较的是**所有胎胚合计**的预计库存 vs 立库总容量（共享空间），维度二比较的是**单胎胚**的预计库存可供硫化时长 vs 6h（各自消化能力）。

#### 开产班次关键产品过滤

- 开产首班（OPEN_START）不排关键产品，除非整个结构全部为关键产品（无替代选择）
- 关键产品定义见 `T_CX_KEY_PRODUCT` 配置表

#### 硫化余量与成型余量过滤

- 硫化余量 <= 0 的任务跳过
- 成型余量 <= 0 的任务跳过

#### 收尾属性计算

每个任务计算以下收尾属性：
- **成型余量**：还需生产的胎胚数量
- **是否3天内紧急收尾**：距离需求日期 <= 3天
- **是否10天内近期收尾**：距离需求日期 <= 10天
- **收尾日**：该结构最迟需要完成成型的日期

#### 收尾余量处理规则

- **非主销产品**：余量 <= 2 则舍弃（不排），余量 > 2 按实际余量下量
- **主销产品**：不足一车（按车容量换算）补足到一车
- 主销判定依据业务规则（如库存周转率、销售优先级等）

#### 停产反推逻辑 V2

`calculateClosingRequiredStockV2`：根据停锅时间反推当天需生产的胎胚总量

```
需胎胚 = (停锅时间 - 当前班次开始时间 - 消化时间) / 单胎单模时长 × 模数
```

- `singleTireMoldSeconds = 86400 / dailyLhCapacity`（单模日硫化量）
- 结果直接截断取整（不向上取整）
- 反推后与正常计划量取较小值封顶
- **完整日期时间匹配**：停锅日期时间与班次起止日期时间精确匹配计算

#### 开产首班6小时产能封顶

`handleOpeningDayTaskV2`：开产首个班次（OPEN_START）产能限制

- 开产首班最多排6小时的产能（非满班8小时）
- 计算逻辑：按机台小时产能 × 6小时计算封顶产量
- 关键产品首班不排（除非无替代）

#### 跨天封顶逻辑

如果明天有停产班次，当前班次的产量不可超过明天停锅前的反推需求：

```
封顶产量 = min(正常计划量, 反推需求 - 库存)
```

跨天班次（isCrossDay=1，如 NIGHT_D2 22:00~05:59）的开始/结束时间需调整日期：
- 开始日期减1天（`calculateStartTime`）
- 结束日期加1天（`calculateShiftEndTime`）

#### 设备停机处理

当机台停机时：
- 检查库存是否够4小时生产
- 库存 >= 4小时产能：产量不变，继续正常生产
- 库存 < 4小时产能：产量减产一半

### 约束条件

- **机台种类上限**: 每台机台最多同时生产N种规格（由 `machineMaxEmbryoTypes` Map控制，默认4种）
- **续作优先**: 正在生产的胎胚必须继续生产（保底预留1个机台）
- **班次均衡**: 波浪分配，班次间车数差不超过1
- **关键产品**: 开产首班不排关键产品（除非全结构均为关键产品）
- **试制约束**: 一天最多2个新胎胚（参数可控），周日默认不做（参数可控），早/中班，双数
- **生产版本**: 按 PRODUCTION_VERSION 过滤可用机台

### 任务类型与处理策略

| 任务类型 | 处理器 | 策略 |
|---------|--------|------|
| **续作任务** | `ContinueTaskProcessor` | 强制保留则在原机台保底预留1台，剩余统一均衡 |
| **新增任务** | `NewTaskProcessor` | 按结构→候选机台→DFS均衡分配 |
| **试制任务** | `TrialTaskProcessor` | 约束检测（数量/时间/机台），约束任务锁定机台，空机台优先+负载不均衡度选择 |
| **量产试制** | `TrialTaskProcessor` | 同试制但不走试制数量/时间约束 |
| **收尾任务** | `TaskGroupService` | 余量处理、主销补整车、非主销≤2舍弃 |

### 均衡分配算法 (BalancingService)

- **DFS 均衡分配**：深度优先搜索 + 剪枝策略，寻找多机台间种类和负荷均衡的分配方案
- **搜索限制**：100 万次，防止极端情况卡死
- **搜索上限剪枝**：搜索超过 100 万次后停止

#### 剪枝策略
1. **剩余负荷可行性剪枝**：剩余机台总产能 < 剩余总需求，直接剪枝
2. **贪心上界剪枝**：当前最大负荷 > 贪心负荷下界 + 1，剪枝

#### 任务排序
- 第一排序：硫化机台数降序（大任务优先）
- 第二排序：候选机台数升序（受限任务优先）

#### 部分解记录
- DFS 在递归过程中记录最优部分解（用于约束冲突场景）
- 当找不到完整解时，使用最优部分解作为结果

#### 贪心兜底方案
- 迭代贪心重试（最多 5 次重试，每次重置机台状态）
- 第一次重试: 硫化机台数降序（大任务优先）
- 后续重试: 硫化机台数降序 + 已失败胚子优先

#### 续作保底预留过滤
- 续作预留前检查机台是否支持当前结构的 PRODUCTION_VERSION
- 不支持的机台跳过保底预留

#### 机台种类上限控制
- 通过 `context.getMachineMaxEmbryoTypes()` 获取 Map<String, Integer>
- 机台编码前缀匹配：遍历Map，找到第一个匹配的前缀，取对应种类上限值
- 未匹配到任何前缀时默认最大4种

### 试制任务处理 (TrialTaskProcessor)

#### 约束检测
- **数量约束**：当天试制+量试的SKU数（按胎胚编码去重）<= `maxTrialSkuPerDay`（默认2，参数可控）
- **时间约束**：只允许早班/中班，不允许夜班
- **周日约束**：`trialAllowedOnSunday=false` 时周日不排试制/量试
- **双数约束**：试制任务产量必须为双数

#### 机台选择策略
1. **空机台优先**：优先选择当前无任务的空机台
2. **负载不均衡度**：无空机台时，选择当前负载最不均衡的机台（负载差异最大者）
   - 计算各机台当前任务总量，选择加入该任务后能使整体负载最均衡的机台
3. **不补整车**：试制任务不执行补整车逻辑，按计算量直接排产
4. **无库存分配**：试制任务不占用库存逻辑
5. **无收尾处理**：试制任务不走收尾余量处理逻辑
6. **PRODUCTION_VERSION过滤**：同其他任务，需匹配机台生产版本

#### 量产试制区分
- 试制任务：走完整约束检测（数量/时间/周日/双数）
- 量试任务：不走试制约束检测，但使用相同的机台选择策略

### 班次排产策略 (ShiftScheduleService)

班次精排按5种任务类型分别处理，将任务计划量分配到具体班次：

| 任务类型 | 策略要点 |
|---------|---------|
| **试制任务** | 只在早班/中班排产，产量必须为双数，不补整车 |
| **停产任务** | 停锅班次不补整车，之前班次整车取整；根据硫化EndTime反推精确产量 |
| **开产任务** | 首班6小时产能封顶（OPEN_START逻辑），关键产品首班不排，不补整车 |
| **收尾任务** | 波浪分配，最后班次可不整车；非主销余量<=2舍弃，主销不足一车补一车 |
| **普通任务** | 波浪放置，班次间车数差不超过1；相邻班次车数差绝对值<=1 |

#### 波浪分配算法 (calculateWaveCars)

将总车数按班次均分，要求相邻班次车数差不超过1，且呈现"两端多、中间少"或均匀分布：
- 计算平均每班车数 = 总车数 / 班次数
- 余数从两端班次开始分配（先排第一个班次，再排最后一个，交替进行）
- 确保任意两个相邻班次的车数差绝对值 <= 1

#### 机台小时产能计算

机台小时产能基于日硫化量和结构配比动态计算：

```
singleTireMoldSeconds = 86400 / (structureRatio × dailyLhCapacity)
hourlyCapacity = 3600 / singleTireMoldSeconds × moldCount
```

- `structureRatio` = 结构硫化配比（`T_MDM_STRUCTURE_LH_RATIO`）
- `dailyLhCapacity` = 物料日硫化量（按日硫化量模式选择字段）
- `moldCount` = 模数（双模=2，单模=1，产量计算时已÷2转单模）
- 成型一条胎的时间（秒）= 86400 / (配比 × 日硫化量)

#### 跨天班次时间计算

- 跨天班次（isCrossDay=1）：开始日期减1天，结束日期加1天
- 用于精确计算停产反推时的可用时间窗口

### 班次类型判定 (ScheduleDayTypeHelper)

按班次级别（而不是天级别）判断开产/停产，核心方法 `determineShiftType`：

| 返回类型 | 条件 |
|---------|------|
| `CLOSED` | 当前班次 SHIFT_FLAG=0（停产） |
| `OPEN_START` | 当前班次=1 且 上班次=0（开产首个班次） |
| `BEFORE_CLOSE` | 当前班次=1 且 下个班次=0（停产前一个班次） |
| `NORMAL` | 上下班次均正常 |

### 精度计划联动硫化扣量

`applyPrecisionHourDeduction`：当成型精度计划扣减产量时，同步更新硫化计划量。

逻辑流程：
1. 计算成型产量 = 库存 + 新排产量
2. 与硫化计划量比较：
   - 若 成型可用量 < 硫化计划量：
     - 生成精度影响备注：`成型精度影响: 库存X+产量Y=Z<硫化计划W, 缺口V条`
     - **同步更新硫化计划量**：将 `LhScheduleResult` 的对应 `class*PlanQty` 设置为 `totalAvailable`
     - 将修改后的硫化结果加入待更新列表

### 日硫化量计算模式

由参数 `DAY_VULCANIZATION_MODE`（`SYS04010001`，优先从 `T_MP_FACTORY_PARAM.SYS0202002` 读取）控制：

| 编码 | 模式 | 对应字段 |
|------|------|---------|
| `"1"` | MES日硫化量 | `mesCapacity` |
| `"2"` | 标准日硫化量（默认） | `standardCapacity` |
| `"3"` | APS日硫化量 | `apsCapacity` |

计算链：
```
dayVulcanizationQty → 按模式选值 → ÷2（双模转单模）→ stockHours = 库存 × 单胎单模时长 / 模数 / 3600
```

原始值映射（工厂参数表使用字母，成型参数表使用数字）：
- `M` → `1`（MES日硫化量）
- `S` → `2`（标准日硫化量，默认）
- `A` → `3`（APS日硫化量）

## 配置说明

### 数据库配置
数据库连接信息通过 `application.yml` 配置，端口5000，支持动态数据源切换。

### 关键配置表
| 表名 | 说明 |
|------|------|
| `T_CX_PARAM_CONFIG` | 系统参数配置（含 `DAY_VULCANIZATION_MODE`、`LOSS_RATE` 等） |
| `T_CX_SHIFT_CONFIG` | 班次配置（含 `IS_CROSS_DAY`、`SCHEDULE_DAY`、`DAY_SHIFT_ORDER`） |
| `T_CX_HOLIDAY_CONFIG` | 节假日配置 |
| `T_CX_KEY_PRODUCT` | 关键产品配置 |
| `T_MDM_MONTH_PLAN_PRODUCT_LH_CAPACITY` | 物料日硫化产能 |
| `T_MDM_STRUCTURE_LH_RATIO` | 结构硫化配比 |
| `T_MDM_WORK_CALENDAR` | 工作日历（SHIFT_FLAG/DAY_FLAG） |
| `MP_CX_CAPACITY_CONFIGURATION` | 结构排产配置（含PRODUCTION_VERSION） |
| `T_LH_PARAMS` | 硫化参数表（停锅/开模时间等，优先读取） |
| `T_MP_FACTORY_PARAM` | 工厂月计划参数表（日硫化模式、试制参数等，优先读取） |

## 数据校验层 (validation)

采用策略模式，通过 `@Component` 自动注册到 `ScheduleDataValidator`。

| 校验策略 | 校验项 | 级别 | 说明 |
|---------|--------|------|------|
| `ShiftConfigValidationStrategy` | 班次配置 | ERROR | 班次配置是否完整 |
| `MoldingMachineValidationStrategy` | 成型机台 | ERROR | 机台基础数据是否完整 |
| `LhScheduleResultValidationStrategy` | 硫化排程结果 | WARN | 硫化任务数据完整性 |
| `MaterialValidationStrategy` | 物料信息 | ERROR | 物料编码和胎胚编码是否完整 |
| `MaterialLhCapacityValidationStrategy` | 物料日硫化产能 | ERROR/WARN | 按参数模式校验对应字段是否有值 |
| `ParamConfigValidationStrategy` | 参数配置 | WARN | 关键参数是否存在 |

校验结果为 ERROR 时阻断排程，WARN 可继续但输出告警。

## 注意事项

1. **不要删除或修改以下文件**:
   - `ApsFormingScheduleApplication.java` - 应用启动类
   - `MybatisPlusConfig.java` - MyBatis Plus 配置
   - `AGENTS.md` - 本项目规范文件

2. **实体类修改注意事项**:
   - 修改实体类时需同步更新对应的 Mapper XML 文件
   - 新增字段需添加到数据库建表语句中

3. **算法修改注意事项**:
   - 核心算法位于 `service/engine` 目录
   - 修改前需充分理解现有逻辑
   - 建议添加单元测试验证修改
   - 新增校验策略：创建类继承 BaseValidationStrategy + @Component

4. **跨天班次注意事项**:
   - NIGHT_D2/NIGHT_D3 等夜班（22:00~05:59）必须设置 `IS_CROSS_DAY=1`
   - `calculateStartTime` 和 `calculateShiftEndTime` 均依赖此字段

5. **开产判定注意事项**:
   - 仅 `OPEN_START`（本班次=1 且 上班次=0）触发现开产逻辑
   - `isOpeningDay`（DAY_FLAG=1）已废弃，不再用于开产判断

6. **库存分配注意事项**:
   - 当前班次计划量=0 的任务跳过库存分配
   - `dayVulcanizationQty=0` 的物料跳过库存分配（数据完整性由 validation 保障）
   - 共用胎胚按日硫化量比例分配，最后一条倒扣

7. **Mapper包扫描注意事项**:
   - MyBatis扫描包为 `com.zlt.aps.cx.mapper`
   - 其他包下的 `@Mapper` 需要移到该包下或配置额外扫描路径

8. **参数加载注意事项**:
   - 参数加载遵循"源头表 > T_CX_PARAM_CONFIG > 硬编码"优先级
   - 修改参数来源时需同时更新 `loadLhParamValue()` 和 `loadFactoryParamValue()` 方法
   - 日硫化量模式原始值（M/S/A）需通过 `convertDayVulcanizationMode()` 转换为数字编码（1/2/3）

## 最近清理记录

### 已删除的废弃代码
- `ScheduleDayTypeHelper.isOpeningDay()` 方法（开产判断改用 OPEN_START）
- `ScheduleContextVo.isOpeningDay` 字段
- `ContinueTaskProcessor.calculatePlannedProduction()` 4参数废弃方法
- `ContinueTaskProcessor.handleEndingRemainder()` 4参数废弃方法
- `ConstraintCheckService.checkKeyProductConstraint()` 接口与实现
- `HolidayScheduleServiceImpl.context.setIsOpeningDay(true)` 调用
- `calculateStockHours` 中的 lhResult 后备计算逻辑
- **`applyStockHoursCap()` 方法**：已移除，6小时封顶逻辑已合并到 S5.2.6.2 立库库容双维度管控中
- **`getStockHoursCap()` 方法**：已移除，不再需要读取 `SYS04050005` 参数
- **`calculateProductionStockHours()` 方法**：已移除，仅用于收尾补产日志的辅助方法
- **`STOCK_HOURS_CAP_THRESHOLD` 常量（值=6）**：已移除，改为类常量 `STOCK_HOURS_CAP`
- **`PARAM_STOCK_HOURS_CAP` 常量（编码=SYS04050005）**：已移除，参数不再使用
- **`embryoTotalMoldMap` 预构建**：重新添加，供维度二（时间）封顶使用

### 立库库容管控改造记录

**改造前**（旧逻辑）：
- 循环开始前预计算 `totalExcessStock = Σ(库存 - 月需求量)`，全局布尔值 `warehouseCapacityExceeded`
- 超限后所有任务跳过，无法区分单个胎胚
- 独立的 `applyStockHoursCap()` 方法做6小时封顶，库存基准为静态快照，共用胎胚时 excessStock 重复扣减

**改造后**（当前逻辑）：
- S5.2.6.2 统一双维度封顶：空间（所有胎胚合计 vs 库容上限）+ 时间（单胎胚可供硫化时长 vs 6h）
- 动态累计 `shiftFormingOutputMap` / `shiftVulcanizingConsumptionMap`，每个任务看到最新的预计库存
- 两维度取较小允许产量，封顶后向下取整车，不够一车归零
- 放在收尾日志打印和成型余量累加之前执行，确保日志和累加使用的是封顶后的最终值
- 第二轮跳过检查同步使用双维度判定

### R2轮次计数器改造记录

**改造前**（旧逻辑）：
- 轮次按每个**胎胚**独立追踪，胎胚A从1到N轮后，换到胎胚B时又从1开始
- 日志显示：`[R2-第1轮] 胎胚=215103130` → `[R2-第4轮] 胎胚=215102626` → `[R2-第1轮] 胎胚=215101726`

**改造后**（当前逻辑）：
- 轮次改为**结构级别全局追踪**，同一结构内的所有任务共享同一个轮次计数器
- 每轮表示"所有任务各分配一车"，第1轮完成后进入第2轮
- 新增 `structGlobalRound` 变量，在 while 循环开始时递增
- 日志显示：`[R2-第1轮] 胎胚=215103130` → `[R2-第1轮] 胎胚=215102626` → `[R2-第2轮] 胎胚=215102626` → `[R2-第2轮] 胎胚=215101726`
