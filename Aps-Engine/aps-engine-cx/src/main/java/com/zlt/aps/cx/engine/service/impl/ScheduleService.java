package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import com.zlt.aps.common.engine.service.MdmMonthProdPlanService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineAutoScheduleRecord;
import com.zlt.aps.cx.engine.domain.CxEngineSapImportBadNumber;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;
import com.zlt.aps.cx.engine.enums.AdjustTypeEnums;
import com.zlt.aps.cx.engine.enums.ClassEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxLhEngineCommonMapper;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineAutoScheduleRecordService;
import com.zlt.aps.cx.engine.service.CxPlanProductStatusService;
import com.zlt.aps.cx.engine.task.CxScheduleTaskService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 引擎核心算法逻辑
 */
@Component("cxScheduleService")
@Slf4j
public class ScheduleService {

    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    @Autowired
    private MdmMonthPlanMainService mdmMonthPlanMainService;

    @Autowired
    private CommonCacheService cacheService;

    @Autowired
    private CxEngineAutoScheduleRecordService cxEngineAutoScheduleRecordService;

    @Autowired
    private CxScheduleTaskService cxScheduleTaskService;

    @Autowired
    private CxPlanProductStatusService cxPlanProductStatusService;

    @Autowired
    private MdmMonthProdPlanService mdmMonthProdPlanService;

    @Autowired
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private CxLhEngineCommonMapper cxLhEngineCommonMapper;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

