package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqLossRate;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢丝圈损耗率管理Service接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface IGsqLossRateService extends IDocService<GsqLossRate> {

    /**
     * 校验"钢丝圈编码+机台编码"组合唯一性
     *
     * @param entity 实体
     * @return 唯一性结果（UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一）
     */
    String checkUnique(GsqLossRate entity);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqLossRate> list, boolean updateSupport, Long importLogId);
}
