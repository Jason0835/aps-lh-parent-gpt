package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxScheduleTaskTime;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.enums.AdjustTypeEnums;
import com.zlt.aps.cx.engine.enums.ClassEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.service.*;
import com.zlt.aps.cx.engine.task.*;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
  * 成型工序引擎算法入口
  * @ClassName CxScheduleEngineServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/22 10:11
  * @Version 1.0
**/
@Service("cxScheduleEngineService")
@Slf4j
public class CxScheduleEngineServiceImpl implements CxScheduleEngineService {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private InsertTaskService insertTaskService;

    @Autowired
    private ChangeMachineTaskService changeMachineTaskService;

    @Autowired
    private ScheduleImportService scheduleImportService;

    @Autowired
    private CommonCacheService cacheService;

    @Autowired
    private ProductTaskService productTaskService;

    @Autowired
    private LastDayScheduleTaskService lastDayScheduleTaskService;

    @Autowired
    private CxAutoScheduleEngine cxAutoScheduleEngine;

    @Autowired
    private CxScheduleTaskTimeService cxScheduleTaskTimeService;



    /**
     * 自动排程引擎算法
     * @Author Joran.Zhang
     * @Description 根据日期进行自动排程
     * @Date 2021/6/22 10:21
     * @Param scheduleDate 排程日期
     * @Return 
     */
    @Override
    public  void allMachineAutoSchedule(Date scheduleDate) throws CxScheduleEngineException {
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        String redisKey= CxPrefixConstants.AUTO_SCHEDULE_PREFIX+scheduleDateStr;
        if(scheduleDate==null){
            log.warn("排程日期为空，排程日期为当前日期，传入日期{}",scheduleDate);
            scheduleDate=new Date();
        }
        if(cacheService.hasKey(redisKey)){
            log.error("【scheduleService.autoSchedule】当前仍在进行自动排程，请5分钟后重试");
            return;
        }
        try {
            cacheService.setIfAbsent(redisKey,scheduleDateStr, CxEngineConstants.AUTO_SCHEDULE_KEY_TIME, TimeUnit.MINUTES);
            //scheduleService.autoSchedule(scheduleDate);
            cxAutoScheduleEngine.autoSchedule(null,scheduleDate);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.schedule.exception"),e.getMessage()));
        } finally {
            //移除redis锁
            cacheService.delRedisKey(redisKey);
        }
    }

    /**
     * 手动插单参数验证及其他相关数据验证
     * @param cxScheduleResult
     * @return
     */
    public ValidateResult insertPreCheck(CxScheduleResult cxScheduleResult){
        if(cxScheduleResult==null){
            return ValidateResult.error(I18nUtil.getMessage("cx.engine.insert.param.empty.error"));
        }
        return insertTaskService.preCheck(cxScheduleResult);
    }

    /**
     * 插单引擎算法
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/6/23 18:06
     * @param
     * @return
     */
    @Override
    public void insertTask(CxScheduleResult cxScheduleResult) throws CxScheduleEngineException {
        insertTaskService.insertScheduleOrder(cxScheduleResult);
    }

    /**
     * 转机台前校验
     * @param cxScheduleResult
     * @param changeMachineCode 待转入机台
     * @return
     */
    @Override
    public ValidateResult changeMachinePreCheck(CxScheduleResult cxScheduleResult, String changeMachineCode) {
        StringBuilder sb=new StringBuilder();
        if(cxScheduleResult==null){
            sb.append(I18nUtil.getMessage("cx.engine.change.machine.param.schedule.error"));
        }

        if(StringUtils.isEmpty(changeMachineCode)){
            sb.append(I18nUtil.getMessage("ex.engine.change.machine.param.machine.code.error"));
        }

        if(StringUtils.isNotEmpty(sb)){
            return  ValidateResult.error(sb.toString());
        }

        return changeMachineTaskService.preChangeCheck(cxScheduleResult,changeMachineCode);
    }

    /**
     * 执行转机台操作
     * @param cxScheduleResult
     * @param changeMachineCode 待转入机台
     */
    @Override
    public void changeMachineTask(CxScheduleResult cxScheduleResult, String changeMachineCode) throws CxScheduleEngineException {
        changeMachineTaskService.changeCxMachine(cxScheduleResult,changeMachineCode);
    }

    /**
     * 投产验证
     * @param cxPlanProductStatus
     * @return
     */
    @Override
    public ValidateResult productPreCheck(CxPlanProductStatus cxPlanProductStatus) {
        if(cxPlanProductStatus==null){
            return ValidateResult.error(I18nUtil.getMessage("cx.engine.product.param.empty.error"));
        }
        return productTaskService.productTaskPreCheck(cxPlanProductStatus);
    }