   /* *//**
     * 成型自动排程引擎算法
     * @param scheduleDate
     * @throws CxScheduleEngineException
     *//*
    @Transactional
    public synchronized void autoSchedule(Date scheduleDate) throws CxScheduleEngineException {
        String scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        String cxBatchNo="";
        String monthPlanApsVersion="";

        //删除成型自动排程记录表
        cxEngineAutoScheduleRecordService.deleteAutoScheduleRecordByScheduleDate(scheduleDateStr);
        //删除成型排程结果表数据
        cacheService.syncCxScheduleToLog(scheduleDateStr,"","");
        //成型自动排程批次号
        cxBatchNo=cacheService.getCxSequence(CxPrefixConstants.SCHEDULE_BATCH_NO_PREFIX+scheduleDateStr, CxPrefixConstants.CX_BATCH_NO_PREFIX+scheduleDateStr);
        //获取生产排程版本
        MdmMonthPlanMain planVersion=mdmMonthPlanMainService.getValidPlanMainVersion(scheduleDate);
        if(planVersion==null){
            String errorMsg= I18nUtil.getMessage("cx.engine.auto.plan.main.empty.error");
            cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,null,cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_FAILE,errorMsg);
            throw new CxScheduleEngineException(errorMsg);
        }

        //月度计划生产排程版本号
        monthPlanApsVersion=planVersion.getMonthPlanApsVersion();
        //删除外胎汇总、成型胎胚月度汇总表中插单的数据且是排程日期当天的数据，然后重新调用月度外胎、胎胚汇总表
        *//**
         * Joran 2021-12-04 多版本改造成型插单不再往月度汇总表写入数据,此处不再进行删除start
         *//*
        boolean singleDataVersion= false;
        if(singleDataVersion){
            cacheService.synRemoveSurplus(scheduleDate,monthPlanApsVersion,"","");
        }
        *//**
         * Joran 2021-12-04 多版本改造成型插单不再往月度汇总表写入数据,此处不再进行删除end
         *//*

        //Joran 2021-11-27 进行月度计划版本中成型收尾硫化尚未收尾数据处理
        cacheService.lhTaskCloseOut(monthPlanApsVersion,scheduleDate,false);

        *//**
         *  1.加载前一天排程任务列表
         *//*
        List<CxEngineScheduleResult> lastDayTaskList =cacheService.getLastPlanResultList(scheduleDate,cxBatchNo,"",false,false);

        //Joran 2021-09-09 查询汇总月度计划初稿数据到排程结果表最新计划数中
        cacheService.updateNewestPlanQty(lastDayTaskList,scheduleDate);

        //更新库存到排程结果记录中
        StringBuilder updateStockLog =new StringBuilder();
        //更新库存到排程对应规格
        cacheService.updateLastDayTaskStock(lastDayTaskList,scheduleDate,updateStockLog,false);
        if(StringUtils.isNotEmpty(updateStockLog)){
            autoScheduleLogService.insertCxScheduleLog(lastDayTaskList.get(0).getCxBatchNo(), "", "【自动排程排程日期："+scheduleDateStr+"前一天排程设置库存日志】",updateStockLog.toString()); //添加日志
        }
        autoScheduleLogService.insertCxScheduleLog(cxBatchNo, "", "【自动排程排程日期："+scheduleDateStr+"获取前一天排程记录】",  toJSONString(lastDayTaskList));
        //移除已收尾的数据
        cacheService.closeOutRemove(monthPlanApsVersion,lastDayTaskList,null,false);
        if(StringUtils.isEmpty(lastDayTaskList)){
            //前一天全部收尾，错误提示
            throw  new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.lastDay.all.closeOutAll.error"));
        }

        //硫化外胎施工信息start
        LhEngineTireConstructionInfo lhEngineTireConstructionInfo=new LhEngineTireConstructionInfo();
        List<LhEngineTireConstructionInfo> constructionInfoList=lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(lhEngineTireConstructionInfo);
        Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap=new HashMap<>();
        if(StringUtils.isNotEmpty(constructionInfoList)){
            sapTireConstructionListMap=constructionInfoList.stream().collect(Collectors.groupingBy(lhEngineScheduleResult -> lhEngineScheduleResult.getSapCode()));
        }
        //硫化外胎施工信息end

        List<CxEngineScheduleResult> newTaskList=new ArrayList<>(lastDayTaskList.size());
        //复制前一天次日一班的数据到新任务一班中
        StringBuilder copyLog=new StringBuilder();
        cacheService.copyLastDayTaskToNewTask(cxBatchNo,newTaskList,lastDayTaskList,sapTireConstructionListMap,scheduleDate,true,copyLog);
        if(StringUtils.isNotEmpty(copyLog)){
            autoScheduleLogService.insertCxScheduleLog(cxBatchNo, "", "【自动排程排程："+scheduleDateStr+"复制前一天计划】", copyLog.toString());
        }
        *//**
         * 2021-06-23跟项目经理确认，上月已投产的继续做，未投产的优先列在新月度计划之前，
         * 后续提供给客户界面选择标记不投产，根据标识进行投产数据抓取
         *//*

        //加载月度计划未投产数据列表
        //Joran 2021-12-23 对比当前排程已经在排程计划中的直接移除这一部分投产数据start
        cacheService.removePlanProductStatusList(monthPlanApsVersion,scheduleDate);
        //Joran 2021-12-23 对比当前排程已经在排程计划中的直接移除这一部分投产数据end
        List<CxPlanProductStatus> cxPlanProductStatusList=cacheService.getPlanProductStatusByApsVersion(monthPlanApsVersion,null,scheduleDate);

        //Joran 2021-12-31 根据前一天计划列表进行投产状态表更新start
        if(StringUtils.isNotEmpty(cxPlanProductStatusList)){
            cacheService.updatePlanProductStatusList(newTaskList,cxPlanProductStatusList);
        }
        //Joran 2021-12-31 根据前一天计划列表进行投产状态表更新end

        //根据月度计划APS版本进行SAP+胎胚代码+库存地点计划量合并
        List<MdmMonthProdPlan> mdmMonthProdPlanList=this.mdmMonthProdPlanService.selectMonthTotalPlanQtyByApsVersion(monthPlanApsVersion);

        if(StringUtils.isEmpty(cxPlanProductStatusList)){
            log.debug("【scheduleService.autoSchedule】自动排程获取未投产列表数据为空");
        }

        if(StringUtils.isEmpty(mdmMonthProdPlanList)){
            log.debug("【scheduleService.autoSchedule】自动排程获取月度计划明细汇总列表数据为空");
        }
        //任务安排start
        cacheService.defaultToProduct(newTaskList);
        //1.现有根据成型机机台进行任务列表拆分
        Map<String,List<CxEngineScheduleResult>> machineTaskMap=CxScheduleUtils.splitTaskByCxMachine(newTaskList);
        //Joran 2022-01-07 根据胎胚进行类型汇总,同胎胚库存不重复进行累加
        Map<String,Double> sameDimensionAvailableClassOneShiftMap=new HashMap<>(); //处理同寸口平均可硫化班次
        copyLog=new StringBuilder();
        Map<String,Integer> embryoCodeTypeTotalMap=cacheService.generateEmbryoTypeMap(newTaskList,sameDimensionAvailableClassOneShiftMap,ClassEnums.CLASS_ONE.getClassIndex(),copyLog);
        try{
            cxScheduleTaskService.autoScheduling(machineTaskMap,cxPlanProductStatusList,mdmMonthProdPlanList,embryoCodeTypeTotalMap,sameDimensionAvailableClassOneShiftMap);
            cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,monthPlanApsVersion,cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_SUCCESS,"自动排程成功");
        }catch (Exception e){
            log.error("【自动排程异常】："+e.getMessage());
            e.printStackTrace();
            cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,monthPlanApsVersion,cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_FAILE,"自动排程异常");
            throw e;
        }
        //任务安排end
    }*/

