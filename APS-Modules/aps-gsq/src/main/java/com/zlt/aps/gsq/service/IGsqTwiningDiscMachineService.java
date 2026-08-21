package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscMachine;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢丝圈缠绕盘-机台关系Service接口
 *
 * @author zlt
 * @date 2026-08-20
 */
public interface IGsqTwiningDiscMachineService extends IDocService<GsqTwiningDiscMachine> {

    /**
     * 校验缠绕盘+机台组合唯一性
     *
     * @param entity 实体（缠绕盘编码+机台编号）
     * @return 唯一性结果（UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一）
     */
    String checkUnique(GsqTwiningDiscMachine entity);

    /**
     * 保存缠绕盘-机台关系（带业务校验）
     * 校验规则：缠绕盘编码必须存在于缠绕盘主表、机台编号必须存在于机台信息表、组合唯一
     *
     * @param entity 实体
     * @return 操作结果
     */
    AjaxResult saveWithCheck(GsqTwiningDiscMachine entity);

    /**
     * 按缠绕盘编码查询机台关系列表（含反显字段）
     *
     * @param twiningDiscCode 缠绕盘编码
     * @return 机台关系列表
     */
    List<GsqTwiningDiscMachine> listByDiscCode(String twiningDiscCode);

    /**
     * 主表反显公式（列表SQL已join反显缠绕盘名称/英寸/排列方式/机台名称，无需公式反显）
     *
     * @return 反显公式数组（空数组）
     */
    String[] getQueryFormulas();
}
