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
     * 校验唯一性（分厂 + 区间起始机台数）
     * @param config 配置对象
     * @return UserConstants.UNIQUE 唯一 / UserConstants.NOT_UNIQUE 不唯一
     */
    String checkUnique(TqStockShiftConfig config);

    /**
     * 校验配置区间的连续性和完整性
     * <p>
     * 规则说明：
     * - 所有区间段必须连续且不重叠
     * - 第1条 MIN_MACHINE_QTY 必须为 1
     * - 后续行 MIN_MACHINE_QTY = 上一行 MAX_MACHINE_QTY + 1
     * - 只有末行允许 MAX_MACHINE_QTY 为 NULL（无上限）
     * - 若有缺口（未被覆盖的正整数）或重叠，校验失败
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
