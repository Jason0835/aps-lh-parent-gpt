package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * TM/TC 自动排程任务草稿公共非持久化模型。
 *
 * <p>只承载两领域同名同类型的算法运行态字段，不对应数据库表，不负责落库。</p>
 */
@Data
public class ScheduleTaskDraftModel implements ScheduleSortableTask, ScheduleQualityTask {

    /** 产品工序编码；TM/TC 在运行态统一使用该字段，边界模型负责映射到产品专属字段。 */
    protected String processCode;
    /** 工单号 */
    protected String orderNo;
    /** 来源成型工单号集合，仅用于解释追踪，不写入产品排程结果工单号 */
    protected String sourceOrderNos;
    /** 成型来源唯一键；同一成型结果行生成的多个班次任务共用该键，仅用于运行态聚合。 */
    protected String formingSourceKey;
    /**
     * 来源成型计划所属排程日期，未来停产来源不得被替换成当前排程日期。
     */
    protected LocalDate formingSourceScheduleDate;
    /** 成型物料编号 */
    protected String materialCode;
    /** 成型物料描述 */
    protected String materialDesc;
    /** 胎胚代码 */
    protected String embryoCode;
    /** 胎胚描述 */
    protected String mainMaterialDesc;
    /** 成型机台编号 */
    protected String cxMachineCode;
    /** 主胶料编码 */
    protected String glueCode;
    /** 基部胶编码，当前只有一个编码时基部胶相似个数退化为 0 或 1 */
    protected String baseGlueCode;
    /** 口型板编码 */
    protected String mouthPlateCode;
    /** 机台编码 */
    protected String machineCode;
    /** 班次顺序 */
    protected Integer shiftOrder;
    /** TASK_SORT 阶段生成的稳定基础优先级，数值越小越优先 */
    protected Integer baseSortIndex;
    /** 计划量计算阶段确定的统一任务顺序，数值越小越优先。 */
    protected Integer planCalcOrderIndex;
    /** 当前班次实际进入机台资源尝试的顺序，仅用于过程日志和解释证据。 */
    protected Integer machineAssignmentSequence;
    /** 成型需求原始映射到的产品逻辑班次，自动提前时与实际排程班次不同。 */
    protected Integer sourceShiftOrder;
    /** 当前班成型产品需求量，单位米 */
    protected BigDecimal currentShiftDemandQty;
    /** 原始当班成型产品需求量，单位米；实验规格只扩展备库窗口，不写入派生补量。 */
    protected BigDecimal originalCurrentShiftDemandQty;
    /** 同产品下一排程班的成型需求量，单位米 */
    protected BigDecimal nextShiftDemandQty;
    /** 当班与下一排程班需求合计，单位米 */
    protected BigDecimal twoShiftDemandQty;
    /** 两班需求扣减班初滚动库存后的缺口，单位米 */
    protected BigDecimal twoShiftStockGapQty;
    /**
     * 历史两班控制标记；普通任务同步本次库存覆盖结论，停产收尾保留原有语义。
     */
    protected Boolean twoShiftStockCovered;
    /**
     * 剩余库存起排班数需求窗口，仅用于库存覆盖判定。
     */
    protected ScheduleStockCoverageWindowModel stockCoverageWindow;
    /**
     * 剩余库存起排班数覆盖窗口累计需求，单位米。
     */
    protected BigDecimal stockCoverageDemandQty;
    /**
     * 覆盖窗口累计需求扣除班初滚动库存后的差额，单位米。
     */
    protected BigDecimal stockCoverageStockGapQty;
    /**
     * 是否命中剩余库存起排班数覆盖规则。
     */
    protected Boolean stockCoverageCovered;
    /**
     * 来源成型行 CLASS1~CLASS8 对应的物料编码，用于覆盖窗口逐班隔离 RECIPE 物料切换。
     */
    protected Map<Integer, String> formingClassProductCodeMap = new LinkedHashMap<>();
    /** 保证范围内成型产品需求量，单位米 */
    protected BigDecimal guardDemandQty;
    /** 库存供应时长窗口的逐班成型需求，首项为当班对应的成型需求，后续为备库保证窗口。 */
    protected Map<Integer, BigDecimal> formingGuardWindowQtyMap = new LinkedHashMap<>();
    /** 库存供应时长窗口内按逻辑班次记录的实际班次时长。 */
    protected Map<Integer, BigDecimal> formingGuardWindowHoursMap = new LinkedHashMap<>();
    /** 来源成型行 CLASS1~CLASS8 的原始计划条数，仅用于重建零需求提前候选保证窗口。 */
    protected Map<Integer, BigDecimal> formingClassQtyMap = new LinkedHashMap<>();
    /** 来源成型行 CLASS1~CLASS8 对应的产品换算长度，单位米，仅用于重建保证窗口。 */
    protected Map<Integer, BigDecimal> formingClassLengthMap = new LinkedHashMap<>();

