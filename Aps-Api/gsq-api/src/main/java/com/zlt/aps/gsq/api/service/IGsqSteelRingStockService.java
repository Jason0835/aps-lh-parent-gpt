package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqSteelRingStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢丝圈库存管理Service接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface IGsqSteelRingStockService extends IDocService<GsqSteelRingStock> {

    /**
     * 校验"库存日期+钢丝圈代码"组合唯一性
     *
     * @param entity 实体
     * @return 唯一性结果（UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一）
     */
    String checkUnique(GsqSteelRingStock entity);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqSteelRingStock> list, boolean updateSupport, Long importLogId);
}
