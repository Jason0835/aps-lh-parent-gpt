package com.zlt.aps.lh.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import com.zlt.aps.lh.api.domain.entity.LhInProductionSpec;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.engine.common.LhCommonService;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.constants.LhEngineParamCodeConstants;
import com.zlt.aps.lh.engine.domain.LhAutoScheduleLog;
import com.zlt.aps.lh.engine.domain.LhEngineParams;
import com.zlt.aps.lh.engine.domain.LhEngineScheduleResult;
import com.zlt.aps.lh.engine.domain.LhSapEmbryoTime;
import com.zlt.aps.lh.engine.domain.LhSapMonthPlanSurplus;
import com.zlt.aps.lh.engine.enums.AnalysisCodeEnum;
import com.zlt.aps.lh.engine.enums.LhClassShiftEnum;
import com.zlt.aps.lh.engine.exception.LhEngineException;
import com.zlt.aps.lh.engine.mapper.CommonCxEngineMapper;
import com.zlt.aps.lh.engine.mapper.CommonLhEngineMapper;
import com.zlt.aps.lh.engine.service.LhEngineAutoScheduleRecordService;
import com.zlt.aps.lh.engine.service.LhEngineParamsService;
import com.zlt.aps.lh.engine.util.LhEngineScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 硫化工序自动排产引擎核心逻辑
 */
@Component("lhEngineNewAutoScheduleTask")
@Slf4j
public class LhEngineNewAutoScheduleTask {
    @Autowired
    private CommonLhEngineMapper commonLhEngineMapper;

    @Autowired
    private CommonCxEngineMapper commonCxEngineMapper;

    @Autowired
    private LhCommonService lhCommonService;

    @Autowired
    private LhEngineAutoScheduleRecordService lhEngineAutoScheduleRecordService;

    @Autowired
    private LhEngineParamsService lhEngineParamsService;

    @Autowired
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private LhScheduleTaskCheck lhScheduleTaskCheck;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;


    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    private Map<String,String> lhParamsMap;

    //成型工序参数
    private Map<String,String> cxParamsMap;

    //所有硫化机台信息列表
    private List<LhMachineInfo> lhMachineInfoList;

    //初始化硫化排程日期对应的成型计划列表
    private List<CxScheduleResult> cxScheduleResultList;

    //初始化昨日对应的模具变更信息
    private List<LhApsMoldAdjustPlan> lastDayLhApsMoldAdjustPlanList;

    //根据机台分组后硫化昨日换模计划
    private Map<String,List<LhApsMoldAdjustPlan>> lastDayChangeMoldMap;

    //初始化今日对应的模具变更信息
    private List<LhApsMoldAdjustPlan> lhApsMoldAdjustPlanList;

    //昨天硫化排程计划
    private List<LhEngineScheduleResult> lastDayLhScheduleResultList;

    //缓存外胎汇总表数据
    private Map<String,LhSapMonthPlanSurplus> lhSapMonthPlanSurplusMap;

    //成型批次号
    private String cxBatchNo;

    //根据胎胚代码分组后的成型任务的全部时间段
    private Map<String,List<LhSapEmbryoTime>> lhEmbryoSupportTimeMap;

    //根据机台分组后硫化今日换模计划
    private Map<String,List<LhApsMoldAdjustPlan>> sapLhApsMoldAdjustPlanMap;

    //根据胎胚代码 获取各个班次的成型胎胚初始库存，最开始初始化为中班开班预计可用库存
    private Map<String,Integer> sapShiftEmbryoStockMap;

    //将昨天的计划根据胎胚代码进行分组
    private Map<String,List<LhEngineScheduleResult>> lastDayEmbryoResultMap;

    //初始化白班开始时间
    private Date lastThreeShiftBeginTime;

    //初始化硫化外胎施工信息
    private Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap;

    //初始化各个班次的开始结束时间
    private Map<String,Date> shiftStartEndTimeMap;

    //成型各个班次是否已经被加过
    private Map<String,Boolean> shiftUpdateCxPlanFlag;

    /**
     * 开始时间key前缀
     */
    public static final String START_TIME_PREFIX=":start_time";

    /**
     * 结束时间key前缀
     */
    public static final String END_TIME_PREFIX=":end_time";

    //工单对应的日志信息
    private Map<String,StringBuilder> orderLogMap;

    //用来存储机台的硫化结束时间
    private Map<String,Date> machineLhEndTime;

    //在产规格初始化根据硫化机进行组装
    private  Map<String, List<LhInProductionSpec>> machineInProductListMap;


    /**
     * 硫化工序自动排程入口
     * @param scheduleDate yyyy-MM-dd
     */
    @Transactional
    public synchronized void autoSchedule(String scheduleDate){
        StringBuilder errorMsg=new StringBuilder();
        StringBuilder logDetail=new StringBuilder();
        //第一步：进行硫化自动排程数据初始化和参数校验
        scheduleInit(scheduleDate,errorMsg,logDetail);
        //创建硫化自动排程批次号
        String lhBatchNo=lhCommonService.createBatchNo(LhEngineConstants.LH_AUTO_BATCH_NO_PREFIX,scheduleDate);
        //数据加载及验证，没有合法数据进行异常抛出
        if(StringUtils.isNotEmpty(errorMsg)){
            lhEngineAutoScheduleRecordService.reGenerageRecord("",lhBatchNo,scheduleDate, LhEngineConstants.LH_AUTO_RECORD_STATUS_FAIL);
            log.error("【新自动排程错误】："+errorMsg.toString());
            autoScheduleLogService.insertLhScheduleLog(lhBatchNo, "", "硫化新自动排程错误",  errorMsg.toString());
            throw new LhEngineException(errorMsg.toString());
        }
        List<LhEngineScheduleResult> insertList=new ArrayList<>();
        //自动排程日期
        Date autoScheduleDate= DateUtils.parseDate(scheduleDate);
        //第二步：进行在产规格匹配机台进行计划安排
        List<LhEngineScheduleResult> lhEngineScheduleResultList = new ArrayList<>();
        //2.1 根据当前机台在产规格数据进行规格数据填充
        dataFillingByInProductSpec(lhEngineScheduleResultList,autoScheduleDate,lhBatchNo,logDetail);
        //2.2 根据今日模具变动单进行规格数据填充
        dataFillingByTodayChangeMold(lhEngineScheduleResultList,autoScheduleDate,lhBatchNo,logDetail);
        //2.3 结合在产规格和换膜计划初始化完毕后再结合成型的数据进行数据补充（同SAP不同胎胚多初始化）
        dataFillingByCxEmbryoCode(lhEngineScheduleResultList,autoScheduleDate,logDetail);
        //2022-07-22 Joran 填充施工相关内容
        dataFillingByConstruction(lhEngineScheduleResultList,logDetail);

        //2.4 填充完数据后进行标记处理
        markTagBySchedule(lhEngineScheduleResultList,autoScheduleDate,logDetail);
        //用于存储同机台同sap不同胎胚的集合，此部分不参与自动排程
        List<LhEngineScheduleResult> sameSapDiffEmbryoList=new ArrayList<>();
        //2.5 按照机台+sap品号进行分组，如果记录数大于2的证明有两个胎胚，该部分不参与自动安排//需要人为干预
        removeSameMachineSapDiffEmbryoList(lhEngineScheduleResultList,sameSapDiffEmbryoList,logDetail);
        if(StringUtils.isNotEmpty(sameSapDiffEmbryoList)){
            //相同SAP不同胎胚的不进行自动排程处理
            insertList.addAll(sameSapDiffEmbryoList);
        }
        //初始化完规格数据后，存在初始化的硫化排程数据后，进行自动计划安排逻辑start
        if(StringUtils.isNotEmpty(lhEngineScheduleResultList)){

            //先优先处理初始化各个硫化机班次可用时间
            calcLhMachineEnableShiftTime(lhEngineScheduleResultList,logDetail);

            //按照胎胚代码对初始化完毕的硫化任务进行分组
            Map<String,List<LhEngineScheduleResult>> embryoCodeTaskMap=lhEngineScheduleResultList.stream().collect(Collectors.groupingBy(LhEngineScheduleResult::getEmbryoCode));
            //根据胎胚分组后进行任务自动安排
            autoScheduleByEmbryoTaskMap(embryoCodeTaskMap,logDetail);
            try {
                logDetail.append("【硫化排程列表数据填充后】").append(toJSONString(lhEngineScheduleResultList)).append(division);

                //Joran 2022-06-20 换模过程工单日志存储start
                 insertOrderNoLog(scheduleDate);
                //Joran 2022-06-20 换模过程工单日志存储end
                if(StringUtils.isNotEmpty(lhEngineScheduleResultList)){
                    insertList.addAll(lhEngineScheduleResultList);
                    //删除排程同步日志表
                    lhScheduleTaskCheck.syncScheduleToLog(scheduleDate);
                    //批量生成硫化排程
                    commonLhEngineMapper.batchInsertLhScheduleResult(insertList);
                    autoScheduleLogService.insertLhScheduleLog(lhBatchNo, "", "自动排程过程日志",  logDetail.toString());
                }
                lhEngineAutoScheduleRecordService.reGenerageRecord(cxBatchNo,lhBatchNo,scheduleDate, LhEngineConstants.LH_AUTO_RECORD_STATUS_SUCCESS);
            } catch (Exception e) {
                lhEngineAutoScheduleRecordService.reGenerageRecord(cxBatchNo,lhBatchNo,scheduleDate, LhEngineConstants.LH_AUTO_RECORD_STATUS_FAIL);
                logDetail.append(e.getMessage());
                autoScheduleLogService.insertLhScheduleLog(lhBatchNo, "", "自动排程失败",  logDetail.toString());
                e.printStackTrace();
                throw new LhEngineException(e.getMessage());
            }finally {
                //最后一步：进行缓存数据删除
                clearCacheData();
            }

        }else{
             lhEngineAutoScheduleRecordService.reGenerageRecord(cxBatchNo,lhBatchNo,scheduleDate, LhEngineConstants.LH_AUTO_RECORD_STATUS_FAIL);
             throw new LhEngineException(I18nUtil.getMessage("lh.engine.auto.schedule.list.empty.error"));
        }
        //初始化完规格数据后，存在初始化的硫化排程数据后，进行自动计划安排逻辑end

    }