    /**
     * 获取导入的外胎废次品数据集
     * @return
     */
    private Map<String, Integer> generateSapRejectQtyMap() {
       Map<String, Integer> sapRejectQtyMap=new HashMap<>();
       List<CxEngineSapImportBadNumber> sapImportBadNumberList=cxLhEngineCommonMapper.selectSapImportBadNumberList(new CxEngineSapImportBadNumber());
       if(StringUtils.isNotEmpty(sapImportBadNumberList)){
           sapRejectQtyMap=sapImportBadNumberList.stream().collect(
                   Collectors.toMap(cxEngineSapImportBadNumber ->cxEngineSapImportBadNumber.getSapCode(),
                           cxEngineSapImportBadNumber -> cxEngineSapImportBadNumber.getBadNum(),(va11,val2) -> val2
                   )
           );
       }
       return sapRejectQtyMap;
    }

    /**
     * 可硫化班次重算，提供给调量、编辑调用
     * @param cxScheduleResult
     */
    @Transactional
    public void reCalcAvalivableLhShift(CxScheduleResult cxScheduleResult, AdjustTypeEnums adjustTypeEnums){
        //1.拿到对应的SAP品号，胎胚代码、排程日期
        String sapCode =cxScheduleResult.getSapCode();
        //胎胚代码
        String embryoCode=cxScheduleResult.getEmbryoCode();

        //排程日期
        Date scheduleDate=cxScheduleResult.getScheduleDate();

        //获取相同规格的
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setSapCode(sapCode);
        condition.setEmbryoCode(embryoCode);
        condition.setScheduleDate(scheduleDate);
        //查询同SAP胎胚库存
        List<CxEngineScheduleResult> cxEngineScheduleResultList=this.cxScheduleEngineMapper.selectCxScheduleResultList(condition);
        if(StringUtils.isEmpty(cxEngineScheduleResultList)){
            log.error("根据条件查询成型排程数据为空，无法进行班次可硫化班数重新计算");
            return;
        }
        //更新库存数
        StringBuilder updateStockLog=new StringBuilder();
        cacheService.updateLastDayTaskStock(cxEngineScheduleResultList,scheduleDate,updateStockLog,false);
        if(StringUtils.isNotEmpty(updateStockLog)){
            autoScheduleLogService.insertCxScheduleLog(cxScheduleResult.getCxBatchNo(), cxScheduleResult.getOrderNo(), "重算可硫化班次，相关规格库存设置",updateStockLog.toString()); //添加日志
        }
        //2.根据SAP+胎胚+机台形成map
        CxEngineScheduleResult localCxEngineScheduleResult=null;
        for(CxEngineScheduleResult cxEngineScheduleResult: cxEngineScheduleResultList){
            if(cxEngineScheduleResult.getId().equals(cxScheduleResult.getId())){
                localCxEngineScheduleResult=cxEngineScheduleResult;
                break;
            }
        }

        //设置调整后的量
       // List<CxEngineScheduleResult> updateList=new ArrayList<>();
        if(localCxEngineScheduleResult!=null){
            localCxEngineScheduleResult.setClass1PlanQty(cxScheduleResult.getClass1PlanQty());
            localCxEngineScheduleResult.setClass2PlanQty(cxScheduleResult.getClass2PlanQty());
            localCxEngineScheduleResult.setClass3PlanQty(cxScheduleResult.getClass3PlanQty());
            localCxEngineScheduleResult.setClass4PlanQty(cxScheduleResult.getClass4PlanQty());
            localCxEngineScheduleResult.setClass5PlanQty(cxScheduleResult.getClass5PlanQty());

            //Joran 2021-10-19 如果原数据没有硫化机数则从传入对象获取设值start
            if(AdjustTypeEnums.CHANGE_LH_MACHINE.getChangeType()==adjustTypeEnums.getChangeType()){
                localCxEngineScheduleResult.setLhMachineQty(cxScheduleResult.getLhMachineQty());
            }
            //Joran 2021-10-19 如果原数据没有硫化机数则从传入对象获取设值end

            //如果使用模数有值的话重新计算单班硫化量
            cacheService.calcSingleShiftLhQty(localCxEngineScheduleResult);

            //Joran 2022-03-03 添加根据类型 如果数据来源于插单的话，则按照新投产规格进行处理 start
            if(CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_INSERT.equals(localCxEngineScheduleResult.getDataSource())){
                localCxEngineScheduleResult.setNewSpecFlag(true);
            }
            //Joran 2022-03-03 添加根据类型 如果数据来源于插单的话，则按照新投产规格进行处理 end

            //重新计算各班可硫化班次
            CxScheduleUtils.calcAllClassAvailableLhShift(localCxEngineScheduleResult);
            //重新赋值
            cxScheduleResult.setClass1AvailableLhShift(localCxEngineScheduleResult.getClass1AvailableLhShift());
            cxScheduleResult.setClass2AvailableLhShift(localCxEngineScheduleResult.getClass2AvailableLhShift());
            cxScheduleResult.setClass3AvailableLhShift(localCxEngineScheduleResult.getClass3AvailableLhShift());
            cxScheduleResult.setClass4AvailableLhShift(localCxEngineScheduleResult.getClass4AvailableLhShift());
            cxScheduleResult.setClass5AvailableLhShift(localCxEngineScheduleResult.getClass5AvailableLhShift());
            cxScheduleResult.setSingleShiftLhQty(localCxEngineScheduleResult.getSingleShiftLhQty());//单班硫化量回写
        }

        //遍历重新计算
       /* for(Map.Entry<String,CxEngineScheduleResult> entry:cxEngineScheduleResultMap.entrySet()){
            CxEngineScheduleResult cxEngineScheduleResult =entry.getValue();
            CxScheduleUtils.calcAllClassAvalableLhShift(cxEngineScheduleResult);
            updateList.add(cxEngineScheduleResult);
        }*/

        //可硫化班次回写
       /* if(cxEngineScheduleResultMap.containsKey(machKey)){
            CxEngineScheduleResult cxEngineScheduleResult= cxEngineScheduleResultMap.get(machKey);
            cxScheduleResult.setClass1AvailableLhShift(cxEngineScheduleResult.getClass1AvailableLhShift());
            cxScheduleResult.setClass2AvailableLhShift(cxEngineScheduleResult.getClass2AvailableLhShift());
            cxScheduleResult.setClass3AvailableLhShift(cxEngineScheduleResult.getClass3AvailableLhShift());
            cxScheduleResult.setClass4AvailableLhShift(cxEngineScheduleResult.getClass4AvailableLhShift());
            cxScheduleResult.setClass5AvailableLhShift(cxEngineScheduleResult.getClass5AvailableLhShift());
        }*/

        //批量更新可硫化班次
       /* if(StringUtils.isNotEmpty(updateList)){
            cxScheduleEngineMapper.updateAvailableBatch(updateList);
        }*/

       //TODO Joran 2021-09-19 插单数据的话月度外胎、胎胚月度剩余量将量扣减 半部件需要重算（确认实际情况再做调整）
       /* if(AdjustTypeEnums.CHANGE_QTY.getChangeType()==adjustTypeEnums.getChangeType()){
            handleInsertOrder(cxScheduleResult);
        }*/
    }

