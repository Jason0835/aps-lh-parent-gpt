package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
    /** 滚动库存是否已覆盖当班与下一排程班需求 */
    protected Boolean twoShiftStockCovered;
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
    /** 成型需求对应的原始逻辑班次，取值1到8 */
    protected Integer formingLogicalShiftOrder;
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

