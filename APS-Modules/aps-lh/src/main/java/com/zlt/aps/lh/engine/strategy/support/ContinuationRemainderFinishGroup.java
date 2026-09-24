package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import lombok.Data;
import java.util.List;

/** 已准入的实际续作收尾集合；不按月计划目标台数裁减，计算阶段不得增减物理机台。 */
@Data
public class ContinuationRemainderFinishGroup {
    /** 原始需求对象，沿用生产余量和日计划账本。 */
    private SkuScheduleDTO sourceSku;
    /** 本组全部原结果，提交时清除未保留的初始排量。 */
    private List<LhScheduleResult> originalResults;
    /** 按既有下机顺序冻结的参与结果；L/R成组后仅T日大余量角色改用保留顺序。 */
    private List<LhScheduleResult> participatingResults;
    /** 前置停产保机结果；余量收尾不因月计划降模新增该身份，已有占用由上下文约束。 */
    private List<LhScheduleResult> stopHoldResults;
}