    /**
     * 成型单机台任务重排
     * @param cxMachineCode 成型机台编号
     * @param scheduleDate
     * @throws CxScheduleEngineException
     */
   /* @Transactional
    public synchronized void singleMachineAutoSchedule(String cxMachineCode,Date scheduleDate) throws CxScheduleEngineException {

        String scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);

        CxEngineAutoScheduleRecord autoScheduleRecord=cxEngineAutoScheduleRecordService.selectAutoScheduleRecordByScheduleDate(scheduleDateStr);
        //1.验证自动排程记录
        if(autoScheduleRecord==null){ //没有自动排程无法进行单机台重排
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.schedule.record.empty.error"));
        }
        String cxBatchNo=autoScheduleRecord.getCxBatchNo();
        String monthPlanApsVersion=autoScheduleRecord.getMonthPlanApsVersion();
        //加载重新排程前的结果
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setCxMachineCode(cxMachineCode);
        condition.setCxScheduleDate(DateUtils.parseDateToStr("yyyyMMdd",scheduleDate));
        List<CxEngineScheduleResult> machineAutoPlanList=cxScheduleEngineMapper.selectCxScheduleResultList(condition);
        if(StringUtils.isEmpty(machineAutoPlanList)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.machine.auto.schedule.empty.error"));
        }
        cxScheduleEngineMapper.syncCxScheduleToLog(scheduleDateStr,cxMachineCode);
        cxScheduleEngineMapper.deleteCxSchedule(scheduleDateStr,cxMachineCode);

        //2.加载前一天机台排程数据
        List<CxEngineScheduleResult> lastDayMachineTaskList =cacheService.getLastPlanResultList(scheduleDate,cxBatchNo,cxMachineCode,false,false);
        //分组标记前一天排程
        Map<String,CxEngineScheduleResult> lastTaskMap=lastDayMachineTaskList.stream().collect(
                Collectors.toMap(
                            cxEngineScheduleResult -> GenerageMapKeyUtils.createMapKey(
                                    cxEngineScheduleResult.getSapCode(),
                                    cxEngineScheduleResult.getEmbryoCode(),
                                    cxEngineScheduleResult.getBomDataVersion()
                                    ),
                            scheduleResult -> scheduleResult,(k1,k2)->k1
                )
        );
        //对前一天的数据进行处理
        if(StringUtils.isEmpty(lastTaskMap)){
            throw  new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.lastDay.all.closeOutAll.error"));
        }

        //移除已收尾的数据
        cacheService.closeOutRemove(monthPlanApsVersion,machineAutoPlanList,null,false);
        List<CxEngineScheduleResult> newTaskList=new ArrayList<>(machineAutoPlanList.size());

        for(CxEngineScheduleResult cxEngineScheduleResult : machineAutoPlanList){
            CxEngineScheduleResult target = BeanConverUtil.conver(cxEngineScheduleResult,CxEngineScheduleResult.class);
            target.setId(null);
            target.setClass1PlanQty(0);
            target.setClass2PlanQty(0);
            target.setClass3PlanQty(0);
            target.setClass4PlanQty(0);
            target.setClass5PlanQty(0);
            target.setClass1Analysis("");
            target.setClass2Analysis("");
            target.setClass3Analysis("");
            target.setClass4Analysis("");
            target.setClass5Analysis("");
            target.setClass2AnalysisInput("");
            target.setClass3AnalysisInput("");
            target.setClass4AnalysisInput("");
            target.setClass5AnalysisInput("");
            target.setClass3PlannedQty(cxEngineScheduleResult.getClass3PlannedQty()==null?0:cxEngineScheduleResult.getClass3PlannedQty());
            target.setMonthRemainQty(cxEngineScheduleResult.getMonthRemainQty()==null?0:cxEngineScheduleResult.getMonthRemainQty());//月度剩余量
            target.setLastClass1PlanQty(cxEngineScheduleResult.getClass1PlanQty()==null?0:cxEngineScheduleResult.getClass1PlanQty());//冗余前一天的一班计划量
            target.setLastClass2PlanQty(cxEngineScheduleResult.getClass2PlanQty()==null?0:cxEngineScheduleResult.getClass2PlanQty());//冗余前一天的二班计划量
            target.setLastClass3PlanQty(cxEngineScheduleResult.getClass3PlanQty()==null?0:cxEngineScheduleResult.getClass3PlanQty());//冗余前一天三班的计划量
            target.setLastClass4PlanQty(cxEngineScheduleResult.getClass4PlanQty()==null?0:cxEngineScheduleResult.getClass4PlanQty());//冗余前一天次日一班的计划量
            target.setLastClass5PlanQty(cxEngineScheduleResult.getClass5PlanQty()==null?0:cxEngineScheduleResult.getClass5PlanQty());//冗余前一天次日二班的计划量
            newTaskList.add(target);
        }
        //Joran 2021-12-30 因为是重排当天的计划所以这里只能获取未投产的数据，已经投产和待发布的不在获取
        List<CxPlanProductStatus> cxPlanProductStatusList=cacheService.getPlanProductStatusByApsVersion(monthPlanApsVersion, CxEngineConstants.MDM_PLAN_PRODUCT_STATUS_NO,scheduleDate);
        //根据月度计划APS版本进行SAP+胎胚代码+库存地点计划量合并
        List<MdmMonthProdPlan> mdmMonthProdPlanList=this.mdmMonthProdPlanService.selectMonthTotalPlanQtyByApsVersion(monthPlanApsVersion);
        try{
            //调用单机台重算方法
            cxScheduleTaskService.reScheduleByMachine(cxMachineCode,newTaskList,lastTaskMap,cxPlanProductStatusList,mdmMonthProdPlanList);
        }catch (Exception e){
            log.error("【成型机台重新自动排程异常】："+e.getMessage());
            e.printStackTrace();
            throw e;
        }
       *//*  String cxBatchNo=autoScheduleRecord.getCxBatchNo();
        String monthPlanApsVersion=autoScheduleRecord.getMonthPlanApsVersion();
        //加载重新排程前的结果
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setCxScheduleDate(DateUtils.parseDateToStr("yyyyMMdd",scheduleDate));
        List<CxEngineScheduleResult> beforeAutoPlanList=cxScheduleEngineMapper.selectCxScheduleResultList(condition);
        if(StringUtils.isEmpty(beforeAutoPlanList)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.machine.auto.schedule.empty.error"));
        }
        List<CxEngineScheduleResult> removePlanList=new ArrayList<>();
        //存储当天排程中胎胚已经安排下的计划量汇总
        Map<String,Integer> embryoCodeMap=generageEmbryoCodeMap(cxMachineCode,beforeAutoPlanList,removePlanList);

        //遍历进行数据迁移日志表和表数据删除start
        removePlanAndLogList(scheduleDate,cxMachineCode,monthPlanApsVersion,removePlanList);
        //遍历进行数据迁移日志表和表数据删除end

        //2.加载前一天机台排程数据
        List<CxEngineScheduleResult> lastDayMachineTaskList =getLastPlanResultList(scheduleDate,cxBatchNo,cxMachineCode,false);
        //Joran 2021-09-16 查询汇总月度计划初稿数据到排程结果表最新计划数中
        updateNewestPlanQty(lastDayMachineTaskList,scheduleDate);
        //更新库存到排程对应规格
        StringBuilder stockLogDetail=new StringBuilder();
        cacheService.updateLastDayTaskStock(lastDayMachineTaskList,scheduleDate,stockLogDetail);
        if(StringUtils.isNotEmpty(stockLogDetail)){
            autoScheduleLogService.insertCxScheduleLog(lastDayMachineTaskList.get(0).getCxBatchNo(), "", "【机台重排排程日期："+scheduleDateStr+"前一天排程设置库存日志】",stockLogDetail.toString()); //添加日志
        }
        autoScheduleLogService.insertCxScheduleLog(cxBatchNo, "", "【机台重排排程日期："+scheduleDateStr+"获取前一天机台排程记录】",  toJSONString(lastDayMachineTaskList));
        //移除已收尾的数据
        closeOutRemove(monthPlanApsVersion,lastDayMachineTaskList,embryoCodeMap);
        if(StringUtils.isEmpty(lastDayMachineTaskList)){
            //前一天全部收尾，错误提示
            throw  new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.lastDay.all.closeOutAll.error"));
        }
        List<CxEngineScheduleResult> newTaskList=new ArrayList<>(lastDayMachineTaskList.size());
        //复制前一天次日一班的数据到新任务一班中
        copyLastDayTaskToNewTask(cxBatchNo,newTaskList,lastDayMachineTaskList,scheduleDate);

        //Joran 2021-12-28 进行更新投产状态表start
        removePlanProductStatusList(monthPlanApsVersion,scheduleDate);
        //Joran 2021-12-28 进行更新投产状态表end
        //加载月度计划未投产数据列表
        List<CxPlanProductStatus> cxPlanProductStatusList=getPlanProductStatusByApsVersion(monthPlanApsVersion);
        //根据月度计划APS版本进行SAP+胎胚代码+库存地点计划量合并
        List<MdmMonthProdPlan> mdmMonthProdPlanList=this.mdmMonthProdPlanService.selectMonthTotalPlanQtyByApsVersion(monthPlanApsVersion);
        if(StringUtils.isEmpty(cxPlanProductStatusList)){
            log.debug("【成型机台重新自动排程】自动排程获取未投产列表数据为空");
        }
        if(StringUtils.isEmpty(mdmMonthProdPlanList)){
            log.debug("【成型机台重新自动排程】自动排程获取月度计划明细汇总列表数据为空");
        }
        Map<String,List<CxEngineScheduleResult>> machineTaskMap=CxScheduleUtils.splitTaskByCxMachine(newTaskList);
        try{
            cxScheduleTaskService.autoScheduling(machineTaskMap,cxPlanProductStatusList,mdmMonthProdPlanList);
        }catch (Exception e){
            log.error("【成型机台重新自动排程异常】："+e.getMessage());
            e.printStackTrace();
            throw e;
        }*//*
    }*/

