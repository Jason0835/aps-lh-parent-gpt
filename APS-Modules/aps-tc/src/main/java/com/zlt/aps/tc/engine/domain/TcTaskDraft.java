package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎侧待排任务草稿。
 *
 * <p>用于在引擎内承载需求、计划量、胶料、口型和机台约束等排程中间数据。
 * 该对象只保存算法输入输出，不直接写数据库，不修改任务链。</p>
 */
@Data
public class TcTaskDraft {

    /** 工单号 */
    private String orderNo;

    /** 来源成型工单号集合，仅用于解释追踪，不写入胎侧排程结果工单号 */
    private String sourceOrderNos;

    /** 胎侧规格编码 */
    private String sidewallCode;

    /** 胎侧施工版本快照 */
    private String constructionVersion;

    /** 胎侧工艺快照 */
    private String sidewallCraft;

    /** 主胶料编码 */
    private String glueCode;

    /** 基部胶编码，当前只有一个编码时基部胶相似个数退化为 0 或 1 */
    private String baseGlueCode;

    /** 口型板编码 */
    private String mouthPlateCode;

    /** 机台编码 */
    private String machineCode;

    /** 班次顺序 */
    private Integer shiftOrder;

    /** TASK_SORT 阶段生成的稳定基础优先级，数值越小越优先 */
    private Integer baseSortIndex;

    /** 当前班成型胎侧需求量，单位米 */
    private BigDecimal currentShiftDemandQty;

    /** 保证范围内成型胎侧需求量，单位米 */
    private BigDecimal guardDemandQty;

    /** 当前班开始滚动库存，单位米 */
    private BigDecimal rollingStockQty;

    /** 6 点胎侧库存快照，单位米 */
    private BigDecimal sixClockStockQty;

    /** 库存最低保证班数 */
    private Integer guardShiftCount;

    /** 保证范围总小时数 */
    private BigDecimal guardRangeHours;

    /** 库存供应成型时长，单位小时；越小表示库存越紧急 */
    private BigDecimal supplyHours;

    /** 当前班库存缺口，单位米 */
    private BigDecimal currentShiftStockGapQty;

    /** 库存保证缺口，单位米 */
    private BigDecimal stockGapQty;

    /** 本次库存实际抵扣量，单位米（当前班初滚动库存冲减当前任务毛需求） */
    private BigDecimal stockDeductQty;

    /** 当前任务完成后的交接班预计库存，单位米，供解释表落库 */
    private BigDecimal planStockQty;

    /** 计划量，单位米 */
    private BigDecimal planQty;

    /** 胎侧肩长，单位米 */
    private BigDecimal sidewallLength;

    /** 胎侧胶重量快照 */
    private BigDecimal sidewallWeight;

    /** 胎侧耐磨胶重量快照 */
    private BigDecimal sidewallWearpRubberWeight;

    /** 月计划剩余量快照 */
    private BigDecimal monthSurplusQty;

    /** 收尾标识，1 表示按收尾规格计算 */
    private String tailFlag;

    /** 收尾成型余量，单位条 */
    private BigDecimal tailBalanceQty;

    /** 损耗率，百分比；兼容旧测试或临时覆盖值 */
    private BigDecimal lossRate;

    /** 机台确认后最终命中的损耗率，百分比 */
    private BigDecimal resolvedLossRate;

    /** 损耗率命中层级 */
    private String lossMatchLevel;

    /** 损耗率命中来源说明 */
    private String lossMatchSource;

    /** 损耗前计划量，单位米 */
    private BigDecimal preLossPlanQty;

    /** 工装限制前计划量，单位米 */
    private BigDecimal planQtyBeforeToolLimit;

    /** 基础应排需求量，单位米 */
    private BigDecimal baseDemandQty;

    /** 损耗补偿量，单位米 */
    private BigDecimal lossAddQty;

    /** 工装限制调整量，单位米 */
    private BigDecimal toolLimitAdjustQty;

    /** 工装限制压掉的待顺延量，单位米 */
    private BigDecimal toolOverflowQty;

    /** 当前任务计算前全局可用工装数量 */
    private BigDecimal availableToolQty;

    /** 当前任务净占用的工装数量，生产为正、成型消耗库存释放占用为负 */
    private BigDecimal toolUsedQty;

    /** 当前任务计算后全局剩余工装数量 */
    private BigDecimal remainingToolQty;

