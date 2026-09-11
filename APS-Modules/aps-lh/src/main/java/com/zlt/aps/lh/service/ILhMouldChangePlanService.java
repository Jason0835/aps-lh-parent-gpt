package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 模具交替计划Service接口
 *
 * @author APS Team
 * @since 2026/04/01 11:
 */
public interface ILhMouldChangePlanService extends IDocService<LhMouldChangePlan> {

    String[] getQueryFormulas();

    /**
     * 解析模具交替计划最终展示的模具号。
     *
     * <p>方法按计划记录的分厂和排程日期匹配模具号，并将原模具号与匹配结果取并集后回填。</p>
     *
     * @param planList 模具交替计划列表
     * @return 回填最终模具号后的模具交替计划列表
     */
    List<LhMouldChangePlan> resolveMouldCode(List<LhMouldChangePlan> planList);

    /**
     * 刷新指定分厂、排程日期和硫化结果批次的模具号。
     *
     * @param factoryCode     工厂编码
     * @param scheduleDate    排程日期
     * @param lhResultBatchNo 硫化结果批次号
     */
    void refreshMouldCode(String factoryCode, Date scheduleDate, String lhResultBatchNo);

    /**
     * 排程发布
     * @param ids 记录ID列表
     * @return 发布结果
     */
    AjaxResult issueSchedule(List<Long> ids);
}