    /**
     * 构建同成型机台代码的计划量信息，如果是相同机台编号则加入删除列表
     * @param cxMachineCode 当前重排成型机台编号
     * @param beforeAutoPlanList 重排之前的计划集合
     * @param removePlanList 同机台计划列表进行删除
     * @return
     */
    private Map<String, Integer> generageEmbryoCodeMap(String cxMachineCode, List<CxEngineScheduleResult> beforeAutoPlanList,List<CxEngineScheduleResult> removePlanList) {
        Map<String,Integer> embryoCodeMap =new HashMap<>();
        Integer totalPlanQty=null;
        for(CxEngineScheduleResult planResult:beforeAutoPlanList){
            String machineCode=planResult.getCxMachineCode();
            String embryoCode=planResult.getEmbryoCode();
            if(StringUtils.equals(cxMachineCode,machineCode)){
                removePlanList.add(planResult);
                continue;
            }else if(embryoCodeMap.containsKey(embryoCode)){
                totalPlanQty=embryoCodeMap.get(embryoCode);
                totalPlanQty+=planResult.getDayTotalPlanQty();
            }else{
                totalPlanQty=planResult.getDayTotalPlanQty();
            }
            embryoCodeMap.put(embryoCode,totalPlanQty);
        }
        return embryoCodeMap;
    }

