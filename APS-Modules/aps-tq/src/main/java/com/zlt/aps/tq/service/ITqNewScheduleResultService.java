package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;

/**
 * 胎圈排程结果Service接口（新版）
 *
 * @author APS
 */
public interface ITqNewScheduleResultService extends IDocService<TqNewScheduleResult> {

    /**
     * 插单
     *
     * @param entity 插单数据
     * @return 结果
     */
    AjaxResult insertOrder(TqNewScheduleResult entity);

    /**
     * 转机台
     *
     * @param entity 转机台数据
     * @return 结果
     */
    AjaxResult changeMachine(TqNewScheduleResult entity);

    /**
     * 调量
     *
     * @param entity 调量数据
     * @return 结果
     */
    AjaxResult changeQty(TqNewScheduleResult entity);

    /**
     * 发布排程到MES
     *
     * @param queryVO 查询条件（含排程日期等）
     * @return 结果
     */
    AjaxResult publish(TqNewScheduleResult queryVO);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 唯一性校验（排程日期+胎圈代码+机台编号）
     *
     * @param entity 校验数据
     * @return 是否唯一
     */
//    Boolean checkUnique(TqNewScheduleResult entity);
}
