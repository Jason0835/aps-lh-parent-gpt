package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 已准入的实际续作收尾集合及只读节点画像，后置匹配不得增减参与机台。
 *
 * @author Codex
 * @date 2026/10/21
 */
@Data
public class ContinuationRemainderFinishGroup {
    /** 原始需求对象，沿用生产余量和日计划账本。 */
    private SkuScheduleDTO sourceSku;
    /** 整组提交时冻结的合法总目标，包含同物料状态的固定排量；保存前用于缺口核对。 */
    private Integer finalTargetQty;
    /** 原机真实容量计算并完成整组提交后的结果，供续作补偿和新增入口共用完成能力结论。 */
    private ContinuationFinishScheduleResult originalMachineFinishPlan;
    /**
     * 节点计算时冻结的物理容量画像，历史匹配只读取，不重新计算节点或选择机台。
     */
    private List<ContinuationEndingMachineProfile> machineProfiles;
    /**
     * 节点计算时已有的后续动作硬截止，匹配提交仍须逐机校验。
     */
    private Map<String, Date> nextActions;
    /** 本组全部原结果，提交时清除未保留的初始排量。 */
    private List<LhScheduleResult> originalResults;
    /** 按既有下机顺序冻结的参与结果；L/R成组后仅T日大余量角色改用保留顺序。 */
    private List<LhScheduleResult> participatingResults;
    /** 前置停产保机结果；余量收尾不因月计划降模新增该身份，已有占用由上下文约束。 */
    private List<LhScheduleResult> stopHoldResults;
}
