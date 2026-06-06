package com.zlt.aps.mp.factory.service;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.factory.dto.MpSkuAdjustInfoVo;

import java.util.Map;

/**
 * 月计划调整：计划待调整量业务服务
 *
 * @author ZLT
 * @date 20260606
 */
public interface MpSkuAdjustInfoService {
    /**
     * 获取某个需求计划版本对应的所有Sku待调整量信息
     *
     * @param condition
     * @param matchVersion
     * @return key = 物料描述+阶段类型
     */
    Map<String, MpSkuAdjustInfoVo> getPendingQtyInfo(FactoryMonthPlanProductionFinalResult condition, String matchVersion);
}
