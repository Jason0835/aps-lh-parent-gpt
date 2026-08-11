package com.zlt.aps.mp.factory.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.mp.api.domain.entity.MpAdjustPlanRequireInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 计划调整信息业务 服务接口
 *
 * @author ZLT
 * 20260716
 */
public interface IMpAdjustPlanRequireInfoService extends IDocService<MpAdjustPlanRequireInfo> {
    /**
     * 根据查询条件，查询信息
     *
     * @param wrapper
     * @return
     */
    List<MpAdjustPlanRequireInfo> getListByCondition(QueryWrapper<MpAdjustPlanRequireInfo> wrapper);
}