    /**
     * 填充初始化硫化排程中是施工相关的信息
     * @param lhEngineScheduleResultList
     * @param logDetail
     */
    private void dataFillingByConstruction(List<LhEngineScheduleResult> lhEngineScheduleResultList, StringBuilder logDetail) {
        logDetail.append("进行初始化填充硫化时长等施工获取数据----》开始").append(division);
        for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
            Double lhTime=lhCommonService.getSapEmbryoCodeSingleLhTime(lhEngineScheduleResult.getSapCode(),lhEngineScheduleResult.getEmbryoCode(),sapTireConstructionListMap);
            //设置硫化时长
            lhEngineScheduleResult.setLhTime(lhTime==null?BigDecimal.ZERO.doubleValue():lhTime);
            //设置规格机台定额
            Integer quota=lhCommonService.calcLhShiftQuotaByMoldNumber(cxParamsMap,lhTime,lhEngineScheduleResult.getUseMoldNumber());
            lhEngineScheduleResult.setQuota(quota);
            //设置规格描述信息
            setSpecDescBySapCode(lhEngineScheduleResult);
        }
        logDetail.append("进行初始化填充硫化时长等施工获取数据----》结束").append(division);
    }

    /**
     *  根据工单号进行日志存储
     */
    private void insertOrderNoLog(String scheduleDate) {
        if(StringUtils.isNotEmpty(orderLogMap)){
           //1.根据排程日期进行日志删除
           commonLhEngineMapper.deleteLhScheduleLogByScheduleDate(scheduleDate);
           Date date=DateUtils.parseDate(scheduleDate);
           List<LhAutoScheduleLog> lhAutoScheduleLogList =new ArrayList<>();
           LhAutoScheduleLog log =null;
           for(Map.Entry<String,StringBuilder> entry:orderLogMap.entrySet()){
                log =new LhAutoScheduleLog();
                log.setOrderNo(entry.getKey());//设置工单号
                StringBuilder orderNoLog=entry.getValue();
                log.setScheduleDate(date);//设置排程日期
                log.setLogDetail(orderNoLog.toString());
                log.setCreateBy(SecurityUtils.getUsername());
                //commonLhEngineMapper.insertLhAutoScheduleLog(log);
                lhAutoScheduleLogList.add(log);
            }
            if(StringUtils.isNotEmpty(lhAutoScheduleLogList)){
                commonLhEngineMapper.batchInsertLhScheduleLogResult(lhAutoScheduleLogList);
            }
        }
    }

    /**
     * 胎胚对应任务计划安排
     * @param embryoCodeTaskMap
     * @param logDetail
     */
    private void autoScheduleByEmbryoTaskMap(Map<String, List<LhEngineScheduleResult>> embryoCodeTaskMap, StringBuilder logDetail) {
        logDetail.append("【胎胚多机台任务班次安排】").append(division);
        if(StringUtils.isNotEmpty(embryoCodeTaskMap)){
            for(Map.Entry<String, List<LhEngineScheduleResult>> entry:embryoCodeTaskMap.entrySet()){
                String embryoCode=entry.getKey();
                logDetail.append(StringUtils.format("【胎胚组计划安排】，当前胎胚代码：{}",embryoCode)).append(division);
                 List<LhEngineScheduleResult> lhEngineScheduleResultList=entry.getValue();
                 for(LhClassShiftEnum cls:LhClassShiftEnum.values()){
                     logDetail.append(StringUtils.format("【胎胚组计划安排】，当前胎胚代码：{},当前班次：{}",embryoCode,cls.getClassName())).append(division);
                     //根据班次进行任务排序进行任务排序
                     sortByScheduleList(embryoCode,lhEngineScheduleResultList,cls,logDetail);
                     //进行全部机台中班任务计划安排
                     setClassShiftPlan(embryoCode,lhEngineScheduleResultList,cls,logDetail);
                 }
                 //计算汇总日计划量
                for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
                    setScheduleDelayPlan(lhEngineScheduleResult,logDetail);
                }

            }
        }
    }

    /**
     * 设置日计划量
     * @param lhEngineScheduleResult
     */
    private void setScheduleDelayPlan(LhEngineScheduleResult lhEngineScheduleResult,StringBuilder logDetail) {
        Integer delayPlanQty=0;
        lhEngineScheduleResult.initLhPlanQty();
        delayPlanQty+=lhEngineScheduleResult.getClass1PlanQty();
        delayPlanQty+=lhEngineScheduleResult.getClass2PlanQty();
        delayPlanQty+=lhEngineScheduleResult.getClass3PlanQty();
        logDetail.append("【汇总日计划量】：当日计划总量：").append(delayPlanQty).append(division);
        lhEngineScheduleResult.setDailyPlanQty(delayPlanQty);
    }

    /**
     * 开始进行计划任务安排
     * @param embryoCode 当前胎胚代码
     * @param lhEngineScheduleResultList 当前胎胚任务列表
     * @param cls 当前计划班次
     * @param logDetail
     */
    private void setClassShiftPlan(String embryoCode,List<LhEngineScheduleResult> lhEngineScheduleResultList,LhClassShiftEnum cls,StringBuilder logDetail) {
       logDetail.append(StringUtils.format("【同胎胚班次计划量安排】,当前胎胚：【{}】，当前班次:【{}】",embryoCode,cls.getClassName())).append(division);
       if(StringUtils.isNotEmpty(sapShiftEmbryoStockMap)&&sapShiftEmbryoStockMap.containsKey(embryoCode)){
           //班次开始时间
           Date shiftBeginTime=getShiftBeginTimeByClass(cls);
           //班次结束时间
           Date shiftEndTime=getShiftEndTimeByClass(cls);
           //遍历同胎胚，同班次计划安排start
           for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
               String lhMachineName=lhEngineScheduleResult.getLhMachineName();
               //当前任务机台可用开始时间
               Date enableStartTime=lhEngineScheduleResult.getEnableStartTime();
               //当前任务机台可用结束时间
               Date enableEndTime=lhEngineScheduleResult.getEnableEndTime();
               if(!lhEngineScheduleResult.getChangeMoldFlag()&&lhEngineScheduleResult.getChangeMoldTime()!=null){
                   enableStartTime=getShiftBeginTimeByClass(cls);
                   enableEndTime=getShiftEndTimeByClass(cls);
               }

               //换模时间
               Date changeMoldTime= lhEngineScheduleResult.getChangeMoldTime();
               String orderNo=lhEngineScheduleResult.getOrderNo();
               StringBuilder orderNoLog=getOrderLogBuilder(orderNo);

               if(enableStartTime==null||enableEndTime==null){
                   throw new LhEngineException(I18nUtil.getMessage("lh.engine.auto.schedule.enableTime.empty.error"));
               }

               //Joran 2022-08-16 当可用结束时间比班次开始时间小的时候，直接不进行自动排程start
               if(enableEndTime.getTime()<=shiftBeginTime.getTime()){
                   logDetail.append(StringUtils.format("【可用结束时间卡控】胎胚代码：{}，班次：{}，当前班次开始时间：{}，结束时间：{}，机台任务可用开始时间：{},机台任务可用结束时间：{},可用时间不在可用范围，不进行任务安排！",
                           embryoCode,cls.getClassName(),DateUtil.formatDatetime(shiftBeginTime),DateUtil.formatDatetime(shiftEndTime),DateUtil.formatDatetime(enableStartTime),
                           DateUtil.formatDatetime(enableEndTime))).append(division);
                   orderNoLog.append(StringUtils.format("【可用结束时间卡控】胎胚代码：{}，班次：{}，当前班次开始时间：{}，结束时间：{}，机台任务可用开始时间：{},机台任务可用结束时间：{},可用时间不在可用范围，不进行任务安排！",
                           embryoCode,cls.getClassName(),DateUtil.formatDatetime(shiftBeginTime),DateUtil.formatDatetime(shiftEndTime),DateUtil.formatDatetime(enableStartTime),
                           DateUtil.formatDatetime(enableEndTime))).append(division);
                   continue;
               }
               //Joran 2022-08-16 当可用结束时间比班次开始时间小的时候，直接不进行自动排程end


               //机台可用时间是否在当前班次
               boolean hasOverLap=LhEngineScheduleUtils.hasOverlap(shiftBeginTime,shiftEndTime,enableStartTime,enableEndTime);
               if(!hasOverLap){
                 logDetail.append(StringUtils.format("胎胚代码：{}，班次：{}，当前班次开始时间：{}，结束时间：{}，机台任务可用开始时间：{},机台任务可用结束时间：{},可用时间不在可用范围，不进行任务安排！",
                                                            embryoCode,cls.getClassName(),DateUtil.formatDatetime(shiftBeginTime),DateUtil.formatDatetime(shiftEndTime),DateUtil.formatDatetime(enableStartTime),
                                                            DateUtil.formatDatetime(enableEndTime))).append(division);
                 orderNoLog.append(StringUtils.format("胎胚代码：{}，班次：{}，当前班次开始时间：{}，结束时间：{}，机台任务可用开始时间：{},机台任务可用结束时间：{},可用时间不在可用范围，不进行任务安排！",
                           embryoCode,cls.getClassName(),DateUtil.formatDatetime(shiftBeginTime),DateUtil.formatDatetime(shiftEndTime),DateUtil.formatDatetime(enableStartTime),
                           DateUtil.formatDatetime(enableEndTime))).append(division);

                 if(changeMoldTime!=null&&lhEngineScheduleResult.getChangeMoldFlag()){
                     //拿到换模班次所在
                     LhClassShiftEnum changeMoldShift=timeInClassShift(changeMoldTime);
                     if(changeMoldShift!=null && changeMoldShift.equals(cls)){
                         //统一进行换模原因标注
                         //markChangeMoldAnalysis(lhEngineScheduleResult,changeMoldShift);
                         logDetail.append(StringUtils.format("可用时间不在可用范围，换模在当前班次，前规格标记，当前不标记！")).append(division);
                         orderNoLog.append(StringUtils.format("可用时间不在可用范围，换模在当前班次，前规格标记，当前不标记！")).append(division);
                         //判断下个班次是否可以开班
                         LhClassShiftEnum nextCls=LhClassShiftEnum.getClassShiftByClassIndex(cls.getClassIndex()+1);
                         if(nextCls!=null){
                             //需要判断下个班次是否可以标记开班
                             markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,changeMoldTime,nextCls,logDetail,orderNoLog);
                         }
                     }

                 }
                 continue;
               }
               Integer estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
               logDetail.append(StringUtils.format("当前机台：{},胎胚代码：{}，班次：{}，班次开班可用库存数:{}",lhMachineName,embryoCode,cls.getClassName(),estimateShiftStock)).append(division);
               orderNoLog.append(StringUtils.format("当前机台：{},胎胚代码：{}，班次：{}，班次开班可用库存数:{}",lhMachineName,embryoCode,cls.getClassName(),estimateShiftStock)).append(division);
               //班次最大计划量先默认为当前班次开班库存数
               Integer currentMaxPlan=null;
               Integer quota=lhEngineScheduleResult.getQuota();
               //如果开始时间 小于等于班次开始时间，可用班次开始时间更改外班次开始时间
               if(enableStartTime.getTime()<= shiftBeginTime.getTime()){
                   enableStartTime=shiftBeginTime;
               }

               //如果结束时间大于等于班次结束时间，可用班次结束时间更改为班次结束时间
               if(enableEndTime.getTime()>= shiftEndTime.getTime()){
                   enableEndTime=shiftEndTime;
               }
               Integer estimateEmbryoStock=estimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
               //胎胚开始时间
               Date embryoStartTime=getEmbryoCodeStartTimeByClassShift(embryoCode,estimateEmbryoStock,cls,logDetail);
               if(embryoStartTime!=null){
                   //如果胎胚开始时间大于班次结束时间，班次不可用，2022-07-05 添加如果还有班次库存的话继续进行安排，避免跳班问题
                  if(estimateEmbryoStock.equals(BigDecimal.ZERO.intValue())&&embryoStartTime.getTime()>enableEndTime.getTime()){
                      logDetail.append(StringUtils.format("当前机台：{},胎胚代码：{}，班次：{}，预计库存：{}，胎胚开始供应时间：{},超过班次结束时间，当前班次不安排计划",lhMachineName,embryoCode,cls.getClassName(),estimateEmbryoStock,DateUtil.formatDatetime(embryoStartTime))).append(division);
                      orderNoLog.append(StringUtils.format("当前机台：{},胎胚代码：{}，班次：{}，预计库存：{}，胎胚开始供应时间：{},超过班次结束时间，当前班次不安排计划",lhMachineName,embryoCode,cls.getClassName(),estimateEmbryoStock,DateUtil.formatDatetime(embryoStartTime))).append(division);
                      continue;
                  }
                  //2022-07-05  胎胚开始时间在班次可用时间之后，只要有库存就扣除掉开始时间到胎胚供应时间的时间差
                  //2022-07-12 如果胎胚时间大于可用开始时间且班次库存足够的话直接时间还是
                  if(embryoStartTime.getTime()>enableStartTime.getTime()){
                      //Joran 2022-07-21 当前班次有预计班次库存(未含成型当班任务)start
                      if(estimateShiftStock>BigDecimal.ZERO.intValue()){
                          currentMaxPlan=estimateShiftStock;
                      }else{
                          //计算出时间差可做的计划量
                          enableStartTime=embryoStartTime;
                      }
                      //Joran 2022-07-21 当前班次有预计班次库存(未含成型当班任务)end
                  }

               }

               logDetail.append(StringUtils.format("当前机台：{},胎胚代码：{}，班次：{}，当前班次开始时间：{}，结束时间：{}，获取到的班次预计库存：{}",lhMachineName,embryoCode,cls.getClassName(),DateUtil.formatDatetime(shiftBeginTime),DateUtil.formatDatetime(shiftEndTime),estimateShiftStock)).append(division);
               orderNoLog.append(StringUtils.format("当前机台：{},胎胚代码：{}，班次：{}，当前班次开始时间：{}，结束时间：{}，获取到的班次预计库存：{}",lhMachineName,embryoCode,cls.getClassName(),DateUtil.formatDatetime(shiftBeginTime),DateUtil.formatDatetime(shiftEndTime),estimateShiftStock)).append(division);

               //开班计划量安排逻辑start
               Boolean isOpenShift=LhEngineScheduleUtils.getClassShiftOpenShiftFlag(lhEngineScheduleResult,cls);
               if(isOpenShift){
                   Integer maxPlanQty=LhEngineScheduleUtils.getClassShiftMaxPlanQty(lhEngineScheduleResult,cls);
                   logDetail.append(StringUtils.format("【开班排班】当前机台：{},当前班次：{}，开班，班次计划量上限：{},机台定额：{}",lhMachineName,cls.getClassName(),maxPlanQty,quota)).append(division);
                   orderNoLog.append(StringUtils.format("【开班排班】当前机台：{},当前班次：{}，开班，班次计划量上限：{},机台定额：{}",lhMachineName,cls.getClassName(),maxPlanQty,quota)).append(division);
                   if(maxPlanQty >= quota){
                       logDetail.append(StringUtils.format("【开班排班】上限值大于等于定额，最大上限为定额：{}",quota)).append(division);
                       maxPlanQty=quota;
                   }
                   //判断班次预计库存是否大于上限，如果大于上限，则班次为上限
                   onOpenShiftClassPlanSet(embryoCode,cls,estimateShiftStock,maxPlanQty,lhEngineScheduleResult,lhEngineScheduleResult.getClassOpenShiftTime(),shiftEndTime,logDetail,orderNoLog);
                   continue;
               }
               //开班计划量安排逻辑end

               //开汽计划量安排逻辑start
               Boolean isOpenStream=LhEngineScheduleUtils.getClassShiftOpenStreamFlag(lhEngineScheduleResult,cls);
               if(isOpenStream){
                   Integer maxPlanQty=LhEngineScheduleUtils.getClassShiftMaxPlanQty(lhEngineScheduleResult,cls);
                   logDetail.append(StringUtils.format("【开汽排班】当前机台：{},当前班次：{}，开汽，班次计划量上限：{},机台定额：{}",lhMachineName,cls.getClassName(),maxPlanQty,quota)).append(division);
                   if(maxPlanQty >= quota){
                       logDetail.append(StringUtils.format("【开汽排班】上限值大于等于定额，最大上限为定额：{}",quota)).append(division);
                       maxPlanQty=quota;
                   }
                   //判断班次预计库存是否大于上限，如果大于上限，则班次为上限
                   onOpenStreamClassPlanSet(embryoCode,cls,estimateShiftStock,maxPlanQty,lhEngineScheduleResult,shiftEndTime,logDetail,orderNoLog);
                   continue;
               }
               //开汽计划量安排逻辑end

               //判断是否执行换模
               Boolean changeMoldFlag=lhEngineScheduleResult.getChangeMoldFlag();
               Integer maxPlanQty=null;
               if(changeMoldFlag){//有换模计划
                   //计算出来的最大计划量需要加上占用的时间
                   if(currentMaxPlan!=null){
                       maxPlanQty=currentMaxPlan;
                   }else{
                       maxPlanQty=getClassMaxPlanQty(cls,lhEngineScheduleResult,enableStartTime,enableEndTime,logDetail,orderNoLog);
                   }
                   //计算出来的最大计划量需要加上占用的时间
                   //计算出来的最大计划量需要加上占用的时间
                   if(maxPlanQty >= quota){
                       logDetail.append(StringUtils.format("【换模排班】上限值大于等于定额，最大上限为定额：{}",quota)).append(division);
                       maxPlanQty=quota;
                   }


                   logDetail.append(StringUtils.format("【换模排班】当前机台：{},当前班次：{}，换模，班次计划量上限：{},机台定额：{}",lhMachineName,cls.getClassName(),maxPlanQty,quota)).append(division);
                   orderNoLog.append(StringUtils.format("【换模排班】当前机台：{},当前班次：{}，换模，班次计划量上限：{},机台定额：{}",lhMachineName,cls.getClassName(),maxPlanQty,quota)).append(division);
                   onChangeMoldClassPlanSet(embryoCode,cls,estimateShiftStock,maxPlanQty,lhEngineScheduleResult,enableStartTime,enableEndTime,logDetail,orderNoLog);
               }else{ //无开班、开汽、换模，为正常排班
                   //计算出来的最大计划量需要加上占用的时间
                   if(currentMaxPlan!=null){
                       maxPlanQty=currentMaxPlan;
                   }else{
                       maxPlanQty=getClassMaxPlanQty(cls,lhEngineScheduleResult,enableStartTime,enableEndTime,logDetail,orderNoLog);
                   }
                   //计算出来的最大计划量需要加上占用的时间
                   if(maxPlanQty >= quota){
                       logDetail.append(StringUtils.format("【正常排班】上限值大于等于定额，最大上限为定额：{}",quota)).append(division);
                       maxPlanQty=quota;
                   }
                   logDetail.append(StringUtils.format("【正常排班】当前机台：{},当前班次：{}，正常，班次计划量上限：{},机台定额：{}",lhMachineName,cls.getClassName(),maxPlanQty,quota)).append(division);
                   orderNoLog.append(StringUtils.format("【正常排班】当前机台：{},当前班次：{}，正常，班次计划量上限：{},机台定额：{}",lhMachineName,cls.getClassName(),maxPlanQty,quota)).append(division);
                   onNormalClassPlanSet(embryoCode,cls,estimateShiftStock,maxPlanQty,lhEngineScheduleResult,enableStartTime,enableEndTime,logDetail,orderNoLog);
               }
           }
           //遍历结束后如果库存够时没有触发更新当班成型，在此进行触发累加成型当班计划量
           updateEstimateLhClassEmbryoStock(embryoCode,cls,logDetail,new StringBuilder());

       }else{
           logDetail.append(StringUtils.format("【同胎胚班次计划量安排】,当前胎胚：【{}】，当前班次:【{}】",embryoCode,cls.getClassName())).append(division);
       }
    }

    /**
     * 标记换模原因分析
     * @param lhEngineScheduleResult
     * @param changeMoldShift
     */
    private void markChangeMoldAnalysis(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum changeMoldShift) {
        LhEngineScheduleUtils.setClassShiftChangeMoldFlag(lhEngineScheduleResult,changeMoldShift,true);
        LhEngineScheduleUtils.setClassShiftAnalysis(lhEngineScheduleResult,changeMoldShift,I18nUtil.getMessage("lh.engine.change.mold.analysis.title"));
        LhEngineScheduleUtils.setAnalysisCode(lhEngineScheduleResult,changeMoldShift,AnalysisCodeEnum.CHANGE_MOLD);
    }

    /**
     * 换模规格计划安排
     * @param embryoCode
     * @param cls
     * @param estimateShiftStock
     * @param maxPlanQty
     * @param lhEngineScheduleResult
     * @param enableStartTime
     * @param enableEndTime
     * @param logDetail
     */
    private void onChangeMoldClassPlanSet(String embryoCode, LhClassShiftEnum cls, Integer estimateShiftStock, Integer maxPlanQty, LhEngineScheduleResult lhEngineScheduleResult, Date enableStartTime, Date enableEndTime,StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【换模后规格计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》开始",embryoCode,cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【换模后规格计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》开始",embryoCode,cls.getClassName())).append(division);
        String lhMachineCode=lhEngineScheduleResult.getLhMachineCode();
        String lhMachineName=lhEngineScheduleResult.getLhMachineName();
        Date changeMoldTime=lhEngineScheduleResult.getChangeMoldTime();
        LhClassShiftEnum changeMoldShift=timeInClassShift(changeMoldTime);
        //计算换模后的时间
        Integer changeMoldHour=getChangeMoldTime(LhEngineParamCodeConstants.CHANGE_MOLD_TIME_HOUR);
        //换模的时间和班次开始+换模时间 如果大于班次+6小时，该班次计划量无效
        Date afterChangeMoldTime=DateUtils.addHours(changeMoldTime,changeMoldHour);
        Date shiftChangeMoldTime= DateUtils.addHours(getShiftBeginTimeByClass(cls),changeMoldHour);

        Integer useMoldNumber=lhEngineScheduleResult.getUseMoldNumber();
        if(useMoldNumber==null){
            useMoldNumber=LhEngineConstants.TWO_MOLD_NUMBER;
        }

        //换模后规格，进行换模原因标记，根据可用时间往前推6个小时找到对应的班次进行换模原因标记
        if(afterChangeMoldTime.getTime() >= shiftChangeMoldTime.getTime()){
            //Joran 2022-11-14 日志打印换模班次为空指针处理start
            String logShiftClassName="";
            if(changeMoldShift!=null){
                logShiftClassName=changeMoldShift.getClassName();
            }
            //Joran 2022-11-14 日志打印换模班次为空指针处理end
            logDetail.append(StringUtils.format("【班次无效任务清零】,当前胎胚：【{}】，当前班次:【{}】,换模后时间：【{}】，班次开始+换模时间后的时间：【{}】",embryoCode,logShiftClassName,DateUtil.formatDatetime(afterChangeMoldTime),DateUtil.formatDatetime(shiftChangeMoldTime))).append(division);
            orderNoLog.append(StringUtils.format("【班次无效任务清零】,当前胎胚：【{}】，当前班次:【{}】,换模后时间：【{}】，班次开始+换模时间后的时间：【{}】",embryoCode,logShiftClassName,DateUtil.formatDatetime(afterChangeMoldTime),DateUtil.formatDatetime(shiftChangeMoldTime))).append(division);
            LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,0);
            //Joran 2022-07-25 下个班次开班逻辑验证start
            Integer nextClassIndex=cls.getClassIndex()+1;
            LhClassShiftEnum nextCls=LhClassShiftEnum.getClassShiftByClassIndex(nextClassIndex);
            logDetail.append(StringUtils.format("【当前班次无法开班】,换模结束时间大于等于开班时间+6小时，当前班次无法开班开汽。下一个开班开汽班次：【{}】",nextCls==null?"":nextCls.getClassName())).append(division);
            orderNoLog.append(StringUtils.format("【换模后规格计划安排】,换模结束时间大于等于开班时间+6小时。下一个开班开汽班次：【{}】",nextCls==null?"":nextCls.getClassName())).append(division);
            if(nextCls!=null){
                //验证是否开班还是开汽
                validateOpenShiftOrOpenStream(lhEngineScheduleResult,enableEndTime,nextCls,logDetail,orderNoLog);
            }
            //Joran 2022-07-25 下个班次开班逻辑验证end
            return;
        }

        Date lhEndTime=getMachineLhEndTime(lhMachineCode);
        if(lhEndTime==null){
            lhEndTime=enableEndTime;
        }

        //获取定额数据
        Integer quota= lhEngineScheduleResult.getQuota();
        if(estimateShiftStock >= maxPlanQty){
            //如果最大计划量小于定额时，证明不是满班排，先预排后进行时间段扣除后计划安排
            LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,enableStartTime);
            if(maxPlanQty<quota){
                Integer planQty= maxPlanQty;
                //获取到胎胚开始供应时间
                Date embryoStartTime=getEmbryoCodeStartTime(embryoCode,logDetail);
                LhClassShiftEnum embryoShift=null;
                if(embryoStartTime!=null){
                    embryoShift=timeInClassShift(embryoStartTime);
                }
                Integer brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
                Integer useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,planQty,brushBagTime);
                //计算预计硫化结束时间
                Date estimateEndTime=DateUtils.addSeconds(enableStartTime,useTotalSecond);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,estimateEndTime,cls,logDetail,orderNoLog);
                logDetail.append(StringUtils.format("【换模排班未满班全额库存】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
                orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
                //整体更新库存逻辑抽取
                updateEstimateLhClassShiftStock(embryoStartTime,estimateEndTime,embryoCode,embryoShift,cls,logDetail,orderNoLog);
                //如果胎胚时间不为空且预计硫化结束时间不为空的话进行计划量补全
                if(embryoStartTime!=null&&estimateEndTime!=null){
                    //获取最新的预计库存
                    estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
                    if(estimateShiftStock>0){
                        //胎胚时间晚于硫化预计结束时间且胎胚时间在当前班次内
                        if(embryoStartTime.getTime()>estimateEndTime.getTime()&&cls.equals(embryoShift)){
                            Integer continuePlanQty = getClassMaxPlanQty(cls,lhEngineScheduleResult,embryoStartTime,enableEndTime,logDetail,orderNoLog);
                            //更新后的库存如果大于计划量和剩余可排任务量的话直接全排
                            if(estimateShiftStock>=(planQty+continuePlanQty)){
                                planQty+=continuePlanQty;
                                logDetail.append(StringUtils.format("【换模排班未满班全额库存】,扣除待料时间差后，汇总两段计划量:【{}】",planQty)).append(division);
                                orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,扣除待料时间差后，汇总两段计划量:【{}】",planQty)).append(division);
                                //更新硫化结束时间
                                setMachineLhEndTime(lhMachineCode,lhMachineName,enableEndTime,cls,logDetail,orderNoLog);
                            }
                            //标记成型待料
                            markSourceLackAnalysis(lhEngineScheduleResult,cls);
                            logDetail.append(StringUtils.format("【换模排班未满班全额库存】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】,胎胚时间大于预计硫化结束时间标记待料原因分析",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
                            orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】，胎胚时间大于预计硫化结束时间标记待料原因分析",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
                        }else{
                            Integer continuePlanQty = getClassMaxPlanQty(cls,lhEngineScheduleResult,estimateEndTime,enableEndTime,logDetail,orderNoLog);
                            //更新后的库存如果大于计划量和剩余可排任务量的话直接全排
                            if(estimateShiftStock>=(planQty+continuePlanQty)){
                                planQty+=continuePlanQty;
                                logDetail.append(StringUtils.format("【换模排班未满班全额库存】,预计硫化结束时间大于胎胚时间无时间差汇总，汇总两段计划量:【{}】",planQty)).append(division);
                                orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,预计硫化结束时间大于胎胚时间无时间差汇总，汇总两段计划量:【{}】",planQty)).append(division);
                                //更新硫化结束时间
                                setMachineLhEndTime(lhMachineCode,lhMachineName,enableEndTime,cls,logDetail,orderNoLog);
                            }
                        }
                        if(planQty>quota){
                            planQty=quota;
                            logDetail.append(StringUtils.format("【换模排班未满班全额库存】,汇总两段计划量,大于定额，取定额:{}为计划量",planQty)).append(division);
                            orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,汇总两段计划量,大于定额，取定额:{}为计划量",planQty)).append(division);
                        }
                        LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
                        //更新胎胚剩余
                        estimateShiftStock-=planQty;
                        //扣除后更新班次预计库存
                        sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
                    }
                }else{ //如果没有胎胚时间，且有库存时按库存进行排载
                    //获取最新的预计库存
                    estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
                    logDetail.append(StringUtils.format("【换模排班未满班全额库存】,没有取到胎胚开始时间对比计划量库存，获取到的库存数据={}",estimateShiftStock)).append(division);
                    orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,没有取到胎胚开始时间对比计划量库存，获取到的库存数据={}",estimateShiftStock)).append(division);
                    if(estimateShiftStock>0){
                        logDetail.append(StringUtils.format("【换模排班未满班全额库存】,有库存数进行库存与最大计划量筛选后安排计划！")).append(division);
                        orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,有库存数进行库存与最大计划量筛选后安排计划")).append(division);
                        if(estimateShiftStock<=planQty){
                            planQty=estimateShiftStock;
                            logDetail.append(StringUtils.format("【换模排班未满班全额库存】,更新最大计划量为库存数:{}",planQty)).append(division);
                            orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,更新最大计划量为库存数:{}",planQty)).append(division);
                        }
                        logDetail.append(StringUtils.format("【换模排班未满班全额库存】,最终计划排载量:{}",planQty)).append(division);
                        orderNoLog.append(StringUtils.format("【换模排班未满班全额库存】,最终计划排载量:{}",planQty)).append(division);
                        LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
                        //更新胎胚剩余
                        estimateShiftStock-=planQty;
                        //扣除后更新班次预计库存
                        sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
                    }

                }
            }else{
                logDetail.append(StringUtils.format("【换模后规格计划安排】,班次预计开始库存：【{}】，班次可排计划量:【{}】,库存足够，当前班次：{}，按最大可排计划量进行安排！",estimateShiftStock,maxPlanQty,cls.getClassName())).append(division);
                orderNoLog.append(StringUtils.format("【换模后规格计划安排】,班次预计开始库存：【{}】，班次可排计划量:【{}】,库存足够，当前班次：{}，按最大可排计划量进行安排！",estimateShiftStock,maxPlanQty,cls.getClassName())).append(division);
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,maxPlanQty);
                LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,enableStartTime);
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,enableEndTime);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,enableEndTime,cls,logDetail,orderNoLog);
                //更新胎胚剩余
                estimateShiftStock-=maxPlanQty;
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
            }
        }else{
            /*//判断奇偶数
            Integer planQty= LhEngineScheduleUtils.getMaxPlanQtyByMoldNumber(estimateShiftStock,useMoldNumber);*/
            //Joran 2022-07-05 如果剩余数就是奇数就是奇数
            Integer planQty=estimateShiftStock;
            LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,enableStartTime);
            //预估班次库存
            estimateShiftStock=estimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
            //获取到胎胚开始供应时间
            Date embryoStartTime=getEmbryoCodeStartTimeByClassShift(embryoCode,estimateShiftStock,cls,logDetail);
            LhClassShiftEnum embryoShift=null;
            if(embryoStartTime!=null){
                embryoShift=timeInClassShift(embryoStartTime);
            }
            Integer brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
            Integer useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,planQty,brushBagTime);
            //计算预计硫化结束时间
            Date estimateEndTime=DateUtils.addSeconds(enableStartTime,useTotalSecond);
            if(estimateShiftStock<=0||estimateEndTime.equals(enableStartTime)){
                estimateEndTime=lhEndTime;
            }

            //Joran 2022-07-05 如果硫化结束时间在班次开始时间之前的，硫化时间为班次开始时间 start
            Date shiftStartTime=getShiftBeginTimeByClass(cls);
            if(estimateEndTime!=null&&estimateEndTime.getTime()<=shiftStartTime.getTime()){
                estimateEndTime=shiftStartTime;
            }
            //Joran 2022-07-05 如果硫化结束时间在班次开始时间之前的，硫化时间为班次开始时间 end


            logDetail.append(StringUtils.format("【换模后规格计划安排】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
            orderNoLog.append(StringUtils.format("【换模后规格计划安排】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);

            updateEstimateLhClassShiftStock(embryoStartTime,estimateEndTime,embryoCode,embryoShift,cls,logDetail,orderNoLog);
            //获取最新的预计库存
            estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
            planQty=estimateShiftStock;
            logDetail.append(StringUtils.format("【换模后规格计划安排】,更新当前班次：【{}】成型计划后，班次预计可用库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            orderNoLog.append(StringUtils.format("【换模后规格计划安排】,更新当前班次：【{}】成型计划后，班次预计可用库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            if(estimateShiftStock >= maxPlanQty){
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,maxPlanQty);
                //重新计算结束时间
                brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
                useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,maxPlanQty,brushBagTime);
                Date endTime=DateUtils.addSeconds(enableStartTime,useTotalSecond);
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,endTime);
                logDetail.append(StringUtils.format("【换模后规格计划安排】,库存足够。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                orderNoLog.append(StringUtils.format("【换模后规格计划安排】,库存足够。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,endTime,cls,logDetail,orderNoLog);
                //更新胎胚剩余
                estimateShiftStock-=maxPlanQty;
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
            }else{
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
                //重新计算结束时间
                brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
                useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,planQty,brushBagTime);
                Date endTime=DateUtils.addSeconds(enableStartTime,useTotalSecond);
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,endTime);
                logDetail.append(StringUtils.format("【换模后规格计划安排】,库存不足。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                orderNoLog.append(StringUtils.format("【换模后规格计划安排】,库存不足。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,endTime,cls,logDetail,orderNoLog);
                estimateShiftStock-=planQty;//班次预计的班次库存清空
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);

                //库存不足时进行下个班次开班开汽逻辑
                logDetail.append(StringUtils.format("【换模后规格计划安排】,库存不足时，进行开班开汽逻辑标记。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(enableStartTime))).append(division);
                orderNoLog.append(StringUtils.format("【换模后规格计划安排】,库存不足时，进行开班开汽逻辑标记。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(enableStartTime))).append(division);
                if(cls!=null&&planQty>BigDecimal.ZERO.intValue()){
                    logDetail.append(StringUtils.format("【换模后规格计划安排】,库存不足时，当前班次开班开汽。当前班次：【{}】",cls.getClassName())).append(division);
                    orderNoLog.append(StringUtils.format("【换模后规格计划安排】,库存不足时，当前班次开班开汽。当前班次：【{}】",cls.getClassName())).append(division);
                    //markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,endTime,cls,logDetail,orderNoLog);
                    //验证是否开班还是开汽
                    validateOpenShiftOrOpenStream(lhEngineScheduleResult,endTime,cls,logDetail,orderNoLog);
                }else{
                    //库存不足时,进行下个班次开班开汽逻辑
                    Integer nextClassIndex=cls.getClassIndex()+1;
                    LhClassShiftEnum nextCls=LhClassShiftEnum.getClassShiftByClassIndex(nextClassIndex);
                    logDetail.append(StringUtils.format("【换模后规格计划安排】,库存不足时，当前班次无法开班开汽。下一个开班开汽班次：【{}】",nextCls==null?"":nextCls.getClassName())).append(division);
                    orderNoLog.append(StringUtils.format("【换模后规格计划安排】,库存不足时，当前班次无法开班开汽。下一个开班开汽班次：【{}】",nextCls==null?"":nextCls.getClassName())).append(division);
                    if(nextCls!=null){
                        //验证是否开班还是开汽
                        validateOpenShiftOrOpenStream(lhEngineScheduleResult,endTime,nextCls,logDetail,orderNoLog);
                    }
                }

            }
        }
        if(changeMoldShift!=null&&cls.equals(changeMoldShift)){
            logDetail.append(StringUtils.format("【换模原因标记】,当前胎胚：【{}】，当前班次:【{}】,前规格标记，当前不标记！",embryoCode,changeMoldShift.getClassName())).append(division);
            orderNoLog.append(StringUtils.format("【换模原因标记】,当前胎胚：【{}】，当前班次:【{}】,前规格标记，当前不标记！",embryoCode,changeMoldShift.getClassName())).append(division);
            //换模原因标记
            //markChangeMoldAnalysis(lhEngineScheduleResult,changeMoldShift);
        }
        logDetail.append(StringUtils.format("【换模后规格计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》结束",embryoCode,cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【换模后规格计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》结束",embryoCode,cls.getClassName())).append(division);
    }

    /**
     * 开班开汽验证
     * @param lhEngineScheduleResult
     * @param endTime
     * @param nextCls
     * @param logDetail
     * @param orderNoLog
     */
    private void validateOpenShiftOrOpenStream(LhEngineScheduleResult lhEngineScheduleResult,Date endTime, LhClassShiftEnum nextCls, StringBuilder logDetail, StringBuilder orderNoLog) {
        Date scheduleDate=lhEngineScheduleResult.getScheduleDate();
        String lhMachineCode=lhEngineScheduleResult.getLhMachineCode();
        String embryoCode=lhEngineScheduleResult.getEmbryoCode();

       //Joran 2022-08-30 如果前面当前班次的前面班次已经有计划量的话，则表示已经开班，那么当前的最新规格应该是自己本身start
        Boolean  isSelf=lastSpecIsSelf(lhEngineScheduleResult,nextCls,logDetail,orderNoLog);
       //Joran 2022-08-30 如果前面当前班次的前面班次已经有计划量的话，则表示已经开班，那么当前的最新规格应该是自己本身end

        LhEngineScheduleResult lastMachineSpec=null;
        if(!isSelf){
            lastMachineSpec=getLastMachineScheduleTask(scheduleDate,lhMachineCode,embryoCode,logDetail,orderNoLog);
        }else{
            lastMachineSpec=lhEngineScheduleResult;
        }
        if(lastMachineSpec!=null){
            Date maxLhEndTime=getMaxLhEndTime(lastMachineSpec,scheduleDate,logDetail,orderNoLog);
            //更新机台规格硫化结束时间
            setMachineLhEndTime(lhMachineCode,lhEngineScheduleResult.getLhMachineName(),maxLhEndTime,nextCls,logDetail,orderNoLog);
            if(changeMoldCheck(lhEngineScheduleResult,lastMachineSpec,logDetail,orderNoLog)){
                markOpenStreamShiftBySingleTask(lastMachineSpec.getEmbryoCode(),maxLhEndTime,nextCls,lhEngineScheduleResult,logDetail,orderNoLog);
            }else{
                markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,endTime,nextCls,logDetail,orderNoLog);
            }
        }else{
            markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,endTime,nextCls,logDetail,orderNoLog);
        }
    }

    /**
     * 前规格是否是自己
     * @param lhEngineScheduleResult
     * @param nextCls
     * @param logDetail
     * @param orderNoLog
     * @return
     */
    private Boolean lastSpecIsSelf(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum nextCls, StringBuilder logDetail, StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【换模排班前规格自身判断】,当前胎胚：【{}】，当前班次:【{}】,前规格是否本身判断--》开始",lhEngineScheduleResult.getEmbryoCode(),nextCls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【换模排班前规格自身判断】,当前胎胚：【{}】，当前班次:【{}】,前规格是否本身判断--》开始",lhEngineScheduleResult.getEmbryoCode(),nextCls.getClassName())).append(division);
        Boolean isSelf=false;
        if(nextCls!=null &&nextCls.getClassIndex()>LhClassShiftEnum.ONE_CLASS_SHIFT.getClassIndex()){
            for(int shift=1;shift<nextCls.getClassIndex();shift++){
                LhClassShiftEnum lastCls=LhClassShiftEnum.getClassShiftByClassIndex(shift);
                Integer lastPlanQty=LhEngineScheduleUtils.getLhClassPlanQty(lhEngineScheduleResult,lastCls);
                if(lastPlanQty>0){
                    isSelf=true;
                    break;
                }
            }
        }
        logDetail.append(StringUtils.format("【换模排班前规格自身判断】,当前胎胚：【{}】，当前班次:【{}】,前规格是否本身判断,结果：【{}】--》结束",lhEngineScheduleResult.getEmbryoCode(),nextCls.getClassName(),isSelf)).append(division);
        orderNoLog.append(StringUtils.format("【换模排班前规格自身判断】,当前胎胚：【{}】，当前班次:【{}】,前规格是否本身判断,结果：【{}】--》结束",lhEngineScheduleResult.getEmbryoCode(),nextCls.getClassName(),isSelf)).append(division);
        return isSelf;
    }

    /**
     * 正常计划安排
     * @param embryoCode
     * @param cls
     * @param estimateShiftStock
     * @param maxPlanQty
     * @param lhEngineScheduleResult
     * @param logDetail
     */
    private void onNormalClassPlanSet(String embryoCode, LhClassShiftEnum cls, Integer estimateShiftStock, Integer maxPlanQty, LhEngineScheduleResult lhEngineScheduleResult,Date shiftBeginTime,Date shiftEndTime,StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【正常计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》开始",embryoCode,cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【正常计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》开始",embryoCode,cls.getClassName())).append(division);

        String lhMachineCode=lhEngineScheduleResult.getLhMachineCode();
        String lhMachineName=lhEngineScheduleResult.getLhMachineName();
        Integer useMoldNumber=lhEngineScheduleResult.getUseMoldNumber();
        if(useMoldNumber==null){
            useMoldNumber=LhEngineConstants.TWO_MOLD_NUMBER;
        }
        Date lhEndTime=getMachineLhEndTime(lhMachineCode);
        if(lhEndTime==null){
            lhEndTime=shiftBeginTime;
        }
        //获取定额数据
        Integer quota= lhEngineScheduleResult.getQuota();
        if(estimateShiftStock >= maxPlanQty){
            //如果最大计划量小于定额时，证明不是满班排，先预排后进行时间段扣除后计划安排
            LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,shiftBeginTime);
            if(maxPlanQty<quota){
                Integer planQty= maxPlanQty;
                //获取到胎胚开始供应时间
                Date embryoStartTime=getEmbryoCodeStartTime(embryoCode,logDetail);
                LhClassShiftEnum embryoShift=null;
                if(embryoStartTime!=null){
                    embryoShift=timeInClassShift(embryoStartTime);
                }
                Integer brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
                Integer useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,planQty,brushBagTime);
                //计算预计硫化结束时间
                Date estimateEndTime=DateUtils.addSeconds(shiftBeginTime,useTotalSecond);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,estimateEndTime,cls,logDetail,orderNoLog);
                logDetail.append(StringUtils.format("【正常排班未满班全额库存】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
                orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
               //整体更新库存逻辑抽取
               updateEstimateLhClassShiftStock(embryoStartTime,estimateEndTime,embryoCode,embryoShift,cls,logDetail,orderNoLog);
               //如果胎胚时间不为空且预计硫化结束时间不为空的话进行计划量补全
               if(embryoStartTime!=null&&estimateEndTime!=null){
                   //获取最新的预计库存
                   estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
                   if(estimateShiftStock>0){
                       //胎胚时间晚于硫化预计结束时间且胎胚时间在当前班次内
                       if(embryoStartTime.getTime()>estimateEndTime.getTime()&&cls.equals(embryoShift)){
                           Integer continuePlanQty = getClassMaxPlanQty(cls,lhEngineScheduleResult,embryoStartTime,shiftEndTime,logDetail,orderNoLog);
                           //更新后的库存如果大于计划量和剩余可排任务量的话直接全排
                           if(estimateShiftStock>=(planQty+continuePlanQty)){
                               planQty+=continuePlanQty;
                               logDetail.append(StringUtils.format("【正常排班未满班全额库存】,扣除待料时间差后，汇总两段计划量:【{}】",planQty)).append(division);
                               orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,扣除待料时间差后，汇总两段计划量:【{}】",planQty)).append(division);
                               //更新硫化结束时间
                               setMachineLhEndTime(lhMachineCode,lhMachineName,shiftEndTime,cls,logDetail,orderNoLog);
                           }
                           //标记成型待料
                           markSourceLackAnalysis(lhEngineScheduleResult,cls);
                           logDetail.append(StringUtils.format("【正常排班未满班全额库存】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】,胎胚时间大于预计硫化结束时间标记待料原因分析",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
                           orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】，胎胚时间大于预计硫化结束时间标记待料原因分析",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
                       }else{
                           Integer continuePlanQty = getClassMaxPlanQty(cls,lhEngineScheduleResult,estimateEndTime,shiftEndTime,logDetail,orderNoLog);
                           //更新后的库存如果大于计划量和剩余可排任务量的话直接全排
                           if(estimateShiftStock>=(planQty+continuePlanQty)){
                               planQty+=continuePlanQty;
                               logDetail.append(StringUtils.format("【正常排班未满班全额库存】,硫化结束时间大于胎胚时间无时间差，汇总两段计划量:【{}】",planQty)).append(division);
                               orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,硫化结束时间大于胎胚时间无时间差，汇总两段计划量:【{}】",planQty)).append(division);
                               //更新硫化结束时间
                               setMachineLhEndTime(lhMachineCode,lhMachineName,shiftEndTime,cls,logDetail,orderNoLog);
                           }
                       }

                       if(planQty>quota){
                           planQty=quota;
                           logDetail.append(StringUtils.format("【正常排班未满班全额库存】,汇总两段计划量,大于定额，取定额:{}为计划量",planQty)).append(division);
                           orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,汇总两段计划量,大于定额，取定额:{}为计划量",planQty)).append(division);
                       }

                       LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
                       //更新胎胚剩余
                       estimateShiftStock-=planQty;
                       //扣除后更新班次预计库存
                       sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
                   }
               }else{ //如果没有胎胚时间，且有库存时按库存进行排载
                   //获取最新的预计库存
                   estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
                   logDetail.append(StringUtils.format("【正常排班未满班全额库存】,没有取到胎胚开始时间对比计划量库存，获取到的库存数据={}",estimateShiftStock)).append(division);
                   orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,没有取到胎胚开始时间对比计划量库存，获取到的库存数据={}",estimateShiftStock)).append(division);
                   if(estimateShiftStock>0){
                       logDetail.append(StringUtils.format("【正常排班未满班全额库存】,有库存数进行库存与最大计划量筛选后安排计划！")).append(division);
                       orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,有库存数进行库存与最大计划量筛选后安排计划")).append(division);
                       if(estimateShiftStock<=planQty){
                           planQty=estimateShiftStock;
                           logDetail.append(StringUtils.format("【正常排班未满班全额库存】,更新最大计划量为库存数：{}",planQty)).append(division);
                           orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,更新最大计划量为库存数：{}",planQty)).append(division);
                       }
                       logDetail.append(StringUtils.format("【正常排班未满班全额库存】,最终计划排载量:{}",planQty)).append(division);
                       orderNoLog.append(StringUtils.format("【正常排班未满班全额库存】,最终计划排载量:{}",planQty)).append(division);
                       LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
                       //更新胎胚剩余
                       estimateShiftStock-=planQty;
                       //扣除后更新班次预计库存
                       sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
                   }

               }
            }else{
                logDetail.append(StringUtils.format("【正常计划安排】,班次预计开始库存：【{}】，班次可排计划量:【{}】,库存足够，当前班次：{}，按最大可排计划量进行安排！",estimateShiftStock,maxPlanQty,cls.getClassName())).append(division);
                orderNoLog.append(StringUtils.format("【正常计划安排】,班次预计开始库存：【{}】，班次可排计划量:【{}】,库存足够，当前班次：{}，按最大可排计划量进行安排！",estimateShiftStock,maxPlanQty,cls.getClassName())).append(division);
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,maxPlanQty);
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,shiftEndTime);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,shiftEndTime,cls,logDetail,orderNoLog);
                //更新胎胚剩余
                estimateShiftStock-=maxPlanQty;
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
            }
        }else{
            //判断奇偶数
            //Integer planQty= LhEngineScheduleUtils.getMaxPlanQtyByMoldNumber(estimateShiftStock,useMoldNumber);
            //Joran 2022-07-05确认如果最终的库存是奇数就直接安排库存
            Integer planQty= estimateShiftStock;
            LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,shiftBeginTime);
            //获取到胎胚开始供应时间
            Date embryoStartTime=getEmbryoCodeStartTime(embryoCode,logDetail);
            LhClassShiftEnum embryoShift=null;
            if(embryoStartTime!=null){
                embryoShift=timeInClassShift(embryoStartTime);
            }
            Integer brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
            Integer useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,planQty,brushBagTime);
            //计算预计硫化结束时间
            Date estimateEndTime=DateUtils.addSeconds(shiftBeginTime,useTotalSecond);
            if(estimateShiftStock<=0||estimateEndTime.equals(shiftBeginTime)){
                estimateEndTime=lhEndTime;
            }
            //Joran 2022-07-05 如果硫化结束时间在班次开始时间之前的，硫化时间为班次开始时间 start
            Date shiftStartTime=getShiftBeginTimeByClass(cls);
            if(estimateEndTime!=null&&estimateEndTime.getTime()<=shiftStartTime.getTime()){
                estimateEndTime=shiftStartTime;
            }
            //Joran 2022-07-05 如果硫化结束时间在班次开始时间之前的，硫化时间为班次开始时间 end

            logDetail.append(StringUtils.format("【正常计划安排】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
            orderNoLog.append(StringUtils.format("【正常计划安排】,胎胚开始时间：【{}】，预排后预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
            updateEstimateLhClassShiftStock(embryoStartTime,estimateEndTime,embryoCode,embryoShift,cls,logDetail,orderNoLog);
            //获取最新的预计库存
            estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
            //planQty= LhEngineScheduleUtils.getMaxPlanQtyByMoldNumber(estimateShiftStock,useMoldNumber);
            planQty=estimateShiftStock;
            logDetail.append(StringUtils.format("【正常计划安排】,更新当前班次：【{}】成型计划后，班次预计可用库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            orderNoLog.append(StringUtils.format("【正常计划安排】,更新当前班次：【{}】成型计划后，班次预计可用库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            if(estimateShiftStock >= maxPlanQty){
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,maxPlanQty);
                //重新计算结束时间
                brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
                useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,maxPlanQty,brushBagTime);
                Date endTime=DateUtils.addSeconds(shiftBeginTime,useTotalSecond);
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,endTime);
                logDetail.append(StringUtils.format("【正常计划安排】,库存足够。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                orderNoLog.append(StringUtils.format("【正常计划安排】,库存足够。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,endTime,cls,logDetail,orderNoLog);
                //更新胎胚剩余
                estimateShiftStock-=maxPlanQty;
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
            }else{
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
                //重新计算结束时间
                brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
                useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,planQty,brushBagTime);
                Date endTime=DateUtils.addSeconds(shiftBeginTime,useTotalSecond);
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,endTime);
                logDetail.append(StringUtils.format("【正常计划安排】,库存不足。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                orderNoLog.append(StringUtils.format("【正常计划安排】,库存不足。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,endTime,cls,logDetail,orderNoLog);
                estimateShiftStock-=planQty;//班次预计的班次库存清空
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);

                //库存不足时,进行下个班次开班开汽逻辑
                Integer nextClassIndex=cls.getClassIndex()+1;
                LhClassShiftEnum nextCls=LhClassShiftEnum.getClassShiftByClassIndex(nextClassIndex);
                logDetail.append(StringUtils.format("【正常计划安排】,库存不足时，对下个班次进行开班开汽逻辑标记。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(shiftBeginTime))).append(division);
                orderNoLog.append(StringUtils.format("【正常计划安排】,库存不足时，对下个班次进行开班开汽逻辑标记。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(shiftBeginTime))).append(division);
                if(nextCls!=null){
                    //Joran 2022-08-16 如果下个班次标记为换模直接不判断开班开汽start
                    Boolean changMoldFlag=LhEngineScheduleUtils.getClassShiftChangeMoldFlag(lhEngineScheduleResult,nextCls);
                    if(!changMoldFlag){
                        markOpenStreamShiftBySingleTask(embryoCode,endTime,nextCls,lhEngineScheduleResult,logDetail,orderNoLog);
                    }else{
                        //库存排完后还有剩余，下个班次标记开班逻辑处理
                        shiftMarkChangeMoldFlag(lhEngineScheduleResult,quota,embryoCode,nextCls,logDetail,orderNoLog);
                    }
                    //Joran 2022-08-16 如果下个班次标记为换模直接不判断开班开汽end
                }


            }
        }
        logDetail.append(StringUtils.format("【正常计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》结束",embryoCode,cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【正常计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》结束",embryoCode,cls.getClassName())).append(division);
    }

    /**
     * 下个班次开班开汽逻辑判断时，下个班次标记换模处理逻辑
     * @param lhEngineScheduleResult
     * @param quota
     * @param embryoCode
     * @param nextCls
     * @param logDetail
     * @param orderNoLog
     */
    private void shiftMarkChangeMoldFlag(LhEngineScheduleResult lhEngineScheduleResult, Integer quota, String embryoCode, LhClassShiftEnum nextCls, StringBuilder logDetail, StringBuilder orderNoLog) {
        //班次开始时间
        Date classBeginTime=getShiftBeginTimeByClass(nextCls);
        //班次结束时间
        Date classEndTime=getShiftEndTimeByClass(nextCls);
        Date enableEndTime=lhEngineScheduleResult.getEnableEndTime();
        if(enableEndTime==null){
            logDetail.append(StringUtils.format("【自动排班，下个班次标记换模】,机台可用结束时间为空，当前班次终止自动排程！")).append(division);
            orderNoLog.append(StringUtils.format("【自动排班，下个班次标记换模】,机台可用结束时间为空，当前班次终止自动排程！")).append(division);
            return;
        }
        if(classBeginTime.getTime()>= enableEndTime.getTime()){
            logDetail.append(StringUtils.format("【自动排班，下个班次标记换模】,机台可用结束时间早于班次开始时间，当前班次终止自动排程！")).append(division);
            logDetail.append(StringUtils.format("【自动排班，下个班次标记换模】,机台可用结束时间早于班次开始时间，当前班次终止自动排程！")).append(division);
            return;
        }
        if(enableEndTime.getTime()>=classEndTime.getTime()){
            enableEndTime=classEndTime;
        }
        //Joran 2022-08-16 如果当前班次标记为换模 则直接按换模来处理
        //获取最新的预计库存
        Integer estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
        if(estimateShiftStock<=BigDecimal.ZERO.intValue()){
            logDetail.append(StringUtils.format("【自动排班，下个班次标记换模】,没有胎胚可用库存，当前班次终止自动排程！")).append(division);
            logDetail.append(StringUtils.format("【自动排班，下个班次标记换模】,没有胎胚可用库存，当前班次终止自动排程！")).append(division);
            return;
        }
        logDetail.append(StringUtils.format("【自动排班，下个班次标记换模】,库存不足时，下个班次已经标记为换模，不走开班开汽业务逻辑。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",nextCls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(enableEndTime))).append(division);
        orderNoLog.append(StringUtils.format("【自动排班，下个班次标记换模】,库存不足时，下个班次已经标记为换模，不走开班开汽业务逻辑。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",nextCls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(enableEndTime))).append(division);
        Integer planQty=getClassMaxPlanQty(nextCls,lhEngineScheduleResult,classBeginTime,enableEndTime,logDetail,orderNoLog);
        if(planQty>=quota){
            planQty=quota;
        }
        if(planQty>=estimateShiftStock){
            planQty=estimateShiftStock;
        }
        LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,nextCls,planQty);
        LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,nextCls,enableEndTime);
        estimateShiftStock-=planQty;//班次预计的班次库存清空
        //扣除后更新班次预计库存
        sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
    }

    /**
     * 整体更新库存逻辑抽取
     * @param embryoStartTime
     * @param estimateEndTime
     * @param embryoCode
     * @param embryoShift
     * @param cls
     * @param logDetail
     * @param orderNoLog
     */
    private void updateEstimateLhClassShiftStock(Date embryoStartTime, Date estimateEndTime, String embryoCode, LhClassShiftEnum embryoShift, LhClassShiftEnum cls, StringBuilder logDetail, StringBuilder orderNoLog) {
        //如果小于3个小时即可以连续。就可以加上当前班次的成型计划
        Long diffHour=null;
        if(embryoStartTime!=null && estimateEndTime!=null){
            diffHour= LhEngineScheduleUtils.diffDate(estimateEndTime,embryoStartTime, LhEngineScheduleUtils.HOUR);
            logDetail.append(StringUtils.format("【公用更新库存逻辑】,胎胚开始时间和硫化结束时间间隔：{}小时",diffHour)).append(division);
            orderNoLog.append(StringUtils.format("【公用更新库存逻辑】,胎胚开始时间和硫化结束时间间隔：{}小时",diffHour)).append(division);
        }
        //更新标记
        String key=GenerageMapKeyUtils.createMapKey(embryoCode,cls.getClassIndex()+"");
        Integer addStockHourCondition=getAddStockHour(LhEngineParamCodeConstants.END_DIFF_START_TIME_HOUR);
        //验证是否更新库存
        Boolean isCheckAddStock=diffHour!=null && diffHour <= addStockHourCondition;
        if(((embryoShift!=null&&(getShiftEndTimeByClass(embryoShift).getTime()<=getShiftEndTimeByClass(cls).getTime()))||isCheckAddStock) &&!shiftUpdateCxPlanFlag.containsKey(key)){
            //胎胚可以连续安排，更新当前班次成型计划到预计班次库存中
            logDetail.append(StringUtils.format("【公用更新库存逻辑】,当前胎胚：【{}】，当前班次:【{}】,胎胚开始时间：【{}】,硫化结束时间：【{}】超过{}小时，进行成型该班计划添加到预计班次库存",embryoCode,cls.getClassName(),DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime),addStockHourCondition)).append(division);
            orderNoLog.append(StringUtils.format("【公用更新库存逻辑】,当前胎胚：【{}】，当前班次:【{}】,胎胚开始时间：【{}】,硫化结束时间：【{}】超过{}小时，进行成型该班计划添加到预计班次库存",embryoCode,cls.getClassName(),DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime),addStockHourCondition)).append(division);
            updateEstimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
        }
    }

    /**
     *  处理开汽班次计划量安排
     * @param embryoCode
     * @param cls
     * @param estimateShiftStock
     * @param planQty
     * @param lhEngineScheduleResult
     * @param logDetail
     */
    private void onOpenStreamClassPlanSet(String embryoCode, LhClassShiftEnum cls, Integer estimateShiftStock, Integer planQty, LhEngineScheduleResult lhEngineScheduleResult,Date shiftEndTime,StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【开汽计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》开始",embryoCode,cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【开汽计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》开始",embryoCode,cls.getClassName())).append(division);
        //开汽时间
        Date openStreamTime=lhEngineScheduleResult.getClassOpenStreamTime();
        String lhMachineCode=lhEngineScheduleResult.getLhMachineCode();
        String lhMachineName=lhEngineScheduleResult.getLhMachineName();
        Date lhEndTime=getMachineLhEndTime(lhMachineCode);

        Integer useMoldNumber=lhEngineScheduleResult.getUseMoldNumber();
        if(useMoldNumber==null){
            useMoldNumber=LhEngineConstants.TWO_MOLD_NUMBER;
        }

        if(lhEndTime==null){
            lhEndTime=openStreamTime;
        }
        //班次是否标记开汽原因
        Boolean openStreamFlag=true;

        if(estimateShiftStock >= planQty){
            logDetail.append(StringUtils.format("【开汽计划安排】,班次预计开始库存：【{}】，班次可排计划量:【{}】,库存足够，当前班次：{}，按最大可排计划量进行安排！",estimateShiftStock,planQty,cls.getClassName())).append(division);
            orderNoLog.append(StringUtils.format("【开汽计划安排】,班次预计开始库存：【{}】，班次可排计划量:【{}】,库存足够，当前班次：{}，按最大可排计划量进行安排！",estimateShiftStock,planQty,cls.getClassName())).append(division);
            LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
            //开汽时间为开始时间
            LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,lhEndTime);
            Integer brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
            Integer useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,planQty,brushBagTime);
            Date endTime=DateUtils.addSeconds(lhEndTime,useTotalSecond);
            //设置结束时间
            LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,endTime);
            //更新硫化结束时间
            setMachineLhEndTime(lhMachineCode,lhMachineName,endTime,cls,logDetail,orderNoLog);
            //更新胎胚剩余
            estimateShiftStock-=planQty;
            //扣除后更新班次预计库存
            sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
        }else{ //小于当前的计划量时，先计算出硫化结束的时间
            //开汽时间为开始时间
            //判断奇偶数
            //Integer enablePlanQty= LhEngineScheduleUtils.getMaxPlanQtyByMoldNumber(estimateShiftStock,useMoldNumber);
            //Joran 2022-07-05 如果剩余数是奇数就直接安排库存数
            Integer enablePlanQty=estimateShiftStock;
            LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,lhEndTime);
            //获取到胎胚开始供应时间
            Date embryoStartTime=getEmbryoCodeStartTime(embryoCode,logDetail);
            Integer brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
            Integer useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,enablePlanQty,brushBagTime);
            //预计硫化结束时间
            Date estimateEndTime=DateUtils.addSeconds(openStreamTime,useTotalSecond);
            if(estimateShiftStock<=0||estimateEndTime.equals(openStreamTime)){
                estimateEndTime=lhEndTime;
            }
            logDetail.append(StringUtils.format("【开汽计划安排】,胎胚开始时间：【{}】，预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);
            orderNoLog.append(StringUtils.format("【开汽计划安排】,胎胚开始时间：【{}】，预计硫化结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(estimateEndTime))).append(division);

            LhClassShiftEnum embryoShift=null;
            if(embryoStartTime!=null){
                embryoShift=timeInClassShift(embryoStartTime);
            }
            updateEstimateLhClassShiftStock(embryoStartTime,estimateEndTime,embryoCode,embryoShift,cls,logDetail,orderNoLog);
            //获取最新的预计库存
            estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
            //enablePlanQty= LhEngineScheduleUtils.getMaxPlanQtyByMoldNumber(estimateShiftStock,useMoldNumber);
            enablePlanQty=estimateShiftStock;
            logDetail.append(StringUtils.format("【开汽计划安排】,更新当前班次：【{}】成型计划后，班次预计可用库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            orderNoLog.append(StringUtils.format("【开汽计划安排】,更新当前班次：【{}】成型计划后，班次预计可用库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            if(estimateShiftStock >= planQty){
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
                //重新计算结束时间
                brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
                useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,planQty,brushBagTime);
                Date endTime=DateUtils.addSeconds(openStreamTime,useTotalSecond);
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,endTime);
                logDetail.append(StringUtils.format("【开汽计划安排】,库存足够。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                orderNoLog.append(StringUtils.format("【开汽计划安排】,库存足够。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,endTime,cls,logDetail,orderNoLog);
                //更新胎胚剩余
                estimateShiftStock-=planQty;
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
            }else if(estimateShiftStock.equals(BigDecimal.ZERO.intValue())||( estimateShiftStock.equals(BigDecimal.ONE.intValue()) &&useMoldNumber.equals(LhEngineConstants.TWO_MOLD_NUMBER))){ //班次预计库存为0时，无法进行开班标记处理
                openStreamFlag=false;//虽然有开汽标记但是当前班次还是无法进行开汽
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,shiftEndTime);
                Integer nextClassIndex=cls.getClassIndex()+1;
                LhClassShiftEnum nextCls=LhClassShiftEnum.getClassShiftByClassIndex(nextClassIndex);
                logDetail.append(StringUtils.format("【开班计划安排】,库存为0时。当前班次：【{}】无法开汽，计划安排后，开汽时间：{}",cls.getClassName(),DateUtil.formatDatetime(openStreamTime))).append(division);
                orderNoLog.append(StringUtils.format("【开班计划安排】,库存为0时。当前班次：【{}】无法开汽，计划安排后，开汽时间：{}",cls.getClassName(),DateUtil.formatDatetime(openStreamTime))).append(division);
                if(nextCls!=null){
                  //Joran 2022-08-17 判断开班开汽 start
                    markOpenStreamOrShift(lhEngineScheduleResult,nextCls,embryoCode,lhEndTime,logDetail,orderNoLog);
                  //Joran 2022-08-17 判断开班开汽 end
                }

            }else{
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,enablePlanQty);
                //重新计算结束时间
                brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
                useTotalSecond=LhEngineScheduleUtils.useTotalSecond(lhEngineScheduleResult,enablePlanQty,brushBagTime);
                Date endTime=DateUtils.addSeconds(openStreamTime,useTotalSecond);
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,endTime);
                logDetail.append(StringUtils.format("【开汽计划安排】,库存不足。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                orderNoLog.append(StringUtils.format("【开汽计划安排】,库存不足。当前班次：【{}】，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(endTime))).append(division);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,endTime,cls,logDetail,orderNoLog);
                estimateShiftStock-=enablePlanQty;//班次预计的班次库存清空
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
            }
        }
        //开汽原因标记
        if(openStreamFlag){
           markOpenStreamAnalysis(lhEngineScheduleResult,cls);
        }else{
            LhEngineScheduleUtils.setClassShiftAnalysis(lhEngineScheduleResult,cls,"");
        }
        logDetail.append(StringUtils.format("【开汽计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》结束",embryoCode,cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【开汽计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》结束",embryoCode,cls.getClassName())).append(division);
    }

    /**
     * 存在下个班次的信息需要结合是否换模来判断是否开班开汽
     * @param lhEngineScheduleResult
     * @param nextCls
     * @param embryoCode
     * @param lhEndTime
     * @param logDetail
     * @param orderNoLog
     */
    private void markOpenStreamOrShift(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum nextCls, String embryoCode, Date lhEndTime, StringBuilder logDetail, StringBuilder orderNoLog) {
        Date changeMoldTime =lhEngineScheduleResult.getChangeMoldTime();
        Date classBeginTime=getShiftBeginTimeByClass(nextCls);
        Date classEndTime=getShiftEndTimeByClass(nextCls);
        //来源于换模计划标记
        Boolean changeMoldFlag=lhEngineScheduleResult.getChangeMoldFlag();
        if(changeMoldTime!=null){
            Integer changeMoldHour=getChangeMoldTime(LhEngineParamCodeConstants.CHANGE_MOLD_TIME_HOUR);
            Date afterChangeMoldTime =DateUtils.addHours(changeMoldTime,changeMoldHour);
            //Joran 2022-08-23 先判断换模时间是否在班次开始时间之前 start
            Boolean isBefore=changeMoldTime.getTime()<=classBeginTime.getTime();
            if(!isBefore){
                //不是的话在验证时间是否在班次时间范围内
                changeMoldFlag=afterChangeMoldTime.getTime()>=classBeginTime.getTime()&&afterChangeMoldTime.getTime()<=classEndTime.getTime();
            }
            changeMoldFlag=isBefore||changeMoldFlag;
            //Joran 2022-08-23 先判断换模时间是否在班次开始时间之前 end
        }
        if(!changeMoldFlag){
            markOpenStreamShiftBySingleTask(embryoCode,lhEndTime,nextCls,lhEngineScheduleResult,logDetail,orderNoLog);
        }else{
            markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,changeMoldTime,nextCls,logDetail,orderNoLog);
        }
    }

    /**
     * 标记开汽原因分析
     * @param lhEngineScheduleResult
     * @param cls
     */
    private void markOpenStreamAnalysis(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum cls) {
        LhEngineScheduleUtils.setClassShiftAnalysis(lhEngineScheduleResult,cls,I18nUtil.getMessage("lh.engine.stream.on.analysis.title"));
        LhEngineScheduleUtils.setAnalysisCode(lhEngineScheduleResult,cls,AnalysisCodeEnum.STREAM_ON);
    }

    /**
     * 开班班次计划量安排
     * @param estimateShiftStock
     * @param planQty
     * @param lhEngineScheduleResult
     * @param shiftBeginTime
     * @param shiftEndTime
     * @param logDetail
     */
    private void onOpenShiftClassPlanSet(String embryoCode,LhClassShiftEnum cls,Integer estimateShiftStock, Integer planQty, LhEngineScheduleResult lhEngineScheduleResult, Date shiftBeginTime, Date shiftEndTime, StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【开班计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》开始",embryoCode,cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【开班计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》开始",embryoCode,cls.getClassName())).append(division);
        String lhMachineCode=lhEngineScheduleResult.getLhMachineCode();
        String lhMachineName=lhEngineScheduleResult.getLhMachineName();
        Date lhEndTime=getMachineLhEndTime(lhMachineCode);
        //班次是否标记开班原因
        Boolean shiftOpenFlag=true;
        if(lhEndTime==null){
            lhEndTime=shiftEndTime;
        }

        Integer useMoldNumber=lhEngineScheduleResult.getUseMoldNumber();
        if(useMoldNumber==null){
            useMoldNumber=LhEngineConstants.TWO_MOLD_NUMBER;
        }

        logDetail.append(StringUtils.format("【开班计划安排】,当前胎胚：【{}】，当前班次:【{}】,获取到的硫化结束时间：【{}】",embryoCode,cls.getClassName(),DateUtil.formatDatetime(lhEndTime))).append(division);
        orderNoLog.append(StringUtils.format("【开班计划安排】,当前胎胚：【{}】，当前班次:【{}】,获取到的硫化结束时间：【{}】",embryoCode,cls.getClassName(),DateUtil.formatDatetime(lhEndTime))).append(division);
        if(estimateShiftStock >= planQty){
            logDetail.append(StringUtils.format("【开班计划安排】,班次预计开始库存：【{}】，班次可排计划量:【{}】,库存足够，当前班次：{}，按最大可排计划量进行安排！",estimateShiftStock,planQty,cls.getClassName())).append(division);
            orderNoLog.append(StringUtils.format("【开班计划安排】,班次预计开始库存：【{}】，班次可排计划量:【{}】,库存足够，当前班次：{}，按最大可排计划量进行安排！",estimateShiftStock,planQty,cls.getClassName())).append(division);
            LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
            //设置结束时间为班次截止时间
            LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,shiftEndTime);
            //后续无用
            LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,shiftBeginTime);
            //更新硫化结束时间
            setMachineLhEndTime(lhMachineCode,lhMachineName,shiftEndTime,cls,logDetail,orderNoLog);
            //更新胎胚剩余
            estimateShiftStock-=planQty;
            //扣除后更新班次预计库存
            sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);

        }else{ //小于当前的计划量时，先计算出硫化结束的时间
            //后续无用
            LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,cls,shiftBeginTime);
            //获取到胎胚开始供应时间
            Date embryoStartTime=getEmbryoCodeStartTime(embryoCode,logDetail);
            logDetail.append(StringUtils.format("【开班计划安排】,胎胚开始时间：【{}】，硫化结束时间为班次结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(lhEndTime))).append(division);
            orderNoLog.append(StringUtils.format("【开班计划安排】,胎胚开始时间：【{}】，硫化结束时间为班次结束时间:【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(lhEndTime))).append(division);

            //如果小于3个小时即可以连续。就可以加上当前班次的成型计划
            LhClassShiftEnum embryoStartShift=null;//胎胚开始班次
            Long diffHour= null;
            if(embryoStartTime!=null){
                //获取胎胚班次
                embryoStartShift=timeInClassShift(embryoStartTime);
                diffHour= LhEngineScheduleUtils.diffDate(lhEndTime,embryoStartTime, LhEngineScheduleUtils.HOUR);
                logDetail.append(StringUtils.format("【开班计划安排】,胎胚开始时间和硫化结束时间间隔：{}小时",diffHour)).append(division);
                orderNoLog.append(StringUtils.format("【开班计划安排】,胎胚开始时间和硫化结束时间间隔：{}小时",diffHour)).append(division);
            }
            //更新标记
            String key=GenerageMapKeyUtils.createMapKey(embryoCode,cls.getClassIndex()+"");
            Integer addStockHourCondition=getAddStockHour(LhEngineParamCodeConstants.END_DIFF_START_TIME_HOUR);
            //验证是否更新库存
            Boolean isCheckAddStock=diffHour!=null && diffHour <= addStockHourCondition;
            if((embryoStartShift!=null&&(getShiftEndTimeByClass(embryoStartShift).getTime()<=getShiftEndTimeByClass(cls).getTime())||isCheckAddStock) &&!shiftUpdateCxPlanFlag.containsKey(key)){
                //胎胚可以连续安排，更新当前班次成型计划到预计班次库存中
                logDetail.append(StringUtils.format("【开班更新胎胚班次预计可用库存】,当前胎胚：【{}】，当前班次:【{}】,胎胚开始时间：【{}】,硫化结束时间：【{}】超过{}小时，进行成型该班计划添加到预计班次库存",embryoCode,cls.getClassName(),DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(lhEndTime),addStockHourCondition)).append(division);
                orderNoLog.append(StringUtils.format("【开班更新胎胚班次预计可用库存】,当前胎胚：【{}】，当前班次:【{}】,胎胚开始时间：【{}】,硫化结束时间：【{}】超过{}小时，进行成型该班计划添加到预计班次库存",embryoCode,cls.getClassName(),DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(lhEndTime),addStockHourCondition)).append(division);
                updateEstimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
            }
            //获取最新的预计库存
            estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
            //判断奇偶数
            //Integer enablePlanQty= LhEngineScheduleUtils.getMaxPlanQtyByMoldNumber(estimateShiftStock,useMoldNumber);
            //Joran 2022-07-05 如果库存是奇数就直接按库存排
            Integer enablePlanQty= estimateShiftStock;
            logDetail.append(StringUtils.format("【开班计划安排】,更新当前班次：【{}】成型计划后，班次预计可用库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            orderNoLog.append(StringUtils.format("【开班计划安排】,更新当前班次：【{}】成型计划后，班次预计可用库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            if(estimateShiftStock >= planQty){
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,planQty);
                logDetail.append(StringUtils.format("【开班计划安排】,库存足够。当前班次：【{}】，计划安排后，更新班次预计库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
                orderNoLog.append(StringUtils.format("【开班计划安排】,库存足够。当前班次：【{}】，计划安排后，更新班次预计库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
                //设置结束时间为班次截止时间
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,shiftEndTime);
                //更新硫化结束时间
                setMachineLhEndTime(lhMachineCode,lhMachineName,shiftEndTime,cls,logDetail,orderNoLog);
                //更新胎胚剩余
                estimateShiftStock-=planQty;
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
            }else if(estimateShiftStock.equals(BigDecimal.ZERO.intValue())||( estimateShiftStock.equals(BigDecimal.ONE.intValue()) &&useMoldNumber.equals(LhEngineConstants.TWO_MOLD_NUMBER))){ //班次预计库存为0时，无法进行开班标记处理
                shiftOpenFlag=false;//虽然有打了开班标记但是当前班次还是无法进行开班
                LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,shiftEndTime);
                Integer nextClassIndex=cls.getClassIndex()+1;
                LhClassShiftEnum nextCls=LhClassShiftEnum.getClassShiftByClassIndex(nextClassIndex);
                logDetail.append(StringUtils.format("【开班计划安排】,库存为0时。当前班次：【{}】无法开班，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(shiftEndTime))).append(division);
                orderNoLog.append(StringUtils.format("【开班计划安排】,库存为0时。当前班次：【{}】无法开班，计划安排后，更新班次预计库存：{}，硫化预计结束时间：{}",cls.getClassName(),estimateShiftStock,DateUtil.formatDatetime(shiftEndTime))).append(division);

                if(nextCls!=null){
                   /* Boolean changeMoldFlag=LhEngineScheduleUtils.getClassShiftChangeMoldFlag(lhEngineScheduleResult,nextCls);
                    if(!changeMoldFlag){
                        markOpenStreamShiftBySingleTask(embryoCode,shiftBeginTime,nextCls,lhEngineScheduleResult,logDetail,orderNoLog);
                    }else{
                        Date changeMoldTime=lhEngineScheduleResult.getChangeMoldTime();
                        markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,changeMoldTime,nextCls,logDetail,orderNoLog);
                    }*/
                   //Joran 2022-08-17 判断开班开汽逻辑
                   markOpenStreamOrShift(lhEngineScheduleResult,nextCls,embryoCode,shiftBeginTime,logDetail,orderNoLog);
                }

            }else{
                LhEngineScheduleUtils.setClassShiftPlanQty(lhEngineScheduleResult,cls,enablePlanQty);
                estimateShiftStock-=enablePlanQty;//班次预计的班次库存清空
                //扣除后更新班次预计库存
                sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
                logDetail.append(StringUtils.format("【开班计划安排】,库存不足。当前班次：【{}】，计划安排后，更新班次预计库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
                orderNoLog.append(StringUtils.format("【开班计划安排】,库存不足。当前班次：【{}】，计划安排后，更新班次预计库存：{}",cls.getClassName(),estimateShiftStock)).append(division);
            }
        }
        //开班原因标记
        if(shiftOpenFlag){
            LhEngineScheduleUtils.setClassShiftAnalysis(lhEngineScheduleResult,cls,I18nUtil.getMessage("lh.engine.open.analysis.title"));
            LhEngineScheduleUtils.setAnalysisCode(lhEngineScheduleResult,cls,AnalysisCodeEnum.OPEN_SHIFT);
        }else{
            LhEngineScheduleUtils.setClassShiftAnalysis(lhEngineScheduleResult,cls,"");
        }
        logDetail.append(StringUtils.format("【开班计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》结束",embryoCode,cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【开班计划安排】,当前胎胚：【{}】，当前班次:【{}】,班次计划安排--》结束",embryoCode,cls.getClassName())).append(division);
    }

    /**
     * 更新硫化机台
     * @param lhMachineCode
     * @param shiftEndTime
     * @param cls
     * @param logDetail
     * @param orderNoLog
     */
    private void setMachineLhEndTime(String lhMachineCode,String lhMachineName, Date shiftEndTime, LhClassShiftEnum cls, StringBuilder logDetail, StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【计划安排更新硫化结束时间】,当前机台：{}，当前班次：{}，更新硫化结束时间：{}",lhMachineName,cls.getClassName(),DateUtil.formatDatetime(shiftEndTime))).append(division);
        orderNoLog.append(StringUtils.format("【计划安排更新硫化结束时间】,当前机台：{}，当前班次：{}，更新硫化结束时间：{}",lhMachineName,cls.getClassName(),DateUtil.formatDatetime(shiftEndTime))).append(division);
        machineLhEndTime.put(lhMachineCode,shiftEndTime);
    }

    /**
     * 获取硫化机台的硫化结束时间
     * @param lhMachineCode
     * @return
     */
    private Date getMachineLhEndTime(String lhMachineCode){
        Date lhEndTime=null;
        if(StringUtils.isNotEmpty(machineLhEndTime)&&machineLhEndTime.containsKey(lhMachineCode)){
            lhEndTime=machineLhEndTime.get(lhMachineCode);
        }
        return lhEndTime;
    }

    /**
     *  对同胎胚的任务进行排序，开班>开汽>正常>换模
     * @param lhEngineScheduleResultList
     * @param cls
     * @param logDetail
     */
    private void sortByScheduleList(String embryoCode,List<LhEngineScheduleResult> lhEngineScheduleResultList, LhClassShiftEnum cls, StringBuilder logDetail) {
        logDetail.append(StringUtils.format("【同胎胚任务排序】,当前胎胚代码：【{}】，当前班次：【{}】，任务列表：【{}】",embryoCode,cls.getClassName(),toJSONString(lhEngineScheduleResultList))).append(division);
        Set<String> orderSapCodeSet=new HashSet<>();
        //1.先处理获取开班的记录
        for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
            String lhMachineName=lhEngineScheduleResult.getLhMachineName();
            String sapCode=lhEngineScheduleResult.getSapCode();
            Boolean isOpenShift=LhEngineScheduleUtils.getClassShiftOpenShiftFlag(lhEngineScheduleResult,cls);
            logDetail.append(StringUtils.format("【开班排序】,当前机台：【{}】，当前班次：【{}】，是否开班：【{}】",lhMachineName,cls.getClassName(),isOpenShift)).append(division);
            if(isOpenShift&&!orderSapCodeSet.contains(sapCode)){
                lhEngineScheduleResult.setPlanSort(1);//开班标记
                orderSapCodeSet.add(sapCode);
            }
        }

        //2.处理获取开汽的记录
        for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
            String lhMachineName=lhEngineScheduleResult.getLhMachineName();
            String sapCode=lhEngineScheduleResult.getSapCode();
            Boolean isOpenStream=LhEngineScheduleUtils.getClassShiftOpenStreamFlag(lhEngineScheduleResult,cls);
            logDetail.append(StringUtils.format("【开汽排序】,当前机台：【{}】，当前班次：【{}】，是否开汽：【{}】",lhMachineName,cls.getClassName(),isOpenStream)).append(division);
            if(isOpenStream&&!orderSapCodeSet.contains(sapCode)){
                lhEngineScheduleResult.setPlanSort(2);//开汽标记
                orderSapCodeSet.add(sapCode);
            }
        }

        //3.无换模
        for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
            String lhMachineName=lhEngineScheduleResult.getLhMachineName();
            String sapCode=lhEngineScheduleResult.getSapCode();
            Boolean isChangeMoldFlag=isChangeMoldTask(lhEngineScheduleResult,cls);
            logDetail.append(StringUtils.format("【换模】,当前机台：【{}】，当前SAP品号，当前班次：【{}】，是否换模：【{}】",lhMachineName,cls.getClassName(),isChangeMoldFlag)).append(division);
            if(isChangeMoldFlag&&!orderSapCodeSet.contains(sapCode)){
                lhEngineScheduleResult.setPlanSort(4);//换模
                orderSapCodeSet.add(sapCode);
            }else if(!isChangeMoldFlag&&!orderSapCodeSet.contains(sapCode)){
                lhEngineScheduleResult.setPlanSort(3);//正常
                orderSapCodeSet.add(sapCode);
            }
        }
        //进行排序
        lhEngineScheduleResultList.sort(Comparator.comparing(LhEngineScheduleResult::getPlanSort,Comparator.nullsLast(Integer::compareTo)));
        logDetail.append(StringUtils.format("【同胎胚任务排序】,当前胎胚代码：【{}】，当前班次：【{}】，排序后任务列表：【{}】",embryoCode,cls.getClassName(),toJSONString(lhEngineScheduleResultList))).append(division);


    }

    /**
     * 判断是否换模
     * @param lhEngineScheduleResult
     * @param cls
     * @return
     */
    private Boolean isChangeMoldTask(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum cls) {
        Date changeMoldTime=lhEngineScheduleResult.getChangeMoldTime();
        Boolean changeMoldFlag=lhEngineScheduleResult.getChangeMoldFlag();
        if(!changeMoldFlag){
            return false;
        }
        //有换模时间是在后规格，换模班次要换模时间之前6个小时的班次
        Integer changeMoldHour=getChangeMoldTime(LhEngineParamCodeConstants.CHANGE_MOLD_TIME_HOUR);
        Date beforeChangeMoldTime=DateUtils.addHours(changeMoldTime,changeMoldHour * -1);

        //获取换模班次
        LhClassShiftEnum changeMoldCls=timeInClassShift(beforeChangeMoldTime);
        if(changeMoldCls==null){
            return false;
        }else if(!changeMoldCls.equals(cls)){
            return false;
        }
        return true;
    }


    /**
     * 处理同硫化机的任务多个任务，存在更换模具的情况，进行任务可用时间记录
     * @param lhEngineScheduleResultList
     */
    private void calcLhMachineEnableShiftTime(List<LhEngineScheduleResult> lhEngineScheduleResultList,StringBuilder logDetail) {
        logDetail.append("【机台可用时间】，开始处理可排任务，同硫化机,规格可用时间处理").append(division);
        if(StringUtils.isNotEmpty(lhEngineScheduleResultList)){
            //按硫化机进行分组
            Map<String,List<LhEngineScheduleResult>> machineCodeTaskMap=lhEngineScheduleResultList.stream().collect(Collectors.groupingBy(LhEngineScheduleResult::getLhMachineCode));
            if(StringUtils.isNotEmpty(machineCodeTaskMap)){
                for(Map.Entry<String,List<LhEngineScheduleResult>> entry:machineCodeTaskMap.entrySet()){
                    String lhMachineCode=entry.getKey();
                    List<LhEngineScheduleResult> machineTaskList=entry.getValue();
                    //同个硫化机多个任务必然存在换模情况
                    if(StringUtils.isNotEmpty(machineTaskList)&&machineTaskList.size()>1){
                        //按照机台的换模时间进行升序，没有换模时间的是前规格
                        machineTaskList.sort(Comparator.comparing(LhEngineScheduleResult::getChangeMoldTime,Comparator.nullsFirst(Date::compareTo)));
                        //前规格没有换模时间的最多为两条，如果为两条的话判断
                        List<LhEngineScheduleResult> beforeTaskList=new ArrayList<>();
                        //Set<String> sapCodeSet=new HashSet<>();
                        //后规格列表
                        List<LhEngineScheduleResult> afterTaskList= new ArrayList<>();
                        //换模不执行列表
                        List<LhEngineScheduleResult> noChangeTaskList= new ArrayList<>();
                        for(LhEngineScheduleResult lhEngineScheduleResult:machineTaskList){
                            Date changeMoldTime=lhEngineScheduleResult.getChangeMoldTime();
                            String sapCode=lhEngineScheduleResult.getSapCode();
                            Boolean changeMoldFlag=lhEngineScheduleResult.getChangeMoldFlag();
                            if(changeMoldTime==null/*&&!sapCodeSet.contains(sapCode)*/){
                                beforeTaskList.add(lhEngineScheduleResult);
                                //sapCodeSet.add(sapCode);
                            }
                            if(changeMoldFlag/*&&!sapCodeSet.contains(sapCode)*/){
                                afterTaskList.add(lhEngineScheduleResult);
                                //sapCodeSet.add(sapCode);
                            }else if(!changeMoldFlag&&changeMoldTime!=null/*&&!sapCodeSet.contains(sapCode)*/){
                                noChangeTaskList.add(lhEngineScheduleResult);
                                //sapCodeSet.add(sapCode);
                            }
                        }
                        /**
                         * Joran 2022-06-22 处理左右模信息L/R多次换模情况 start
                         */
                        //前规格有两个，那么必然是L/R
                        Boolean beforeHasLeftRightMold=false;
                        if(StringUtils.isNotEmpty(beforeTaskList)&&beforeTaskList.size()>1){
                            beforeHasLeftRightMold=true;
                        }
                        //后规格按照换模时间进行升序
                        Map<Date, List<LhEngineScheduleResult>> afterTaskChangeMoldTimeMap = afterTaskList.stream()
                                .sorted(Comparator.comparing(LhEngineScheduleResult::getChangeMoldTime))
                                .collect(Collectors.groupingBy(LhEngineScheduleResult::getChangeMoldTime, LinkedHashMap::new,Collectors.toList()));
                        buildLeftRightMoldIndex(beforeHasLeftRightMold,afterTaskChangeMoldTimeMap);
                        /**
                         * Joran 2022-06-22 处理左右模信息L/R多次换模情况 end
                         */


                        //根据换模时间进行分组,根据换模时间降序，过滤掉前规格数据
                        Map<Date, List<LhEngineScheduleResult>> changeMoldTimeMap = afterTaskList.stream()
                                .filter(item -> item.getChangeMoldFlag())
                                .sorted(Comparator.comparing(LhEngineScheduleResult::getChangeMoldTime).reversed())
                                .collect(Collectors.groupingBy(LhEngineScheduleResult::getChangeMoldTime, LinkedHashMap::new,Collectors.toList()));

                        //从换模时间大的开始进行逆推前规格可用的时间
                        Date lastEndTime=getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT); //获取到最后的换膜时间
                        //记录后规格的换膜时间
                        Date lastChangeMoldTime=null;
                        Date changeMoldTime=null;//取到最小的换膜时间为前规格的结束时间
                        if(StringUtils.isNotEmpty(changeMoldTimeMap)){
                            for(Map.Entry<Date, List<LhEngineScheduleResult>> changeMoldEntry:changeMoldTimeMap.entrySet()){
                                changeMoldTime=changeMoldEntry.getKey();
                                List<LhEngineScheduleResult> sameChangeTaskList=changeMoldEntry.getValue();
                                Integer changeMoldHour=getChangeMoldTime(LhEngineParamCodeConstants.CHANGE_MOLD_TIME_HOUR);
                                Date taskBeginTime=DateUtils.addHours(changeMoldTime,changeMoldHour);
                                for(LhEngineScheduleResult lhEngineScheduleResult:sameChangeTaskList){
                                    //前一个较大的换膜时间为当前的换膜时间start
                                     markChangeMoldByLastChangeMoldTime(lastChangeMoldTime,lhEngineScheduleResult,logDetail);
                                    //前一个较大的换膜时间为当前的换膜时间end
                                    lhEngineScheduleResult.setEnableEndTime(lastEndTime);
                                    lhEngineScheduleResult.setEnableStartTime(taskBeginTime);
                                }
                                lastEndTime=changeMoldTime;//将换模时间做为上一个规格的可用时间
                                //记录上一个换模时间
                                lastChangeMoldTime=changeMoldTime;
                            }
                        }

                        //将最后的的换膜时间作为当前前规格的开始时间
                        Date shiftBeginTime=getShiftBeginTimeByClass(LhClassShiftEnum.ONE_CLASS_SHIFT); //获取到班次开始时间
                        if(StringUtils.isNotEmpty(beforeTaskList)){
                            Date enableEndTime=getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT);
                            if(changeMoldTime!=null){
                                enableEndTime=changeMoldTime;
                            }
                            for (LhEngineScheduleResult beforeTask:beforeTaskList){
                                beforeTask.setEnableStartTime(shiftBeginTime);
                                beforeTask.setEnableEndTime(enableEndTime);
                                //对前规格标注换模原因
                                markChangeMoldByLastChangeMoldTime(lastChangeMoldTime,beforeTask,logDetail);
                            }
                        }

                        //对不换的规格时间进行班次计划时间设置
                        if(StringUtils.isNotEmpty(noChangeTaskList)){
                            Date enableEndTime=getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT);
                            for (LhEngineScheduleResult noChangeTask:noChangeTaskList){
                                noChangeTask.setEnableStartTime(shiftBeginTime);
                                noChangeTask.setEnableEndTime(enableEndTime);
                            }
                        }

                        //Joran 2022-06-30  时间为空防错处理 start
                        Date enableEndTime=getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT);
                        for(LhEngineScheduleResult noTimeTask:machineTaskList){
                           if(noTimeTask.getEnableStartTime()==null){
                               noTimeTask.setEnableStartTime(shiftBeginTime);
                           }
                            if(noTimeTask.getEnableEndTime()==null){
                                noTimeTask.setEnableEndTime(enableEndTime);
                            }
                        }
                        //Joran 2022-06-30  时间为空防错处理 end

                    }else if(StringUtils.isNotEmpty(machineTaskList)&&machineTaskList.size()==1){ //无换模
                        logDetail.append(StringUtils.format("【机台可用时间】当前硫化机：{}，只有单个任务意味着没有换模，可用时间为都可用",lhMachineCode)).append(division);
                        LhEngineScheduleResult lhEngineScheduleResult=machineTaskList.get(0);
                        lhEngineScheduleResult.setEnableStartTime(getShiftBeginTimeByClass(LhClassShiftEnum.ONE_CLASS_SHIFT));
                        lhEngineScheduleResult.setEnableEndTime(getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT));
                    }
                }
            }
        }
    }

    /**
     * 换模原因标记在前规格上
     * @param lastChangeMoldTime
     * @param lhEngineScheduleResult
     * @param logDetail
     */
    private void markChangeMoldByLastChangeMoldTime(Date lastChangeMoldTime, LhEngineScheduleResult lhEngineScheduleResult, StringBuilder logDetail) {
        String orderNo=lhEngineScheduleResult.getOrderNo();
        //获取工单日志构造器
        StringBuilder orderNoLog=getOrderLogBuilder(orderNo);
        if(lastChangeMoldTime!=null){
            logDetail.append(StringUtils.format("【前规格换模原因标注】，后规格换模时间为：【{}】,进行前规格换模原因标注！",DateUtil.formatDatetime(lastChangeMoldTime))).append(division);
            orderNoLog.append(StringUtils.format("【前规格换模原因标注】，后规格换模时间为：【{}】,进行前规格换模原因标注！",DateUtil.formatDatetime(lastChangeMoldTime))).append(division);
            LhClassShiftEnum beforeSpecChangeShift=timeInClassShift(lastChangeMoldTime);
            String shiftClassName=beforeSpecChangeShift==null?"昨日白班":beforeSpecChangeShift.getClassName();
            if(beforeSpecChangeShift!=null){
                markChangeMoldAnalysis(lhEngineScheduleResult,beforeSpecChangeShift);
            }
            logDetail.append(StringUtils.format("【前规格换模原因标注】,前规格换模所在班次：【{}】",shiftClassName)).append(division);
            orderNoLog.append(StringUtils.format("【前规格换模原因标注】,前规格换模所在班次：【{}】",shiftClassName)).append(division);
        }

    }

    /**
     * 处理左右模
     * @param beforeHasLeftRightMold
     * @param afterTaskChangeMoldTimeMap
     */
    private void buildLeftRightMoldIndex(Boolean beforeHasLeftRightMold, Map<Date, List<LhEngineScheduleResult>> afterTaskChangeMoldTimeMap) {
        if(StringUtils.isNotEmpty(afterTaskChangeMoldTimeMap)){
            Integer index=null;
            for(Map.Entry<Date, List<LhEngineScheduleResult>> changeMoldEntry:afterTaskChangeMoldTimeMap.entrySet()){
                List<LhEngineScheduleResult> sameChangeTaskList=changeMoldEntry.getValue();
                if(beforeHasLeftRightMold){
                    index=1;
                }
                if(StringUtils.isNotEmpty(sameChangeTaskList)&&sameChangeTaskList.size()>1){
                    for(LhEngineScheduleResult lhEngineScheduleResult:sameChangeTaskList){
                        String leftRightMold=lhEngineScheduleResult.getLeftRightMold();
                        String moldIndex=index==null?"":index+"";
                        leftRightMold+=moldIndex;
                        lhEngineScheduleResult.setLeftRightMold(leftRightMold);
                    }
                    //如果没有的话就初始化为1，如果有就继续往后加1
                    index=index==null?1:index+1;
                }
            }
        }
    }

    /**
     * 组装同机台，同sap不同胎胚的数据集合
     * @param lhEngineScheduleResultList
     * @param sameSapDiffEmbryoList
     */
    private void removeSameMachineSapDiffEmbryoList(List<LhEngineScheduleResult> lhEngineScheduleResultList, List<LhEngineScheduleResult> sameSapDiffEmbryoList,StringBuilder logDetail) {
        logDetail.append("【移除同机台，同SAP，不同胎胚数据】,不进行自动排程").append(division);
        if(StringUtils.isNotEmpty(lhEngineScheduleResultList)){
            List<LhEngineScheduleResult> emptyEmbryoCodeList=new ArrayList<>();
            //获取当天计划班次最大的时间
            Date latestMoldTime=getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT);
            Integer changeMoldHour=getChangeMoldTime(LhEngineParamCodeConstants.CHANGE_MOLD_TIME_HOUR);
            for(LhEngineScheduleResult scheduleResult:lhEngineScheduleResultList){
                String machineCode=scheduleResult.getLhMachineCode();
                String sapCode=scheduleResult.getSapCode();
                String embryoCode=scheduleResult.getEmbryoCode();
                String orderNo=scheduleResult.getOrderNo();
                StringBuilder orderNoLog=getOrderLogBuilder(orderNo);
                if(StringUtils.isEmpty(embryoCode)){
                    logDetail.append(StringUtils.format("硫化机台编号:{},SAP品号:{},胎胚代码为空，数据移除，不做自动排程！",machineCode,sapCode)).append(division);
                    orderNoLog.append(StringUtils.format("硫化机台编号:{},SAP品号:{},胎胚代码为空，数据移除，不做自动排程！",machineCode,sapCode)).append(division);
                    emptyEmbryoCodeList.add(scheduleResult);
                }

                Date changeMoldTime=scheduleResult.getChangeMoldTime();
                if(changeMoldTime!=null){
                    Date afterChangeTime=DateUtils.addHours(changeMoldTime,changeMoldHour);
                    if(afterChangeTime.getTime() >= latestMoldTime.getTime()){
                        logDetail.append(StringUtils.format("硫化机台编号:{},SAP品号:{},前规格换模后时间：{}，超过计划班次范围，后规格无法开班！数据移除，不做自动排程！",machineCode,sapCode,DateUtil.formatDatetime(afterChangeTime))).append(division);
                        orderNoLog.append(StringUtils.format("硫化机台编号:{},SAP品号:{},前规格换模后时间：{}，超过计划班次范围，后规格无法开班！数据移除，不做自动排程！",machineCode,sapCode,DateUtil.formatDatetime(afterChangeTime))).append(division);
                        scheduleResult.setClass1Analysis("");
                        scheduleResult.setClass2Analysis("");
                        scheduleResult.setClass3Analysis("");
                    }

                }

            }
            //移除空胎胚代码
            if(StringUtils.isNotEmpty(emptyEmbryoCodeList)){
                lhEngineScheduleResultList.removeAll(emptyEmbryoCodeList);
            }

            //移除完空胎胚代码后，剩余的再来筛选同SAP多个胎胚的数据
            if (StringUtils.isNotEmpty(lhEngineScheduleResultList)){
                Map<String,List<LhEngineScheduleResult>> sameMachineSapTaskListMap=lhEngineScheduleResultList.stream().collect(Collectors.groupingBy(result -> GenerageMapKeyUtils.createMapKey(result.getLhMachineCode(),result.getSapCode())));
                if(StringUtils.isNotEmpty(sameMachineSapTaskListMap)){
                    for(Map.Entry<String,List<LhEngineScheduleResult>> entry:sameMachineSapTaskListMap.entrySet()){
                        String key =entry.getKey();
                        List<LhEngineScheduleResult> sameSapList=entry.getValue();
                        if(StringUtils.isNotEmpty(sameSapList)&&sameSapList.size()>1){
                            //遍历如果相同SAP品号是不同胎胚的就移除标记，否则不进行移除
                            Boolean isRemove=true;
                            String lastEmbryoCode="";
                            for(LhEngineScheduleResult lhEngineScheduleResult:sameSapList){
                                //当前胎胚
                                String embryoCode=lhEngineScheduleResult.getEmbryoCode();
                                if(StringUtils.isEmpty(lastEmbryoCode)){
                                    lastEmbryoCode=embryoCode;
                                    continue;
                                }else if(embryoCode.equals(lastEmbryoCode)){
                                    isRemove=false;
                                    break;
                                }
                            }
                            logDetail.append(StringUtils.format("存在同机台，同sap,不同胎胚的数据，进行数据移除，移除键值：{}",key)).append(division);
                            if(isRemove){
                                sameSapDiffEmbryoList.addAll(sameSapList);
                            }

                        }
                    }
                }
            }

            //将相同sap不同胎胚的进行移除
            if(StringUtils.isNotEmpty(sameSapDiffEmbryoList)){
                lhEngineScheduleResultList.removeAll(sameSapDiffEmbryoList);
            }

            //将空胎胚的数据也缓存起来
            if(StringUtils.isNotEmpty(emptyEmbryoCodeList)){
                sameSapDiffEmbryoList.addAll(emptyEmbryoCodeList);
            }
        }
    }

    /**
     * 遍历所有初始化的规格后进行开班、开汽、停汽标记
     * @param lhEngineScheduleResultList
     * @param autoScheduleDate
     * @param logDetail
     */
    private void markTagBySchedule(List<LhEngineScheduleResult> lhEngineScheduleResultList, Date autoScheduleDate, StringBuilder logDetail) {
      logDetail.append("初始化完中班规格以及胎胚信息填充完毕后，开始进行迭代处理开汽、开班、停汽、标记和班次上限处理").append(division);
      for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
          if(!lhEngineScheduleResult.getLastDayChangeMoldFlag()&&!lhEngineScheduleResult.getToDayChangeMoldFlag()){
              logDetail.append("进行当前在产规格开班、开汽、停汽逻辑处理》》》").append(division);
              //单条在产规格标记处理
              singleTaskByInProductionSpec(lhEngineScheduleResult,logDetail);
          }else{//换模计划初始化的数据处理
              //存在换膜计划，开班、停汽标记处理
              singleTaskByChangeMold(lhEngineScheduleResult,logDetail);
          }
      }
    }

    /**
     * 昨日换模计划开班、停汽标记处理
     * @param lhEngineScheduleResult
     * @param logDetail
     */
    private void singleTaskByChangeMold(LhEngineScheduleResult lhEngineScheduleResult, StringBuilder logDetail) {
        //工单号
        String orderNo=lhEngineScheduleResult.getOrderNo();
        //获取工单日志构建器
        StringBuilder orderNoLog=getOrderLogBuilder(orderNo);
        logDetail.append(StringUtils.format("【昨日换模标记】开始进行，昨日存在换膜计划的规格，开班、停汽标记处理")).append(division);
        orderNoLog.append(StringUtils.format("【昨日换模标记】开始进行，昨日存在换膜计划的规格，开班、停汽标记处理")).append(division);

        //获取对应的换膜时间
        Date changeMoldTime=lhEngineScheduleResult.getChangeMoldTime();
        logDetail.append(StringUtils.format("【昨日换模标记】当前规格的换膜时间：{}",DateUtil.formatDatetime(changeMoldTime))).append(division);
        orderNoLog.append(StringUtils.format("【昨日换模标记】当前规格的换膜时间：{}",DateUtil.formatDatetime(changeMoldTime))).append(division);
        //换模所在的班次
        LhClassShiftEnum changeMoldShift=timeInClassShift(changeMoldTime);
        if(changeMoldShift!=null){
            markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,changeMoldTime,changeMoldShift,logDetail,orderNoLog);
        }else{
            //需要判断当前机台在中班之前是否有进行开班，如果有开班的话，就不再进行开班操作
            String lhMachineCode=lhEngineScheduleResult.getLhMachineCode();
            String embryoCode=lhEngineScheduleResult.getEmbryoCode();
            Date scheduleDate=lhEngineScheduleResult.getScheduleDate();
            LhEngineScheduleResult machineLastMaxSpec=getLastMachineScheduleTask(scheduleDate,lhMachineCode,embryoCode,logDetail,orderNoLog);
            //当前机台编码
            if(!changeMoldCheck(lhEngineScheduleResult,machineLastMaxSpec,logDetail,orderNoLog)){
                markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,changeMoldTime,LhClassShiftEnum.ONE_CLASS_SHIFT,logDetail,orderNoLog);
            }else{
                if(machineLastMaxSpec!=null){
                    Date maxLhEndTime=getMaxLhEndTime(machineLastMaxSpec,scheduleDate,logDetail,orderNoLog);
                    //更新机台规格硫化结束时间
                    setMachineLhEndTime(lhMachineCode,lhEngineScheduleResult.getLhMachineName(),maxLhEndTime,LhClassShiftEnum.ONE_CLASS_SHIFT,logDetail,orderNoLog);
                    for(LhClassShiftEnum lhClassShiftEnums:LhClassShiftEnum.values()){
                        //预估班次库存
                        Integer estimateShiftStock=estimateLhClassEmbryoStock(embryoCode,lhClassShiftEnums,logDetail,orderNoLog);
                        //如果有胎胚的话就开汽时间
                        Date embryoStartTime=getEmbryoCodeStartTimeByClassShift(embryoCode,estimateShiftStock,lhClassShiftEnums,logDetail);
                        if(embryoStartTime==null){
                            logDetail.append(StringUtils.format("【昨日换模标记】昨日开班，今日开汽逻辑判断，硫化结束时间：{}",DateUtil.formatDatetime(maxLhEndTime))).append(division);
                            orderNoLog.append(StringUtils.format("【昨日换模标记】昨日开班，今日开汽逻辑判断，硫化结束时间：{}",DateUtil.formatDatetime(maxLhEndTime))).append(division);
                            LhClassShiftEnum nextCls=LhClassShiftEnum.getClassShiftByClassIndex(lhClassShiftEnums.getClassIndex()+1);
                            if(nextCls!=null){
                                lhEngineScheduleResult.setEnableStartTime(getShiftBeginTimeByClass(nextCls));
                                lhEngineScheduleResult.setEnableEndTime(getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT));
                            }else{
                                lhEngineScheduleResult.setEnableStartTime(getShiftBeginTimeByClass(lhClassShiftEnums));
                                lhEngineScheduleResult.setEnableEndTime(getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT));
                            }
                            continue;
                        }
                        markOpenStreamShiftBySingleTask(machineLastMaxSpec.getEmbryoCode(),maxLhEndTime,lhClassShiftEnums,lhEngineScheduleResult,logDetail,orderNoLog);
                        break;
                    }

                }

            }
        }
        /*if(changeMoldShift!=null){ //数据异常超过当天计划的班次，就跳过不处理了
            logDetail.append(StringUtils.format("【昨日换模标记】当前规格的换膜后的时间：{},换模时间所在的班次：{}",DateUtil.formatDatetime(changeMoldTime),changeMoldShift.getClassName())).append(division);
            orderNoLog.append(StringUtils.format("【昨日换模标记】当前规格的换膜后的时间：{},换模时间所在的班次：{}",DateUtil.formatDatetime(changeMoldTime),changeMoldShift.getClassName())).append(division);
            markOpenShiftByLastDayChangeMold(lhEngineScheduleResult,changeMoldTime,changeMoldShift,logDetail,orderNoLog);
        }else{
            logDetail.append(StringUtils.format("【昨日换模标记】当前规格的换膜时间：{},没匹配到对应的班次，不进行处理",DateUtil.formatDatetime(changeMoldTime))).append(division);
            orderNoLog.append(StringUtils.format("【昨日换模标记】当前规格的换膜时间：{},没匹配到对应的班次，不进行处理",DateUtil.formatDatetime(changeMoldTime))).append(division);

        }*/

    }

    /**
     * 集合机台在产规格校验如果，如果机台在产规格与当前计划规格一致，则表示已经更换完模具，不再开班
     * @param lhEngineScheduleResult
     * @return 返回true表示已经开班，否则就是需要判断开班
     */
    private boolean changeMoldCheck(LhEngineScheduleResult lhEngineScheduleResult,LhEngineScheduleResult machineLastMaxSpec, StringBuilder logDetail,StringBuilder orderNoLog) {
        Boolean changeMoldFlag=false;
        String sapCode=lhEngineScheduleResult.getSapCode();
        if(machineLastMaxSpec!=null){
            String lastProductSapCode=machineLastMaxSpec.getSapCode();
            if(lastProductSapCode.equals(sapCode)){
                changeMoldFlag=true;
            }
        }
        logDetail.append(StringUtils.format("【昨日换模标记】判断是否已经换模开班:{}",changeMoldFlag)).append(division);
        orderNoLog.append(StringUtils.format("【昨日换模标记】判断是否已经换模开班:{}",changeMoldFlag)).append(division);

        return changeMoldFlag;
    }

    /**
     * 换模开班标记处理
     * @param lhEngineScheduleResult
     * @param changeMoldTime
     * @param cls
     * @param logDetail
     */
    private void markOpenShiftByLastDayChangeMold(LhEngineScheduleResult lhEngineScheduleResult, Date changeMoldTime, LhClassShiftEnum cls, StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【换模开班】换模时间所在的班次：【{}】",cls==null?"昨天白班":cls.getClassName())).append(division);
        orderNoLog.append(StringUtils.format("【换模开班】换模时间所在的班次：【{}】",cls==null?"昨天白班":cls.getClassName())).append(division);
        Integer changeMoldTimeHour=getChangeMoldTime(LhEngineParamCodeConstants.CHANGE_MOLD_TIME_HOUR);
        logDetail.append(StringUtils.format("【换模开班】获取到更换模具时长：【{}】小时",changeMoldTimeHour)).append(division);
        orderNoLog.append(StringUtils.format("【换模开班】获取到更换模具时长：【{}】小时",changeMoldTimeHour)).append(division);
        //获取换模时间后所在的班次
        Date afterChangeMoldTime=DateUtils.addHours(changeMoldTime,changeMoldTimeHour);//换膜后的时间
        logDetail.append(StringUtils.format("【换模开班】更换模具后的时间：【{}】",DateUtil.formatDatetime(afterChangeMoldTime))).append(division);
        orderNoLog.append(StringUtils.format("【换模开班】更换模具后的时间：【{}】",DateUtil.formatDatetime(afterChangeMoldTime))).append(division);
        //判断加上6个小时后，班次存不存在，只能是昨日白班开班或者计划班次之后开班
        LhClassShiftEnum afterChangeCls=timeInClassShift(afterChangeMoldTime);
        String embryoCode=lhEngineScheduleResult.getEmbryoCode();
        //前一个班次如果存在先进行前一个班次的计划任务添加
       // LhClassShiftEnum beforeCls=LhClassShiftEnum.getClassShiftByClassIndex(cls.getClassIndex()-1);
        Integer estimateShiftStock=0;
        if(cls!=null){
            //updateEstimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
            estimateShiftStock=estimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
        }

        if(afterChangeCls==null){
            //获取中班的开始时间
            Date classOneShiftBeginTime=getShiftBeginTimeByClass(LhClassShiftEnum.ONE_CLASS_SHIFT);
            //昨日白班已换模，判断是否需要进行中班开班
            if(afterChangeMoldTime.getTime()< classOneShiftBeginTime.getTime()){
                //在昨日白班进行换模，中班需要进行开班开汽标记校验
                openShiftOperation(lhEngineScheduleResult,cls,logDetail,orderNoLog);
                return;
            }

            //获取中班的开始时间
            Date classThreeShiftEndTime=getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT);
            if(afterChangeMoldTime.getTime()> classThreeShiftEndTime.getTime()){
                //换模时间后时间超过三班，需要判断中班是否可以开班开汽
                openShiftOperation(lhEngineScheduleResult,cls,logDetail,orderNoLog);
                return;
            }

        }
        //获取班次的开始时间,如果班次为空则默认表示是白班，白班开始时间
        Date shiftBeginTime=getShiftBeginTimeByClass(cls);
        //判断下个班次可不可以开班条件
        Date nextShiftOpenShiftTimeCondition=DateUtils.addHours(shiftBeginTime,changeMoldTimeHour);
        //判断下个班次可安排的计划量
        if(afterChangeMoldTime.getTime() > nextShiftOpenShiftTimeCondition.getTime()){
            logDetail.append(StringUtils.format("【换模开班】换模后的时间：【{}】,大于限定时间：【{}】,本班次无法进行开班标记,判断下个班次是否可以开班", DateUtil.formatDatetime(afterChangeMoldTime),DateUtil.formatDatetime(nextShiftOpenShiftTimeCondition))).append(division);
            orderNoLog.append(StringUtils.format("【换模开班】换模后的时间：【{}】,大于限定时间：【{}】,本班次无法进行开班,判断下个班次是否可以开班", DateUtil.formatDatetime(afterChangeMoldTime),DateUtil.formatDatetime(nextShiftOpenShiftTimeCondition))).append(division);

            LhClassShiftEnum nextCls=null;
            if(afterChangeCls.equals(cls)){

                Integer nextClsIndex=afterChangeCls.getClassIndex() + 1;
                nextCls=LhClassShiftEnum.getClassShiftByClassIndex(nextClsIndex);
                if(nextCls==null){
                    logDetail.append(StringUtils.format("【换模开班】需要标记开班的下个班次不在当天计划班次内，所以不标记了！")).append(division);
                    orderNoLog.append(StringUtils.format("【换模开班】需要标记开班的下个班次不在当天计划班次内，所以不标记了！")).append(division);
                    return ;
                }
                //更新班次时间不可用
                lhEngineScheduleResult.setEnableStartTime(getShiftBeginTimeByClass(nextCls));
                lhEngineScheduleResult.setEnableEndTime(getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT));
                logDetail.append(StringUtils.format("【换模开班】获取到下一个开班班次：【{}】",nextCls.getClassName())).append(division);
                orderNoLog.append(StringUtils.format("【换模开班】获取到下一个开班班次：【{}】", nextCls.getClassName())).append(division);
            }else{
                nextCls=afterChangeCls;
            }

            //本班次无法开班，下个班次进行开班标记
            openShiftOperation(lhEngineScheduleResult,nextCls,logDetail,orderNoLog);

        }else{//在限定时间内，判断胎胚时间,如果复合本班开班
            logDetail.append(StringUtils.format("【换模开班】换模后的时间：【{}】,小于限定时间：【{}】,本班次可以进行开班标记判断", DateUtil.formatDatetime(afterChangeMoldTime),DateUtil.formatDatetime(nextShiftOpenShiftTimeCondition))).append(division);
            orderNoLog.append(StringUtils.format("【换模开班】换模后的时间：【{}】,小于限定时间：【{}】,本班次可以进行开班标记判断", DateUtil.formatDatetime(afterChangeMoldTime),DateUtil.formatDatetime(nextShiftOpenShiftTimeCondition))).append(division);
            //获取胎胚开始供应时间
            Date embryoStartTime=getEmbryoCodeStartTimeByClassShift(embryoCode,estimateShiftStock,cls,logDetail);
            if(embryoStartTime!=null){
                //开班处理以及班次上限设定
                calcOpenShiftClassMaxPlanQty(cls,lhEngineScheduleResult,embryoStartTime,logDetail,orderNoLog);
            }else{
                logDetail.append(StringUtils.format("【换模开班】当前胎胚：【{}】,未找到对应的开始供应时间，当前班次开班无法开班，看下个班次是否可以开班标记",embryoCode)).append(division);
                orderNoLog.append(StringUtils.format("【换模开班】当前胎胚：【{}】,未找到对应的开始供应时间，当前班次开班无法开班，看下个班次是否可以开班标记",embryoCode)).append(division);
                LhClassShiftEnum nextCls=null;
               // updateEstimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
                Integer nextClsIndex=cls.getClassIndex() + 1;
                nextCls=LhClassShiftEnum.getClassShiftByClassIndex(nextClsIndex);
                if(nextCls==null){
                    logDetail.append(StringUtils.format("【换模开班】需要标记开班的下个班次不在当天计划班次内，所以不标记了！")).append(division);
                    orderNoLog.append(StringUtils.format("【换模开班】需要标记开班的下个班次不在当天计划班次内，所以不标记了！")).append(division);
                    return ;
                }
                //更新班次时间不可用
                lhEngineScheduleResult.setEnableStartTime(getShiftBeginTimeByClass(nextCls));
                lhEngineScheduleResult.setEnableEndTime(getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT));
                logDetail.append(StringUtils.format("【换模开班】获取到下一个开班判断班次：【{}】",nextCls.getClassName())).append(division);
                orderNoLog.append(StringUtils.format("【换模开班】获取到下一个开班判断班次：【{}】", nextCls.getClassName())).append(division);
                //本班次无法开班，下个班次进行开班标记
                openShiftOperation(lhEngineScheduleResult,nextCls,logDetail,orderNoLog);
            }
        }


    }

    /**
     * 所有开班停汽标记统一入口处理
     * @param lhEngineScheduleResult
     * @param cls
     * @param logDetail
     * @param orderNoLog
     */
    private void openShiftOperation(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum cls, StringBuilder logDetail,StringBuilder orderNoLog) {
        String embryoCode=lhEngineScheduleResult.getEmbryoCode();
        //预估班次库存
        Integer estimateShiftStock=estimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
        //获取胎胚开始供应时间
        Date embryoStartTime=getEmbryoCodeStartTimeByClassShift(embryoCode,estimateShiftStock,cls,logDetail);
        logDetail.append(StringUtils.format("【换模开班】判断班次获取到的胎胚开始供应时间：【{}】",DateUtil.formatDatetime(embryoStartTime))).append(division);
        orderNoLog.append(StringUtils.format("【换模开班】判断班次获取到的胎胚开始供应时间：【{}】", DateUtil.formatDatetime(embryoStartTime))).append(division);
        if(embryoStartTime!=null){
            //开班处理以及班次上限设定
            calcOpenShiftClassMaxPlanQty(cls,lhEngineScheduleResult,embryoStartTime,logDetail,orderNoLog);
        }
    }

    /**
     * 标记停汽原因
     * @param lhEngineScheduleResult
     * @param nextCls
     */
    private void markStopStreamAnalysis(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum nextCls) {
        LhEngineScheduleUtils.setClassShiftOcclusionFlag(lhEngineScheduleResult,nextCls,true);
        LhEngineScheduleUtils.setClassShiftAnalysis(lhEngineScheduleResult,nextCls,I18nUtil.getMessage("lh.engine.occlusion.analysis.title"));
        LhEngineScheduleUtils.setAnalysisCode(lhEngineScheduleResult,nextCls,AnalysisCodeEnum.STREAM_OFF);
    }

    /**
     * 获取班次时间
     * @param cls
     * @return
     */
    private Date getShiftTimeByClass(LhClassShiftEnum cls,String shiftTimePrefix){
       String key=cls.getClassIndex()+shiftTimePrefix;
       return shiftStartEndTimeMap.get(key);
    }

    /**
     * 获取班次开始时间
     * @param cls
     * @return
     */
    private Date getShiftBeginTimeByClass(LhClassShiftEnum cls){
        if(cls==null){
            return lastThreeShiftBeginTime;
        }
        return getShiftTimeByClass(cls,START_TIME_PREFIX);
    }

    /**
     * 获取班次结束时间
     * @param cls
     * @return
     */
    private Date getShiftEndTimeByClass(LhClassShiftEnum cls){
        if(cls==null){
            Date lastThreeShiftEndTime =DateUtils.addHours(lastThreeShiftBeginTime,8);
            lastThreeShiftEndTime=DateUtils.addSeconds(lastThreeShiftEndTime,-1);
            return lastThreeShiftEndTime;
        }
        return getShiftTimeByClass(cls,END_TIME_PREFIX);
    }

    /**
     * 初始化胎胚施工信息
     * @param logDetail
     */
    private void initSapTireConstructionListMap(StringBuilder logDetail) {
        logDetail.append("结合初始化生成的规格进行生成硫化外胎施工信息初始化...").append(division);
        //硫化外胎施工信息start
        LhEngineTireConstructionInfo condition=new LhEngineTireConstructionInfo();
        List<LhEngineTireConstructionInfo> constructionInfoList=lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(condition);
        if(StringUtils.isNotEmpty(constructionInfoList)){
            sapTireConstructionListMap.putAll(constructionInfoList.stream().collect(Collectors.groupingBy(lhEngineTireConstructionInfo -> lhEngineTireConstructionInfo.getSapCode())));
        }
        //硫化外胎施工信息end
        logDetail.append("初始化后的外胎施工数据信息记录数：").append(StringUtils.isEmpty(sapTireConstructionListMap)?0:sapTireConstructionListMap.size()).append(division);

    }

    /**
     * 第一步数据初始化过程
     * @param scheduleDate 硫化自动排程日期 yyyy-MM-dd
     * @param errorMsg 错误信息
     * @param logDetail 错误日志
     */
    private void scheduleInit(String scheduleDate, StringBuilder errorMsg, StringBuilder logDetail) {
        if(StringUtils.isEmpty(scheduleDate)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.auto.schedule.scheduleDate.empty.error"));
            logDetail.append(errorMsg).append(division);
            return;
        }
        //初始化缓存集合
        initCacheData();
        logDetail.append("硫化自动排程数据初始化开始...").append(division);

        //硫化白班开始时间
        Date date= DateUtils.parseDate(scheduleDate);
        //27号的计划 开始时间是26号
        Date lastDate=DateUtils.addDays(date,-1);
        Date classShiftBeginTime= LhEngineScheduleUtils.formatDateByZero(lastDate);
        lastThreeShiftBeginTime= DateUtils.addHours(classShiftBeginTime, LhEngineConstants.CLASS_SHIFT_HOUR );

        //成型工序参数初始化
        cxParamsMap=lhCommonService.loadCxParams();

        //1.1 初始化各个班次的开始时间 结束时间
        initShiftStartEndTime(logDetail);

        //1.2 初始化昨日硫化计划
        initLastDayLhSchedule(scheduleDate,logDetail);//TODO 测试先修改有数据的日期 2022-06-15
        //1.3 初始化硫化机台数据
        initLhMachineList(errorMsg,logDetail);
        //1.4 初始化昨日和今日的换膜计划信息
        initLhMoldAdjustPlanList(scheduleDate,logDetail); //TODO 测试先修改有数据的日期 2022-06-24
        //1.5 初始化成型今日排程计划信息
        initCxScheduleList(scheduleDate,errorMsg,logDetail); //TODO 测试需要进行注释 2022-02-12
        //1.6 初始化硫化在产规格数
        initLhInProductSpec(scheduleDate,errorMsg,logDetail);//TODO 测试需要进行日期固定传入 2022-03-22
        //1.7 初始化硫化工序参数
        initLhParams(logDetail);
        //1.8 初始化硫化外胎汇总数据
        //initLhSapMonthSurplus("2022-02-12",errorMsg,logDetail); //TODO 测试获取存在月度外胎汇总的数据
        //1.9 初始化成型全部任务的时间段集合
        initSapEmbryoSupportMap(scheduleDate,logDetail);//TODO 测试获取存在月度外胎汇总的数据 2022-02-12
        //1.10 初始化硫化外胎施工信息
        initSapTireConstructionListMap(logDetail);

        logDetail.append("硫化自动排程数据初始化结束...").append(division);
    }

    /**
     * 初始化各个班次的开始时间 结束时间
     * @param logDetail
     */
    private void initShiftStartEndTime(StringBuilder logDetail) {
        logDetail.append("开始初始化各个班次的开始时间，结束时间").append(division);
        //遍历班次进行开始时间结束时间段
        for (LhClassShiftEnum cls :LhClassShiftEnum.values()){
            Date classShiftBeginTime=DateUtils.addHours(lastThreeShiftBeginTime, LhEngineConstants.CLASS_SHIFT_HOUR * cls.getClassIndex());
            shiftStartEndTimeMap.put(cls.getClassIndex()+START_TIME_PREFIX,classShiftBeginTime);
            Date nextShiftBeginTime=DateUtils.addHours(lastThreeShiftBeginTime, LhEngineConstants.CLASS_SHIFT_HOUR * (cls.getClassIndex()+1));
            nextShiftBeginTime=DateUtils.addSeconds(nextShiftBeginTime,-1);
            shiftStartEndTimeMap.put(cls.getClassIndex()+END_TIME_PREFIX,nextShiftBeginTime);
        }
        logDetail.append("初始化完毕,班次时间结果集合：").append(toJSONString(shiftStartEndTimeMap)).append(division);
    }


    /**
     * 初始化数据缓存集合
     */
    private void initCacheData() {
        lhParamsMap=new HashMap<>();
        lhMachineInfoList= new ArrayList<>();
        cxScheduleResultList=new ArrayList<>();
        lastDayLhApsMoldAdjustPlanList=new ArrayList<>();
        lhApsMoldAdjustPlanList=new ArrayList<>();
        lastDayLhScheduleResultList=new ArrayList<>();
        lhSapMonthPlanSurplusMap=new HashMap<>();
        cxBatchNo="";
        lhEmbryoSupportTimeMap=new HashMap<>();
        sapShiftEmbryoStockMap=new HashMap<>();
        lastDayEmbryoResultMap=new HashMap<>();
        sapLhApsMoldAdjustPlanMap=new HashMap<>();
        lastDayChangeMoldMap=new HashMap<>();
        cxParamsMap=new HashMap<>();
        sapTireConstructionListMap=new HashMap<>();
        shiftStartEndTimeMap=new HashMap<>();
        shiftUpdateCxPlanFlag=new HashMap<>();
        orderLogMap=new HashMap<>();
        machineLhEndTime =new HashMap<>();
        machineInProductListMap =new HashMap<>();
    }

    /**
     *  加载处理组装前一天硫化排程数据
     * @param scheduleDate yyyy-MM-dd
     */
    private void initLastDayLhSchedule(String scheduleDate, StringBuilder logDetail) {
        LhEngineScheduleResult condition=new LhEngineScheduleResult();
        Date date= DateUtils.parseDate(scheduleDate);
        Date lastDate=DateUtils.addDays(date,-1);
        String lastDateStr=DateUtils.parseDateToStr("yyyy-MM-dd",lastDate);
        condition.setLhScheduleDate(lastDateStr);
        logDetail.append(StringUtils.format("开始初始化昨日硫化计划，初始化日期：【{}】..",lastDateStr)).append(division);
        lastDayLhScheduleResultList=this.commonLhEngineMapper.selectLhEngineScheduleResultList(condition);
        if(StringUtils.isNotEmpty(lastDayLhScheduleResultList)){
            //根据SAP品号进行昨日硫化计划分组
            lastDayEmbryoResultMap=lastDayLhScheduleResultList.stream().collect(Collectors.groupingBy(lastDayResult -> lastDayResult.getEmbryoCode()));
        }
        logDetail.append(StringUtils.format("初始化完毕，初始化日期：【{}】，初始化记录数：【{}】",lastDateStr,StringUtils.isEmpty(lastDayLhScheduleResultList)?0:lastDayLhScheduleResultList.size())).append(division);

    }

    /**
     * 初始化硫化机台信息
     * @param errorMsg
     * @param logDetail
     */
    private void initLhMachineList(StringBuilder errorMsg,StringBuilder logDetail) {
        //获取所有可用硫化机台信息列表
        logDetail.append("开始初始化硫化机台数据").append(division);
        LhMachineInfo condition=new LhMachineInfo();
        condition.setStatus(LhEngineConstants.LH_MACHINE_STATUS_ENABLE);
        lhMachineInfoList=commonLhEngineMapper.selectMachineInfoList(condition);
        if(StringUtils.isEmpty(lhMachineInfoList)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.auto.schedule.machine.empty.error"));
            logDetail.append(logDetail).append(division);
            return;
        }
        logDetail.append("机台初始化完毕，记录数：").append(lhMachineInfoList.size()).append(division);
    }

    /**
     * 初始化昨日的换膜计划和今日的换模计划
     * @param scheduleDate yyyy-MM-dd
     * @param logDetail
     */
    private void initLhMoldAdjustPlanList(String scheduleDate, StringBuilder logDetail) {
        Date date= DateUtils.parseDate(scheduleDate);
        Date lastTwoDate=DateUtils.addDays(date,-2); //下达日期比排程下的时间要往前挪一天，所以实际的前一天数据是往前挪2天
        LhApsMoldAdjustPlan condition=new LhApsMoldAdjustPlan();
        condition.setPlanDate(lastTwoDate);
        //condition.setIsExecute(LhEngineConstants.MOLD_EXECUTE_STATUS_YES);// TODO 只查询已执行的模具计划
        logDetail.append("开始初始化昨日的模具变动计划数据").append(division);
        lastDayLhApsMoldAdjustPlanList=this.commonLhEngineMapper.selectLhApsMoldAdjustPlanList(condition);
        logDetail.append("昨日模具变动计划数=").append(StringUtils.isEmpty(lastDayLhApsMoldAdjustPlanList)?0:lastDayLhApsMoldAdjustPlanList.size()).append(division);
        logDetail.append("开始初始化今日的模具变动计划数据").append(division);
        condition=new LhApsMoldAdjustPlan();
        Date lastOneDate=DateUtils.addDays(date,-1); //下达日期比排程下的时间要往前挪一天，所以实际的当天数据是往前挪1天
        condition.setPlanDate(lastOneDate);
        condition.setIsExecute(LhEngineConstants.MOLD_EXECUTE_STATUS_YES);// 今日的换模计划只查询已执行的模具计划，2022-07-01 与建伟沟通今日如果不执行的就不导入
        lhApsMoldAdjustPlanList=this.commonLhEngineMapper.selectLhApsMoldAdjustPlanList(condition);
        if(StringUtils.isNotEmpty(lhApsMoldAdjustPlanList)){
           //根据硫化机台编号进行模具变动计划分组
            sapLhApsMoldAdjustPlanMap=lhApsMoldAdjustPlanList.stream().collect(Collectors.groupingBy(LhApsMoldAdjustPlan::getLhMachineCode));
        }
        logDetail.append("今日模具变动计划数=").append(StringUtils.isEmpty(lhApsMoldAdjustPlanList)?0:lhApsMoldAdjustPlanList.size()).append(division);
    }

    /**
     * 初始化成型今日排程计划数据
     * @param scheduleDate yyyy-MM-dd
     * @param errorMsg
     * @param logDetail
     */
    private void initCxScheduleList(String scheduleDate, StringBuilder errorMsg, StringBuilder logDetail) {
        logDetail.append(StringUtils.format("开始初始化今日成型计划，初始化日期：【{}】，start..",scheduleDate)).append(division);
        CxScheduleResult scheduleResult=new CxScheduleResult();
        scheduleResult.setScheduleDateStr(scheduleDate);
        cxScheduleResultList=this.commonCxEngineMapper.selectCxScheduleResultList(scheduleResult);
        if(StringUtils.isEmpty(cxScheduleResultList)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.auto.schedule.cx.scheduleResult.empty.error"));
            logDetail.append(errorMsg).append(division);
        }else{
            //初始化成型批次号
            cxBatchNo=cxScheduleResultList.get(0).getCxBatchNo();
            //更新成型早8点库存
            //lhCommonService.updateLastDayTaskStock(cxScheduleResultList,scheduleDate);
            //组装SAP品号在成型中班（16:00）班次的计划量,后续班次需要硫化安排计划后才可以进行预计
            estimateLhClassOneStock(scheduleDate,LhClassShiftEnum.ONE_CLASS_SHIFT,logDetail);

        }
        logDetail.append(StringUtils.format("成型计划初始化完毕，初始化日期：【{}】，初始化记录数：【{}】",scheduleDate,StringUtils.isEmpty(cxScheduleResultList)?0:cxScheduleResultList.size())).append(division);

    }

    /**
     * 初始化硫化在产规格信息
     * @param scheduleDate  yyyy-MM-dd
     * @param errorMsg
     * @param logDetail
     */
    private void initLhInProductSpec(String scheduleDate, StringBuilder errorMsg, StringBuilder logDetail) {
        Date date= DateUtils.parseDate(scheduleDate);
        Date productDate=DateUtils.addDays(date,-1);
        logDetail.append(StringUtils.format("开始初始化硫化机在产规格数据，初始化日期：【{}】，开始..",DateUtil.formatDate(productDate))).append(division);
        LhInProductionSpec lhInProductionSpec=new LhInProductionSpec();
        lhInProductionSpec.setProductDate(productDate);
        List<LhInProductionSpec> lhInProductionSpecList=this.commonLhEngineMapper.selectLhInProductionSpecList(lhInProductionSpec);
        if(StringUtils.isEmpty(lhInProductionSpecList)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.auto.schedule.lh.in.product.spec.empty.error"));
            logDetail.append(errorMsg).append(division);
            return;
        }
        //根据硫化机台编号进行分组
        machineInProductListMap = CollectionUtil.toMapList(lhInProductionSpecList, LhInProductionSpec::getLhMachineCode);
        if(StringUtils.isNotEmpty(machineInProductListMap)){
            //特殊处理因为Mes单规格也会给两条数据，所以需要去重
            for(Map.Entry<String, List<LhInProductionSpec>> entry:machineInProductListMap.entrySet()){
                //硫化机台
                String machineCode=entry.getKey();
                List<LhInProductionSpec> specList=entry.getValue();
                if(StringUtils.isNotEmpty(specList)){
                    Set<String> sapCodeSet=new HashSet<>();
                    List<LhInProductionSpec> onlySapCodeList=new ArrayList<>();
                    for(LhInProductionSpec inProductionSpec:specList){
                        String sapCode=inProductionSpec.getSapCode();
                        if(StringUtils.isEmpty(sapCode)){
                            logDetail.append(StringUtils.format("当前机台：【{}】,在产sap品号为空，机台生产单模情况",lhInProductionSpec.getLhMachineCode())).append(division);
                        }
                        if(!sapCodeSet.contains(sapCode)){
                            onlySapCodeList.add(inProductionSpec);
                            sapCodeSet.add(sapCode);
                        }
                    }
                    machineInProductListMap.put(machineCode,onlySapCodeList);
                }
            }
        }

        logDetail.append(StringUtils.format("在产规格初始化完毕，初始化日期：【{}】，初始化在产规格信息：【{}】",DateUtil.formatDate(productDate),toJSONString(machineInProductListMap))).append(division);
    }

    /**
     * 初始化硫化工序参数
     * @param logDetail
     */
    private void initLhParams(StringBuilder logDetail) {
        logDetail.append("开始初始化硫化工序参数...").append(division);
        //初始化硫化工序参数
        List<LhEngineParams> lhEngineParamsList =this.lhEngineParamsService.selectParamsList(new LhEngineParams());
        if(StringUtils.isNotEmpty(lhEngineParamsList)){
            lhParamsMap= lhEngineParamsList.stream().collect(Collectors.toMap(LhEngineParams::getParamCode, LhEngineParams::getParamValue));
        }
    }

    /**
     * 初始化硫化外胎汇总数据
     * @param scheduleDate yyyy-MM-dd
     * @param errorMsg
     * @param logDetail
     */
    private void initLhSapMonthSurplus(String scheduleDate, StringBuilder errorMsg, StringBuilder logDetail) {
        logDetail.append("开始初始化外胎月度计划汇总数据").append(division);
        Date date= DateUtils.parseDate(scheduleDate);
        String monthPlanApsVersion="";
        MdmMonthPlanMain planVersion=lhCommonService.getValidPlanMainVersion(date);
        if(planVersion==null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.auto.plan.main.empty.error"));
            logDetail.append(errorMsg).append(division);
            return;
        }
        monthPlanApsVersion=planVersion.getMonthPlanApsVersion();
        LhSapMonthPlanSurplus condition=new LhSapMonthPlanSurplus();
        condition.setMonthPlanApsVersion(monthPlanApsVersion);
        List<LhSapMonthPlanSurplus> lhSapMonthPlanSurplusList=commonLhEngineMapper.selectLhSapMonthPlanSurplusList(condition);
        if(StringUtils.isNotEmpty(lhSapMonthPlanSurplusList)){
            lhSapMonthPlanSurplusMap=lhSapMonthPlanSurplusList.stream().collect(Collectors.toMap(LhSapMonthPlanSurplus::getSapCode,lhSapMonthPlanSurplus -> lhSapMonthPlanSurplus));
        }
        logDetail.append("汇总数据结果数量=").append(StringUtils.isEmpty(lhSapMonthPlanSurplusList)?0:lhSapMonthPlanSurplusList.size()).append(division);
    }

    /**
     * 初始化排程日期对应的成型胎胚任务时间段
     * @param scheduleDate yyyy-MM-dd
     * @param logDetail
     */
    private void initSapEmbryoSupportMap(String scheduleDate,StringBuilder logDetail) {
        logDetail.append("开始初始化成型胎胚开始时间数据，排程日期=").append(scheduleDate).append(division);
        List<LhSapEmbryoTime> lhSapEmbryoTimeList=commonCxEngineMapper.selectEmbryoTimeList(scheduleDate);
        if(StringUtils.isNotEmpty(lhSapEmbryoTimeList)){
            lhEmbryoSupportTimeMap=lhSapEmbryoTimeList.stream().collect(Collectors.groupingBy(LhSapEmbryoTime::getEmbryoCode));
            if(StringUtils.isNotEmpty(lhEmbryoSupportTimeMap)){
                for(Map.Entry<String,List<LhSapEmbryoTime>> entry:lhEmbryoSupportTimeMap.entrySet()){
                    String getEmbryoCode=entry.getKey();
                    List<LhSapEmbryoTime> sameSapEmbryoTimeList=entry.getValue();
                    List<LhSapEmbryoTime> mergeEmbryoTimeList= new ArrayList<>();
                    mergeEmbryoTime(sameSapEmbryoTimeList,mergeEmbryoTimeList,logDetail);
                    lhEmbryoSupportTimeMap.put(getEmbryoCode,mergeEmbryoTimeList);
                }
            }
        }
        logDetail.append("初始化完毕，数据集合=").append(toJSONString(lhEmbryoSupportTimeMap)).append(division);

    }

    /**
     * 开始进行多段时间组装
     * @param lhSapEmbryoTimeList 前置时间集合
     * @param mergeEmbryoTimeList 重新组装后时间集合
     * @param logDetail
     */
    private void mergeEmbryoTime(List<LhSapEmbryoTime> lhSapEmbryoTimeList, List<LhSapEmbryoTime> mergeEmbryoTimeList, StringBuilder logDetail) {
        logDetail.append("开始进行多段时间拼接组装方法》》").append(division);
        if(StringUtils.isNotEmpty(lhSapEmbryoTimeList)&&lhSapEmbryoTimeList.size()>1){
            //开始遍历全部的时间段start
            LhSapEmbryoTime lastSapEmbryoTime=null; //保存上一段时间对象
            for(int i=0,len=lhSapEmbryoTimeList.size();i<len;i++){
                LhSapEmbryoTime lhSapEmbryoTime=lhSapEmbryoTimeList.get(i);
                if(i==0){
                    lastSapEmbryoTime=lhSapEmbryoTime;
                    continue;
                }
                //开始时间
                Date estimateStartTime=lhSapEmbryoTime.getEstimateStartTime();
                //结束时间
                Date estimateEndTime=lhSapEmbryoTime.getEstimateEndTime();

                if(lastSapEmbryoTime!=null){
                    //前一个开始时间
                    Date lastEstimateStartTime=lastSapEmbryoTime.getEstimateStartTime();
                    //前一个结束时间
                    Date lastEstimateEndTime=lastSapEmbryoTime.getEstimateEndTime();
                    //先判断是否是没有交集的,没有交集的直接把上一个时间段放入集合，将当前对象复制为新的上一段时间
                    boolean hasOverLap=LhEngineScheduleUtils.hasOverlap(lastEstimateStartTime,lastEstimateEndTime,estimateStartTime,estimateEndTime);

                    if(!hasOverLap){
                        logDetail.append("时间段不存在交集，不需要进行合并处理").append(division);
                        //没有交集的直接放入集合
                        mergeEmbryoTimeList.add(lastSapEmbryoTime);
                        lastSapEmbryoTime=lhSapEmbryoTime;
                    }else{
                        logDetail.append("时间段存在交集，需要进行合并处理").append(division);
                        //存在交集的处理一下取时间小的为开始时间，时间大的为结束时间
                        if(estimateStartTime.getTime()>lastEstimateStartTime.getTime()){
                            logDetail.append("当前开始时间大于前一个开始时间，以上一个时间为开始时间").append(division);
                            //取小的为最新的时间
                            lhSapEmbryoTime.setEstimateStartTime(lastEstimateStartTime);
                        }

                        if(estimateEndTime.getTime()<lastEstimateEndTime.getTime()){
                            logDetail.append("当前结束时间小于前一个结束时间，以上一个时间为结束时间").append(division);
                            //取大的为最新的时间
                            lhSapEmbryoTime.setEstimateEndTime(lastEstimateEndTime);
                        }
                        lastSapEmbryoTime=lhSapEmbryoTime;
                    }
                }
                //最后一个时间段直接放入
                if(i==len-1){
                    mergeEmbryoTimeList.add(lhSapEmbryoTime);
                }

            }
            //开始遍历全部的时间段end
        }else{
            logDetail.append("当前不存在多段时间，不需要进行处理...").append(division);
            mergeEmbryoTimeList.addAll(lhSapEmbryoTimeList);
        }
    }

    /**
     * 根据机台当前在产规格进行硫化排程初始化规格
     * @param lhEngineScheduleResultList
     * @param logDetail
     */
    private void dataFillingByInProductSpec(List<LhEngineScheduleResult> lhEngineScheduleResultList,Date autoScheduleDate,String lhBatchNo, StringBuilder logDetail) {
        logDetail.append("开始结合当前在产规格进行硫化排程规格填充").append(division);
        if(StringUtils.isNotEmpty(machineInProductListMap)){
            //根据硫化机台编号进行分组
            logDetail.append(StringUtils.format("当前获取到的在产规格集合：【{}】",toJSONString(machineInProductListMap))).append(division);

             //组装昨日换模计划
            createLastDayChangeMoldMap(logDetail);

            //遍历全部机台信息进行规格初始化start
            for(LhMachineInfo lhMachineInfo:lhMachineInfoList){
                String machineCode=lhMachineInfo.getMachineCode();//硫化机台
                String machineName=lhMachineInfo.getMachineName();//硫化机台
                if((StringUtils.isEmpty(machineInProductListMap)||!machineInProductListMap.containsKey(machineCode))&&(StringUtils.isEmpty(lastDayChangeMoldMap)||!lastDayChangeMoldMap.containsKey(machineCode))){
                    logDetail.append(StringUtils.format("当前机台：【{}】，没找到在产规格数据,也没有昨日换模计划数据,跳过不自动填充",machineName)).append(division);
                    continue;
                }
                if(StringUtils.isEmpty(lastDayChangeMoldMap)||!lastDayChangeMoldMap.containsKey(machineCode)){
                    //没有昨日换模计划的直接以在产的规格进行安排(前规格计划)
                    addTaskByLhInProductSpec(machineCode,machineName,autoScheduleDate,lhBatchNo,lhEngineScheduleResultList,logDetail);
                }else{
                    //存在昨日换模计划的则以昨日换模计划后规格进行数据初始化
                    addTaskByLastDayMoldChangePlan(lastDayChangeMoldMap,machineCode,machineName,autoScheduleDate,lhBatchNo,lhEngineScheduleResultList,logDetail);
                }

            }
            //遍历全部机台信息进行规格初始化end
        }
        logDetail.append("结束结合当前在产规格+昨日换模计划 进行硫化排程规格填充排程结果数据集：").append(toJSONString(lhEngineScheduleResultList)).append(division);
    }

    /**
     * 添加初始化规格根据在产规格信息
     * @param machineCode 当前硫化机
     */
    private void addTaskByLhInProductSpec(String machineCode,String machineName,Date autoScheduleDate,String lhBatchNo,List<LhEngineScheduleResult> lhEngineScheduleResultList, StringBuilder logDetail) {
        logDetail.append("初始化新规格通过昨日在产规格数据进行初始化...").append(division);
        //模数的理解：在产规格初始化已经去重了 也就是 L/R如果相同规格只会有1条，如果还是多条的话证明左右模不一样，那模数只能是1，否则就是2模
        List<LhInProductionSpec> lhInProductionSpecs=machineInProductListMap.get(machineCode);
        if(StringUtils.isNotEmpty(lhInProductionSpecs)&&lhInProductionSpecs.size()>1){
            logDetail.append(StringUtils.format("当前机台：【{}】，存在多条在产规格，产生多条规格填充逻辑",machineName)).append(division);
            //存在多条在产规格
            for(LhInProductionSpec lhInProductionSpec:lhInProductionSpecs){

                if (checkEmptyMold(lhInProductionSpec,logDetail)){
                    //空模，不初始任务
                    continue;
                }
                String leftRightMold=lhInProductionSpec.getLeftRightMold();
                String sapCode=lhInProductionSpec.getSapCode();
                Integer moldNumber=BigDecimal.ONE.intValue();
                appendInitTaskToList(machineCode,machineName,sapCode,moldNumber,false,false,false,autoScheduleDate,lhBatchNo,leftRightMold,null,lhEngineScheduleResultList);
            }
        }else if(StringUtils.isNotEmpty(lhInProductionSpecs)&&lhInProductionSpecs.size() == 1){
            logDetail.append(StringUtils.format("当前机台：【{}】，存在单条在产规格，产生单条规格填充逻辑",machineName)).append(division);
            //只存在单条在产规格数据
            LhInProductionSpec lhInProductionSpec=lhInProductionSpecs.get(0);
            if (checkEmptyMold(lhInProductionSpec,logDetail)){
                //空模，不初始任务
                return;
            }
            String sapCode=lhInProductionSpec.getSapCode();
            Integer moldNumber= LhEngineConstants.TWO_MOLD_NUMBER;
            appendInitTaskToList(machineCode,machineName,sapCode,moldNumber,false,false,false,autoScheduleDate,lhBatchNo,null,null,lhEngineScheduleResultList);
        }
    }

    /**
     * 检查是否空模
     * @param lhInProductionSpec 在产规格
     * @param logDetail 日志明细
     * @return 空模-true/ 非空模-false
     */
    private boolean checkEmptyMold(LhInProductionSpec lhInProductionSpec,StringBuilder logDetail){
        boolean bResult = BigDecimal.ONE.toString().equals(lhInProductionSpec.getIsEmptyMold());
        if (bResult){
            logDetail.append(StringUtils.format("当前机台：【{}】，存在空模！",lhInProductionSpec.getLhMachineName())).append(division);
        }
        return bResult;
    }

    /**
     *  在产规格没有换模计划，则认为规格一致，需要进行开班逻辑判断
     * @param lhEngineScheduleResult
     */
    private void singleTaskByInProductionSpec(LhEngineScheduleResult lhEngineScheduleResult,StringBuilder logDetail) {
        //获取昨日的硫化计划，如果有进行时间判断start
        String lhMachineCode=lhEngineScheduleResult.getLhMachineCode();
        String embryoCode=lhEngineScheduleResult.getEmbryoCode();
        //工单号
        String orderNo=lhEngineScheduleResult.getOrderNo();
        //获取工单日志构建器
        StringBuilder orderNoLog=getOrderLogBuilder(orderNo);
        //当前排程日期
        Date scheduleDate=lhEngineScheduleResult.getScheduleDate();
        if(StringUtils.isNotEmpty(embryoCode)&&!lhEngineScheduleResult.getIsMoreEmbryoCode()){
          logDetail.append("在产规格没有换模计划，查询机台的前规格信息进行判断").append(division);
          orderNoLog.append("在产规格没有换模计划，查询机台的前规格信息进行判断").append(division);
          //获取硫化机台最后的任务
          LhEngineScheduleResult maxEndTimeSpec=getLastMachineScheduleTask(scheduleDate,lhMachineCode,embryoCode,logDetail,orderNoLog);
          if(maxEndTimeSpec!=null){
              Date maxLhEndTime=getMaxLhEndTime(maxEndTimeSpec,scheduleDate,logDetail,orderNoLog);
              //更新机台规格硫化结束时间
              setMachineLhEndTime(lhMachineCode,lhEngineScheduleResult.getLhMachineName(),maxLhEndTime,LhClassShiftEnum.ONE_CLASS_SHIFT,logDetail,orderNoLog);
              //一班开汽判断
              //markOpenStreamShiftBySingleTask(maxEndTimeSpec.getEmbryoCode(),maxLhEndTime,LhClassShiftEnum.ONE_CLASS_SHIFT,lhEngineScheduleResult,logDetail,orderNoLog);
          }
        }else{
            logDetail.append("填充胎胚代码，没有对应的昨日排程计划，无法获取时间，不能进行开班标记分析.").append(division);
        }
        //获取昨日的硫化计划，如果有进行时间判断end
    }

    /**
     * 获取当前机台最近的任务
     * @param scheduleDate 当前排程日期
     * @param lhMachineCode 当前硫化机台
     * @param embryoCode  当前胎胚代码
     * @param logDetail 日志
     * @param orderNoLog 工单日志
     * @return
     */
    private LhEngineScheduleResult getLastMachineScheduleTask(Date scheduleDate, String lhMachineCode,String embryoCode, StringBuilder logDetail, StringBuilder orderNoLog) {
        LhEngineScheduleResult maxEndTimeSpec=null;
        List<LhEngineScheduleResult> historyLhScheduleResultList=new ArrayList<>();
        //根据机台和排程日期往前找最近的硫化排程信息，找到后进行规格匹配，规格一致则走开汽，规格不一致则走开班
        Date lastDate=scheduleDate;
        //最大任务追溯天数
        Integer maxTraceDays=getMaxTraceDays(LhEngineParamCodeConstants.TRACE_MAX_DAYS);
        Integer step=0;
        while(step<=maxTraceDays){
            step+=1;//累计追加的天数
            LhEngineScheduleResult condition=new LhEngineScheduleResult();
            lastDate=DateUtils.addDays(lastDate,-1);
            String lastDateStr=DateUtils.parseDateToStr("yyyy-MM-dd",lastDate);
            condition.setLhScheduleDate(lastDateStr);
            condition.setLhMachineCode(lhMachineCode);
            condition.setOrderByPlanQtyStr("Y");//根据计划量进行降序排序
            historyLhScheduleResultList=this.commonLhEngineMapper.selectLhEngineScheduleResultList(condition);
            logDetail.append(StringUtils.format("【获取机台最近的计划】,获取的当前日期：【{}】，机台编号：【{}】,获取的数据结果：【{}】",lastDateStr,lhMachineCode,toJSONString(historyLhScheduleResultList))).append(division);
            orderNoLog.append(StringUtils.format("【获取机台最近的计划】,获取的当前日期：【{}】，机台编号：【{}】,获取的数据结果：【{}】",lastDateStr,lhMachineCode,toJSONString(historyLhScheduleResultList))).append(division);
            if(StringUtils.isNotEmpty(historyLhScheduleResultList)){
                logDetail.append(StringUtils.format("【获取机台最近的计划】,获取的当前日期：【{}】，机台编号：【{}】,数据找到，结束追溯",lastDateStr,lhMachineCode)).append(division);
                orderNoLog.append(StringUtils.format("【获取机台最近的计划】,获取的当前日期：【{}】，机台编号：【{}】,数据找到，结束追溯",lastDateStr,lhMachineCode)).append(division);
                break;
            }
        }
        if(StringUtils.isNotEmpty(historyLhScheduleResultList)){
            logDetail.append(StringUtils.format("当前硫化机台:【{}】,胎胚代码:【{}】,获取距离最近的硫化计划:【{}】",lhMachineCode,embryoCode,toJSONString(historyLhScheduleResultList))).append(division);
            orderNoLog.append(StringUtils.format("当前硫化机台:【{}】,胎胚代码:【{}】,获取距离最近的硫化计划:【{}】",lhMachineCode,embryoCode,toJSONString(historyLhScheduleResultList))).append(division);
            //存在当前胎胚规格
            maxEndTimeSpec=historyLhScheduleResultList.get(0); //获取结束时间最大的规格
            //Joran 2022-07-13 判断是否有左右模信息,如果有左右模信息的话，追溯结果集大于2条，取另一边模看下是否匹配胎胚，如果匹配胎胚，直接讲当前胎胚匹配的规格当做前规格start
            if(!StringUtils.equals(embryoCode,maxEndTimeSpec.getEmbryoCode())){
                String maxEndTimeSpecLeftRightMold=maxEndTimeSpec.getLeftRightMold();
                if(StringUtils.isNotEmpty(maxEndTimeSpecLeftRightMold)&&historyLhScheduleResultList.size()>1){
                    LhEngineScheduleResult otherMoldSpec=historyLhScheduleResultList.get(1);
                    if(otherMoldSpec != null && embryoCode != null && embryoCode.equals(otherMoldSpec.getEmbryoCode())){
                        maxEndTimeSpec=otherMoldSpec;
                    }
                }
            }
            //Joran 2022-07-13 判断是否有左右模信息,如果有左右模信息的话，追溯结果集大于2条，取另一边模看下是否匹配胎胚，如果匹配胎胚，直接讲当前胎胚匹配的规格当做前规格end


        }

        return maxEndTimeSpec;
    }

    /**
     * 标注开汽开班班次
     * @param maxEndSpecEmbryoCode 前规格胎胚
     * @param maxLhEndTime  最大硫化结束时间
     * @param lhEngineScheduleResult
     * @param logDetail
     */
    private void markOpenStreamShiftBySingleTask(String maxEndSpecEmbryoCode,Date maxLhEndTime,LhClassShiftEnum cls,LhEngineScheduleResult lhEngineScheduleResult,StringBuilder logDetail,StringBuilder orderNoLog) {
        String embryoCode=lhEngineScheduleResult.getEmbryoCode();

        //前一个班次如果存在先进行前一个班次的计划任务添加
        int estimateEmbryoStock=getStockByEmbryoCode(embryoCode);
        if(cls!=null){
            estimateEmbryoStock = estimateLhClassEmbryoStock(embryoCode,cls,logDetail,orderNoLog);
        }

        //获取成型胎胚开始时间
        Date embryoStartTime=getEmbryoCodeStartTimeByClassShift(embryoCode,estimateEmbryoStock,cls,logDetail);
        if(maxLhEndTime!=null && embryoStartTime!=null){
            logDetail.append(StringUtils.format("胎胚供应时间：【{}】,硫化结束时间：【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(maxLhEndTime))).append(division);
            orderNoLog.append(StringUtils.format("胎胚供应时间：【{}】,硫化结束时间：【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(maxLhEndTime))).append(division);
            if(embryoStartTime.getTime() <= maxLhEndTime.getTime()){
                logDetail.append(StringUtils.format("胎胚供应时间：【{}】,早于硫化结束时间：【{}】,胎胚可以供应，不用进行标记",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(maxLhEndTime))).append(division);
                orderNoLog.append(StringUtils.format("胎胚供应时间：【{}】,早于硫化结束时间：【{}】,胎胚可以供应，不用进行标记",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(maxLhEndTime))).append(division);
                return;
            }
            //计算结束时间和成型胎胚的供应时间小时差
            Long diffHour= LhEngineScheduleUtils.diffDate(maxLhEndTime,embryoStartTime, LhEngineScheduleUtils.HOUR);
            logDetail.append("硫化结束时间和成型胎胚差异时间=").append(diffHour).append("小时").append(division);
            orderNoLog.append("硫化结束时间和成型胎胚差异时间=").append(diffHour).append("小时").append(division);
            Integer cxWaitMaterialHour=getCxWaitMaterialHour(LhEngineParamCodeConstants.CX_WAIT_MATERIAL_HOUR);
            logDetail.append("标记待料条件差异时间=").append(cxWaitMaterialHour).append("小时").append(division);
            orderNoLog.append("标记待料条件差异时间=").append(cxWaitMaterialHour).append("小时").append(division);
            Integer openStreamEndTime=getOpenStreamEndTime(LhEngineParamCodeConstants.OPEN_STREAM_END_HOUR);
            logDetail.append("标记开汽上限差异时间=").append(openStreamEndTime).append("小时").append(division);
            orderNoLog.append("标记开汽上限差异时间=").append(openStreamEndTime).append("小时").append(division);

            //增加一个开汽热模时间 addy by nick 2023-05-11
            Integer lhOpenStreamPreheatHourStr = getOpenStreamPreheatTime(LhEngineParamCodeConstants.OPEN_STREAM_PREHEAT_TIME);
            if(diffHour <= cxWaitMaterialHour){ //硫化结束时间和成型胎胚开始供应时间<=3小时，成型待料
                logDetail.append("硫化结束时间和成型胎胚开始供应时间<=3小时，成型待料").append(division);
                LhClassShiftEnum lackCls=timeInClassShift(maxLhEndTime);
                if(lackCls!=null){
                    orderNoLog.append("硫化结束时间和成型胎胚开始供应时间<=3小时，待料班次：").append(lackCls.getClassName()).append(division);
                    //标记成型待料原因分析
                    markSourceLackAnalysis(lhEngineScheduleResult,lackCls);
                }else{
                    orderNoLog.append("硫化结束时间和成型胎胚开始供应时间<=3小时，待料班次超过当天计划班次,不进行待料标记").append(division);
                }
            }

            //为了兼容历史数据，如果没有获取到胎胚的数据则还是按照原来的逻辑，根据时间差判断
            if(StringUtils.isEmpty(maxEndSpecEmbryoCode) &&diffHour > cxWaitMaterialHour){
                if(diffHour<= openStreamEndTime){//标记需要开汽，开汽时间=胎胚开始供应时间，开汽班次的上限=开汽时间至本班结束时间的时长/单胎时长 * 模数
                    logDetail.append("标记需要开汽，开汽时间=胎胚开始供应时间，开汽班次的上限=开汽时间至本班结束时间的时长/单胎时长 * 模数").append(division);
                    orderNoLog.append("标记需要开汽，开汽时间=胎胚开始供应时间，胎胚开始供应时间：").append(DateUtil.formatDatetime(embryoStartTime)).append(division);
                    Date newEmbryoStartTime = DateUtils.addMinutes(embryoStartTime,lhOpenStreamPreheatHourStr);
                    orderNoLog.append("标记需要开汽，开汽时间=胎胚开始供应时间+热模时间，热模开汽时间：").append(DateUtil.formatDatetime(newEmbryoStartTime)).append(division);
                    lhEngineScheduleResult.setClassOpenStreamTime(newEmbryoStartTime);
                    //标记为开汽，且设定班次计划上限设定
                    markOpenStreamAndSetMaxPlanQty(embryoStartTime,newEmbryoStartTime,lhEngineScheduleResult,logDetail,orderNoLog);
                }else{ //大于16小时，标记需开班，班次根据成型供应时间判断，开班班次的开班计划上线更新
                    logDetail.append("大于16小时，标记需开班，班次根据成型供应时间判断，开班班次的开班计划上线更新").append(division);
                    orderNoLog.append("大于16小时，标记需开班，班次根据成型供应时间判断，开班班次的开班计划上线更新，胎胚开始供应时间：").append(DateUtil.formatDatetime(embryoStartTime)).append(division);
                    lhEngineScheduleResult.setClassOpenShiftTime(embryoStartTime);//开班时间
                    markOpenShiftAndSetMaxPlanQty(embryoStartTime,lhEngineScheduleResult,logDetail,orderNoLog);
                }
            }else if(diffHour > cxWaitMaterialHour&&embryoCode.equals(maxEndSpecEmbryoCode)){ //规格相同
                logDetail.append("机台的上一个规格是同规格，则为开汽标注").append(division);
                orderNoLog.append("机台的上一个规格是同规格，则为开汽标注，胎胚开始供应时间：").append(DateUtil.formatDatetime(embryoStartTime)).append(division);
                Date newEmbryoStartTime = DateUtils.addMinutes(embryoStartTime,lhOpenStreamPreheatHourStr);
                orderNoLog.append("机台的上一个规格是同规格，则为开汽标注，热模开汽时间：").append(DateUtil.formatDatetime(newEmbryoStartTime)).append(division);
                lhEngineScheduleResult.setClassOpenStreamTime(newEmbryoStartTime);
                //标记为开汽，且设定班次计划上限设定
                markOpenStreamAndSetMaxPlanQty(embryoStartTime,newEmbryoStartTime,lhEngineScheduleResult,logDetail,orderNoLog);
            }else if(diffHour > cxWaitMaterialHour&&!embryoCode.equals(maxEndSpecEmbryoCode)){
                logDetail.append("机台的上一个规格是不同规格，则为开班标注").append(division);
                orderNoLog.append("机台的上一个规格是不同规格，则为开班标注，胎胚开始供应时间：").append(DateUtil.formatDatetime(embryoStartTime)).append(division);
                lhEngineScheduleResult.setClassOpenShiftTime(embryoStartTime);//开班时间
                markOpenShiftAndSetMaxPlanQty(embryoStartTime,lhEngineScheduleResult,logDetail,orderNoLog);
            }

        }
    }

    /**
     * 成型待料原因分析标记
     * @param lhEngineScheduleResult
     * @param lackCls
     */
    private void markSourceLackAnalysis(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum lackCls) {
        LhEngineScheduleUtils.setClassShiftAnalysis(lhEngineScheduleResult,lackCls,I18nUtil.getMessage("lh.engine.cx.resource.lack.analysis.title"));
        LhEngineScheduleUtils.setAnalysisCode(lhEngineScheduleResult,lackCls,AnalysisCodeEnum.RESOURCE_LACK);
    }

    /**
     * 预计班次可用库存
     * @param embryoCode
     * @param cls
     * @param logDetail
     * @param orderNoLog
     * @return
     */
    private int estimateLhClassEmbryoStock(String embryoCode, LhClassShiftEnum cls, StringBuilder logDetail, StringBuilder orderNoLog) {
        Integer estimateShiftStock=getStockByEmbryoCode(embryoCode);
        if(cls==null){
            logDetail.append(StringUtils.format("【预估预计库存】,当前胎胚：【{}】,当前班次信息异常",embryoCode)).append(division);
            orderNoLog.append(StringUtils.format("【预估预计库存】,当前胎胚：【{}】,当前班次信息异常",embryoCode)).append(division);
            return 0;
        }
        if(StringUtils.isEmpty(sapShiftEmbryoStockMap)){
            logDetail.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次:【{}】,没有预计库存信息",embryoCode,cls.getClassName())).append(division);
            orderNoLog.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次:【{}】,没有预计库存信息",embryoCode,cls.getClassName())).append(division);
            return 0;
        }
        if(StringUtils.isNotEmpty(cxScheduleResultList)){
            //根据外胎品号进行成型任务分组，获取同胎胚的全部任务
            Map<String, List<CxScheduleResult>> sapEmbryoListMap = CollectionUtil.toMapList(cxScheduleResultList, CxScheduleResult::getEmbryoCode);
            if(StringUtils.isNotEmpty(sapEmbryoListMap)){
                for(Map.Entry<String, List<CxScheduleResult>> entry:sapEmbryoListMap.entrySet()){
                    String embryoKey=entry.getKey();
                    if(embryoKey.equals(embryoCode)){ //只处理当前胎胚
                        if(sapShiftEmbryoStockMap.containsKey(embryoCode)){
                            List<CxScheduleResult> cxScheduleResults=entry.getValue();
                            Integer totalPlanQty=0;//获取班次的全部胎胚计划量
                            //汇总成型班次计划
                            for(CxScheduleResult cxScheduleResult:cxScheduleResults){
                                Integer currentClassPlanQty= LhEngineScheduleUtils.getCxCurrentClassPlanQty(cxScheduleResult,cls);
                                totalPlanQty+=currentClassPlanQty;
                            }
                            estimateShiftStock+=totalPlanQty;
                            logDetail.append(StringUtils.format("【预估预计库存】,当前胎胚：【{}】,当前班次:【{}】,预估后的库存：【{}】",embryoCode,cls.getClassName(),estimateShiftStock)).append(division);
                            orderNoLog.append(StringUtils.format("【预估预计库存】,当前胎胚：【{}】,当前班次:【{}】,预估后的库存：【{}】",embryoCode,cls.getClassName(),estimateShiftStock)).append(division);

                        }
                    }
                }
            }
        }
        return estimateShiftStock;
    }

    /**
     * 获取最大规格的硫化结束时间
     * @param maxEndTimeSpec
     * @param logDetail
     * @return
     */
    private Date getMaxLhEndTime(LhEngineScheduleResult maxEndTimeSpec,Date scheduleDate, StringBuilder logDetail,StringBuilder orderNoLog) {
        String sapCode=maxEndTimeSpec.getSapCode();
        String embryoCode=maxEndTimeSpec.getEmbryoCode();
        logDetail.append(StringUtils.format("【获取最大硫化结束时间】，当前SAP品号：【{}】，胎胚代码：【{}】",sapCode,embryoCode)).append(division);
        orderNoLog.append(StringUtils.format("【获取最大硫化结束时间】，当前SAP品号：【{}】，胎胚代码：【{}】",sapCode,embryoCode)).append(division);
        Date lastDate=DateUtils.addDays(scheduleDate,-1);
        Date maxLhEndTime=null;
        Date class3EndTime=maxEndTimeSpec.getClass3EndTime();
        Date class2EndTime=maxEndTimeSpec.getClass2EndTime();
        Date class1EndTime=maxEndTimeSpec.getClass1EndTime();
        if(class3EndTime!=null){
            maxLhEndTime=class3EndTime;
        }else if(class2EndTime!=null){
            maxLhEndTime=class2EndTime;
        }else if(class1EndTime!=null){
            maxLhEndTime=class1EndTime;
        }else{
            //各个班次都没有计划量量，默认为昨日计划中班开班时间
            maxLhEndTime=initLastEndTime(lastDate);
        }
        logDetail.append(StringUtils.format("【获取最大硫化结束时间】，当前SAP品号：【{}】，胎胚代码：【{}】,硫化结束时间:【{}】",sapCode,embryoCode,DateUtil.formatDatetime(maxLhEndTime))).append(division);
        orderNoLog.append(StringUtils.format("【获取最大硫化结束时间】，当前SAP品号：【{}】，胎胚代码：【{}】,硫化结束时间:【{}】",sapCode,embryoCode,DateUtil.formatDatetime(maxLhEndTime))).append(division);
        return maxLhEndTime;
    }

    /**
     *  标记开班逻辑
     * @param embryoStartTime
     * @param lhEngineScheduleResult
     * @param logDetail
     */
    private void markOpenShiftAndSetMaxPlanQty(Date embryoStartTime, LhEngineScheduleResult lhEngineScheduleResult, StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append("【开班标记】初始化过程中，结合硫化结束时间和胎胚开始供应时间,符合标记开班逻辑.>").append(division);
        //判断时间是否在班次时间范围内，如果是进行标注，如果不是，不进行开班
        LhClassShiftEnum shiftCls=timeInClassShift(embryoStartTime);
        String shiftClsName=shiftCls==null?"【超过计划班次】":"【"+shiftCls.getClassName()+"】";
        //开汽班次标记
        LhEngineScheduleUtils.setClassShiftOpenShiftFlag(lhEngineScheduleResult,shiftCls,null!=shiftCls);
        orderNoLog.append(StringUtils.format("【开班标记】,当前胎胚开始供应时间：【{}】，所在的班次：【{}】，是否进行开班标记：【{}】",DateUtil.formatDatetime(embryoStartTime),shiftClsName,null!=shiftCls)).append(division);
        //开汽班次计划上限计算
        if(shiftCls!=null){
            //结合开汽时间与班次结束时间
            calcOpenShiftClassMaxPlanQty(shiftCls,lhEngineScheduleResult,embryoStartTime,logDetail,orderNoLog);
            lhEngineScheduleResult.setPlanSort(1);//开班生产顺序
        }
    }

    /**
     * 计算开班的计划上限
     * @param shiftCls
     * @param lhEngineScheduleResult
     * @param embryoStartTime
     * @param logDetail
     */
    private void calcOpenShiftClassMaxPlanQty(LhClassShiftEnum shiftCls, LhEngineScheduleResult lhEngineScheduleResult, Date embryoStartTime, StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【开班标记】开始计算班次：【{}】，胎胚开始供应时间：【{}】，最大计划量逻辑",shiftCls.getClassName(),DateUtil.formatDatetime(embryoStartTime))).append(division);
        orderNoLog.append(StringUtils.format("【开班标记】开始计算班次：【{}】，胎胚开始供应时间：【{}】，最大计划量逻辑",shiftCls.getClassName(),DateUtil.formatDatetime(embryoStartTime))).append(division);
        //获取班次开始时间
        Date startTime=getShiftBeginTimeByClass(shiftCls);
        //开班10条即，当前班次开班时间+4个小时
        Integer allowMaxPlanHour=getAllowMaxPlanHour(LhEngineParamCodeConstants.ALLOW_MAX_PLAN_HOUR);
        Integer changeMoldTimeHour=getChangeMoldTime(LhEngineParamCodeConstants.CHANGE_MOLD_TIME_HOUR);
        Date openShiftByTenConditionTime=DateUtils.addHours(startTime,allowMaxPlanHour);
        Date openShiftByFourConditionEndTime=DateUtils.addHours(startTime,changeMoldTimeHour);
        if(openShiftByTenConditionTime.getTime() >= embryoStartTime.getTime()){
            Integer openShiftMaxPlanQty=getOpenShiftMaxPlan(LhEngineParamCodeConstants.OPEN_SHIFT_MAX_PLAN);
            Integer useMoldNumber=lhEngineScheduleResult.getUseMoldNumber();
            if(useMoldNumber==null){
                useMoldNumber=LhEngineConstants.TWO_MOLD_NUMBER;
            }
            //如果是1模时需要进行减半
            if(useMoldNumber.equals(BigDecimal.ONE.intValue())){
                openShiftMaxPlanQty=BigDecimal.valueOf(openShiftMaxPlanQty).divide(BigDecimal.valueOf(2),0,RoundingMode.CEILING).intValue();
            }
            logDetail.append(StringUtils.format("【开班标记】胎胚开始供应时间：【{}】，开班时间+4小时时间：【{}】， 胎胚开始时间早于开班时间+4小时，可安排计划量=【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(openShiftByTenConditionTime),openShiftMaxPlanQty)).append(division);
            orderNoLog.append(StringUtils.format("【开班标记】胎胚开始供应时间：【{}】，开班时间+4小时时间：【{}】，胎胚开始时间早于开班时间+4小时，可安排计划量=【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(openShiftByTenConditionTime),openShiftMaxPlanQty)).append(division);
            //填充开班原因分析
            markOpenShiftAnalysis(lhEngineScheduleResult, shiftCls);
            LhEngineScheduleUtils.setClassShiftMaxPlanQty(lhEngineScheduleResult,shiftCls,openShiftMaxPlanQty);

        }else if(openShiftByTenConditionTime.getTime() < embryoStartTime.getTime()&&embryoStartTime.getTime() <= openShiftByFourConditionEndTime.getTime()){
            Integer openShiftMinPlanQty=getOpenShiftMinPlan(LhEngineParamCodeConstants.OPEN_SHIFT_MIN_PLAN);
            Integer useMoldNumber=lhEngineScheduleResult.getUseMoldNumber();
            if(useMoldNumber==null){
                useMoldNumber=LhEngineConstants.TWO_MOLD_NUMBER;
            }
            //如果是1模时需要进行减半
            if(useMoldNumber.equals(BigDecimal.ONE.intValue())){
                openShiftMinPlanQty=BigDecimal.valueOf(openShiftMinPlanQty).divide(BigDecimal.valueOf(2),0,RoundingMode.CEILING).intValue();
            }
            logDetail.append(StringUtils.format("【开班标记】胎胚开始供应时间：【{}】，开班时间+6小时时间：【{}】，胎胚开始时间晚于开班时间+4小时且早于开班时间+6小时，可安排计划量=【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(openShiftByFourConditionEndTime),openShiftMinPlanQty)).append(division);
            orderNoLog.append(StringUtils.format("【开班标记】胎胚开始供应时间：【{}】，开班时间+6小时时间：【{}】，胎胚开始时间晚于开班时间+4小时且早于开班时间+6小时，可安排计划量=【{}】",DateUtil.formatDatetime(embryoStartTime),DateUtil.formatDatetime(openShiftByFourConditionEndTime),openShiftMinPlanQty)).append(division);
            //填充开班原因分析
            markOpenShiftAnalysis(lhEngineScheduleResult, shiftCls);
            LhEngineScheduleUtils.setClassShiftMaxPlanQty(lhEngineScheduleResult,shiftCls,openShiftMinPlanQty);
        }else{
            //Joran 2022-08-24 D08夜班开班不了问题处理，超过开班4条的限制，证明本班无法开班，尝试下个班次再开班
            Integer nextClsIndex=shiftCls.getClassIndex() + 1;
            LhClassShiftEnum nextCls=LhClassShiftEnum.getClassShiftByClassIndex(nextClsIndex);
            if(nextCls==null){
                Date nextShiftBeginTime=DateUtils.addSeconds(getShiftEndTimeByClass(shiftCls),1);
                lhEngineScheduleResult.setEnableStartTime(nextShiftBeginTime);
                lhEngineScheduleResult.setEnableEndTime(DateUtils.addHours(nextShiftBeginTime,8));
                logDetail.append(StringUtils.format("【开班标记】需要标记开班的下个班次不在当天计划班次内，所以不标记了！")).append(division);
                orderNoLog.append(StringUtils.format("【开班标记】需要标记开班的下个班次不在当天计划班次内，所以不标记了！")).append(division);
                return ;
            }
            //更新班次时间不可用
            lhEngineScheduleResult.setEnableStartTime(getShiftBeginTimeByClass(nextCls));
            lhEngineScheduleResult.setEnableEndTime(getShiftEndTimeByClass(LhClassShiftEnum.THREE_CLASS_SHIFT));
            logDetail.append(StringUtils.format("【开班标记】获取到下一个开班判断班次：【{}】",nextCls.getClassName())).append(division);
            orderNoLog.append(StringUtils.format("【开班标记】获取到下一个开班判断班次：【{}】", nextCls.getClassName())).append(division);
            //本班次无法开班，下个班次进行开班标记
            openShiftOperation(lhEngineScheduleResult,nextCls,logDetail,orderNoLog);
            //Joran 2022-08-24 D08夜班开班不了问题处理，超过开班4条的限制，证明本班无法开班，尝试下个班次再开班
        }
        logDetail.append(StringUtils.format("【开班标记】开始计算班次：【{}】，胎胚开始供应时间：【{}】，逻辑结束..>>>>>",shiftCls.getClassName(),DateUtil.formatDatetime(embryoStartTime))).append(division);
        orderNoLog.append(StringUtils.format("【开班标记】开始计算班次：【{}】，胎胚开始供应时间：【{}】，逻辑结束..>>>>>",shiftCls.getClassName(),DateUtil.formatDatetime(embryoStartTime))).append(division);

    }

    /**
     * 填写开班原因分析
     * @param lhEngineScheduleResult
     * @param shiftCls
     */
    private void markOpenShiftAnalysis(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum shiftCls) {
        LhEngineScheduleUtils.setClassShiftAnalysis(lhEngineScheduleResult,shiftCls, I18nUtil.getMessage("lh.engine.open.analysis.title"));
        LhEngineScheduleUtils.setAnalysisCode(lhEngineScheduleResult,shiftCls, AnalysisCodeEnum.OPEN_SHIFT);
        LhEngineScheduleUtils.setClassShiftOpenShiftFlag(lhEngineScheduleResult,shiftCls,true);//标记开班
    }

    /**
     * 进行开汽标记
     * @param lhEngineScheduleResult
     * @param logDetail
     */
    private void markOpenStreamAndSetMaxPlanQty(Date embryoStartTime,Date nextEmbryoStartTime,LhEngineScheduleResult lhEngineScheduleResult, StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append("【开汽标记】初始化过程中，结合硫化结束时间和胎胚开始供应时间,符合标记开汽逻辑.>").append(division);
        //开汽及开汽+热模的时间跨班---Start
        //判断开汽及开汽+热模的时间是否存在跨班的情况，若跨班，两个班次均标记开汽 add by pancd 2023.05.17
        LhClassShiftEnum curShiftCls=timeInClassShift(embryoStartTime);
        LhClassShiftEnum nextShiftCls=timeInClassShift(nextEmbryoStartTime);
        if (curShiftCls != null && nextShiftCls != null && !curShiftCls.equals(nextShiftCls)){
            //跨班，后班次做开汽班次标记
            LhEngineScheduleUtils.setClassShiftOpenStreamFlag(lhEngineScheduleResult,nextShiftCls,null!=nextShiftCls);
            //结合开汽时间与班次结束时间
            calcOpenStreamClassMaxPlanQty(nextShiftCls,lhEngineScheduleResult,nextEmbryoStartTime,logDetail,orderNoLog);
            logDetail.append(StringUtils.format("【开汽热模班次】,存在跨班,开汽热模班次：【{}】！",nextShiftCls.getClassName())).append(division);
            orderNoLog.append(StringUtils.format("【开汽热模班次】,存在跨班,开汽热模班次：【{}】！",nextShiftCls.getClassName())).append(division);
        }
        //开汽及开汽+热模的时间跨班---End

        //判断时间是否在班次时间范围内，如果是进行标注，如果不是，不进行开汽
        //LhClassShiftEnum nextShiftCls=timeInClassShift(nextEmbryoStartTime);
        String shiftClsName=curShiftCls==null?"【超过计划班次】":"【"+curShiftCls.getClassName()+"】";
        //开汽班次标记
        LhEngineScheduleUtils.setClassShiftOpenStreamFlag(lhEngineScheduleResult,curShiftCls,null!=curShiftCls);
        orderNoLog.append(StringUtils.format("【开汽标记】,当前胎胚开始供应时间：【{}】，所在的班次：【{}】，是否进行开汽标记：【{}】",DateUtil.formatDatetime(nextEmbryoStartTime),shiftClsName,null!=nextShiftCls)).append(division);
        //开汽班次计划上限计算
        if(curShiftCls!=null){
            //结合开汽时间与班次结束时间
            if (!curShiftCls.equals(nextShiftCls)){
                //若存在跨班，开汽当前班次的最大计划量置0
                LhEngineScheduleUtils.setClassShiftMaxPlanQty(lhEngineScheduleResult,curShiftCls,0);
            }else{
                //同班次，开汽实际时间+热模时间
                calcOpenStreamClassMaxPlanQty(curShiftCls,lhEngineScheduleResult,nextEmbryoStartTime,logDetail,orderNoLog);
            }

            lhEngineScheduleResult.setPlanSort(2);//开汽生产顺序

            //停汽标记逻辑：当前班次往前的有计划量班次标记为停汽 start
            int openStreamIndex=curShiftCls.getClassIndex();
            logDetail.append("判断前班次是否需要停汽.>").append(division);
            orderNoLog.append("判断前班次是否需要停汽").append(division);
            while(openStreamIndex>0){
                //前一个班次
                openStreamIndex-=1;
                LhClassShiftEnum stopShiftCls=LhClassShiftEnum.getClassShiftByClassIndex(openStreamIndex);
                if(stopShiftCls!=null){
                    logDetail.append(StringUtils.format("【开汽判断前规格停汽】,停汽班次：【{}】,该班次计划量：【{}】",stopShiftCls.getClassName(),LhEngineScheduleUtils.getLhClassPlanQty(lhEngineScheduleResult,stopShiftCls))).append(division);
                    orderNoLog.append(StringUtils.format("【开汽判断前规格停汽】,停汽班次：【{}】,该班次计划量：【{}】",stopShiftCls.getClassName(),LhEngineScheduleUtils.getLhClassPlanQty(lhEngineScheduleResult,stopShiftCls))).append(division);
                    boolean isShiftChangeMold= LhEngineScheduleUtils.getClassShiftChangeMoldFlag(lhEngineScheduleResult,stopShiftCls);
                    logDetail.append(StringUtils.format("【开汽判断前规格停汽】,停汽班次：【{}】,当前班次换膜标记：【{}】",stopShiftCls.getClassName(),isShiftChangeMold)).append(division);
                    orderNoLog.append(StringUtils.format("【开汽判断前规格停汽】,停汽班次：【{}】,当前班次换膜标记：【{}】",stopShiftCls.getClassName(),isShiftChangeMold)).append(division);
                    if(!isShiftChangeMold){
                        //标记停汽原因分析
                        markStopStreamAnalysis(lhEngineScheduleResult,stopShiftCls);
                    }else{
                        logDetail.append(StringUtils.format("【开汽判断前规格停汽】,停汽班次：【{}】,当前班次已经标记换模，不进行停汽标记！",stopShiftCls.getClassName())).append(division);
                        orderNoLog.append(StringUtils.format("【开汽判断前规格停汽】,停汽班次：【{}】,当前班次已经标记换模，不进行停汽标记！",stopShiftCls.getClassName())).append(division);
                    }

                    //Joran 2022-06-23 一直往前打停汽标记，如果有量之后就不打了
                    if(LhEngineScheduleUtils.getLhClassPlanQty(lhEngineScheduleResult,stopShiftCls)>0){
                        break;
                    }
                }
            }
            //停汽标记逻辑：当前班次往前的有计划量班次标记为停汽 end

        }
    }

    /**
     * 计算班次最大计划量上限
     * @param shiftCls
     * @param lhEngineScheduleResult
     * @param embryoStartTime
     */
    private void calcOpenStreamClassMaxPlanQty(LhClassShiftEnum shiftCls, LhEngineScheduleResult lhEngineScheduleResult, Date embryoStartTime,StringBuilder logDetail,StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【开汽】开始计算班次：【{}】，胎胚开始供应时间：【{}】,最大计划量逻辑",shiftCls.getClassName(),DateUtil.formatDatetime(embryoStartTime))).append(division);
        orderNoLog.append(StringUtils.format("【开汽】开始计算班次：【{}】，胎胚开始供应时间：【{}】,最大计划量逻辑",shiftCls.getClassName(),DateUtil.formatDatetime(embryoStartTime))).append(division);
        Date endTime=getShiftEndTimeByClass(shiftCls);
        orderNoLog.append(StringUtils.format("【开汽】获取到的班次结束时间：【{}】",DateUtil.formatDatetime(endTime))).append(division);
        //使用模数
        Integer maxPlanQty=getClassMaxPlanQty(shiftCls,lhEngineScheduleResult,embryoStartTime,endTime,logDetail,orderNoLog);
        LhEngineScheduleUtils.setClassShiftMaxPlanQty(lhEngineScheduleResult,shiftCls,maxPlanQty);
        logDetail.append(StringUtils.format("【开汽】开始计算班次：【{}】，最大计划量：【{}】",shiftCls.getClassName(),maxPlanQty)).append(division);
        orderNoLog.append(StringUtils.format("【开汽】开始计算班次：【{}】，最大计划量：【{}】",shiftCls.getClassName(),maxPlanQty)).append(division);
    }

    /**
     * 根据时间段可安排的任务最大量
     * @param shiftCls
     * @param lhEngineScheduleResult
     * @param startTime
     * @param endTime
     * @param logDetail
     * @return
     */
    private Integer getClassMaxPlanQty(LhClassShiftEnum shiftCls, LhEngineScheduleResult lhEngineScheduleResult,Date startTime,Date endTime,StringBuilder logDetail,StringBuilder orderNoLog){
        Date shiftBeginTime=getShiftBeginTimeByClass(shiftCls);
        Date shiftEndTime=getShiftEndTimeByClass(shiftCls);
        Integer maxPlanQty=BigDecimal.ZERO.intValue();
        if(startTime.getTime()<=shiftBeginTime.getTime()){
            startTime=shiftBeginTime;
        }
        if(shiftEndTime.getTime()<=endTime.getTime()){
            endTime=shiftEndTime;
        }
        logDetail.append(StringUtils.format("【班次最大计划量】,当前班次:【{}】,开始时间：【{}】，结束时间：【{}】",shiftCls.getClassName(),DateUtil.formatDatetime(startTime),DateUtil.formatDatetime(endTime))).append(division);
        orderNoLog.append(StringUtils.format("【班次最大计划量】,当前班次:【{}】,开始时间：【{}】，结束时间：【{}】",shiftCls.getClassName(),DateUtil.formatDatetime(startTime),DateUtil.formatDatetime(endTime))).append(division);

        if(shiftBeginTime.equals(startTime)&&shiftEndTime.equals(endTime)){
            Integer quota=lhEngineScheduleResult.getQuota();
            logDetail.append(StringUtils.format("【班次最大计划量】,当前班次:【{}】,可用时间为整班,直接给定机台定额为最大班次计划量,定额：【{}】",shiftCls.getClassName(),quota)).append(division);
            orderNoLog.append(StringUtils.format("【班次最大计划量】,当前班次:【{}】,可用时间为整班,直接给定机台定额为最大班次计划量,定额：【{}】",shiftCls.getClassName(),quota)).append(division);
            maxPlanQty=quota;
            return maxPlanQty;
        }

        /**
         * Joran 2022-07-08 处理数据异常，结束时间在开始时间在前的直接不计算计划量给0
         */
        if(endTime.getTime()<startTime.getTime()){
            return 0;
        }

        //使用模数
        Integer useMoldNumber=lhEngineScheduleResult.getUseMoldNumber();
        Double lhTime=lhEngineScheduleResult.getLhTime();
        //调用时间计算计划量
        maxPlanQty=calcMaxPlanQtyByTime(useMoldNumber,lhTime,startTime,endTime);
        logDetail.append(StringUtils.format("【班次最大计划量】,当前班次:【{}】,获取到的最大可排计划量：【{}】",shiftCls.getClassName(),maxPlanQty)).append(division);
        orderNoLog.append(StringUtils.format("【班次最大计划量】,当前班次:【{}】,获取到的最大可排计划量：【{}】",shiftCls.getClassName(),maxPlanQty)).append(division);
        return maxPlanQty;
    }

    /**
     * 根据时间、模数、硫化时长等进行计划量计算
     * @param useMoldNumber
     * @param lhTime
     * @param startTime
     * @param endTime
     * @return
     */
    private Integer calcMaxPlanQtyByTime(Integer useMoldNumber, Double lhTime, Date startTime, Date endTime) {
        Integer maxPlanQty=BigDecimal.ZERO.intValue();
        Integer brushBagTime=lhCommonService.getBrushBagTime(cxParamsMap);
        //班次计划上限=(结束时间-开始时间)/硫化时间 * 模数
        Long diffMin= LhEngineScheduleUtils.diffDate(startTime,endTime, LhEngineScheduleUtils.MINUTES);
        //时间差/硫化时间 向下取整。再乘以模数
        maxPlanQty=BigDecimal.valueOf(diffMin).divide(BigDecimal.valueOf(lhTime+brushBagTime),0, RoundingMode.CEILING).multiply(BigDecimal.valueOf(useMoldNumber)).setScale(0,RoundingMode.CEILING).intValue();
        //根据单双模来调整计划量，只有单模才允许出现单数，否则向上补双
        maxPlanQty= LhEngineScheduleUtils.getMaxPlanQtyByMoldNumber(maxPlanQty,useMoldNumber);
        return maxPlanQty;
    }

    /**
     * 判断时间是否在时间范围内
     * @param embryoStartTime
     * @return
     */
    private LhClassShiftEnum timeInClassShift(Date embryoStartTime) {
        LhClassShiftEnum shiftCls=null;
        if(embryoStartTime==null){
            return shiftCls;
        }
        //遍历所有班次
       for(LhClassShiftEnum cls:LhClassShiftEnum.values()){
         Date startTime=getShiftBeginTimeByClass(cls);
         Date endTime=getShiftEndTimeByClass(cls);
         if(startTime.getTime()<=embryoStartTime.getTime() && endTime.getTime()>= embryoStartTime.getTime()){
             shiftCls= cls;
             break;
         }
       }
       return shiftCls;
    }

    /**
     * 根据胎胚代码获取胎胚开始时间
     * @param embryoCode
     * @param logDetail
     * @return
     */
    private Date getEmbryoCodeStartTime(String embryoCode, StringBuilder logDetail) {
        Date embryoStartTime=null;
        logDetail.append("开始根据胎胚代码获取胎胚开始时间逻辑，当前胎胚代码：").append(embryoCode).append(division);
        if(StringUtils.isNotEmpty(lhEmbryoSupportTimeMap)&&lhEmbryoSupportTimeMap.containsKey(embryoCode)){
            List<LhSapEmbryoTime> lhSapEmbryoTimes=lhEmbryoSupportTimeMap.get(embryoCode);
            //根据开始时间升序排序,取第一条
            Comparator<LhSapEmbryoTime> startTimeAsc = Comparator.comparing(LhSapEmbryoTime::getEstimateStartTime);
            Collections.sort(lhSapEmbryoTimes,startTimeAsc);
            embryoStartTime=lhSapEmbryoTimes.get(0).getEstimateStartTime();
        }
        logDetail.append(StringUtils.format("当前胎胚代码：【{}】,获取到的胎胚开始时间：【{}】",embryoCode,embryoStartTime==null?"空":DateUtil.formatDatetime(embryoStartTime))).append(division);
        return embryoStartTime;
    }

    /**
     * 结合班次获取对应班次的轮胎供应时间
     * @param embryoCode
     * @param cls
     * @param logDetail
     * @return
     */
    private Date getEmbryoCodeStartTimeByClassShift(String embryoCode,Integer estimateShiftStock,LhClassShiftEnum cls, StringBuilder logDetail){
        Date embryoStartTime=null;
        logDetail.append(StringUtils.format("【班次胎胚供应时间获取】，当前胎胚：【{}】,当前班次：【{}】",embryoCode,cls==null?"昨日白班":cls.getClassName())).append(division);
        if(StringUtils.isNotEmpty(lhEmbryoSupportTimeMap)&&lhEmbryoSupportTimeMap.containsKey(embryoCode)){
            List<LhSapEmbryoTime> lhSapEmbryoTimes=lhEmbryoSupportTimeMap.get(embryoCode);
            //根据开始时间升序排序,取第一条
            Comparator<LhSapEmbryoTime> startTimeAsc = Comparator.comparing(LhSapEmbryoTime::getEstimateStartTime);
            Collections.sort(lhSapEmbryoTimes,startTimeAsc);
            Date shiftBeginTime=getShiftBeginTimeByClass(cls);
            Date shiftEndTime=getShiftEndTimeByClass(cls);
            for(LhSapEmbryoTime lhSapEmbryoTime:lhSapEmbryoTimes){
                //胎胚预计开始时间
                Date estimateStartTime=lhSapEmbryoTime.getEstimateStartTime();
                //胎胚预计结束时间
                Date estimateEndTime=lhSapEmbryoTime.getEstimateEndTime();
                //分段的情况，如果是预计结束时间在班次之前的直接取下一段
                if(estimateEndTime.getTime() < shiftBeginTime.getTime()){
                    logDetail.append(StringUtils.format("【胎胚前段】胎胚代码：【{}】，预计结束时间：【{}】,班次开始时间：【{}】,当前胎胚时间段不可用",embryoCode,DateUtil.formatDatetime(estimateEndTime),DateUtil.formatDatetime(shiftBeginTime))).append(division);

                    continue;
                }
                //分段的后半段，胎胚开始时间在班次结束时间的也不拿
                if(estimateStartTime.getTime() > shiftEndTime.getTime()){
                    logDetail.append(StringUtils.format("【胎胚后段】胎胚代码：【{}】，预计开始时间：【{}】,班次结束时间：【{}】,当前胎胚时间段不可用",embryoCode,DateUtil.formatDatetime(estimateStartTime),DateUtil.formatDatetime(shiftEndTime))).append(division);
                    continue;
                }

                //小于班次的开始时间则为 班次开始时间
                if(estimateStartTime.getTime()<= shiftBeginTime.getTime()){
                    logDetail.append(StringUtils.format("【班次时段】胎胚代码：【{}】，预计开始时间：【{}】,班次开始时间：【{}】,当前胎胚开始时间为班次开始时间",embryoCode,DateUtil.formatDatetime(estimateStartTime),DateUtil.formatDatetime(shiftBeginTime))).append(division);
                    embryoStartTime=shiftBeginTime;
                }else{
                    logDetail.append(StringUtils.format("【胎胚时段】胎胚代码：【{}】，预计开始时间：【{}】,班次开始时间：【{}】,当前胎胚开始时间为胎胚时间",embryoCode,DateUtil.formatDatetime(estimateStartTime),DateUtil.formatDatetime(shiftBeginTime))).append(division);

                    embryoStartTime=estimateStartTime;
                }
            }
        }
        //预先更新班次库存
        //updateEstimateLhClassEmbryoStock(embryoCode,cls,logDetail,new StringBuilder());
        //如果胎胚时间为空，查看预计库存是否可用，如果可用的话，直接拿当前班次的开始时间为胎胚时间
        if(embryoStartTime==null&&estimateShiftStock>0){
            embryoStartTime=getShiftBeginTimeByClass(cls);
            logDetail.append(StringUtils.format("【换模开班】：当前成型未获取到胎胚开始时间，但是班次有预计可用库存：【{}】，胎胚开始时间重置为当前班次开始时间：【{}】",estimateShiftStock,DateUtil.formatDatetime(embryoStartTime))).append(division);
        }

        return embryoStartTime;
    }

    /**
     * 初始化默认时间为中班开班时间
     * @param lastScheduleDate
     * @return
     */
    private Date initLastEndTime(Date lastScheduleDate) {
        lastScheduleDate= LhEngineScheduleUtils.formatDateByZero(lastScheduleDate);
        lastScheduleDate=DateUtils.addHours(lastScheduleDate,16);
        return  lastScheduleDate;
    }

    /**
     * 添加初始化规格根据昨日换模计划信息
     * @param lastDayChangeMoldMap
     * @param machineCode
     * @param lhEngineScheduleResultList
     * @param logDetail
     */
    private void addTaskByLastDayMoldChangePlan(Map<String, List<LhApsMoldAdjustPlan>> lastDayChangeMoldMap, String machineCode,String machineName,Date autoScheduleDate,String lhBatchNo, List<LhEngineScheduleResult> lhEngineScheduleResultList, StringBuilder logDetail) {
        if(StringUtils.isNotEmpty(lastDayChangeMoldMap)&&lastDayChangeMoldMap.containsKey(machineCode)){
            List<LhApsMoldAdjustPlan> lhApsMoldAdjustPlans=lastDayChangeMoldMap.get(machineCode);
            if(StringUtils.isNotEmpty(lhApsMoldAdjustPlans)&&lhApsMoldAdjustPlans.size()>1){
                logDetail.append(StringUtils.format("当前机台：【{}】，存在昨日存在多条换模计划，产生多条规格填充逻辑",machineName)).append(division);
                //Joran 2022-07-01 遍历换模计划如果都不执行的直接通过在产规格初始化start
                List<LhApsMoldAdjustPlan> notExecuteList=new ArrayList<>();
                for(LhApsMoldAdjustPlan lhApsMoldAdjustPlan:lhApsMoldAdjustPlans) {
                    String isExecute=lhApsMoldAdjustPlan.getIsExecute();
                    Boolean executeStatus=LhEngineConstants.MOLD_EXECUTE_STATUS_YES.equals(isExecute);
                    //如果标记为不执行的进行不执行记录
                    if(!executeStatus){
                        notExecuteList.add(lhApsMoldAdjustPlan);
                    }
                }
                //全部都不执行的直接根据在产规格初始化start
                if(notExecuteList.size()==lhApsMoldAdjustPlans.size()){
                    logDetail.append(StringUtils.format("当前机台：【{}】，昨日换模计划不执行，通过在产规格进行初始化逻辑",machineName)).append(division);
                    addTaskByLhInProductSpec(machineCode,machineName,autoScheduleDate,lhBatchNo,lhEngineScheduleResultList,logDetail);
                }else{
                      //移除掉不执行的列表
                    if(StringUtils.isNotEmpty(notExecuteList)){
                        lhApsMoldAdjustPlans.removeAll(notExecuteList);
                    }
                      Boolean lastDayChangeMoldFlag=true;
                      Boolean toDayChangeMoldFlag=false;
                      //结合换模计划+在产规格进行初始化任务
                      buildTaskToListByGroup(machineCode,machineName,autoScheduleDate,lhBatchNo,lhEngineScheduleResultList,lhApsMoldAdjustPlans,lastDayChangeMoldFlag,toDayChangeMoldFlag,logDetail);
                }
                //全部都不执行的直接根据在产规格初始化end

                //Joran 2022-07-01 遍历换模计划如果都不执行的直接通过在产规格初始化end

            }else if(StringUtils.isNotEmpty(lhApsMoldAdjustPlans)&&lhApsMoldAdjustPlans.size() == 1){
                logDetail.append(StringUtils.format("当前机台：【{}】，昨日存在单条换模计划，产生单条规格填充逻辑",machineName)).append(division);
                //只存在单条在产规格数据
                LhApsMoldAdjustPlan lhApsMoldAdjustPlan=lhApsMoldAdjustPlans.get(0);
                String sapCode="";
                String isExecute=lhApsMoldAdjustPlan.getIsExecute();
                Boolean executeStatus=LhEngineConstants.MOLD_EXECUTE_STATUS_YES.equals(isExecute);
                //换模计划标记为执行逻辑
                if(executeStatus){
                    sapCode=lhApsMoldAdjustPlan.getAfterSapCode();
                    Date changeMoldTime=lhApsMoldAdjustPlan.getChangeMoldTime();
                    Integer moldNumber= LhEngineConstants.TWO_MOLD_NUMBER;
                    appendInitTaskToList(machineCode,machineName,sapCode,moldNumber,true,false,executeStatus,autoScheduleDate,lhBatchNo,null,changeMoldTime,lhEngineScheduleResultList);
                }else{ //如果换模计划不执行就以在产规格进行数据初始化
                    if(machineInProductListMap.containsKey(machineCode)){
                        logDetail.append(StringUtils.format("当前机台：【{}】，昨日换模计划不执行，通过在产规格进行初始化逻辑",machineName)).append(division);
                        addTaskByLhInProductSpec(machineCode,machineName,autoScheduleDate,lhBatchNo,lhEngineScheduleResultList,logDetail);
                    }
                }

            }
        }
    }

    /**
     * 换模计划加在产规格进行组合构建
     * @param machineCode 当前硫化机台编码
     * @param machineName 当前硫化机
     * @param lhBatchNo 硫化批次号
     * @param lhEngineScheduleResultList 构建数据集合
     * @param lhApsMoldAdjustPlans 换模计划列表
     * @param lastDayChangeMoldFlag 昨日换模计划标记
     * @param toDayChangeMoldFlag 今日换模计划标记
     */
    private void buildTaskToListByGroup(String machineCode, String machineName,Date autoScheduleDate, String lhBatchNo, List<LhEngineScheduleResult> lhEngineScheduleResultList, List<LhApsMoldAdjustPlan> lhApsMoldAdjustPlans, Boolean lastDayChangeMoldFlag, Boolean toDayChangeMoldFlag,StringBuilder logDetail) {
        //存在多条在产规格
        Set<String> sapCodeSet=new HashSet<>();//存在2换1只需要初始化一条
        logDetail.append("【组合换模计划+在产规格】进行数据初始化逻辑").append(division);
        if(StringUtils.isNotEmpty(lhApsMoldAdjustPlans)&&lhApsMoldAdjustPlans.size() > 1){
            //遍历剩余执行的换膜计划进行计划安排start
            for(LhApsMoldAdjustPlan lhApsMoldAdjustPlan:lhApsMoldAdjustPlans) {
                String sapCode =lhApsMoldAdjustPlan.getAfterSapCode();
                String isExecute=lhApsMoldAdjustPlan.getIsExecute();
                Boolean executeStatus=LhEngineConstants.MOLD_EXECUTE_STATUS_YES.equals(isExecute);
                Date changeMoldTime = lhApsMoldAdjustPlan.getChangeMoldTime();
                String leftRightMold = lhApsMoldAdjustPlan.getLeftRightMold();
                Integer useMoldNumber = BigDecimal.ONE.intValue();
                if(sapCodeSet.contains(sapCode)){
                    continue;
                }
                if (StringUtils.isEmpty(leftRightMold)) {//属于后规格1个，但是模具变动单两条，所以就初始化一条排程即可
                    useMoldNumber = LhEngineConstants.TWO_MOLD_NUMBER;
                }
                appendInitTaskToList(machineCode, machineName, sapCode, useMoldNumber, lastDayChangeMoldFlag, toDayChangeMoldFlag,executeStatus, autoScheduleDate, lhBatchNo, leftRightMold, changeMoldTime, lhEngineScheduleResultList);
                sapCodeSet.add(sapCode);
            }
            //遍历剩余执行的换膜计划进行计划安排end

        }else if(StringUtils.isNotEmpty(lhApsMoldAdjustPlans)&&lhApsMoldAdjustPlans.size() == 1){ //换模计划多条只执行一条那就需要集合在产规格进行数据补充
            logDetail.append(StringUtils.format("【多条换模计划组合】,当前机台：【{}】，昨日换模计划只执行单条，产生单条规格填充逻辑",machineName)).append(division);
            //只执行单条换模计划
            LhApsMoldAdjustPlan lhApsMoldAdjustPlan=lhApsMoldAdjustPlans.get(0);
            String sapCode=lhApsMoldAdjustPlan.getAfterSapCode();
            String leftRightMold = lhApsMoldAdjustPlan.getLeftRightMold();
            Integer useMoldNumber = BigDecimal.ONE.intValue();
            Date changeMoldTime = lhApsMoldAdjustPlan.getChangeMoldTime();
            appendInitTaskToList(machineCode, machineName, sapCode, useMoldNumber, lastDayChangeMoldFlag, toDayChangeMoldFlag,true, autoScheduleDate, lhBatchNo, leftRightMold, changeMoldTime, lhEngineScheduleResultList);
            sapCodeSet.add(sapCode);
            //存在在产规格数据
            if(StringUtils.isNotEmpty(machineInProductListMap)&&machineInProductListMap.containsKey(machineCode)){
                //如果在产规格是两个规格
                List<LhInProductionSpec> lhInProductionSpecList=machineInProductListMap.get(machineCode);
                //如果在产规格是1条的话直接拆成单模原规格还是继续
               if(StringUtils.isNotEmpty(lhInProductionSpecList)&&lhInProductionSpecList.size()==1){
                   LhInProductionSpec lhInProductionSpec=lhInProductionSpecList.get(0);
                   if (checkEmptyMold(lhInProductionSpec,logDetail)){
                       //空模，不初始任务
                       return;
                   }
                   String productSapCode=lhInProductionSpec.getSapCode();
                   String inLeftRightMold="L".equalsIgnoreCase(leftRightMold)?"R":"L";
                   appendInitTaskToList(machineCode,machineName,productSapCode,useMoldNumber,false,false,false,autoScheduleDate,lhBatchNo,inLeftRightMold,null,lhEngineScheduleResultList);
               }else{
                   //存在多条在产规格,就遇到与换模计划单同边的不处理，继续下一个在产规格
                   for(LhInProductionSpec lhInProductionSpec:lhInProductionSpecList){
                       String inLeftRightMold=lhInProductionSpec.getLeftRightMold();
                       String productSapCode=lhInProductionSpec.getSapCode();
                       if(inLeftRightMold.equalsIgnoreCase(leftRightMold)){
                           continue;
                       }
                       if(sapCodeSet.contains(productSapCode)){
                           continue;
                       }
                       if (checkEmptyMold(lhInProductionSpec,logDetail)){
                           //空模，不初始任务
                           continue;
                       }
                       appendInitTaskToList(machineCode,machineName,productSapCode,useMoldNumber,false,false,false,autoScheduleDate,lhBatchNo,inLeftRightMold,null,lhEngineScheduleResultList);
                   }
               }
            }
        }

    }

    /**
     * 添加初始任务到排程结果列表中
     * @param machineCode 硫化机台编号
     * @param sapCode sap品号
     * @param moldNumber 模数
     * @param lhEngineScheduleResultList 硫化排程结果
     */
    private void appendInitTaskToList(String machineCode,String machineName, String sapCode, Integer moldNumber,Boolean lastDayChangeMoldFlag,Boolean toDayChangeMoldFlag,Boolean changMoldFlag,Date autoScheduleDate,String lhBatchNo,String leftRightMold,Date changeMoldTime,List<LhEngineScheduleResult> lhEngineScheduleResultList) {
        LhEngineScheduleResult lhEngineScheduleResult=createLhScheduleResult(machineCode,machineName,sapCode,moldNumber,autoScheduleDate,lhBatchNo,leftRightMold);
        lhEngineScheduleResult.setLastDayChangeMoldFlag(lastDayChangeMoldFlag);
        lhEngineScheduleResult.setChangeMoldFlag(changMoldFlag);//标记为不是换模
        lhEngineScheduleResult.setToDayChangeMoldFlag(toDayChangeMoldFlag);
        lhEngineScheduleResult.setChangeMoldTime(changeMoldTime); //如果是换模计划初始化的记录换模时间,后面会使用到
        lhEngineScheduleResultList.add(lhEngineScheduleResult);

        //Joran 2022-06-20 提出单个工单号过程日志需要记录 start
        if(lastDayChangeMoldFlag){
            //初始化工单对应的日志对象信息
            initOrderNoLogMap(lhEngineScheduleResult,"【昨日换模计划初始化】");
        }else if(toDayChangeMoldFlag){
            //初始化工单对应的日志对象信息
            initOrderNoLogMap(lhEngineScheduleResult,"【今日换模计划初始化】");
        }else{
            initOrderNoLogMap(lhEngineScheduleResult,"【在产规格初始化】");
        }
        //Joran 2022-06-20 提出单个工单号过程日志需要记录 end

    }

    /**
     * 初始化排程时进行工单日志初始化
     * @param lhEngineScheduleResult
     */
    private void initOrderNoLogMap(LhEngineScheduleResult lhEngineScheduleResult,String fromTag) {
        String orderNo=lhEngineScheduleResult.getOrderNo();
        if(orderLogMap!=null&&!orderLogMap.containsKey(orderNo)){
            StringBuilder orderNoLog=new StringBuilder();
            orderNoLog.append(StringUtils.format("【工单日志初始化】：当前工单号：【{}】，初始化数据来源：【{}】",orderNo,fromTag)).append(division);
            orderLogMap.put(orderNo,orderNoLog);
        }
    }

    /**
     * 创建硫化排程结果对象
     * @param machineCode
     * @param machineName
     * @param sapCode
     * @param moldNumber
     * @param autoScheduleDate
     * @param lhBatchNo
     * @param leftRightMold
     * @return
     */
    private LhEngineScheduleResult createLhScheduleResult(String machineCode,String machineName, String sapCode, Integer moldNumber,Date autoScheduleDate,String lhBatchNo,String leftRightMold){
        LhEngineScheduleResult lhEngineScheduleResult = new LhEngineScheduleResult();
        //添加批次号
        lhEngineScheduleResult.setBatchNo(lhBatchNo);
        //添加工单号
        lhEngineScheduleResult.setOrderNo(lhCommonService.createOrderNo(LhEngineConstants.LH_AUTO_ORDER_NO_PREFIX,DateUtils.parseDateToStr("yyyyMMdd",autoScheduleDate)));
        lhEngineScheduleResult.setLhMachineCode(machineCode);
        lhEngineScheduleResult.setLhMachineName(machineName);
        lhEngineScheduleResult.setSapCode(sapCode);
        lhEngineScheduleResult.setLeftRightMold(leftRightMold);
        lhEngineScheduleResult.setScheduleDate(autoScheduleDate);
        lhEngineScheduleResult.setUseMoldNumber(moldNumber);//模数
        lhEngineScheduleResult.setClass1PlanQty(0);
        lhEngineScheduleResult.setClass2PlanQty(0);
        lhEngineScheduleResult.setClass3PlanQty(0);
        lhEngineScheduleResult.setCreateBy(SecurityUtils.getUsername());
        lhEngineScheduleResult.setProductionStatus(LhEngineConstants.LH_SCHEDULE_PRODUCT_STATUS_UNDO);//未生产
        lhEngineScheduleResult.setIsRelease(LhEngineConstants.LH_SCHEDULE_IS_RELEASE_NO);//未发布
        return lhEngineScheduleResult;
    }

    /**
     * 构建昨日换模计划数据
     * @param logDetail
     */
    private void createLastDayChangeMoldMap(StringBuilder logDetail) {
        logDetail.append("进行构建昨日换模计划数据").append(division);
        if(StringUtils.isNotEmpty(lastDayLhApsMoldAdjustPlanList)){
            lastDayChangeMoldMap=initChangeMoldMap(lastDayLhApsMoldAdjustPlanList,logDetail);
        }
    }

    /**
     * 根据传入的换膜计划信息进行换模数据组装
     * @param lhApsMoldAdjustPlanList
     * @param logDetail
     * @return
     */
    private Map<String,List<LhApsMoldAdjustPlan>> initChangeMoldMap(List<LhApsMoldAdjustPlan> lhApsMoldAdjustPlanList, StringBuilder logDetail) {
        logDetail.append("开始构建昨日换模计划数据>>>").append(division);
        Map<String,List<LhApsMoldAdjustPlan>> lastDayMoldMap=CollectionUtil.toMapList(lhApsMoldAdjustPlanList, LhApsMoldAdjustPlan::getLhMachineCode);
        //处理如果存在多次换模的保留时间最大的换膜记录，最多两条start
        Map<String,List<LhApsMoldAdjustPlan>> changeMoldMap=new HashMap<>();
        for(Map.Entry<String,List<LhApsMoldAdjustPlan>> entry:lastDayMoldMap.entrySet()){
            String lhMachineCode=entry.getKey();
            List<LhApsMoldAdjustPlan> moldAdjustPlans=entry.getValue();
            Set<String> existSapCode=new HashSet<>();
            if(StringUtils.isNotEmpty(moldAdjustPlans)&&moldAdjustPlans.size()> 1){
                logDetail.append(StringUtils.format("当前硫化机编号：【{}】,属于多条换模记录逻辑，筛选处理...",lhMachineCode)).append(division);
                //根据换模时间进行倒序排序取同SAP品号的取换模时间比较大的
                Comparator<LhApsMoldAdjustPlan> changeMoldTimeDesc = Comparator.comparing(LhApsMoldAdjustPlan::getChangeMoldTime).reversed();
                Collections.sort(moldAdjustPlans,changeMoldTimeDesc);
                int count=1;
                List<LhApsMoldAdjustPlan> moldAdjustPlanList=new ArrayList<>(2);
                for(LhApsMoldAdjustPlan lhApsMoldAdjustPlan:moldAdjustPlans){
                    String sapCode=lhApsMoldAdjustPlan.getAfterSapCode();
                    if(existSapCode.contains(sapCode)){
                        changeMoldMap.put(lhMachineCode,moldAdjustPlanList);
                        break;
                    }
                    if(count==moldAdjustPlans.size()){
                        moldAdjustPlanList.add(lhApsMoldAdjustPlan);
                        changeMoldMap.put(lhMachineCode,moldAdjustPlanList);
                        break;
                    }
                    moldAdjustPlanList.add(lhApsMoldAdjustPlan);
                    existSapCode.add(sapCode);
                    count+=1;
                }
            }else{
                logDetail.append(StringUtils.format("当前硫化机编号：【{}】,属于单条换模记录逻辑，无需处理...",lhMachineCode)).append(division);
                //如果只是单条的规格，则直接添加
                changeMoldMap.put(lhMachineCode,moldAdjustPlans);
            }
        }
        logDetail.append(StringUtils.format("处理后的结果集：【{}】",toJSONString(changeMoldMap))).append(division);
        //处理如果存在多次换模的保留时间最大的换膜记录，最多两条end
        return changeMoldMap;
    }

    /**
     * 根据今日模具变动单进行硫化排程规格数据填充
     * @param lhEngineScheduleResultList 初始化排程结果
     * @param logDetail
     */
    private void dataFillingByTodayChangeMold(List<LhEngineScheduleResult> lhEngineScheduleResultList,Date autoScheduleDate,String lhBatchNo, StringBuilder logDetail) {
        logDetail.append("开始结合今日换模计划进行硫化排程规格填充").append(division);
        if(StringUtils.isNotEmpty(sapLhApsMoldAdjustPlanMap)){
            //根据硫化机台编号进行分组
            Map<String, List<LhApsMoldAdjustPlan>> lhApsMoldAdjustPlanMap = sapLhApsMoldAdjustPlanMap;
            for(LhMachineInfo lhMachineInfo:lhMachineInfoList){
                String machineCode=lhMachineInfo.getMachineCode();//硫化机台
                String machineName=lhMachineInfo.getMachineName();//硫化机台
                if(StringUtils.isEmpty(lhApsMoldAdjustPlanMap)||!lhApsMoldAdjustPlanMap.containsKey(machineCode)){
                    logDetail.append(StringUtils.format("当前机台：【{}】，没找到当日换模计划数据，跳过不自动填充",machineName)).append(division);
                    continue;
                }
                List<LhApsMoldAdjustPlan> lhApsMoldAdjustPlans=lhApsMoldAdjustPlanMap.get(machineCode);
                if(StringUtils.isNotEmpty(lhApsMoldAdjustPlans)&&lhApsMoldAdjustPlans.size()>1){
                    logDetail.append(StringUtils.format("当前机台：【{}】，存在多条换模计划，产生多条规格填充逻辑",machineName)).append(division);
                     Set<String> sapCodeSet=new HashSet<>();//存在2换1只需要初始化一条
                    //存在多条在产规格
                    for(LhApsMoldAdjustPlan lhApsMoldAdjustPlan:lhApsMoldAdjustPlans){
                        String sapCode="";
                        String isExecute=lhApsMoldAdjustPlan.getIsExecute();
                        String leftRightMold=lhApsMoldAdjustPlan.getLeftRightMold();
                        Date changeMoldTime=lhApsMoldAdjustPlan.getChangeMoldTime();
                        Boolean executeStatus=LhEngineConstants.MOLD_EXECUTE_STATUS_YES.equals(isExecute);
                        if(executeStatus){
                            sapCode=lhApsMoldAdjustPlan.getAfterSapCode();
                        }else{
                            //换模规格不执行，换模时间置空，不做开班逻辑
                            sapCode=lhApsMoldAdjustPlan.getBeforeSapCode();
                        }
                        Integer useMoldNumber = BigDecimal.ONE.intValue();
                        if(sapCodeSet.contains(sapCode)){
                            continue;
                        }
                        if (StringUtils.isEmpty(leftRightMold)) {//属于后规格1个，但是模具变动单两条，所以就初始化一条排程即可
                            useMoldNumber = LhEngineConstants.TWO_MOLD_NUMBER;
                        }
                        appendInitTaskToList(machineCode,machineName,sapCode,useMoldNumber,false,true,executeStatus,autoScheduleDate,lhBatchNo,leftRightMold,changeMoldTime,lhEngineScheduleResultList);
                        sapCodeSet.add(sapCode);
                    }
                }else if(StringUtils.isNotEmpty(lhApsMoldAdjustPlans)&&lhApsMoldAdjustPlans.size() == BigDecimal.ONE.intValue()){
                    logDetail.append(StringUtils.format("当前机台：【{}】，存在单条换模计划，产生单条规格填充逻辑",machineName)).append(division);
                    //只存在单条在产规格数据
                    LhApsMoldAdjustPlan lhApsMoldAdjustPlan=lhApsMoldAdjustPlans.get(0);
                    String isExecute=lhApsMoldAdjustPlan.getIsExecute();
                    String sapCode="";
                    Boolean executeStatus=LhEngineConstants.MOLD_EXECUTE_STATUS_YES.equals(isExecute);
                    if(executeStatus){
                        sapCode=lhApsMoldAdjustPlan.getAfterSapCode();
                    }else{
                        //换模规格不执行，换模时间置空，不做开班逻辑
                        sapCode=lhApsMoldAdjustPlan.getBeforeSapCode();
                    }
                    Date changeMoldTime=lhApsMoldAdjustPlan.getChangeMoldTime();
                    Integer useMoldNumber= LhEngineConstants.TWO_MOLD_NUMBER; //单条默认为双模
                    appendInitTaskToList(machineCode,machineName,sapCode,useMoldNumber,false,true,executeStatus,autoScheduleDate,lhBatchNo,null,changeMoldTime,lhEngineScheduleResultList);
                }
            }
        }
        logDetail.append("结束结合今日换模计划进行硫化排程规格填充排程结果数据集：").append(toJSONString(lhEngineScheduleResultList)).append(division);
    }

    /**
     * 结合成型排程数据进行不同胎胚数据初始化补充
     * @param lhEngineScheduleResultList
     * @param autoScheduleDate
     * @param logDetail
     */
    private void dataFillingByCxEmbryoCode(List<LhEngineScheduleResult> lhEngineScheduleResultList, Date autoScheduleDate,StringBuilder logDetail) {
        logDetail.append("【填充胎胚代码】开始结合今日成型排程计划进行硫化排程规格填充》》》").append(division);
        //对同SAP的成型计划进行分组
        Map<String,List<CxScheduleResult>> sapCodeCxResultListMap=new HashMap<>();
        if(StringUtils.isNotEmpty(cxScheduleResultList)){
            //对同SAP的成型计划进行分组
            sapCodeCxResultListMap=CollectionUtil.toMapList(cxScheduleResultList, cxScheduleResult -> cxScheduleResult.getSapCode());
        }
        List<LhEngineScheduleResult> newAddTaskList =new ArrayList<>();
        //遍历所有初始化完毕的硫化任务start
        for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
            String lhSapCode=lhEngineScheduleResult.getSapCode();//硫化的sap品号
            String orderNo=lhEngineScheduleResult.getOrderNo();//获取工单
            StringBuilder orderNoLog=getOrderLogBuilder(orderNo);
            String lhEmbryoCode="";
            if(StringUtils.isNotEmpty(sapCodeCxResultListMap)&&sapCodeCxResultListMap.containsKey(lhSapCode)){
                List<CxScheduleResult> sapCxScheduleResultList=sapCodeCxResultListMap.get(lhSapCode);
                //胎胚代码进行分组
                Map<String,List<CxScheduleResult>> embryoCodeResultMap=CollectionUtil.toMapList(sapCxScheduleResultList, cxScheduleResult -> cxScheduleResult.getEmbryoCode());
                if(embryoCodeResultMap.size()>1){
                    Set<String> existEmbryoCodeSet=new HashSet<>();
                    for(Map.Entry<String,List<CxScheduleResult>> entry:embryoCodeResultMap.entrySet()){
                        String embryoCode=entry.getKey();
                        if(existEmbryoCodeSet.contains(embryoCode)){
                            continue;
                        }else if(StringUtils.isEmpty(lhEmbryoCode)&&validateEmbryoStock(embryoCode)){
                            lhEngineScheduleResult.setEmbryoCode(embryoCode);
                            lhEmbryoCode=embryoCode;
                            logDetail.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，存在多个胎胚的成型任务，进行胎胚数据填充,胎胚代号：【{}】",lhSapCode,embryoCode)).append(division);
                            //单条工单日志
                            orderNoLog.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，存在多个胎胚的成型任务，进行胎胚数据填充,胎胚代号：【{}】",lhSapCode,embryoCode)).append(division);
                            existEmbryoCodeSet.add(embryoCode);
                            continue;
                        }else if(validateEmbryoStock(embryoCode)){ //多条有库存才进行初始化
                            //单条工单日志
                            orderNoLog.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，存在多个不同的胎胚，自动创建任务：【{}】",lhSapCode,embryoCode)).append(division);
                            //多个胎胚创建新任务
                            //新构建一个胎胚
                            LhEngineScheduleResult newEmbryoCodeLhResult=BeanConverUtil.conver(lhEngineScheduleResult,LhEngineScheduleResult.class);
                            lhEmbryoCode=embryoCode;
                            newEmbryoCodeLhResult.setEmbryoCode(embryoCode);
                            newEmbryoCodeLhResult.setClass1StartTime(null);
                            newEmbryoCodeLhResult.setRemark("同外胎不同胎胚自动创建任务");
                            newEmbryoCodeLhResult.setOrderNo(lhCommonService.createOrderNo(LhEngineConstants.LH_AUTO_ORDER_NO_PREFIX,DateUtils.parseDateToStr("yyyy-MM-dd",autoScheduleDate)));
                            //lhEngineScheduleResultList.add(newEmbryoCodeLhResult);
                            newAddTaskList.add(newEmbryoCodeLhResult);
                            logDetail.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，存在多个胎胚的成型任务，进行胎胚数据填充,创建硫化任务：【{}】",lhSapCode,toJSONString(newEmbryoCodeLhResult))).append(division);
                            //新建任务初始化日志对象
                            initOrderNoLogMap(newEmbryoCodeLhResult,"【同SAP不同胎胚，任务自动创建】");
                            existEmbryoCodeSet.add(embryoCode);
                        }
                    }
                }else{
                    CxScheduleResult cxScheduleResult=sapCxScheduleResultList.get(0);
                    String embryoCode=cxScheduleResult.getEmbryoCode();
                    lhEngineScheduleResult.setEmbryoCode(embryoCode);
                    logDetail.append(StringUtils.format("》当前SAP品号：【{}】，只有一个胎胚的成型任务，进行胎胚数据填充,胎胚代号：【{}】",lhSapCode,embryoCode)).append(division);
                    //单条工单日志
                    orderNoLog.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，只有一个胎胚的成型任务，胎胚代号：【{}】",lhSapCode,embryoCode)).append(division);
                }
            }else{
                addEmbryoCodeByConstructionInfo(lhEngineScheduleResult,newAddTaskList,lhSapCode,lhEmbryoCode,logDetail,orderNoLog);
            }
            //存储工单日志对象
            orderLogMap.put(orderNo,orderNoLog);
        }
        //遍历所有初始化完毕的硫化任务end
        if(StringUtils.isNotEmpty(newAddTaskList)){
            lhEngineScheduleResultList.addAll(newAddTaskList);
        }
        logDetail.append("》》》结合今日成型排程计划进行硫化排程规格填充结束").append(division);
    }

    /**
     * 通过施工进行胎胚代码填充
     * @param lhEngineScheduleResult
     * @param newAddTaskList
     * @param lhSapCode
     * @param logDetail
     * @param orderNoLog
     */
    private void addEmbryoCodeByConstructionInfo(LhEngineScheduleResult lhEngineScheduleResult, List<LhEngineScheduleResult> newAddTaskList, String lhSapCode,String lhEmbryoCode, StringBuilder logDetail, StringBuilder orderNoLog) {
        logDetail.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，没有对应的成型任务信息，通过外胎施工进行填充",lhSapCode)).append(division);
        if(StringUtils.isNotEmpty(sapTireConstructionListMap)&&sapTireConstructionListMap.containsKey(lhSapCode)){
            List<LhEngineTireConstructionInfo> lhTireConstructionInfos=sapTireConstructionListMap.get(lhSapCode);
            if(StringUtils.isNotEmpty(lhTireConstructionInfos)&&lhTireConstructionInfos.size()==1){
                String embryoCode=lhTireConstructionInfos.get(0).getEmbryoCode();
                lhEngineScheduleResult.setEmbryoCode(embryoCode);
                logDetail.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，存在胎胚的外胎施工信息，进行胎胚数据填充,胎胚代号：【{}】",lhSapCode,embryoCode)).append(division);
                //单条工单日志
                orderNoLog.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，只有一个SAP的外胎施工，胎胚代号：【{}】",lhSapCode,embryoCode)).append(division);
            }else if(StringUtils.isNotEmpty(lhTireConstructionInfos)&&lhTireConstructionInfos.size()>1){
                Set<String> existEmbryoCodeSet=new HashSet<>();
                for(LhEngineTireConstructionInfo lhEngineTireConstructionInfo:lhTireConstructionInfos){
                    String embryoCode=lhEngineTireConstructionInfo.getEmbryoCode();
                    //单条工单日志
                    orderNoLog.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，只有多个SAP的外胎施工，胎胚代号：【{}】",lhSapCode,embryoCode)).append(division);
                    if(existEmbryoCodeSet.contains(embryoCode)){
                        continue;
                    }else if(StringUtils.isEmpty(lhEmbryoCode)&&validateEmbryoStock(embryoCode)){
                        lhEmbryoCode=embryoCode;
                        lhEngineScheduleResult.setEmbryoCode(embryoCode);
                        existEmbryoCodeSet.add(embryoCode);
                    }else if(validateEmbryoStock(embryoCode)){
                        //新构建一个胎胚
                        LhEngineScheduleResult newEmbryoCodeLhResult=BeanConverUtil.conver(lhEngineScheduleResult,LhEngineScheduleResult.class);
                        lhEmbryoCode=embryoCode;
                        newEmbryoCodeLhResult.setEmbryoCode(embryoCode);
                        newEmbryoCodeLhResult.setClass1StartTime(null);
                        newEmbryoCodeLhResult.setRemark("同外胎不同胎胚自动创建任务");
                        newEmbryoCodeLhResult.setOrderNo(lhCommonService.createOrderNo(LhEngineConstants.LH_AUTO_ORDER_NO_PREFIX,DateUtils.parseDateToStr("yyyy-MM-dd",lhEngineScheduleResult.getScheduleDate())));
                        newAddTaskList.add(newEmbryoCodeLhResult);
                        logDetail.append(StringUtils.format("【填充胎胚代码】当前SAP品号：【{}】，存在多个胎胚的施工信息，进行胎胚数据填充,创建硫化任务：【{}】",lhSapCode,toJSONString(newEmbryoCodeLhResult))).append(division);
                        //新建任务初始化日志对象
                        initOrderNoLogMap(newEmbryoCodeLhResult,"【结合SAP多条胎胚施工，任务自动创建】");
                        existEmbryoCodeSet.add(embryoCode);
                    }

                }

            }
        }
    }

    /**
     * 获取工单的日志构建器
     * @param orderNo
     * @return
     */
    private StringBuilder getOrderLogBuilder(String orderNo) {
        StringBuilder orderLog=orderLogMap.containsKey(orderNo)?orderLogMap.get(orderNo):new StringBuilder();//获取初始化的工单日志对象
        return orderLog;
    }

    /**
     * 验证库存是否存在
     * @param embryoCode
     * @return
     */
    private boolean validateEmbryoStock(String embryoCode) {
        boolean existStock=StringUtils.isNotEmpty(sapShiftEmbryoStockMap)&&sapShiftEmbryoStockMap.containsKey(embryoCode)&&sapShiftEmbryoStockMap.get(embryoCode)>0;
        return existStock;
    }

    /**
     *  切换下一个班次时，进行班次的预计库存更新，硫化时单班排载的，单次排载会进行扣减，那么更新的话只需要网上加对应成型班次的计划量
     * @param classShift
     * @param logDetail
     */
    private void updateEstimateLhClassEmbryoStock(String embryoCode,LhClassShiftEnum classShift, StringBuilder logDetail,StringBuilder orderNoLog){
       if(classShift==null){
           logDetail.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次信息异常",embryoCode)).append(division);
           orderNoLog.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次信息异常",embryoCode)).append(division);
           return;
       }
       if(StringUtils.isEmpty(sapShiftEmbryoStockMap)){
           logDetail.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次:【{}】,没有预计库存信息",embryoCode,classShift.getClassName())).append(division);
           orderNoLog.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次:【{}】,没有预计库存信息",embryoCode,classShift.getClassName())).append(division);
           return;
       }
       String key=GenerageMapKeyUtils.createMapKey(embryoCode,classShift.getClassIndex()+"");
       if(shiftUpdateCxPlanFlag.containsKey(key)){
           logDetail.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次:【{}】,成型班次计划已添加",embryoCode,classShift.getClassName())).append(division);
           orderNoLog.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次:【{}】,成型班次计划已添加",embryoCode,classShift.getClassName())).append(division);
            return;
       }
       if(StringUtils.isNotEmpty(cxScheduleResultList)){
         //根据外胎品号进行成型任务分组，获取同胎胚的全部任务
         Map<String, List<CxScheduleResult>> sapEmbryoListMap = CollectionUtil.toMapList(cxScheduleResultList, CxScheduleResult::getEmbryoCode);
         if(StringUtils.isNotEmpty(sapEmbryoListMap)){
            for(Map.Entry<String, List<CxScheduleResult>> entry:sapEmbryoListMap.entrySet()){
                String embryoKey=entry.getKey();
                if(embryoKey.equals(embryoCode)){ //只处理当前胎胚
                    if(sapShiftEmbryoStockMap.containsKey(embryoCode)){
                        List<CxScheduleResult> cxScheduleResults=entry.getValue();
                        Integer totalPlanQty=0;//获取班次的全部胎胚计划量
                        //汇总成型班次计划
                        for(CxScheduleResult cxScheduleResult:cxScheduleResults){
                            Integer currentClassPlanQty= LhEngineScheduleUtils.getCxCurrentClassPlanQty(cxScheduleResult,classShift);
                            totalPlanQty+=currentClassPlanQty;
                        }
                        Integer estimateShiftStock=sapShiftEmbryoStockMap.get(embryoCode);
                        estimateShiftStock+=totalPlanQty;
                        //增加班次预计可用胎胚库存
                        sapShiftEmbryoStockMap.put(embryoCode,estimateShiftStock);
                        //更新后全部标注，防止重复累加
                        shiftUpdateCxPlanFlag.put(key,true);
                        logDetail.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次:【{}】,更新后的库存：【{}】",embryoCode,classShift.getClassName(),estimateShiftStock)).append(division);
                        orderNoLog.append(StringUtils.format("【更新预计库存】,当前胎胚：【{}】,当前班次:【{}】,更新后的库存：【{}】",embryoCode,classShift.getClassName(),estimateShiftStock)).append(division);

                    }
                }
            }
         }
       }
    }

    /**
     * 获取硫化中班（16:00）预计开始胎胚库存
     * @param classShift
     */
    private void estimateLhClassOneStock(String scheduleDate,LhClassShiftEnum classShift, StringBuilder logDetail) {
        logDetail.append("开始进行成型计划开班预计库存数据组装，当前班次：").append(classShift.getClassName()).append(division);

        Date date = DateUtils.parseDate(scheduleDate);
        String lastDateStr= DateUtils.parseDateToStr("yyyy-MM-dd",DateUtils.addDays(date,-1));
        CxStock stockCondition=new CxStock();
        stockCondition.setStockDateStr(lastDateStr);
        List<CxStock> stockList =this.commonCxEngineMapper.selectMergeCxStockList(stockCondition);
        Map<String, Integer> embryoCodeStockMap =stockList.stream().collect(Collectors.toMap(CxStock::getEmbryoCode,CxStock::getStockRealNum));
        if(StringUtils.isNotEmpty(embryoCodeStockMap)){
            sapShiftEmbryoStockMap.putAll(embryoCodeStockMap);
        }
        if(StringUtils.isNotEmpty(cxScheduleResultList)){
            //根据外胎品号进行成型任务分组，获取同胎胚的全部任务
            Map<String, List<CxScheduleResult>> sapEmbryoListMap = CollectionUtil.toMapList(cxScheduleResultList, CxScheduleResult::getEmbryoCode);
            //汇总拿到的8点胎胚库存
            if(StringUtils.isNotEmpty(sapEmbryoListMap)){
                for(Map.Entry<String, List<CxScheduleResult>> entry:sapEmbryoListMap.entrySet()){
                    String embryoCode=entry.getKey();
                    List<CxScheduleResult> cxScheduleResults=entry.getValue();
                    //从库存中获取全部的库存信息
                    Integer totalEightStock=getStockByEmbryoCode(embryoCode);
                    //遍历成型任务进行库存+白班计划量获取start
                    if(StringUtils.isNotEmpty(cxScheduleResults)){
                        //汇总早8点+成型白班计划的初始值
                        for(CxScheduleResult cxScheduleResult:cxScheduleResults){
                            Integer lastClassPlan= LhEngineScheduleUtils.getCxBeforeClassPlanQty(cxScheduleResult,classShift);
                            totalEightStock+=lastClassPlan;
                        }
                    }
                    //遍历成型任务进行库存+白班计划量获取end
                    //存储预计开始胎胚库存
                    sapShiftEmbryoStockMap.put(embryoCode,totalEightStock);
                }
            }
        }
        //Joran 2022-06-24 如果成型没有计划时需要遍历获取硫化的计划start
        if(StringUtils.isNotEmpty(sapShiftEmbryoStockMap)){
            for(Map.Entry<String,Integer> embryoStockMap:sapShiftEmbryoStockMap.entrySet()){
                String embryoCode=embryoStockMap.getKey();
                Integer totalEightStock=embryoStockMap.getValue();
                //判断当前sap是否有白班计划，如果有的话需要扣除对应的白班计划
                Integer embryoPlanQty=0;
                if(StringUtils.isNotEmpty(lastDayEmbryoResultMap)&&lastDayEmbryoResultMap.containsKey(embryoCode)){
                    List<LhEngineScheduleResult> sapLastDayResults=lastDayEmbryoResultMap.get(embryoCode);
                    //遍历昨日的计划进行昨日同SAP计划量汇总start
                    for(LhEngineScheduleResult lhScheduleResult:sapLastDayResults){
                        //获取昨日计划白班的计划量
                        Integer lastClass3PlanQty=LhEngineScheduleUtils.getLhClassPlanQty(lhScheduleResult,LhClassShiftEnum.THREE_CLASS_SHIFT);
                        embryoPlanQty+=lastClass3PlanQty;
                    }
                    //遍历昨日的计划进行昨日同SAP计划量汇总end
                }
                //最后用总胎胚数-总硫化计划如果小于0则记录为0
                Integer classOneBeginStock=totalEightStock>embryoPlanQty? totalEightStock-embryoPlanQty:0;
                //存储预计开始胎胚库存
                sapShiftEmbryoStockMap.put(embryoCode,classOneBeginStock);
            }
        }
        //Joran 2022-06-24 如果成型没有计划时需要遍历获取硫化的计划end


    }

    /**
     * 获取胎胚库存
     * @param embryoCode
     * @return
     */
    private Integer getStockByEmbryoCode(String embryoCode) {
        return sapShiftEmbryoStockMap.containsKey(embryoCode)?sapShiftEmbryoStockMap.get(embryoCode):0;
    }

    /**
     * 成型排程结果表进行外胎规格设定
     * @param lhEngineScheduleResult
     */
    private void setSpecDescBySapCode(LhEngineScheduleResult lhEngineScheduleResult){
        if(lhEngineScheduleResult==null){
            log.error("【设置规格型号】,入参错误!");
            return;
        }
        String sapCode =lhEngineScheduleResult.getSapCode();
        String embryoCode=lhEngineScheduleResult.getEmbryoCode();
        if(StringUtils.isEmpty(sapCode)){
            log.error("【设置规格型号】通过SAP品号获取规格型号异常");
        }
        LhEngineTireConstructionInfo lhEngineTireConstructionInfo=null;
        if(StringUtils.isNotEmpty(sapTireConstructionListMap)&&sapTireConstructionListMap.containsKey(sapCode)){
            List<LhEngineTireConstructionInfo> lhEngineTireConstructionInfoList=sapTireConstructionListMap.get(sapCode);
            if(StringUtils.isEmpty(lhEngineTireConstructionInfoList)){
                log.error("【设置规格型号】,未找到硫化施工信息!");
                return;
            }
            lhEngineTireConstructionInfo=lhEngineTireConstructionInfoList.get(0);
            String key =GenerageMapKeyUtils.createMapKey(sapCode,embryoCode);
            for(LhEngineTireConstructionInfo constructionInfo:lhEngineTireConstructionInfoList){
                String machKey=GenerageMapKeyUtils.createMapKey(constructionInfo.getSapCode(),constructionInfo.getEmbryoCode());
                if(key.equals(machKey)){
                    lhEngineTireConstructionInfo=constructionInfo;
                    break;
                }
            }
        }else{
            lhEngineTireConstructionInfo=lhEngineTireConstructionInfoService.getLhConstructionInfoByCondition(sapCode,embryoCode);
        }

        if(lhEngineTireConstructionInfo==null){
            log.error("【设置规格型号】,未找到硫化施工信息!");
            return;
        }
        lhEngineScheduleResult.setSpecDesc(lhEngineTireConstructionInfo.getSpecDesc());
        log.debug("【设置规格型号】SAP品号："+sapCode+";--->规格描述="+lhEngineScheduleResult.getSpecDesc());
    }

    /**
     * 获取成型待料判断条件
     * @param key
     * @return
     */
    private Integer getCxWaitMaterialHour(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 3;
        }
        String cxWaitMaterialHourStr=lhParamsMap.get(key);
        return Integer.valueOf(cxWaitMaterialHourStr);
    }

    /**
     * 开汽标记结束小时
     * @param key
     * @return
     */
    private Integer getOpenStreamEndTime(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 16;
        }
        String openStreamEndTimeHourStr=lhParamsMap.get(key);
        return Integer.valueOf(openStreamEndTimeHourStr);
    }

    /**
     * 获取更换模具时长
     * @param key
     * @return
     */
    private Integer getChangeMoldTime(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 6;
        }
        String changeMoldHourStr=lhParamsMap.get(key);
        return Integer.valueOf(changeMoldHourStr);
    }

    /**
     * 当班换模后可安排最大计划量小时
     * @param key
     * @return
     */
    private Integer getAllowMaxPlanHour(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 4;
        }
        String allowMaxPlanHourStr=lhParamsMap.get(key);
        return Integer.valueOf(allowMaxPlanHourStr);
    }

    /**
     * 获取开班的最大计划量
     * @param key
     * @return
     */
    private Integer getOpenShiftMaxPlan(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 10;
        }
        String openShiftMaxPlanStr=lhParamsMap.get(key);
        return Integer.valueOf(openShiftMaxPlanStr);
    }

    /**
     * 获取开班的最小计划量
     * @param key
     * @return
     */
    private Integer getOpenShiftMinPlan(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 4;
        }
        String openShiftMinPlanStr=lhParamsMap.get(key);
        return Integer.valueOf(openShiftMinPlanStr);
    }

    /**
     * 获取是否补充成型班次库存，硫化结束时间和胎胚开始时间时间差
     * @param key
     * @return
     */
    private Integer getAddStockHour(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 3;
        }
        String addStrockDiffHourStr=lhParamsMap.get(key);
        return Integer.valueOf(addStrockDiffHourStr);
    }

    /**
     * 获取最大的追溯天数
     * @param key
     * @return
     */
    private Integer getMaxTraceDays(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 30;
        }
        String maxTraceDays=lhParamsMap.get(key);
        return Integer.valueOf(maxTraceDays);
    }

    /**
     * 获取开汽班次热模时间
     * @param key
     * @return
     */
    private Integer getOpenStreamPreheatTime(String key){
        if(StringUtils.isEmpty(lhParamsMap)||!lhParamsMap.containsKey(key)){
            return 120;
        }
        String lhOpenStreamPreheatHourStr=lhParamsMap.get(key);
        return Integer.valueOf(lhOpenStreamPreheatHourStr);
    }

    /**
     * 清空属性缓存数据
     */
    private void clearCacheData() {
        lhParamsMap=null;
        lhMachineInfoList= null;
        cxScheduleResultList=null;
        lastDayLhApsMoldAdjustPlanList=null;
        lhApsMoldAdjustPlanList=null;
        lastDayLhScheduleResultList=null;
        lhSapMonthPlanSurplusMap=null;
        cxBatchNo="";
        lhEmbryoSupportTimeMap=null;
        sapShiftEmbryoStockMap=null;
        lastDayEmbryoResultMap=null;
        sapLhApsMoldAdjustPlanMap=null;
        lastDayChangeMoldMap=null;
        cxParamsMap=null;
        sapTireConstructionListMap=null;
        shiftStartEndTimeMap=null;
        shiftUpdateCxPlanFlag=null;
        orderLogMap=null;
        machineLhEndTime=null;
        machineInProductListMap=null;
    }

}