    /**
     * 设置库存覆盖窗口并复制对象，避免派生任务与来源任务共享可变窗口。
     *
     * @param stockCoverageWindow 库存覆盖窗口
     */
    public void setStockCoverageWindow(ScheduleStockCoverageWindowModel stockCoverageWindow) {
        this.stockCoverageWindow = stockCoverageWindow == null ? null : stockCoverageWindow.copy();
    }
    /** 来源行硫化余量封顶条数，空值表示不封顶。 */
    protected BigDecimal formingGuardFormingQtyLimit;
    /** 当前班开始滚动库存，单位米 */
    protected BigDecimal rollingStockQty;
    /** 6 点产品库存快照，单位米 */
    protected BigDecimal sixClockStockQty;
    /** 月计划剩余量快照，沿用上游任务值；无值时保持为空。 */
    protected BigDecimal monthSurplusQty;
    /** 库存最低保证班数 */
    protected Integer guardShiftCount;
    /** 保证范围总小时数 */
    protected BigDecimal guardRangeHours;
    /** 库存供应成型时长，单位小时；越小表示库存越紧急 */
    protected BigDecimal supplyHours;
    /** 当前班库存缺口，单位米 */
    protected BigDecimal currentShiftStockGapQty;
    /** 库存保证缺口，单位米 */
    protected BigDecimal stockGapQty;
    /** 本次库存实际抵扣量，单位米（当前班初滚动库存冲减当前任务毛需求） */
    protected BigDecimal stockDeductQty;
    /** 当前任务完成后的交接班预计库存，单位米，供解释表落库 */
    protected BigDecimal planStockQty;
    /** 计划量，单位米 */
    protected BigDecimal planQty;
    /** 收尾标识，1 表示按收尾规格计算 */
    protected String tailFlag;
    /** 收尾成型余量，单位条 */
    protected BigDecimal tailBalanceQty;
    /**
     * 来源成型计划的原始收尾余量，保留 CX_REMAIN_QTY 为空的语义，仅用于来源收尾班次判定。
     */
    protected BigDecimal formingTailRemainQty;
    /**
     * 来源成型计划按原始 CLASS1~CLASS8 累计确定的收尾班次。
     */
    protected Integer formingTailShiftOrder;
    /**
     * 同产品代码所有成型来源中最晚的收尾班次。
     */
    protected Integer productTailShiftOrder;
    /**
     * 同产品代码所有有效成型来源是否均已确定收尾班次。
     */
    protected Boolean productTailDecisionAvailable;
    /** 成型需求对应的原始逻辑班次，取值1到8 */
    protected Integer formingLogicalShiftOrder;
    /**
     * 当前任务实际对应的成型需求班次，按来源日期映射为实际生产日期和班次。
     */
    protected ScheduleFormingShiftKey formingDemandShiftKey;
    /**
     * 当前需求算法实际覆盖的成型班次集合，不包含后续保证窗口。
     */
    protected List<ScheduleFormingShiftKey> formingDemandShiftKeyList = new ArrayList<>();
    /**
     * 当前任务目标生产班次对应的实际生产日期和班次。
     */
    protected ScheduleFormingShiftKey targetProductionShiftKey;
    /**
     * 成型来源收尾计算快照，独立于任务合并结果保存。
     */
    protected List<ScheduleFormingTailSourceModel> formingTailSourceList = new ArrayList<>();
    /**
     * 未来停产需求前移分配明细。
     */
    protected List<ScheduleFutureDemandAllocationModel> futureDemandAllocationList = new ArrayList<>();
    /**
     * 当前任务实际覆盖的成型班次集合，专供普通收尾判定使用。
     */
    protected Set<ScheduleFormingShiftKey> formingCoverageShiftKeySet = new LinkedHashSet<>();
    /**
     * 产品级最终收尾判定结果。
     */
    protected ScheduleProductTailDecisionModel productTailDecision;
    /**
     * 是否为未来停产需求补充任务，避免把复制的数量窗口当成普通收尾覆盖窗口。
     */
    protected Boolean futureShutdownSupplementTask;
    /** 是否命中成型连续停产自动收尾 */
    protected Boolean formingShutdownCloseOutFlag;
    /** 成型连续停产收尾需求量，单位米；按最后开放成型班原始需求计算 */
    protected BigDecimal formingShutdownCloseOutDemandQty;
    /** 损耗率，百分比；兼容旧测试或临时覆盖值 */
    protected BigDecimal lossRate;
    /** 机台确认后最终命中的损耗率，百分比 */
    protected BigDecimal resolvedLossRate;
    /** 损耗率命中层级 */
    protected String lossMatchLevel;
    /** 损耗率命中来源说明 */
    protected String lossMatchSource;
    /** 损耗前计划量，单位米 */
    protected BigDecimal preLossPlanQty;
    /** 工装限制前计划量，单位米 */
    protected BigDecimal planQtyBeforeToolLimit;
    /** 基础应排需求量，单位米 */
    protected BigDecimal baseDemandQty;
    /** 损耗补偿量，单位米 */
    protected BigDecimal lossAddQty;
    /** 工装限制调整量，单位米 */
    protected BigDecimal toolLimitAdjustQty;
    /** 工装限制压掉的待顺延量，单位米 */
    protected BigDecimal toolOverflowQty;
    /** 当前任务计算前全局可用工装数量 */
    protected BigDecimal availableToolQty;
    /** 当前任务实际生产占用的工装数量；成型消耗释放在任务结算时按产品即时记录 */
    protected BigDecimal toolUsedQty;
    /** 当前任务计算后全局剩余工装数量 */
    protected BigDecimal remainingToolQty;
    /** 当前任务最后一次全局工装账本结算序号 */
    protected Integer toolLedgerOrder;
    /** 最小起排调整量，单位米 */
    protected BigDecimal minStartAdjustQty;
    /** 尾数取整或收尾调整量，单位米 */
    protected BigDecimal tailRoundAdjustQty;
    /** 产能调整量，单位米 */
    protected BigDecimal capacityAdjustQty;
    /** 计划量计算公式说明 */
    protected String calcFormulaDesc;
    /** 总工装数量 */
    protected BigDecimal totalToolQty;
    /** 产品卷曲长度 */
    protected BigDecimal curlRollLength;
    /** 默认工装卷曲长度 */
    protected BigDecimal defaultCurlRollLength;
    /** 最小起排量 */
    protected BigDecimal minStartQty;
    /** 机台剩余产能，单位米 */
    protected BigDecimal machineRemainCapacity;
    /** 机台生产速度，单位米/小时 */
    protected BigDecimal machineSpeed;
    /** 检修时长，单位小时 */
    protected BigDecimal maintenanceHours;
    /** 上个规格切换时长，单位小时 */
    protected BigDecimal previousSpecSwitchHours;
    /** 上个胶料切换时长，单位小时 */
    protected BigDecimal previousGlueSwitchHours;
    /** 上个主胶料切换固定产能扣减量，单位米 */
    protected BigDecimal previousGlueSwitchCapacityDeduct;
    /** 按当前班次开始时间和库存供应时长推算的库存不足时间 */
    protected Date stockShortageTime;
    /** 按统一默认速度折算的预计生产小时数 */
    protected BigDecimal estimatedProductionHours;
    /** 扣除工艺停放时间和预计生产时间后的最晚开始时间 */
    protected Date latestStartTime;
    /** 是否命中定点生产机台 */
    protected Boolean fixedMachineMatched;
    /** 需求量，单位米 */
    protected BigDecimal demandQty;
    /** 未排原因编码 */
    protected String unplannedReasonCode;
    /** 未排原因描述 */
    protected String unplannedReasonDesc;
    /** 业务键后缀，用于拆分来源任务或顺延任务，避免同规格同班次任务业务键冲突 */
    protected String businessKeySuffix;
    /** 同产品同班次计划量汇总组业务键 */
    protected String planGroupKey;
    /** 汇总组包含的原始来源任务业务键列表 */
    protected java.util.List<String> sourceTaskBusinessKeyList;
    /** 是否为仅用于落库追溯的原始来源解释任务 */
    protected Boolean sourceExplainTask;
    /** 来源任务参与汇总计算的需求量 */
    protected BigDecimal sourceRequiredQty;
    /** 汇总组来源任务数量 */
    protected Integer groupSourceCount;
    /** 汇总组库存抵扣前需求量 */
    protected BigDecimal groupRequiredQty;
    /** 汇总组库存抵扣后基础需求量 */
    protected BigDecimal groupBaseDemandQty;
    /** 汇总组最小起排调整量 */
    protected BigDecimal groupMinStartAdjustQty;
    /** 汇总组收尾或卷曲取整调整量 */
    protected BigDecimal groupRoundAdjustQty;
    /** 汇总组最终计划量 */
    protected BigDecimal groupFinalPlanQty;
    /** 两班库存判断中由下一排程班需求反向生成的当班候选标识 */
    protected Boolean twoShiftLeadTask;
    /** 是否命中小胶种连续生产规则 */
    protected Boolean smallGlueFlag;

