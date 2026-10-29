package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 真实未排分支产生的舍弃量不可变快照，只作记录，不参与排产计算。
 *
 * <p>来源记录仅用于按对象身份撤销试算；数量、原因及原始需求身份均在登记时冻结。
 * 同一原始需求的多条阶段投影归并后，仍可用冻结身份关联最终未排。</p>
 *
 * @author APS
 * @date 2026/10/28
 */
@Getter
public final class EndingDiscardCandidate {

    /**
     * 当前未排投影来源，归并时可转移关联，只使用对象身份，不重新读取冻结数量。
     */
    private final LhUnscheduledResult sourceResult;
    /**
     * 规则命中时的原始需求标识。
     */
    private final String demandKey;
    /**
     * 规则命中时的物料编码。
     */
    private final String materialCode;
    /**
     * 规则命中时的产品状态。
     */
    private final String productStatus;
    /**
     * 规则命中时的未排原因编码。
     */
    private final String reasonCode;
    /**
     * 原规则已经计算的实际余量，不随最终未排数量改写。
     */
    private final int discardPlanQty;

    /**
     * 冻结公共收集器已确认的正量规则命中。
     *
     * @param result 已完成原因和原始需求关联的真实未排记录
     */
    public EndingDiscardCandidate(LhUnscheduledResult result) {
        sourceResult = result;
        demandKey = result.getRuntimeDemandKey();
        materialCode = result.getMaterialCode();
        productStatus = result.getProductStatus();
        reasonCode = result.getUnscheduledReasonCode();
        discardPlanQty = result.getUnscheduledQty();
    }

    /**
     * 转移已归并投影的来源关联，规则命中时的所有数量及业务身份保持不变。
     *
     * @param candidate 原不可变快照
     * @param result 归并后承接该需求的未排投影
     */
    private EndingDiscardCandidate(EndingDiscardCandidate candidate, LhUnscheduledResult result) {
        sourceResult = result;
        demandKey = candidate.demandKey;
        materialCode = candidate.materialCode;
        productStatus = candidate.productStatus;
        reasonCode = candidate.reasonCode;
        discardPlanQty = candidate.discardPlanQty;
    }

    /**
     * 生成承接归并投影的新快照，禁止从归并数量重建舍弃量。
     *
     * @param result 本次归并保留的同需求投影
     * @return 只转移来源关联、保留原数量的新快照
     */
    public EndingDiscardCandidate rebindTo(LhUnscheduledResult result) {
        return sourceResult == result ? this : new EndingDiscardCandidate(this, result);
    }

    /**
     * 构造本轮 SKU 与舍弃原因的分组键，产品状态保持独立。
     *
     * @param result 已确认物料及原因的未排记录
     * @return SKU 与原因分组键
     */
    public static String buildSkuReasonKey(LhUnscheduledResult result) {
        return result.getMaterialCode() + '|' + StringUtils.trimToEmpty(result.getProductStatus())
                + '|' + result.getUnscheduledReasonCode();
    }

    /**
     * 判断最终未排是否仍属于同一需求和同一舍弃原因。
     *
     * @param result 收口后的最终未排记录
     * @return 原始需求、SKU 和原因一致时返回 true
     */
    public boolean matchesFinalResult(LhUnscheduledResult result) {
        boolean sameDemand = StringUtils.isNotEmpty(demandKey)
                ? StringUtils.equals(demandKey, result.getRuntimeDemandKey()) : sourceResult == result;
        return sameDemand && StringUtils.equals(materialCode, result.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(productStatus),
                StringUtils.trimToEmpty(result.getProductStatus()))
                && StringUtils.equals(reasonCode, result.getUnscheduledReasonCode());
    }
}
