package com.zlt.aps.cx.engine.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.engine.enums.AdjustTypeEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;

import java.util.Date;
import java.util.List;

/**
 * 成型工序排程引擎接口
 * @ClassName CxScheduleEngineService
 * @Description 引擎部分
 * @Author Joran.Zhang
 * @Date 2021-06-22 9:09
 * @Version 1.0
 **/
public interface CxScheduleEngineService  {

    /**
     *  自动排程
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/6/22 10:21
     * @Param scheduleDate 排程日期
     * @Return
     */
    public void allMachineAutoSchedule(Date scheduleDate) throws CxScheduleEngineException;

    /**
     * 手动插单参数验证及其他相关数据验证
     * @param cxScheduleResult
     * @return
     */
    public ValidateResult insertPreCheck(CxScheduleResult cxScheduleResult);

    /**
     * 成型插单
     * @param cxScheduleResult
     */
    public void insertTask(CxScheduleResult cxScheduleResult) throws CxScheduleEngineException;

    /**
     * 转机台前验证，验证通过才允许转机台
     * @param cxScheduleResult
     * @param changeMachineCode 待转入机台
     * @return
     */
    public ValidateResult changeMachinePreCheck(CxScheduleResult cxScheduleResult,String changeMachineCode);

    /**
     * 执行转机台操作
     * @param cxScheduleResult
     * @param changeMachineCode 待转入机台
     */
    public void changeMachineTask(CxScheduleResult cxScheduleResult,String changeMachineCode) throws CxScheduleEngineException;


    /**
     * 成型工序投产校验
     * @param cxPlanProductStatus
     * @return
     */
    public ValidateResult productPreCheck(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 成型工序收尾投产
     * @param cxPlanProductStatus
     */
    public void closeOutProductTask(CxPlanProductStatus cxPlanProductStatus) throws CxScheduleEngineException;

    /**
     * 成型工序投产校验
     * @param cxPlanProductStatus
     * @return
     */
    public ValidateResult reProductTaskPreCheck(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 成型工序计划投产
     * @param cxPlanProductStatus
     */
    public void productTask(CxPlanProductStatus cxPlanProductStatus) throws CxScheduleEngineException;


    /**
     * 批量导入成型排程数据
     * @param cxScheduleResultList
     * @param scheduleDate
     * @return
     */
    public List<ImportErrorLog> batchImportSchedule(List<CxScheduleResult> cxScheduleResultList, Date scheduleDate, Long importLogId) throws CxScheduleEngineException;

    /**
     * 重新计算可硫化班次
     * @param cxScheduleResult
     */
    public void calcAvaliableClassShift(CxScheduleResult cxScheduleResult, AdjustTypeEnums adjustTypeEnums);

    /**
     * 根据成型机台进行机台自动重排
     * @param cxMachineCode
     * @param scheduleDate
     * @throws CxScheduleEngineException
     */
    public void  singleMachineAutoSchedule(String cxMachineCode,Date scheduleDate) throws CxScheduleEngineException;

    /**
     *  成型排程选择确定硫化机提供重算单班硫化产能重算功能
     *  2021-09-16 项目经理确认只重算单班硫化量，不去更改自动排程结果
     * @param cxScheduleResult
     */
    //public void reCalcSingleShiftLhQty(CxScheduleResult cxScheduleResult) throws CxScheduleEngineException;

    /**
     * 重算单班硫化量后进行同胎胚外胎不同单班硫化量合并处理
     * @param cxScheduleResult
     * @throws CxScheduleEngineException
     */
    public void reSetSingleLhShiftQty(CxScheduleResult cxScheduleResult) throws CxScheduleEngineException;

    /**
     * 生成增补计划列表
     * @param suppleDate 增补计划日期，一般是前一天排程日期
     */
    public void createSupplePlanTask(Date suppleDate) throws CxScheduleEngineException;

    /**
     * 确认增补计划列表，将增补计划数据增补到日期对应的排程结果中
     * @param suppleDate
     * @throws CxScheduleEngineException
     */
    public void autoSuppleScheduleTask(Date suppleDate) throws CxScheduleEngineException;

    /**
     * 自动排程前进行前一天增补计划生成确认情况，执行后才可验证通过进行自动排程
     * @param scheduleDate 自动排程日期
     * @return
     */
    public ValidateResult  autoScheduleValidateSupplePlanByScheduleDate(Date scheduleDate);

    /**
     * 重新生成增补计划列表
     * @param suppleDate 增补计划日期，一般是前一天排程日期
     */
    public void reCreateSupplePlanTask(Date suppleDate) throws CxScheduleEngineException;

    /**
     * 重新处理同机台增补计划单班硫化量
     * @param cxLastDaySupplePlanDto
     * @throws CxScheduleEngineException
     */
    public void reSetSupplePlanSingleLhShiftQty(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) throws CxScheduleEngineException;

    /**
     * 调量进行班次计划量定额校验
     * @param cxScheduleResult
     * @return
     */
    public ValidateResult changePlanQtyPreCheck(CxScheduleResult cxScheduleResult);

    /**
     * 根据排程日期进行成型排程任务时间计算
     * @param scheduleDate
     */
    public void  calcCxScheduleTaskListTime(Date scheduleDate);


}