    /** 新规格判断与提前排产证据。 */
    protected ScheduleNewSpecInfoModel newSpecInfo;
    /** 实验规格判断与提前备库窗口证据。 */
    protected ScheduleExperimentSpecInfoModel experimentSpecInfo;

    /**
     * 设置成型备库窗口明细并复制容器，避免派生任务与来源任务共享可变映射。
     *
     * @param formingGuardWindowQtyMap 窗口班次明细
     */
    public void setFormingGuardWindowQtyMap(Map<Integer, BigDecimal> formingGuardWindowQtyMap) {
        this.formingGuardWindowQtyMap = formingGuardWindowQtyMap == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(formingGuardWindowQtyMap);
    }

    /**
     * 设置成型备库窗口实际时长明细并复制容器。
     *
     * @param formingGuardWindowHoursMap 窗口班次实际时长明细
     */
    public void setFormingGuardWindowHoursMap(Map<Integer, BigDecimal> formingGuardWindowHoursMap) {
        this.formingGuardWindowHoursMap = formingGuardWindowHoursMap == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(formingGuardWindowHoursMap);
    }

    /**
     * 设置来源成型班次计划条数并复制容器。
     *
     * @param formingClassQtyMap CLASS1~CLASS8 计划条数
     */
    public void setFormingClassQtyMap(Map<Integer, BigDecimal> formingClassQtyMap) {
        this.formingClassQtyMap = formingClassQtyMap == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(formingClassQtyMap);
    }

