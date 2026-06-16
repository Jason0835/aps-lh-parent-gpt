package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面待排任务草稿。
 *
 * <p>用于在引擎内承载需求、计划量、胶料、口型和机台约束等排程中间数据。
 * 该对象只保存算法输入输出，不直接写数据库，不修改任务链。</p>
 */
@Data
public class TmTaskDraft {

    /** 工单号 */
    private String orderNo;

    /** 胎面规格编码 */
    private String treadCode;

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

    /** 当前班成型胎面需求量，单位米 */
    private BigDecimal currentShiftDemandQty;

    /** 保证范围内成型胎面需求量，单位米 */
    private BigDecimal guardDemandQty;

    /** 当前班开始滚动库存，单位米 */
    private BigDecimal rollingStockQty;

    /** 6 点胎面库存快照，单位米 */
    private BigDecimal sixClockStockQty;

    /** 库存最低保证班数 */
    private Integer guardShiftCount;

    /** 保证范围总小时数 */
    private BigDecimal guardRangeHours;

    /** 计划量，单位米 */
    private BigDecimal planQty;

    /** 总工装数量 */
    private BigDecimal totalToolQty;

    /** 胎面卷曲长度 */
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

    /** 是否命中定点生产机台 */
    private Boolean fixedMachineMatched;

    /** 需求量，单位米 */
    private BigDecimal demandQty;

    /** 未排原因编码 */
    private String unplannedReasonCode;

    /** 未排原因描述 */
    private String unplannedReasonDesc;

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
     * @return 稳定业务键；订单号为空时使用胎面、机台、班次组合兜底
     */
    public String getBusinessKey() {
        return String.join("|", orderNo, treadCode, glueCode, mouthPlateCode);
    }

    /**
     * 判断字符串是否非空。
     *
     * @param value 待判断字符串
     * @return true 表示包含非空白字符
     */
    private boolean isNotBlank(String value) {
        return value != null && value.trim().length() > 0;
    }
}
