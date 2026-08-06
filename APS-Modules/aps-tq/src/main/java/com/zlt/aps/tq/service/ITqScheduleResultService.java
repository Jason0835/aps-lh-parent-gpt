package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqChangeMachineDTO;
import com.zlt.aps.tq.api.domain.dto.TqInsertOrderDTO;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.domain.vo.TqInsertTaskRequestVo;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎圈排程结果Service接口
 *
 * @author APS
 */
public interface ITqScheduleResultService extends IDocService<TqScheduleResult> {

    /**
     * 插单前校验
     *
     * @param dto 插单数据
     * @return 校验结果
     */
    AjaxResult validateInsertOrder(TqInsertOrderDTO dto);

    /**
     * 插单（旧接口，直接操作数据库，不支持锚点插入和resequence重排）
     *
     * @param dto 插单数据
     * @return 结果
     * @deprecated 已被 {@link #insertTask(TqInsertTaskRequestVo)} 替代，新接口走任务链路径，支持锚点插入、resequence重排
     */
    @Deprecated
    AjaxResult insertOrder(TqInsertOrderDTO dto);

    /**
     * 转机台前校验
     *
     * @param dto 转机台数据
     * @return 校验结果
     */
    AjaxResult validateChangeMachine(TqChangeMachineDTO dto);

    /**
     * 转机台（旧接口，直接操作数据库，不支持锚点和resequence重排）
     *
     * @param dto 转机台数据
     * @return 结果
     * @deprecated 已被 {@link #batchChangeMachine(List)} 替代，新接口走任务链路径，支持锚点、目标班次、批量操作
     */
    @Deprecated
    AjaxResult changeMachine(TqChangeMachineDTO dto);

    /**
     * 调量前校验
     *
     * @param entity 调量数据
     * @return 校验结果
     */
    AjaxResult validateChangeQty(TqScheduleResult entity);

    /**
     * 调量（旧接口，直接操作数据库，不支持resequence重排）
     *
     * @param entity 调量数据
     * @return 结果
     * @deprecated 已被 {@link #batchChangeQty(List)} 替代，新接口走任务链路径，支持批量操作
     */
    @Deprecated
    AjaxResult changeQty(TqScheduleResult entity);

    /**
     * 逻辑删除排程记录（旧接口，直接操作数据库，不支持resequence重排）
     *
     * @param ids 需要删除的记录ID列表
     * @return 结果
     * @deprecated 已被 {@link #batchDelete(List)} 替代，新接口走任务链路径，删除后resequence重排
     */
    @Deprecated
    AjaxResult logicDeleteByIds(List<Long> ids);

    /**
     * 发布排程到MES
     *
     * @param queryVO 查询条件（含排程日期等）
     * @return 结果
     */
    AjaxResult publish(TqScheduleResult queryVO);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 人工插单（新接口，走任务链路径，支持锚点插入、resequence 重排）。
     *
     * @param vo 插单请求
     * @return 结果
     */
    AjaxResult insertTask(TqInsertTaskRequestVo vo);

    /**
     * 批量转机台（走任务链路径，支持锚点、目标班次）。
     *
     * @param list 转机台请求列表
     * @return 结果
     */
    AjaxResult batchChangeMachine(List<TqScheduleResult> list);

    /**
     * 批量调量（走任务链路径）。
     *
     * @param list 调量请求列表
     * @return 结果
     */
    AjaxResult batchChangeQty(List<TqScheduleResult> list);

    /**
     * 批量删除（走任务链路径，删除后 resequence 重排）。
     *
     * @param ids 排程记录ID列表
     * @return 结果
     */
    AjaxResult batchDelete(List<Long> ids);
}