    /**
     * 设置来源成型班次换算长度并复制容器。
     *
     * @param formingClassLengthMap CLASS1~CLASS8 换算长度
     */
    public void setFormingClassLengthMap(Map<Integer, BigDecimal> formingClassLengthMap) {
        this.formingClassLengthMap = formingClassLengthMap == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(formingClassLengthMap);
    }

    /**
     * 设置来源成型班次物料编码快照并复制容器。
     *
     * @param formingClassProductCodeMap CLASS1~CLASS8 物料编码
     */
    public void setFormingClassProductCodeMap(Map<Integer, String> formingClassProductCodeMap) {
        this.formingClassProductCodeMap = formingClassProductCodeMap == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(formingClassProductCodeMap);
    }

    /**
     * 设置成型来源收尾快照并复制容器。
     *
     * @param formingTailSourceList 来源收尾快照
     */
    public void setFormingTailSourceList(List<ScheduleFormingTailSourceModel> formingTailSourceList) {
        this.formingTailSourceList = formingTailSourceList == null
                ? new ArrayList<>() : new ArrayList<>(formingTailSourceList);
    }

    /**
     * 设置未来停产前移分配明细并复制容器。
     *
     * @param futureDemandAllocationList 前移分配明细
     */
    public void setFutureDemandAllocationList(
            List<ScheduleFutureDemandAllocationModel> futureDemandAllocationList) {
        this.futureDemandAllocationList = futureDemandAllocationList == null
                ? new ArrayList<>() : new ArrayList<>(futureDemandAllocationList);
    }