    /** 当前任务最后一次全局工装账本结算序号 */
    private Integer toolLedgerOrder;
    /** 最小起排调整量，单位米 */
    private BigDecimal minStartAdjustQty;

    /** 尾数取整或收尾调整量，单位米 */
    private BigDecimal tailRoundAdjustQty;

    /** 产能调整量，单位米 */
    private BigDecimal capacityAdjustQty;

    /** 计划量计算公式说明 */
    private String calcFormulaDesc;

    /** 总工装数量 */
    private BigDecimal totalToolQty;

    /** 胎侧卷曲长度 */
    private BigDecimal curlRollLength;

    /** 默认工装卷曲长度 */
    private BigDecimal defaultCurlRollLength;

    /** 最小起排量 */
    private BigDecimal minStartQty;

    /** 机台剩余产能，单位米 */
    private BigDecimal machineRemainCapacity;

    /** 机台生产速度，单位米/小时 */
    private BigDecimal machineSpeed;

    /** 检修时长，单位小时 */
    private BigDecimal maintenanceHours;

    /** 上个规格切换时长，单位小时 */
    private BigDecimal previousSpecSwitchHours;

    /** 上个胶料切换时长，单位小时 */
    private BigDecimal previousGlueSwitchHours;

    /** 上个主胶料切换固定产能扣减量，单位米 */
    private BigDecimal previousGlueSwitchCapacityDeduct;

    /** 上个任务口型板是否切换(与前一有效任务口型不同且均非空)，用于规格切换次数统计(详设§14.11 胶料/口型) */
    private Boolean previousMouthPlateSwitched;

    /** 按当前班次开始时间和库存供应时长推算的库存不足时间 */
    private Date stockShortageTime;

    /** 按统一默认速度折算的预计生产小时数 */
    private BigDecimal estimatedProductionHours;

    /** 扣除工艺停放时间和预计生产时间后的最晚开始时间 */
    private Date latestStartTime;

    /** 是否命中定点生产机台 */
    private Boolean fixedMachineMatched;

    /** 需求量，单位米 */
    private BigDecimal demandQty;

    /** 未排原因编码 */
    private String unplannedReasonCode;

    /** 未排原因描述 */
    private String unplannedReasonDesc;

    /** 业务键后缀，用于拆分来源任务或顺延任务，避免同规格同班次任务业务键冲突 */
    private String businessKeySuffix;

    /** 同胎侧同班次计划量汇总组业务键 */
    private String planGroupKey;

    /** 汇总组包含的原始来源任务业务键列表 */
    private java.util.List<String> sourceTaskBusinessKeyList;

    /** 是否为仅用于落库追溯的原始来源解释任务 */
    private Boolean sourceExplainTask;

    /** 来源任务参与汇总计算的需求量 */
    private BigDecimal sourceRequiredQty;

    /** 汇总组来源任务数量 */
    private Integer groupSourceCount;

    /** 汇总组库存抵扣前需求量 */
    private BigDecimal groupRequiredQty;

    /** 汇总组库存抵扣后基础需求量 */
    private BigDecimal groupBaseDemandQty;

    /** 汇总组最小起排调整量 */
    private BigDecimal groupMinStartAdjustQty;

    /** 汇总组收尾或卷曲取整调整量 */
    private BigDecimal groupRoundAdjustQty;

    /** 汇总组最终计划量 */
    private BigDecimal groupFinalPlanQty;

    /** 新规格判断与提前排产证据 */
    private TcNewSpecInfo newSpecInfo;

    /** 是否命中小胶种连续生产规则 */
    private Boolean smallGlueFlag;
    /** 实验规格判断与固定计划量证据 */
    private TcExperimentSpecInfo experimentSpecInfo;

    /**
     * 判断任务是否未分配机台。
     *
     * @return true 表示未分配机台
     */
    public boolean isUnassigned() {
        return machineCode == null || machineCode.trim().isEmpty();
    }

    /**
     * 生成任务业务键。
     *
     * @return 稳定业务键；按胎侧、胶料、口型、班次和可选后缀定位任务，避免解释追踪互相覆盖
     */
    public String getBusinessKey() {
        String businessKey = String.join("|", safe(sidewallCode), safe(glueCode), safe(mouthPlateCode), safe(shiftOrder));
        if (businessKeySuffix == null || businessKeySuffix.trim().isEmpty()) {
            return businessKey;
        }
        return businessKey + "|" + businessKeySuffix;
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
