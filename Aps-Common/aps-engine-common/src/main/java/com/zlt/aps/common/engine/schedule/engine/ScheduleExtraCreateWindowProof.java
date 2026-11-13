package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 新行窗口门禁证据；静态白名单通过不代表口型资产独占验证通过。
 *
 * <p>停放上下限与生产起止时点门禁已按业务确认下线，本证据只保留需求窗口范围、
 * 成型日历开班与口型静态白名单等需求侧结论；通过的成型逻辑班即全窗口合格消费班，
 * 不再携带生产节点时间字段。</p>
 */
@Data
public class ScheduleExtraCreateWindowProof {
    /**
     * 是否存在同时满足全部窗口条件的真实需求。
     */
    private boolean ready;
    /**
     * 通过门禁的损耗前真实消费总米数，生产净请求不得超过此上限。
     */
    private BigDecimal eligibleDemandQty = BigDecimal.ZERO;
    /**
     * 被排除的正需求米数，不能通过补卷重新加回。
     */
    private BigDecimal excludedDemandQty = BigDecimal.ZERO;
    /**
     * 口型按用户确认采用静态白名单，禁止改名为实物台账验证。
     */
    private String mouthPlateProofMode = "STATIC_ALLOW_LIST";
    /**
     * 实际采用的窗口配置，供解释核对，不是另行计算的参数默认值。
     */
    private Integer currentFormingLogicalShiftOrder;
    private Integer guardShiftCount;
    /**
     * 静态白名单核对的口型编码。
     */
    private String mouthPlateCode;
    /**
     * 通过需求侧门禁的成型逻辑班列表，与需求段顺序一致。
     */
    private List<Integer> eligibleFormingShiftOrders = new ArrayList<>();
    /**
     * 稳定技术原因码，领域边界负责国际化。
     */
    private Set<String> reasonCodes = new LinkedHashSet<>();

    /**
     * 复制证据，派生草稿不得共享可变班次及原因集合。
     *
     * @return 独立证据快照，数值、字符串均为不可变类型
     */
    public ScheduleExtraCreateWindowProof copy() {
        ScheduleExtraCreateWindowProof copy = new ScheduleExtraCreateWindowProof();
        copy.setReady(this.ready);
        copy.setEligibleDemandQty(this.eligibleDemandQty);
        copy.setExcludedDemandQty(this.excludedDemandQty);
        copy.setMouthPlateProofMode(this.mouthPlateProofMode);
        copy.setCurrentFormingLogicalShiftOrder(this.currentFormingLogicalShiftOrder);
        copy.setGuardShiftCount(this.guardShiftCount);
        copy.setMouthPlateCode(this.mouthPlateCode);
        copy.setEligibleFormingShiftOrders(new ArrayList<>(this.eligibleFormingShiftOrders));
        copy.setReasonCodes(new LinkedHashSet<>(this.reasonCodes));
        return copy;
    }
}
