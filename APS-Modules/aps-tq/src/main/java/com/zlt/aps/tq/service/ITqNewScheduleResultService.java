package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqChangeMachineDTO;
import com.zlt.aps.tq.api.domain.dto.TqInsertOrderDTO;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎圈排程结果Service接口（新版）
 *
 * @author APS
 */
public interface ITqNewScheduleResultService extends IDocService<TqNewScheduleResult> {

    /**
     * 插单前校验
     *
     * @param dto 插单数据
     * @return 校验结果
     */
    AjaxResult validateInsertOrder(TqInsertOrderDTO dto);

    /**
     * 插单
     *
     * @param dto 插单数据
     * @return 结果
     */
    AjaxResult insertOrder(TqInsertOrderDTO dto);

    /**
     * 转机台前校验
     *
     * @param dto 转机台数据
     * @return 校验结果
     */
    AjaxResult validateChangeMachine(TqChangeMachineDTO dto);

    /**
     * 转机台
     *
     * @param dto 转机台数据
     * @return 结果
     */
    AjaxResult changeMachine(TqChangeMachineDTO dto);

    /**
     * 调量
     *
     * @param entity 调量数据
     * @return 结果
     */
    AjaxResult changeQty(TqNewScheduleResult entity);

    /**
     * 逻辑删除排程记录
     *
     * @param ids 需要删除的记录ID列表
     * @return 结果
     */
    AjaxResult logicDeleteByIds(List<Long> ids);

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
}
