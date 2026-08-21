package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSpec;
import com.zlt.bill.common.service.IDocService;

/**
 * 钢丝圈缠绕盘-规格关系Service接口
 *
 * @author zlt
 * @date 2026-08-21
 */
public interface IGsqTwiningDiscSpecService extends IDocService<GsqTwiningDiscSpec> {

    /**
     * 校验缠绕盘+钢丝圈规格组合唯一性
     *
     * @param entity 实体（缠绕盘编码+钢丝圈编号）
     * @return 唯一性结果（UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一）
     */
    String checkUnique(GsqTwiningDiscSpec entity);

    /**
     * 保存缠绕盘-规格关系（带业务校验）
     * 校验规则：缠绕盘编码必须存在于缠绕盘主表、钢丝圈编号必须存在于施工信息表、组合唯一；
     * 钢丝圈名称为空时按编号从施工信息表反显
     *
     * @param entity 实体
     * @return 操作结果
     */
    AjaxResult saveWithCheck(GsqTwiningDiscSpec entity);

    /**
     * 主表反显公式（列表SQL已join反显缠绕盘名称/英寸/排列方式，无需公式反显）
     *
     * @return 反显公式数组（空数组）
     */
    String[] getQueryFormulas();
}