    /**
     * 设置当前需求覆盖的成型班次并复制容器。
     *
     * @param formingDemandShiftKeyList 当前需求覆盖班次
     */
    public void setFormingDemandShiftKeyList(List<ScheduleFormingShiftKey> formingDemandShiftKeyList) {
        this.formingDemandShiftKeyList = formingDemandShiftKeyList == null
                ? new ArrayList<>() : new ArrayList<>(formingDemandShiftKeyList);
    }

    /**
     * 设置实际覆盖成型班次并复制容器。
     *
     * @param formingCoverageShiftKeySet 覆盖班次集合
     */
    public void setFormingCoverageShiftKeySet(Set<ScheduleFormingShiftKey> formingCoverageShiftKeySet) {
        this.formingCoverageShiftKeySet = formingCoverageShiftKeySet == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(formingCoverageShiftKeySet);
    }

    /**
     * 判断任务是否未分配机台。
     *
     * @return true 表示未分配机台
     */
    public boolean isUnassigned() {
        return this.machineCode == null || this.machineCode.trim().isEmpty();
    }

    /**
     * 生成任务稳定业务键。
     *
     * @return 按产品、胶料、口型、班次和可选后缀生成的稳定业务键
     */
    @Override
    public String getBusinessKey() {
        String businessKey = String.join("|", this.safe(this.processCode), this.safe(this.glueCode),
                this.safe(this.mouthPlateCode), this.safe(this.shiftOrder));
        if (this.businessKeySuffix == null || this.businessKeySuffix.trim().isEmpty()) {
            return businessKey;
        }
        return businessKey + "|" + this.businessKeySuffix;
    }

    /** @return 领域产品标准长度，基础模型不提供 */
    @Override
    public BigDecimal getQualityProductLength() {
        return null;
    }

    /** @return 领域口型板切换标记，基础模型默认未切换 */
    @Override
    public Boolean getQualityMouthPlateSwitched() {
        return Boolean.FALSE;
    }

    /** @return 统一产品工序编码 */
    public String getProcessCode() {
        return this.processCode;
    }

    /** @return 新规格公共证据 */
    public ScheduleNewSpecInfoModel getCommonNewSpecInfo() {
        return this.newSpecInfo;
    }

    /** @return 实验规格公共证据 */
    public ScheduleExperimentSpecInfoModel getCommonExperimentSpecInfo() {
        return this.experimentSpecInfo;
    }

    /**
     * 将对象转换为业务键片段。
     *
     * @param value 原始对象
     * @return 非空字符串；空值返回空串
     */
    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

