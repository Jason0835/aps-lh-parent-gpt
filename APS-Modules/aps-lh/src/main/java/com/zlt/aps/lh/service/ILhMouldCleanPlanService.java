package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhMouldCleanPlan;
import com.zlt.bill.common.service.IDocService;

/**
 * 模具清洗计划Service接口
 *
 * @author APS Team
 * @since 2026/04/10
 */
public interface ILhMouldCleanPlanService extends IDocService<LhMouldCleanPlan> {

    /**
     * 从模具清洗预警同步生成计划（增量，只取最新版本号预警）
     *
     * @return 同步数量
     */
    int syncFromMouldCleanWarn();

    /**
     * 基于全部预警数据全量生成清洗计划（不限制版本号）
     * 用于临时任务：清空后重新同步全部预警数据后，基于所有预警数据重新生成计划
     * 与syncFromMouldCleanWarn的区别：不限制DATA_VERSION，取全部预警数据来生成计划
     *
     * @return 同步数量
     */
    int syncAllFromMouldCleanWarn();

    /**
     * 清空模具清洗预警和清洗计划表全部数据（物理删除）
     * 用于临时任务：重新同步前先清空旧数据
     */
    void cleanAllWarnAndPlan();
}
