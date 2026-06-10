package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleResultTemplateImportVO;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 硫化排程主服务接口
 * <p>排程入口，负责编排排程全流程</p>
 *
 * @author APS
 */
public interface ILhScheduleService extends IDocService<LhScheduleResult> {

    /**
     * 执行自动排程
     *
     * @param request 排程请求参数
     * @return 排程响应结果
     */
    LhScheduleResponseDTO executeSchedule(LhScheduleRequestDTO request);

    /**
     * 发布排程结果到MES
     *
     * @param batchNo 批次号
     * @return 发布响应结果
     */
    LhScheduleResponseDTO publishSchedule(String batchNo);

    /**
     * 根据排程结束日构建窗口内 8 个班次的日期展示列表
     *
     * @param scheduleDate 排程日期（窗口结束日，取日期部分）；为 null 时返回空列表
     * @return 班次 1～8 与对应 MM/dd，顺序与默认 8 班模板日历日一致
     */
    List<LhScheduleShiftDateVO> listScheduleShiftDates(Date scheduleDate);

    /**
     * 转机台前校验接口
     * @param dto 参数
     * @return 结果
     */
    AjaxResult changeMachinePreCheck(LhTransferDeskDTO dto);

    /**
     * 转机台操作
     * @param dto 参数
     * @return 结果
     */
    AjaxResult changeMachine(LhTransferDeskDTO dto);

    /**
     * 调量前校验
     * @param dto 参数
     * @return 结果
     */
    AjaxResult adjustQuantityPreCheck(LhScheduleResultUpdateDTO dto);

    /**
     * 调量操作
     * @param dto 参数
     * @return 结果
     */
    AjaxResult adjustQuantity(LhScheduleResultUpdateDTO dto);

    /**
     * 根据单条硫化排程结果文字示方更新。
     *
     * @param dto 生成入参
     * @return 处理结果
     */
    AjaxResult generateTextMouldChangePlan(LhGenerateTextMouldPlanDTO dto);

    /**
     * 计划更新。
     *
     * @param scheduleResult 当前硫化排程结果
     * @return 处理结果
     */
    AjaxResult increaseMouldStartPlan(LhScheduleResult scheduleResult);

    /**
     * 批量计划更新。
     *
     * @param scheduleResultList 硫化排程结果列表
     * @return 处理结果
     */
    AjaxResult batchIncreaseMouldStartPlan(List<LhScheduleResult> scheduleResultList);

    /**
     * 导出数据
     *
     @param list 数据列表
     * @param result 排程日期
     * @return 导出数据
     */
    byte[] exportData(List<LhScheduleResult> list, LhScheduleResult result);

    /**
     * 下载导入模板（不含物料描述列，物料描述通过物料编码自动带出）
     *
     * @param result 查询条件（工厂、排程日期等）
     * @return 导入模板Excel字节数组
     */
    byte[] downloadImportTemplate(LhScheduleResult result);


    AjaxResult importScheduleTemplate(List<LhScheduleResultTemplateImportVO> list, LhScheduleResult result, boolean updateSupport, Long id);

    /**
     * 构建硫化产量今天夜班Map
     *
     * @param list 排程结果列表
     * @return key=工厂编码|物料编码, value=今天夜班产量(BigDecimal)
     */
    Map<String, Object> buildTodayNightFinishQtyMap(List<LhScheduleResult> list);

    /**
     * 构建指定 T 日的硫化产量今天夜班Map。
     *
     * @param list 排程结果列表
     * @param scheduleDate 排程日期，传入时按该日期查询 T_LH_SCHE_FINISH_QTY.CLASS1_FINISH_QTY
     * @return key=工厂编码|物料编码, value=今天夜班产量(BigDecimal)
     */
    Map<String, Object> buildTodayNightFinishQtyMap(List<LhScheduleResult> list, Date scheduleDate);

}