    /**
     * 已收尾规格再次投产
     * @param cxPlanProductStatus
     * @throws CxScheduleEngineException
     */
    @Override
    public void closeOutProductTask(CxPlanProductStatus cxPlanProductStatus) throws CxScheduleEngineException {
        if(cxPlanProductStatus==null){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.product.param.empty.error"));
        }
        productTaskService.closeOutReProduct(cxPlanProductStatus);
    }

    /**
     * 收尾规格再次投产校验
     * @param cxPlanProductStatus
     * @return
     */
    @Override
    public ValidateResult reProductTaskPreCheck(CxPlanProductStatus cxPlanProductStatus) {
        if(cxPlanProductStatus==null){
            return ValidateResult.error(I18nUtil.getMessage("cx.engine.product.param.empty.error"));
        }
        return productTaskService.reProductTaskPreCheck(cxPlanProductStatus);
    }

    /**
     * 任务投产
     * @param cxPlanProductStatus
     * @throws CxScheduleEngineException
     */
    @Override
    public void productTask(CxPlanProductStatus cxPlanProductStatus) throws CxScheduleEngineException {
        productTaskService.productTask(cxPlanProductStatus);
    }

    /**
     * 导入自动验证及自动填充数据
     * @param cxScheduleResultList
     * @param scheduleDate
     * @return
     */
    @Override
    public List<ImportErrorLog> batchImportSchedule(List<CxScheduleResult> cxScheduleResultList, Date scheduleDate, Long importLogId) throws CxScheduleEngineException {
        if(StringUtils.isEmpty(cxScheduleResultList)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.import.list.empty.error"));
        }

        if(scheduleDate==null){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.import.scheduleDate.empty.error"));
        }

        return scheduleImportService.batchImportSchedule(cxScheduleResultList,scheduleDate,importLogId);
    }

    /**
     * 重新计算各个班次可硫化班次数
     * @param cxScheduleResult
     */
    @Override
    public void calcAvaliableClassShift(CxScheduleResult cxScheduleResult, AdjustTypeEnums adjustTypeEnums) {
        scheduleService.reCalcAvalivableLhShift(cxScheduleResult,adjustTypeEnums);
    }

    /**
     * 成型机台任务重排
     * @param cxMachineCode 重排成型机台
     * @param scheduleDate 重排排程日期
     * @throws CxScheduleEngineException 自动排程异常
     */
    @Override
    public void singleMachineAutoSchedule(String cxMachineCode, Date scheduleDate) throws CxScheduleEngineException {
        if(StringUtils.isEmpty(cxMachineCode)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.machine.auto.machineCode.empty.error"));
        }

        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        String redisKey= CxPrefixConstants.AUTO_SCHEDULE_PREFIX+scheduleDateStr;
        if(scheduleDate==null){
            log.warn("排程日期为空，排程日期为当前日期，传入日期{}",scheduleDate);
            scheduleDate=new Date();
        }
        if(cacheService.hasKey(redisKey)){
            log.error("【cxAutoScheduleEngine.autoSchedule】当前仍在进行自动排程，请5分钟后重试");
            return;
        }
        try {
            cacheService.setIfAbsent(redisKey,scheduleDateStr, CxEngineConstants.AUTO_SCHEDULE_KEY_TIME, TimeUnit.MINUTES);
            cxAutoScheduleEngine.autoSchedule(cxMachineCode,scheduleDate);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.schedule.exception"),e.getMessage()));
        } finally {
            //移除redis锁
            cacheService.delRedisKey(redisKey);
        }
        /*if(scheduleDate==null){
            log.warn("排程日期为空，排程日期为当前日期，传入日期{}",scheduleDate);
            scheduleDate=new Date();
        }
        try {
            scheduleService.singleMachineAutoSchedule(cxMachineCode,scheduleDate);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.schedule.exception"),e.getMessage()));
        }*/
    }

    /**
     * 提供成型确认硫化机后进行单班硫化量重算
     * @param cxScheduleResult
     */
/*    @Override
    public void reCalcSingleShiftLhQty(CxScheduleResult cxScheduleResult)throws CxScheduleEngineException {
        scheduleService.reCalcSingleShiftLhQty(cxScheduleResult);
    }*/

    /**
     * 重新设值同机台同胎胚不同SAP单班硫化量
     * @param cxScheduleResult
     * @throws CxScheduleEngineException
     */
    @Override
    public void reSetSingleLhShiftQty(CxScheduleResult cxScheduleResult) throws CxScheduleEngineException {
        scheduleService.reSetSingleLhShiftQty(cxScheduleResult);
    }

    /**
     * 根据前一天排程完成情况生成增补计划确认列表
     * @param suppleDate 增补计划日期，一般是前一天排程日期
     * @throws CxScheduleEngineException
     */
    @Override
    public void createSupplePlanTask(Date suppleDate) throws CxScheduleEngineException {
        lastDayScheduleTaskService.autoMixLastDaySchedule(suppleDate);
    }

