package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSub;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢丝圈缠绕盘Service接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface IGsqTwiningDiscService extends IDocService<GsqTwiningDisc> {

    /**
     * 校验缠绕盘编码唯一性
     *
     * @param entity 实体
     * @return 唯一性结果（UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一）
     */
    String checkUnique(GsqTwiningDisc entity);

    /**
     * 保存钢丝圈缠绕盘（主表+子表），事务级联保存
     * - id为空：新增主表及子表
     * - id不为空：更新主表，先删除旧子表再保存新子表
     *
     * @param entity 实体（含 subList 子表数据）
     * @return 操作结果
     */
    AjaxResult saveMainAndSub(GsqTwiningDisc entity);

    /**
     * 删除钢丝圈缠绕盘（逻辑删除主表，并级联逻辑删除子表）
     *
     * @param ids 主表ID集合
     * @return 操作结果
     */
    AjaxResult removeMainAndSub(List<Long> ids);

    /**
     * 获取主表反显公式（用于列表/导出反显）
     *
     * @return 反显公式数组
     */
    String[] getQueryFormulas();

    /**
     * 获取子表反显公式（钢丝圈名称根据钢丝圈编号从施工信息表反显）
     *
     * @return 反显公式数组
     */
    String[] getSubQueryFormulas();

    /**
     * 根据主表ID查询子表数据并反显钢丝圈名称
     *
     * @param discId 主表ID
     * @return 子表列表（含反显名称）
     */
    List<GsqTwiningDiscSub> querySubListByDiscId(Long discId);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqTwiningDisc> list, boolean updateSupport, Long importLogId);
}
