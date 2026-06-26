package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 胎圈备库班数配置 Service接口
 *
 * @author zlt
 * @date 2026-06-25
 */
public interface ITqStockShiftConfigService extends IDocService<TqStockShiftConfig> {

    /**
     * 校验唯一性
     * @param config 配置对象
     * @return UserConstants.UNIQUE 唯一 / UserConstants.NOT_UNIQUE 不唯一
     */
    String checkUnique(TqStockShiftConfig config);

    /**
     * 校验配置规则的交叉情况
     * 校验新增/修改的规则是否与现有规则存在范围交叉
     * <p>
     * 规则说明：
     * - MACHINE_RANGE 与 MACHINE_COUNT 组合构成范围条件
     * - 不同规则的范围不允许有交集，确保任意台数值最多只命中一条规则
     * - 例如：已有「GE 3」(≥3)，不允许再新增「LE 5」(≤5)，因为台数4同时满足两条规则
     * </p>
     *
     * @param config 配置实体
     * @return UserConstants.UNIQUE 表示无交叉（校验通过），UserConstants.NOT_UNIQUE 表示存在交叉
     */
    String checkRangeCross(TqStockShiftConfig config);

    /**
     * 查询配置列表
     * @param config 查询条件
     * @return 配置列表
     */
    List<TqStockShiftConfig> listStockShiftConfig(TqStockShiftConfig config);

    /**
     * 删除全部(逻辑删)
     */
    void deleteAll();
}