    /**
     * 删除计划信息及相关日志表数据
     * @param scheduleDate 排程日期
     * @param cxMachineCode 成型机台编号
     * @param monthPlanApsVersion 月度计划版本号
     * @param removePlanList 移除的计划列表
     */
    private void removePlanAndLogList(Date scheduleDate,String cxMachineCode, String monthPlanApsVersion, List<CxEngineScheduleResult> removePlanList) {
        String scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        if(StringUtils.isNotEmpty(removePlanList)){
            for(CxEngineScheduleResult syncToLogResult:removePlanList){
                //1.排程结果表日志迁移，数据删除
                cacheService.syncCxScheduleToLog(scheduleDateStr,cxMachineCode,syncToLogResult.getOrderNo());
                //2.月度汇总表插单数据删除
                cacheService.synRemoveSurplus(scheduleDate,monthPlanApsVersion,syncToLogResult.getSapCode(),syncToLogResult.getEmbryoCode());
            }
        }
    }

    /**
     * 对外提供成型排程确认硫化机后进行单班硫化量重算
     * @param cxScheduleResult
     */
    public void reCalcSingleShiftLhQty(CxScheduleResult cxScheduleResult) throws CxScheduleEngineException {
        if(cxScheduleResult==null){
            log.error("【确定硫化机后，重算单班硫化量失败】：入参对象有误！");
            return;
        }
        //转换成具体对象
        CxEngineScheduleResult target = BeanConverUtil.conver(cxScheduleResult,CxEngineScheduleResult.class);
        if(target==null){
            log.error("【重算单班硫化量，计算失败】：对象转换异常！");
            return;
        }
        cacheService.calcSingleShiftLhQty(target);
        //重设单班硫化量
        cxScheduleResult.setSingleShiftLhQty(target.getSingleShiftLhQty());
    }

    /**
     * 重新设置同机台同胎胚不同外胎的单班硫化量汇总
     * @param scheduleResult
     */
    public void reSetSingleLhShiftQty(CxScheduleResult scheduleResult) {
        String embryoCode=scheduleResult.getEmbryoCode();
        Date scheduleDate=scheduleResult.getScheduleDate();
        String machineCode=scheduleResult.getCxMachineCode();
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setEmbryoCode(embryoCode);
        condition.setScheduleDate(scheduleDate);
        condition.setCxMachineCode(machineCode);
        //查询分配硫化机相同机台相同胎胚的成型排程列表
        List<CxEngineScheduleResult> cxEngineScheduleResultList=this.cxScheduleEngineMapper.selectCxScheduleResultList(condition);
        if(StringUtils.isNotEmpty(cxEngineScheduleResultList)){
            CxScheduleUtils.calcMachineSpecLhShiftCount(cxEngineScheduleResultList);
            //进行更新单班硫化量和留存单班硫化量
            cxScheduleEngineMapper.updateSingleLhQtyBatch(cxEngineScheduleResultList);
        }

    }


}