    /**
     *  自动根据增补日期对应的增补计划增补到排程结果中
     * @param suppleDate 增补日期
     * @throws CxScheduleEngineException
     */
    @Override
    public void autoSuppleScheduleTask(Date suppleDate) throws CxScheduleEngineException {
        lastDayScheduleTaskService.cxScheduleAutoSupple(suppleDate);
    }

    /**
     *  自动排程前进行前一天增补计划生成和确认情况校验，通过后才可以进行自动排程
     * @param scheduleDate 自动排程日期
     * @return
     */
    @Override
    public ValidateResult autoScheduleValidateSupplePlanByScheduleDate(Date scheduleDate) {
        return lastDayScheduleTaskService.beforeAutoScheduleValidate(scheduleDate);
    }

    @Override
    public void reCreateSupplePlanTask(Date suppleDate) throws CxScheduleEngineException {
        lastDayScheduleTaskService.reCreateLastDaySchedule(suppleDate);
    }

    /**
     * 重新设值同机台同胎胚不同SAP单班硫化量
     * @param cxLastDaySupplePlanDto
     * @throws CxScheduleEngineException
     */
    @Override
    public void reSetSupplePlanSingleLhShiftQty(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) throws CxScheduleEngineException {
        lastDayScheduleTaskService.reSetSupplePlanSingleLhShiftQty(cxLastDaySupplePlanDto);
    }

    /**
     * 调量保存前置校验各个班次的计划量
     * @param cxScheduleResult
     * @return
     */
    @Override
    public ValidateResult changePlanQtyPreCheck(CxScheduleResult cxScheduleResult) {
        StringBuilder sb=new StringBuilder();
        if(cxScheduleResult==null){
            sb.append(I18nUtil.getMessage("cx.engine.validate.param.empty.error"));
        }
        String sourceMachineCode=cxScheduleResult.getCxMachineCode();
        if(StringUtils.isEmpty(sourceMachineCode)){
            sb.append(I18nUtil.getMessage("cx.engine.validate.machineCode.empty.error"));
        }
        if(StringUtils.isNotEmpty(sb)){
            return  ValidateResult.error(sb.toString());
        }
        CxEngineScheduleResult cxEngineScheduleResult =new CxEngineScheduleResult();
        BeanUtils.copyProperties(cxScheduleResult,cxEngineScheduleResult);
        //获取到机台对应的定额信息
        Integer machineQuota=cacheService.getQuotaByMachineEmbryoCode(cxScheduleResult.getCxMachineCode(),cxScheduleResult.getEmbryoCode(),cxScheduleResult.getBomDataVersion(),new StringBuilder());
        StringBuilder tipMsg=new StringBuilder();
        //验证开始班次下标
        Integer classIndex=ClassEnums.CLASS_ONE.getClassIndex();
        do{
            Integer currentPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,classIndex);
            if(machineQuota < currentPlanQty){
                tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.changeQty.class"+classIndex+"PlanQty.tip"),currentPlanQty,machineQuota));
            }
            classIndex++;
        }while(classIndex <= ClassEnums.CLASS_FIVE.getClassIndex());

        if(StringUtils.isNotEmpty(tipMsg)){
            return ValidateResult.success(tipMsg.toString());
        }
        return ValidateResult.success();
    }

    /**
     * 根据排程日期进行计算成型排程任务的时间集合
     * @param scheduleDate
     */
    @Override
    public void calcCxScheduleTaskListTime(Date scheduleDate) {
       //1.删除排程日期对应的任务时间
       cxScheduleTaskTimeService.deleteCxScheduleTaskTimeByScheduleDate(scheduleDate);
       //2.查询当天日期所对应的全部排程计划
       List<CxEngineScheduleResult> dayScheduleTaskList=cacheService.listScheduleTaskListByScheduleDate(scheduleDate);
        if(StringUtils.isNotEmpty(dayScheduleTaskList)){
            Map<String,String> cxParams=cacheService.loadCxParamsMap();
            Map<String, EngineProductConstructionInfo> engineConstructionInfoMap=cacheService.loadEngineConstructionMapFromRedis();
            //用来存储所有任务时间
            List<CxScheduleTaskTime> scheduleTaskTimeList =new ArrayList<>();
            //根据机台进行任务拆分
            Map<String,List<CxEngineScheduleResult>> machineTaskMap= CxScheduleUtils.splitTaskByCxMachine(dayScheduleTaskList);
            for(Map.Entry<String,List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()){
                String cxMachineCode=entry.getKey();
                List<CxEngineScheduleResult> machineList=entry.getValue();
                cacheService.calcMachineTaskTime(cxMachineCode,machineList,scheduleTaskTimeList,CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_AUTO,engineConstructionInfoMap,cxParams);
            }
            //计算完后清空内存缓存
            cacheService.clearCacheData();
            //3.批量进行任务时间存
            if(StringUtils.isNotEmpty(scheduleTaskTimeList)){
                cxScheduleTaskTimeService.batchInsertCxScheduleTaskTime(scheduleTaskTimeList);
            }

        }
    }


}
