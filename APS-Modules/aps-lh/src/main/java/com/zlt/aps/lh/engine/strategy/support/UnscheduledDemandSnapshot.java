package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;

import java.time.LocalDate;

/**
 * 硫化未排原始需求快照。
 *
 * <p>快照在目标量清零、候选移除和提前生产日期调整前建立，只服务未排分类和诊断，
 * 不参与排产准入、排序、排量或资源扣账。</p>
 *
 * @author APS
 */
@Data
public class UnscheduledDemandSnapshot {

    /** 本批次内稳定的原始需求标识。 */
    private String demandKey;
    /** 需求来源类型。 */
    private String sourceType;
    /** 物料编码。 */
    private String materialCode;
    /** 产品状态。 */
    private String productStatus;
    /** 正常开产日期，禁止写入受限后的实际开产日期。 */
    private LocalDate normalProductionDate;
    /** 原始需求是否仍属于本次有效需求。 */
    private boolean effectiveDemand;
    /** 原始需求数量，仅用于诊断展示。 */
    private int originalDemandQty;
    /** 月计划年份。 */
    private Integer monthPlanYear;
    /** 月计划月份。 */
    private Integer monthPlanMonth;
    /** 月计划需求版本。 */
    private String monthPlanVersion;
    /** 月计划排产版本。 */
    private String productionVersion;

    /**
     * 复制快照，供候选试算回滚保存独立状态。
     *
     * @return 快照副本
     */
    public UnscheduledDemandSnapshot copy() {
        UnscheduledDemandSnapshot target = new UnscheduledDemandSnapshot();
        target.setDemandKey(demandKey);
        target.setSourceType(sourceType);
        target.setMaterialCode(materialCode);
        target.setProductStatus(productStatus);
        target.setNormalProductionDate(normalProductionDate);
        target.setEffectiveDemand(effectiveDemand);
        target.setOriginalDemandQty(originalDemandQty);
        target.setMonthPlanYear(monthPlanYear);
        target.setMonthPlanMonth(monthPlanMonth);
        target.setMonthPlanVersion(monthPlanVersion);
        target.setProductionVersion(productionVersion);
        return target;
    }
}
