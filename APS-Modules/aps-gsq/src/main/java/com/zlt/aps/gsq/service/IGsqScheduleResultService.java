package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.dto.GsqChangeMachineDTO;
import com.zlt.aps.gsq.api.domain.dto.GsqInsertOrderDTO;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.vo.GsqInsertTaskRequestVo;
import com.zlt.aps.gsq.api.domain.vo.GsqScheduleShiftDateVO;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 钢丝圈排程结果Service接口
 *
 * @author APS
 */
public interface IGsqScheduleResultService extends IDocService<GsqScheduleResult> {

    /**
     * 自动排程
     * 委托给 GsqEngineService.autoGsqSchedule 执行自动排程
     *
     * @param queryVO 排程参数（含排程日期、分厂编码）
     * @return 排程结果
     */
    AjaxResult autoPlan(GsqScheduleResult queryVO);

    /**
     * 回填胎圈排程结果数据到 TQ_CLASS1~6_PLAN 字段
     * 根据钢丝圈代码和排程日期查询对应的胎圈排程结果，将胎圈6班计划量回填到钢丝圈排程结果
     *
     * @param scheduleList 钢丝圈排程结果列表
     */
    void fillTqPlanQty(List<GsqScheduleResult> scheduleList);

    /**
     * 插单前校验
     * 校验规则：
     * 1. 排程日期不能为空，且需在生产周期内
     * 2. 钢丝圈代码不能为空，施工必须存在
     * 3. 机台编号不能为空
     * 4. 6个班次中至少有一个班次的计划量有值
     * 5. 有计划量的班次，顺序也必须有值；反之亦然
     * 6. 只能往当前班次或后续班次插单
     * 7. 插单只能加到第二个在产规格之后
     *
     * @param dto 插单数据
     * @return 校验结果
     */
    AjaxResult validateInsertOrder(GsqInsertOrderDTO dto);

    /**
     * 插单（旧接口，直接操作数据库，不支持锚点插入和resequence重排）
     *
     * @param dto 插单数据
     * @return 结果
     * @deprecated 已被 {@link #insertTask(GsqInsertTaskRequestVo)} 替代，新接口走任务链路径，支持锚点插入、resequence重排
     */
    @Deprecated
    AjaxResult insertOrder(GsqInsertOrderDTO dto);

    /**
     * 转机台前校验
     *
     * @param dto 转机台数据
     * @return 校验结果
     */
    AjaxResult validateChangeMachine(GsqChangeMachineDTO dto);

    /**
     * 转机台（旧接口，直接操作数据库，不支持锚点和resequence重排）
     *
     * @param dto 转机台数据
     * @return 结果
     * @deprecated 已被 {@link #batchChangeMachine(List)} 替代，新接口走任务链路径，支持锚点、目标班次、批量操作
     */
    @Deprecated
    AjaxResult changeMachine(GsqChangeMachineDTO dto);

    /**
     * 调量前校验
     * 校验规则：
     * 1. 排程记录必须存在且未删除
     * 2. 至少有一个班次的计划量被修改
     * 3. 计划量不能小于0
     * 4. 历史班次不允许修改计划量
     * 5. 非历史班次的计划量不能小于完成量
     *
     * @param entity 调量数据
     * @return 校验结果
     */
    AjaxResult validateChangeQty(GsqScheduleResult entity);

    /**
     * 调量（旧接口，直接操作数据库，不支持resequence重排）
     *
     * @param entity 调量数据
     * @return 结果
     * @deprecated 已被 {@link #batchChangeQty(List)} 替代，新接口走任务链路径，支持批量操作
     */
    @Deprecated
    AjaxResult changeQty(GsqScheduleResult entity);

    /**
     * 逻辑删除前校验
     * 校验规则：
     * 1. 记录必须存在且未删除
     * 2. 发布成功次数必须等于0（已发布成功的计划不允许删除，只能调量）
     * 3. 必须未发送给MES（mesId为空；已发送给MES的计划不允许删除，只能调量）
     *
     * @param ids 需要校验的记录ID列表
     * @return 校验结果（通过返回success，失败返回error及不允许删除的原因）
     */
    AjaxResult validateLogicDelete(List<Long> ids);

    /**
     * 逻辑删除排程记录（旧接口，直接操作数据库，不支持resequence重排）
     * 只能删除发布成功次数等于0且未发送给MES的计划
     * 删除前会执行 validateLogicDelete 校验，校验失败直接返回
     *
     * @param ids 需要删除的记录ID列表
     * @return 结果
     * @deprecated 已被 {@link #batchDelete(List)} 替代，新接口走任务链路径，删除后resequence重排
     */
    @Deprecated
    AjaxResult logicDeleteByIds(List<Long> ids);

    /**
     * 发布排程到MES
     * 仅处理发布状态为"未发布(0)"、"待发布(5)"、"发布失败(2)"的记录，其余状态忽略。
     * 6班→3天拆分映射：
     * Day1(D日)：MID=钢丝圈1班
     * Day2(D+1日)：NIGHT=钢丝圈2班, DAY=钢丝圈3班, MID=钢丝圈4班
     * Day3(D+2日)：NIGHT=钢丝圈5班, DAY=钢丝圈6班
     * TQ_CLASS1~6_PLAN全量传递到每条记录
     *
     * @param queryVO 查询条件（含 scheduleDate、factoryCode、ids）
     * @return 结果
     */
    AjaxResult publish(GsqScheduleResult queryVO);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 唯一性校验
     * 根据排程日期、钢丝圈代码、机台编号校验唯一性
     *
     * @param entity 待校验记录
     * @return UserConstants.UNIQUE="0" 唯一，UserConstants.NOT_UNIQUE="1" 不唯一
     */
    String checkUnique(GsqScheduleResult entity);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录数
     *
     * @param scheduleDate 排程日期
     * @return 记录数
     */
    int isReleasingOrTimeoutByDate(Date scheduleDate);

    /**
     * 根据排程日期构建6个班次的日期展示列表
     * 钢丝圈排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早（D=排程日期-2，即今天）：
     * 班次1：D日中班，班次2~4：D+1日(夜/早/中)，班次5~6：D+2日(夜/早)
     *
     * @param queryVO 查询条件
     * @return 班次日期列表
     */
    List<GsqScheduleShiftDateVO> listScheduleShiftDates(GsqScheduleResult queryVO);

    /**
     * 人工插单（新接口，走任务链路径，支持锚点插入、resequence 重排）。
     *
     * @param vo 插单请求
     * @return 结果
     */
    AjaxResult insertTask(GsqInsertTaskRequestVo vo);

    /**
     * 批量转机台（走任务链路径，支持锚点、目标班次）。
     *
     * @param list 转机台请求列表
     * @return 结果
     */
    AjaxResult batchChangeMachine(List<GsqScheduleResult> list);

    /**
     * 批量调量（走任务链路径）。
     *
     * @param list 调量请求列表
     * @return 结果
     */
    AjaxResult batchChangeQty(List<GsqScheduleResult> list);

    /**
     * 批量删除（走任务链路径，删除后 resequence 重排）。
     *
     * @param ids 排程记录ID列表
     * @return 结果
     */
    AjaxResult batchDelete(List<Long> ids);
}
