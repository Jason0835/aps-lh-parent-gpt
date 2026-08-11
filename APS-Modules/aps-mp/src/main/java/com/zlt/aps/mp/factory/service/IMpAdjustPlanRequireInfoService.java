package com.zlt.aps.mp.factory.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
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
     * @param wrapper 查询包装器
     * @return 查询结果
     */
    List<MpAdjustPlanRequireInfo> getListByCondition(QueryWrapper<MpAdjustPlanRequireInfo> wrapper);

    /**
     * 导入计划调整需求信息数据
     *
     * @param list          导入数据
     * @param updateSupport 已存在数据是否更新
     * @param importLogId   导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<MpAdjustPlanRequireInfo> list, boolean updateSupport, Long importLogId);
}
