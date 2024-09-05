package com.zlt.aps.cx.engine.task;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.CxEngineChangeLhMachineService;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineLastDaySupplePlan;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxEngineSuppleBatchRecord;
import com.zlt.aps.cx.engine.domain.CxMiddleNightFinishQty;
import com.zlt.aps.cx.engine.enums.ClassEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxEngineLastDayScheduleMapper;
import com.zlt.aps.cx.engine.service.CxEngineLastDaySupplePlanService;
import com.zlt.aps.cx.engine.service.CxEngineSuppleBatchRecordService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 前一天成型排程计划调整
 */
@Slf4j
@Service("lastDayScheduleTaskService")
public class LastDayScheduleTaskService {

    @Autowired
    private MdmMonthPlanMainService mdmMonthPlanMainService;

    @Autowired
    private CommonCacheService commonCacheService;

    @Autowired
    private CxEngineLastDayScheduleMapper cxEngineLastDayScheduleMapper;

    @Autowired
    private CxEngineSuppleBatchRecordService cxEngineSuppleBatchRecordService;

    @Autowired
    private CxEngineLastDaySupplePlanService cxEngineLastDaySupplePlanService;

    @Autowired
    private CxEngineChangeLhMachineService cxEngineChangeLhMachineService;

    //缓存各个班次的班次时长
    private Map<String,Double> machineShiftHourMap=new HashMap<>();

    //所有施工信息
    private Map<String, EngineProductConstructionInfo> engineConstructionInfoMap;

    //成型工序参数
    private  Map<String,String> cxParamsMap;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private CxEngineCommonService cxEngineCommonService;

    /**
     * 成型机台当前在产规格胎胚（<机台编号,胎胚代码>）
     */
    private Map<String,String> machineInProductMap;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符


    /**
     * 结合中夜班实际完成情况，进行规格调整修正主要修正是三班及预排班计划
     * @param lastDate 调整前一天的日期
     * @throws CxScheduleEngineException
     */
    @Transactional
    public synchronized void autoMixLastDaySchedule(Date lastDate) throws CxScheduleEngineException{

        String lastDateStr= DateUtils.parseDateToStr("yyyyMMdd",lastDate);

        StringBuilder logDetail=new StringBuilder("【结合中夜班完成情况】，生成前日三班增补计划日志：").append(division);

        //验证是否有存在批次未确认的增补计划
        String errorMsg=cxEngineSuppleBatchRecordService.isExistRecordByDateStr(lastDateStr);
        if(StringUtils.isNotEmpty(errorMsg)){
            throw new CxScheduleEngineException(errorMsg);
        }

        //生成增补计划批次表
        String suppleBatchNo=commonCacheService.getCxSequence(CxPrefixConstants.SUPPLE_BATCH_NO_PREFIX+lastDateStr, CxPrefixConstants.SUPPLE_BATCH_PREFIX+lastDateStr);
        cxEngineSuppleBatchRecordService.createSuppleBatchRecord(suppleBatchNo,lastDate);

        logDetail.append("【前天三班计划增补】调整前一天计划，前一天排程日期："+lastDateStr).append(division);
        //获取生产排程版本
        MdmMonthPlanMain planVersion=mdmMonthPlanMainService.getValidPlanMainVersion(lastDate);
        if(planVersion==null){
            errorMsg= I18nUtil.getMessage("cx.engine.auto.plan.main.empty.error");
            throw new CxScheduleEngineException(errorMsg);
        }
        //月度计划生产排程版本号
        String monthPlanApsVersion=planVersion.getMonthPlanApsVersion();

        //Joran 2021-11-27 进行月度计划版本中成型收尾硫化尚未收尾数据处理
        commonCacheService.lhTaskCloseOut(monthPlanApsVersion,lastDate,false);

        //Joran 2022-02-07 加载前一天排程计划的列表
        List<CxEngineScheduleResult> lastDateScheduleList=commonCacheService.getLastPlanResultList(lastDate,"","",false,true);
        /**
         * 更新前一天日期库存数据start
         */
        StringBuilder updateStockLog =new StringBuilder();
        commonCacheService.updateLastDayTaskStock(lastDateScheduleList,lastDate,updateStockLog,true);
        /**
         * 更新前一天日期库存数据end
         */

        //Joran 2022-04-14进行昨日排程的硫化机台信息复制一份到类型为增补计划类型start
        boolean isChange=false;
        if(isChange){
            cxEngineChangeLhMachineService.buildSuppleCxChangeLhMachine(lastDate,CxEngineConstants.CHANGE_MACHINE_DATA_SOURCE_SUPPLE);
        }
        //Joran 2022-04-14进行昨日排程的硫化机台信息复制一份到类型为增补计划类型end

        //移除已收尾的数据和月度剩余量信息
        commonCacheService.closeOutRemove(monthPlanApsVersion,lastDateScheduleList,null,false);

        //现有根据成型机机台进行任务列表拆分
        Map<String,List<CxEngineScheduleResult>> machineTaskMap= CxScheduleUtils.splitTaskByCxMachine(lastDateScheduleList);
        if(StringUtils.isEmpty(machineTaskMap)){
            logDetail.append("【前天三班计划增补】前一天排程计划全部收尾，不需要进行增补").append(division);
            return;
        }
        /**
         * 获取中夜班完成量并进行数据结构处理start
         */
        CxMiddleNightFinishQty condition=new CxMiddleNightFinishQty();
        condition.setScheduleDateStr(lastDateStr);
        List<CxMiddleNightFinishQty> cxMiddleNightFinishQtyList = cxEngineLastDayScheduleMapper.listCxFinish(condition);
        log.debug("【前天三班计划增补】获取到的中夜班完成量集合"+ JSON.toJSONString(cxMiddleNightFinishQtyList));
        logDetail.append("【前天三班计划增补】获取到的中夜班完成量集合").append(JSON.toJSONString(cxMiddleNightFinishQtyList)).append(division);
        Map<String,List<CxMiddleNightFinishQty>> machineOrderFinishListMap=new HashMap<>();
        if(StringUtils.isNotEmpty(cxMiddleNightFinishQtyList)){
            machineOrderFinishListMap=cxMiddleNightFinishQtyList.stream().collect(Collectors.groupingBy(CxMiddleNightFinishQty::getCxMachineCode));
        }
        /**
         * 获取中夜班完成量并进行数据结构处理end
         */

        //Joran 2022-03-02 获取机台在产规格胎胚
        //Joran 2022-02-24 成型机台在产规格初始化获取
        machineInProductMap=cxEngineCommonService.cxMachineInProductSpecMap(lastDate);
        //Joran 2022-01-18

        handleSchedulePlanQty(machineTaskMap,machineOrderFinishListMap,suppleBatchNo,logDetail);

        String title="【结合中夜班完成情况，生成差异增补计划列表】";
        autoScheduleLogService.insertCxScheduleLog("", "", title,logDetail.toString()); //添加日

    }



    /**
     * 处理排程计划量
     * @param machineTaskMap 根据机台分组的计划
     * @param machineOrderFinishListMap 根据机台分组的完成量总夜班完成量
     * @param suppleBatchNo 增补计划批次号
     */
    private void handleSchedulePlanQty(Map<String, List<CxEngineScheduleResult>> machineTaskMap, Map<String, List<CxMiddleNightFinishQty>> machineOrderFinishListMap,String suppleBatchNo,StringBuilder logDetail) {

        logDetail.append("【处理排程计划量】开始遍历所有机台进行单机台任务增补计划生成").append(division);
        /**
         * 遍历所有机台任务进行成型计划处理start
         */
        //结合中夜班计划和中夜班完成量情况生成对应增补计划列表
        List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList =new ArrayList<>();
        logDetail.append("【增补计划列表】初始化=========>").append(division);
        for(Map.Entry<String, List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()){
            //存储机台增补计划集合
            List<CxEngineLastDaySupplePlan> machineSupplePlanList=new ArrayList<>();
            //机台编号
            String cxMachineCode=entry.getKey();
            logDetail.append("当前机台编号【").append(cxMachineCode).append("】").append(division);
            //机台排程任务列表
            List<CxEngineScheduleResult> machineScheduleList=entry.getValue();
            logDetail.append("当前机台编号【").append(cxMachineCode).append("】任务列表").append(JSON.toJSONString(machineScheduleList)).append(division);
            //遍历机台中所有工单任务的差异量，生成增补计划
            calcDiffQtyData(cxMachineCode,machineScheduleList,machineOrderFinishListMap,suppleBatchNo,machineSupplePlanList,logDetail);
            if(StringUtils.isNotEmpty(machineSupplePlanList)){
                cxEngineLastDaySupplePlanList.addAll(machineSupplePlanList);
            }
        }
        /**
         * 遍历所有机台任务进行成型计划处理end
         */
        if(StringUtils.isNotEmpty(cxEngineLastDaySupplePlanList)){
            logDetail.append("【处理排程计划量】自动创建增补计划列表").append(JSON.toJSONString(cxEngineLastDaySupplePlanList));
            cxEngineLastDaySupplePlanService.batchInsertLastDaySupplePlan(cxEngineLastDaySupplePlanList);
        }

    }

    /**
     * 根据排序后的集合进行默认生产顺序设置
     * @param cxEngineLastDaySupplePlanList
     */
    private void sortByListAddPlanDefaultSort(String cxMachineCode,List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList) {
        /**
         * 遍历进行默认生产顺序设置
         */
        Integer defaultBeginSort=BigDecimal.ONE.intValue();
        defaultBeginSort = setInProductSpec(cxMachineCode,defaultBeginSort,cxEngineLastDaySupplePlanList);
        //2022-03-02 此处调整成从成型在产数据中获取在产规格在第一个顺序中


        //优先排产投产规格顺序
        for(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan:cxEngineLastDaySupplePlanList){

            if(CxEngineConstants.TO_PRODUCT_NO.equals(cxEngineLastDaySupplePlan.getToProduct())){
                continue;
            }

            if(cxEngineLastDaySupplePlan.getIsProduct()){
                continue;
            }
            cxEngineLastDaySupplePlan.setPlanSort(defaultBeginSort);
            defaultBeginSort++;
        }

        //再排产未投顺序
         for(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan:cxEngineLastDaySupplePlanList){
           if(CxEngineConstants.TO_PRODUCT_NO.equals(cxEngineLastDaySupplePlan.getToProduct())){
             cxEngineLastDaySupplePlan.setPlanSort(defaultBeginSort);
             defaultBeginSort++;
           }
        }
    }

    /**
     * 设置在产的规格为第一个顺序
     * @param cxMachineCode
     * @param defaultBeginSort
     * @param cxEngineLastDaySupplePlanList
     * @return
     */
    private Integer setInProductSpec(String cxMachineCode, Integer defaultBeginSort, List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList) {
        if(StringUtils.isEmpty(machineInProductMap)||!machineInProductMap.containsKey(cxMachineCode)){
            log.debug(StringUtils.format("当前机台:{},未找到在产规格,排序默认从三班计划量最小的优先进行排序"));
            return  defaultBeginSort;
        }
        //获取到在产胎胚
        String inProductEmbryoCode=machineInProductMap.get(cxMachineCode);
        for(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan : cxEngineLastDaySupplePlanList){
            String embryoCode=cxEngineLastDaySupplePlan.getEmbryoCode();

            //Joran 2022-03-04 标记不投产的不进行生产顺序标记 start
            if(CxEngineConstants.TO_PRODUCT_NO.equals(cxEngineLastDaySupplePlan.getToProduct())){
                continue;
            }
            //Joran 2022-03-04 标记不投产的不进行生产顺序标记 end

            if(embryoCode.equals(inProductEmbryoCode)){
                //设置为第一个顺序
                cxEngineLastDaySupplePlan.setPlanSort(defaultBeginSort);
                //标记为在产规格
                cxEngineLastDaySupplePlan.setIsProduct(true);
                //顺序往后
                defaultBeginSort+=1;
                break;
            }
        }
        return defaultBeginSort;

    }

    /**
     *  计算机台工单任务列表中的差异数据计划
     * @param cxMachineCode 成型机台编号
     * @param machineScheduleList 机台工单任务列表
     * @param machineOrderFinishListMap 机台工单完成量回报数据集
     * @param suppleBatchNo 增补计划批次号
     */
    private void calcDiffQtyData(String cxMachineCode,List<CxEngineScheduleResult> machineScheduleList, Map<String, List<CxMiddleNightFinishQty>> machineOrderFinishListMap,String suppleBatchNo, List<CxEngineLastDaySupplePlan> machineSupplePlanList,StringBuilder logDetail) {

        logDetail.append("【计算机台工单任务列表中的差异数据计划】结合中夜班计划量和中夜班完成情况，进行差异增补计划生成方法》》》》》").append(division);
        //加载成型差异量增补限定参数
      /*  Integer defaultDiffQtyCondition= CxEngineConstants.DEFAULT_FINISH_PLAN_DIFF_CONDITION;
        Map<String,String> params=commonCacheService.loadCxParamsMap();
        if(StringUtils.isNotEmpty(params)&&params.containsKey(CxParamCodeConstants.FINISH_PLAN_DIFF_CONDITION)){
            String diffQtyConditionStr=params.get(CxParamCodeConstants.FINISH_PLAN_DIFF_CONDITION);
            defaultDiffQtyCondition=Integer.parseInt(diffQtyConditionStr);
        }
        log.debug("【前天三班计划增补】获取成型参数差异量增补限定量="+defaultDiffQtyCondition);
        logDetail.append("【前天三班计划增补】获取成型参数差异量增补限定量=").append(defaultDiffQtyCondition).append(division);*/
        //机台对应工单中夜班完成量列表
        Map<String, CxMiddleNightFinishQty> unionKeyMap =new HashMap<>();
        if(StringUtils.isNotEmpty(machineOrderFinishListMap)&&machineOrderFinishListMap.containsKey(cxMachineCode)){
            List<CxMiddleNightFinishQty> finishQtyList=machineOrderFinishListMap.get(cxMachineCode);
            unionKeyMap = CollectionUtil.toMap(finishQtyList, finishQty -> GenerageMapKeyUtils.createMapKey(finishQty.getEmbryoCode(),finishQty.getBomDataVersion()));
        }

        for(CxEngineScheduleResult cxEngineScheduleResult:machineScheduleList){
            //成型工单号
            String cxOrderNo=cxEngineScheduleResult.getOrderNo();
            String embryoCode=cxEngineScheduleResult.getEmbryoCode();
            String bomDataVersion =cxEngineScheduleResult.getBomDataVersion();
            String machKey=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
            //中夜班完成量
            Integer middleNightFinishQty=0;
            //中夜班计划量
            Integer middleNightPlanQty=cxEngineScheduleResult.getClass1PlanQty()+cxEngineScheduleResult.getClass2PlanQty();
            logDetail.append("【前天三班计划增补】基础信息：").append(StringUtils.format("胎胚代码={}，施工版本：{},中夜班计划量={}",embryoCode,bomDataVersion,middleNightPlanQty)).append(division);
            if(unionKeyMap.containsKey(machKey)){
                CxMiddleNightFinishQty cxMiddleNightFinishQty=unionKeyMap.get(machKey);
                //获取中夜班完成量
                middleNightFinishQty=cxMiddleNightFinishQty.getMiddleNightQty();
            }
            //中夜班计划量和完成量差异值
            Integer diffQty=middleNightPlanQty-middleNightFinishQty;
            log.debug(StringUtils.format("【前天三班计划增补】,成型工单号：{},中夜班计划量：{}，中夜班完成量：{},最终差异量：{}",cxOrderNo,middleNightPlanQty,middleNightFinishQty,diffQty));
            logDetail.append("【前天三班计划增补】基础信息：").append(StringUtils.format("【前天三班计划增补】,成型工单号：{},中夜班计划量：{}，中夜班完成量：{},最终差异量：{}",cxOrderNo,middleNightPlanQty,middleNightFinishQty,diffQty)).append(division);

            //存在生产不足计划量进行计划量补充
            /*if(diffQty > 0 && diffQty > defaultDiffQtyCondition){
                CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan =BeanConverUtil.conver(cxEngineScheduleResult,CxEngineLastDaySupplePlan.class);
                cxEngineLastDaySupplePlan.setId(null);
                cxEngineLastDaySupplePlan.setSuppleBatchNo(suppleBatchNo);
                cxEngineLastDaySupplePlan.setSupplePlanQty(diffQty);
                cxEngineLastDaySupplePlan.setBaseVale(null);
                cxEngineLastDaySupplePlanList.add(cxEngineLastDaySupplePlan);
            }*/
            //重新计算可硫化班数
            CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
            CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan =BeanConverUtil.conver(cxEngineScheduleResult,CxEngineLastDaySupplePlan.class);
            cxEngineLastDaySupplePlan.setId(null);
            cxEngineLastDaySupplePlan.setSuppleBatchNo(suppleBatchNo);
            //根据昨日排程生成全部的增补计划
            cxEngineLastDaySupplePlan.setSupplePlanQty(diffQty>0?diffQty:0);
            cxEngineLastDaySupplePlan.setBaseVale(null);
            //Joran 2022-03-15 Nick 2023-12-08 白班计划量将三班的计划量复制进去
            cxEngineLastDaySupplePlan.setClass3PlannedQty(cxEngineScheduleResult.getClass3PlanQty());

            machineSupplePlanList.add(cxEngineLastDaySupplePlan);

        }

        //现根据增补计划中三班的平均可硫化班次进行升序排序
        Comparator<CxEngineLastDaySupplePlan> class3PlannedAvailableShift = Comparator.comparing(CxEngineLastDaySupplePlan::getClass3PlannedAvailableLhShift);
        Collections.sort(machineSupplePlanList,class3PlannedAvailableShift);
        //根据排序后的集合进行增补计划生产顺序默认排序
        sortByListAddPlanDefaultSort(cxMachineCode,machineSupplePlanList);
    }

    /**
     * 增补计划自动进行成型排程结果增补
     * @param suppleDate
     * @throws CxScheduleEngineException
     */
    @Transactional
    public synchronized void cxScheduleAutoSupple(Date suppleDate) throws CxScheduleEngineException{

        boolean isCancel=true;
        if(isCancel){
            log.debug("计划增补接口已经调整为成型自动排程时进行处理，接口功能关闭。");
            return;
        }

        StringBuilder logDetail = new StringBuilder("【成型前日排程自动增补日志】：").append(division);
        String title="【成型前日排程计划增补过程】";
        /**
         * 根据增补日期获取增补计划start
         */
        String suppleDateStr= DateUtils.parseDateToStr("yyyyMMdd",suppleDate);

        //先验证是否存在批次未确认的数据
        List<CxEngineSuppleBatchRecord> unConfirmList=getSuppleBatchRecordByCondition(suppleDateStr,CxEngineConstants.SUPPLE_BATCH_STATUS_NO);
        if(StringUtils.isEmpty(unConfirmList)){
            logDetail.append("增补批次中没有未确认的增补计划，不再继续往下执行自动增补").append(division);
            autoScheduleLogService.insertCxScheduleLog("", "", title,logDetail.toString()); //添加日志
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.supple.unConfirm.empty.error"),suppleDateStr));
        }

        CxEngineLastDaySupplePlan condition= new CxEngineLastDaySupplePlan();
        condition.setSuppleDateStr(suppleDateStr);
        List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList =this.cxEngineLastDaySupplePlanService.selectSupplePlanListByCondition(condition);

        //不管有没有增补计划都进行状态更新为已确认
        CxEngineSuppleBatchRecord confirmRecord = new CxEngineSuppleBatchRecord();
        confirmRecord.setStatus(CxEngineConstants.SUPPLE_BATCH_STATUS_YES);
        confirmRecord.setSuppleDateStr(suppleDateStr);
        cxEngineSuppleBatchRecordService.updateCxEngineSuppleBatchRecord(confirmRecord);

        if(StringUtils.isEmpty(cxEngineLastDaySupplePlanList)){
            logDetail.append("没有对应的增补计划不需要进行增补").append(division);
            autoScheduleLogService.insertCxScheduleLog("", "", title,logDetail.toString()); //添加日志
            //throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.supple.unConfirm.empty.error"),suppleDateStr));
            return;
        }
        //获取到增补批次号
        String suppleBatchNo=cxEngineLastDaySupplePlanList.get(0).getSuppleBatchNo();

        //加载施工和工序参数
        initNeedBaseData();

        Map<String,List<CxEngineLastDaySupplePlan>>  machineSupplePlanMap=cxEngineLastDaySupplePlanList.stream().collect(Collectors.groupingBy(CxEngineLastDaySupplePlan::getCxMachineCode));
        /**
         * 根据增补日期获取增补计划end
         */

        /**
         * 根据增补日期获取增补日期对应的成型排程计划列表 start
         */
        List<CxEngineScheduleResult> lastDateScheduleList=commonCacheService.getLastPlanResultList(suppleDate,"","",false,true);
        List<CxEngineScheduleResult> toProductList =new ArrayList<>();
        toProductList(lastDateScheduleList,toProductList);
        Map<String,List<CxEngineScheduleResult>> machineTaskMap= CxScheduleUtils.splitTaskByCxMachine(toProductList);
        if(StringUtils.isEmpty(machineTaskMap)){
            logDetail.append("【前天三班计划增补】前一天排程计划全部收尾，不需要进行增补").append(division);
            return;
        }

        /**
         * 更新前一天日期库存数据start
         */
        commonCacheService.updateLastDayTaskStock(lastDateScheduleList,suppleDate,logDetail,true);
        /**
         * 更新前一天日期库存数据end
         */

        //对机台中的任务列表进行顺序安排
        setMachinePlanSort(machineTaskMap);

        //验证是否排序成功
        //testSortSuccess(machineTaskMap);
        /**
         * 根据增补日期获取增补日期对应的成型排程计划列表 end
         */
        //自动进行计划量增补功能
        List<CxEngineScheduleResult> suppleScheduleList= null;
        try {
            suppleScheduleList = scheduleAutoSupple(machineTaskMap,machineSupplePlanMap,logDetail);
            //计划增补更新，成型前日计划版本留存
            if(StringUtils.isNotEmpty(suppleScheduleList)){
                cxEngineLastDaySupplePlanService.batchUpdateLastScheduleTask(suppleBatchNo,suppleScheduleList,lastDateScheduleList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CxScheduleEngineException(e.getMessage());
        }finally {
            autoScheduleLogService.insertCxScheduleLog("", "", title,logDetail.toString()); //添加日志
        }


    }

    /**
     * 初始化所需要的基础数据
     */
    private void initNeedBaseData() {
        //加载成型参数
        cxParamsMap=commonCacheService.loadCxParamsMap();
        //加载全部胎胚的施工信息
        engineConstructionInfoMap=commonCacheService.loadEngineConstructionMapFromRedis();
    }

    /**
     *  自动进行计划量增补
     * @param machineTaskMap 所有机台计划列表
     * @param machineSupplePlanMap 所有机台任务增补计划
     */
    private  List<CxEngineScheduleResult>  scheduleAutoSupple(Map<String, List<CxEngineScheduleResult>> machineTaskMap, Map<String, List<CxEngineLastDaySupplePlan>> machineSupplePlanMap,StringBuilder logDetail) {
        //记录所有工单调整结果
        List<CxEngineScheduleResult> suppleScheduleList=new ArrayList<>();
        //遍历所有机台计划查找是否有需要增补的计划工单
        machineShiftHourMap=new HashMap<>();

        //记录规格各个班次的计划量
        Map<String,Integer> taskShiftPlanQty=new HashMap<>();
        //记录任务各个班次的原因分析
        Map<String,String> taskShiftAnalysis=new HashMap<>();

        /**
         * 开始遍历全部机台任务列表start
         */
        for (Map.Entry<String, List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()) {
            String machineCode=entry.getKey();
            //初始化机台各个班次的剩余时长
            initMachineClassShiftHour(machineCode);
            logDetail.append("当前机台编号：【"+machineCode+"】自动增补日志.").append(division);
            //机台没有对应的增补计划则跳过 遍历其他机台任务
            if(!machineSupplePlanMap.containsKey(machineCode)){
                logDetail.append("没有机台增补计划，自动跳过不进行自动增补").append(division);
                continue;
            }
            //获取到机台增补计划列表
            List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList=machineSupplePlanMap.get(machineCode);
            if(StringUtils.isEmpty(cxEngineLastDaySupplePlanList)){
                logDetail.append("机台增补计划列表为空，自动跳过不进行自动增补").append(division);
                continue;
            }
            List<CxEngineScheduleResult> sameMachineTaskList=entry.getValue();
            //转换成工单增补计划集合
            Map<String, CxEngineLastDaySupplePlan> orderNoSuppleMap = CollectionUtil.toMap(cxEngineLastDaySupplePlanList, CxEngineLastDaySupplePlan::getOrderNo);
            /**
             * 开始遍历机台所有的任务进行任务量增补start
             */
            CxEngineScheduleResult beforeCxEngineScheduleResult=null; //前一个增补计划
            //计划增补结束班次，下个计划开始班次
            Integer beforeSuppleEndClsIndex=ClassEnums.CLASS_THREE.getClassIndex();

            //原因分析获取的步数
            Integer analysisStep=0;
            //原因分析开始下标
            Integer analysisBeginIndex=ClassEnums.CLASS_THREE.getClassIndex();
            /**
             * 遍历单个机台全部任务列表进行增补start
             */
            for(CxEngineScheduleResult cxEngineScheduleResult:sameMachineTaskList){
                //从三班开始各个班次的计划量和原因分析进行【工单号+班次】缓存保留
                CxScheduleUtils.cacheFromClass3PlanQtyAndAnalysis(cxEngineScheduleResult,taskShiftPlanQty,taskShiftAnalysis);
                //复制一份出来调整
                CxEngineScheduleResult currentCxScheduleResultCopy=BeanConverUtil.conver(cxEngineScheduleResult,CxEngineScheduleResult.class);
                //胎胚代码
                String embryoCode=currentCxScheduleResultCopy.getEmbryoCode();
                //施工版本信息
                String bomDataVersion=currentCxScheduleResultCopy.getBomDataVersion();
                String orderNo=currentCxScheduleResultCopy.getOrderNo();

                if(beforeSuppleEndClsIndex > ClassEnums.CLASS_FIVE.getClassIndex()){
                    logDetail.append("当前机台班次已全部被占满：【"+machineCode+"】机台其他计划自动进行清空，清零操作.").append(division);
                    //清空3班及之后的计划量和原因分析
                    CxScheduleUtils.cleanFromClass3PlanQtyAndAnalysis(currentCxScheduleResultCopy);
                    //重新计算各个班次可硫化班次数
                    CxScheduleUtils.calcAllClassAvailableLhShift(currentCxScheduleResultCopy);
                    //添加到增补计划列表中
                    suppleScheduleList.add(currentCxScheduleResultCopy);
                    beforeCxEngineScheduleResult=currentCxScheduleResultCopy;
                    continue;
                }
                Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(machineCode,embryoCode,bomDataVersion,logDetail);
                //Integer machineQuota=350; //TODO 测试写死定额
                logDetail.append("机台编号定额：【"+machineQuota+"】.").append(division);
                /**
                 * 存在前规格增补start
                 */
                if(beforeCxEngineScheduleResult != null){
                    //当前规格
                    String afterKey=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
                    //前规格
                    String beforeKey=GenerageMapKeyUtils.createMapKey(beforeCxEngineScheduleResult.getEmbryoCode(),beforeCxEngineScheduleResult.getBomDataVersion());

                    logDetail.append("当前工单号：【"+currentCxScheduleResultCopy.getOrderNo()+"】前规格工单号【"+beforeCxEngineScheduleResult.getOrderNo()+"】，").append("后续可增补开始班次：").append(beforeSuppleEndClsIndex).append(division);

                    //获取剩余总任务量(三班及往后的任务量)
                    int taskTotalQty=currentCxScheduleResultCopy.getAfterClass3PlanQty();
                    //当前规格三班后没有任务量的话，则不用进行调整
                    if(taskTotalQty > 0) {
                        //清空3班及之后的计划量和原因分析
                        CxScheduleUtils.cleanFromClass3PlanQtyAndAnalysis(currentCxScheduleResultCopy);
                        //更换工装时长
                        Double changeSpecTime=commonCacheService.taskChangeSpecTime(engineConstructionInfoMap,cxParamsMap,beforeKey,afterKey,logDetail);
                        //Double changeSpecTime=2.0D; //TODO 施工缺失，先写死方便测试
                        logDetail.append("工装更换时长【"+changeSpecTime+"】").append(division);
                        //前规格增补班次剩余时间获取
                        Double machineShiftHour=getMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex);
                        logDetail.append("获取班次剩余时长【"+machineShiftHour+"】").append(division);
                        //班次剩余时间小于更换工装时间，直接往下一个班次排
                        if(machineShiftHour < changeSpecTime){
                            logDetail.append("班次剩余时长小于更换工装时间，直接进入下个班次开始安排计划").append(division);
                            //下个班次
                            int nextClsIndex=beforeSuppleEndClsIndex+1;
                            //所有班次已经排完，机台不再进行增补记录
                            if(nextClsIndex > ClassEnums.CLASS_FIVE.getClassIndex()){
                                logDetail.append("超过可安排限定班，不再进行规格计划安排，规格计划数，原因分析自动清0和清空").append(division);
                                //重新计算各个班次可硫化班次数
                                CxScheduleUtils.calcAllClassAvailableLhShift(currentCxScheduleResultCopy);
                                //添加到增补计划列表中
                                suppleScheduleList.add(currentCxScheduleResultCopy);
                                break;
                            }else{ //总任务量安排
                                beforeSuppleEndClsIndex=nextClsIndex;
                                //遍历开始处理重新安排班次计划
                                while(beforeSuppleEndClsIndex <= ClassEnums.CLASS_FIVE.getClassIndex() && taskTotalQty > 0){
                                    if(taskTotalQty >= machineQuota){ //剩余任务量大于定额直接安排定额数据
                                        logDetail.append("可安排剩余任务量1=").append(taskTotalQty).append("，大于机台定额，直接安排定额，然后进行下个班次，当前班次【").append(beforeSuppleEndClsIndex).append("】").append(division);
                                        //设置任务量
                                        CxScheduleUtils.setClassShiftPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex),machineQuota);
                                        //平移原因分析
                                        CxScheduleUtils.setClassAnalysisByMap(currentCxScheduleResultCopy,beforeSuppleEndClsIndex,analysisBeginIndex,analysisStep,taskShiftAnalysis);
                                        taskTotalQty-=machineQuota;
                                        //班次剩余时间设置为0
                                        updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex,BigDecimal.ZERO.doubleValue());
                                        beforeSuppleEndClsIndex++;//继续下一个班次
                                        analysisStep++;//原因分析继续往下
                                    }else{
                                        logDetail.append("可安排剩余任务量1=").append(taskTotalQty).append("，小于机台定额，直接安排可安排计划量，当前班次【").append(beforeSuppleEndClsIndex).append("】").append(division);
                                        //设置任务量为剩余任务量
                                        CxScheduleUtils.setClassShiftPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex),taskTotalQty);
                                        //平移原因分析
                                        CxScheduleUtils.setClassAnalysisByMap(currentCxScheduleResultCopy,beforeSuppleEndClsIndex,analysisBeginIndex,analysisStep,taskShiftAnalysis);
                                        taskTotalQty=0;
                                        //计算班次剩余时间
                                        Double shiftRemainTime=commonCacheService.getClassShiftRemainTime(machineShiftHour,machineQuota,taskTotalQty);
                                        //扣除掉更换工装的时长还有剩余的时间才是班次当前可用时间
                                        updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex,shiftRemainTime);
                                        analysisStep++;//原因分析继续往下
                                        logDetail.append("计算班次剩余时间1=").append("【").append(shiftRemainTime).append("】").append(division);

                                    }
                                }

                            }
                        }else{ //当前班次开始安排
                            //计算班次剩余时间
                            logDetail.append("班次剩余时长大于更换工装时间，当前班次开始进行增补").append(division);
                            machineShiftHour-=changeSpecTime;
                            logDetail.append("扣除更换工装时长，剩余时长=").append(machineShiftHour).append(division);
                            BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR)); //一个小时生产多少
                            logDetail.append("计算小时产量=").append(hourCountBig).append(division);
                            BigDecimal remainTimeBig=BigDecimal.valueOf(machineShiftHour); //剩余时间
                            logDetail.append("剩余时间=").append(remainTimeBig).append(division);
                            BigDecimal currentPlanQty=remainTimeBig.multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN);
                            logDetail.append("当前班次可排计划量=").append(currentPlanQty).append(division);
                            Integer planQty=currentPlanQty.intValue();
                            if(taskTotalQty <= planQty){ //剩余任务量小于可安排的计划量时，则只安排剩余任务量
                                logDetail.append("后续三班计划总量小于可排计划量，所以安排计划量=").append(taskTotalQty).append(division);
                                //设置任务量
                                CxScheduleUtils.setClassShiftPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex),taskTotalQty);
                                //平移原因分析
                                CxScheduleUtils.setClassAnalysisByMap(currentCxScheduleResultCopy,beforeSuppleEndClsIndex,analysisBeginIndex,analysisStep,taskShiftAnalysis);
                                //计算班次剩余时间
                                Double shiftRemainTime=commonCacheService.getClassShiftRemainTime(machineShiftHour,machineQuota,taskTotalQty);
                                //扣除掉更换工装的时长还有剩余的时间才是班次当前可用时间
                                updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex,shiftRemainTime);
                                analysisStep++;//原因分析继续往下
                                logDetail.append("当前班次【").append(beforeSuppleEndClsIndex).append("】,剩余时间=").append(shiftRemainTime).append(division);
                            }else{
                                logDetail.append("后续三班计划总量大于可排计划量，所以安排计划量=").append(planQty).append(division);
                                //设置任务量
                                CxScheduleUtils.setClassShiftPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex),planQty);
                                //平移原因分析
                                CxScheduleUtils.setClassAnalysisByMap(currentCxScheduleResultCopy,beforeSuppleEndClsIndex,analysisBeginIndex,analysisStep,taskShiftAnalysis);
                                taskTotalQty-=planQty;
                                //班次剩余时间设置为0
                                updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex,BigDecimal.ZERO.doubleValue());
                                beforeSuppleEndClsIndex++;//继续下一个班次
                                analysisStep++;//原因分析继续往下
                                //遍历开始处理重新安排班次计划
                                while(beforeSuppleEndClsIndex <= ClassEnums.CLASS_FIVE.getClassIndex() && taskTotalQty > 0){
                                    logDetail.append("往后遍历安排计划量》》》》》》》》》").append(division);
                                    if(taskTotalQty >= machineQuota){ //剩余任务量大于定额直接安排定额数据
                                        logDetail.append("可安排剩余任务量2=").append(taskTotalQty).append("，大于机台定额，直接安排定额，然后进行下个班次，当前班次【").append(beforeSuppleEndClsIndex).append("】").append(division);
                                        //设置任务量
                                        CxScheduleUtils.setClassShiftPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex),machineQuota);
                                        //平移原因分析
                                        CxScheduleUtils.setClassAnalysisByMap(currentCxScheduleResultCopy,beforeSuppleEndClsIndex,analysisBeginIndex,analysisStep,taskShiftAnalysis);
                                        taskTotalQty-=machineQuota;
                                        //班次剩余时间设置为0
                                        updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex,BigDecimal.ZERO.doubleValue());
                                        beforeSuppleEndClsIndex++;//继续下一个班次
                                        analysisStep++;//原因分析继续往下
                                    }else{
                                        logDetail.append("可安排剩余任务量2=").append(taskTotalQty).append("，小于机台定额，直接安排可安排计划量，当前班次【").append(beforeSuppleEndClsIndex).append("】").append(division);
                                        //设置任务量为剩余任务量
                                        CxScheduleUtils.setClassShiftPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex),taskTotalQty);
                                        //平移原因分析
                                        CxScheduleUtils.setClassAnalysisByMap(currentCxScheduleResultCopy,beforeSuppleEndClsIndex,analysisBeginIndex,analysisStep,taskShiftAnalysis);
                                        taskTotalQty=0;
                                        //计算班次剩余时间
                                        Double shiftRemainTime=commonCacheService.getClassShiftRemainTime(machineShiftHour,machineQuota,taskTotalQty);
                                        //扣除掉更换工装的时长还有剩余的时间才是班次当前可用时间
                                        updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex,shiftRemainTime);
                                        analysisStep++;//原因分析继续往下
                                        logDetail.append("计算班次剩余时间2=").append("【").append(shiftRemainTime).append("】").append(division);

                                    }
                                }
                            }
                        }
                    }else{
                        logDetail.append("【当前规格后三班无计划量】》当前规格三班后任务剩余量=【"+taskTotalQty+"】不再进行规格任务量填充").append(division);
                    }
                }
                /**
                 * 存在前规格增补end
                 */

                if(!orderNoSuppleMap.containsKey(orderNo)){
                    logDetail.append("当前工单：【"+orderNo+"】没有增补计划，不需要处理，跳过.").append(division);
                    //重新计算各个班次可硫化班次数
                    CxScheduleUtils.calcAllClassAvailableLhShift(currentCxScheduleResultCopy);
                    //添加到增补计划列表中
                    suppleScheduleList.add(currentCxScheduleResultCopy);
                    beforeCxEngineScheduleResult=currentCxScheduleResultCopy;
                    //从当前安排的计划班次之后的剩余时间开始计算班次剩余时间
                    //updateShiftHourByNextShift(machineCode,machineQuota,beforeSuppleEndClsIndex,currentCxScheduleResultCopy,logDetail);
                    continue;
                }
                //获取到增补计划
                CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan=orderNoSuppleMap.get(orderNo);

                //增补量
                Integer supplePlanQty=cxEngineLastDaySupplePlan.getSupplePlanQty();
                logDetail.append("当前工单：【"+orderNo+"】获取到的增补量="+supplePlanQty+".").append(division);

                //遍历当前计划增补班次计划量
                while(beforeSuppleEndClsIndex <= ClassEnums.CLASS_FIVE.getClassIndex() && supplePlanQty > 0){
                    //获取调整开始班次的计划量
                    Integer suppleClassPlanQty= CxScheduleUtils.getCurrentClassPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex));
                    logDetail.append("当前班次序号：【"+beforeSuppleEndClsIndex+"】,获取到的班次计划量="+suppleClassPlanQty).append(division);
                    //计划量小于定额时可以进行增补，否则继续下个班次
                    if(machineQuota > suppleClassPlanQty){
                        int currentSuppleQty=machineQuota-suppleClassPlanQty;
                        logDetail.append("【机台定额大于班次计划量】当前班次定额和差异量结果=：【"+currentSuppleQty+"】").append(division);
                        if(currentSuppleQty > supplePlanQty){
                            logDetail.append("增补计划量小于差异量,当前规格计划增补为原来计划量+增补量，下一个计划规格需要进行班次计划量调整或班次后移").append(division);
                            currentSuppleQty=supplePlanQty;
                            supplePlanQty=0;
                            suppleClassPlanQty+=currentSuppleQty;
                            CxScheduleUtils.setClassShiftPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex),suppleClassPlanQty);
                            logDetail.append("计划增补后的班次计划量="+CxScheduleUtils.getCurrentClassPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex))).append(division);
                            String key=GenerageMapKeyUtils.createMapKey(machineCode,beforeSuppleEndClsIndex+"");
                            Double shiftHour=machineShiftHourMap.get(key);
                            //更新班次剩余时间
                            updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex,commonCacheService.getClassShiftRemainTime(shiftHour,machineQuota,suppleClassPlanQty));
                            beforeCxEngineScheduleResult=currentCxScheduleResultCopy;
                        }else{
                            logDetail.append("增补计划量大于差异量,班次计划量先塞满，当前计划后续班次继续增补！").append(division);
                            supplePlanQty-=currentSuppleQty;
                            logDetail.append("当前剩余增补量="+supplePlanQty).append(division);
                            suppleClassPlanQty+=currentSuppleQty;
                            CxScheduleUtils.setClassShiftPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex),suppleClassPlanQty);
                            logDetail.append("计划增补后的班次计划量="+CxScheduleUtils.getCurrentClassPlanQty(currentCxScheduleResultCopy,ClassEnums.getClassEnums(beforeSuppleEndClsIndex))).append(division);
                            //更新班次剩余时间
                            updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex, BigDecimal.ZERO.doubleValue());
                            beforeSuppleEndClsIndex++;
                            logDetail.append("下个班次的班次顺序="+beforeSuppleEndClsIndex).append(division);
                        }
                    }else{
                        //更新班次剩余时间
                        updateMachineShiftHourMap(machineCode,beforeSuppleEndClsIndex, BigDecimal.ZERO.doubleValue());
                        //往下个班次
                        beforeSuppleEndClsIndex++;
                        logDetail.append("【当前计划量满班排载】直接进入下个班次，下个班次的班次顺序="+beforeSuppleEndClsIndex).append(division);
                    }
                }

                //从当前安排的计划班次之后的剩余时间开始计算班次剩余时间
                //updateShiftHourByNextShift(machineCode,machineQuota,beforeSuppleEndClsIndex,currentCxScheduleResultCopy,logDetail);
                //重新计算各个班次可硫化班次数
                CxScheduleUtils.calcAllClassAvailableLhShift(currentCxScheduleResultCopy);
                //添加到增补计划列表中
                suppleScheduleList.add(currentCxScheduleResultCopy);
            }
            /**
             * 遍历单个机台全部任务列表进行增补end
             */
        }
        /**
         * 开始遍历全部机台任务列表end
         */

        //如果存在更新计划的话则根据工单进行更新
        if(StringUtils.isNotEmpty(suppleScheduleList)){
            appendLogDetail(suppleScheduleList,taskShiftPlanQty,taskShiftAnalysis,logDetail);
        }
        return suppleScheduleList;



    }

    /**
     * 因为是按顺序进行调整，所以中间可能存在没计划再最后又有计划，所以每个规格安排后之后的各个班次都要计算前一个规格计划中剩余的时间
     * @param machineCode 当前机台编号
     * @param machineQuota 当前规格定额
     * @param beforeSuppleEndClsIndex 已经安排的班次
     * @param currentCxScheduleResultCopy 当前排程计划信息
     * @param logDetail 继续下个班次及之后的计划
     */
   /* private void updateShiftHourByNextShift(String machineCode, Integer machineQuota, Integer beforeSuppleEndClsIndex, CxEngineScheduleResult currentCxScheduleResultCopy,StringBuilder logDetail) {
        logDetail.append("【开始计算当前计划后续所有班次的剩余时间】，当前机台编号【"+machineCode+"】，当前机台规格定额=【"+machineQuota+"】当前排班结束的班次下标").append(beforeSuppleEndClsIndex).append(division);
        //遍历所有班次
        for(ClassEnums cls:ClassEnums.values()){
            if(cls.getClassIndex()<=beforeSuppleEndClsIndex){
                logDetail.append("【开始计算当前计划后续所有班次的剩余时间】，前班次【").append(cls.getClassIndex()).append("】，跳过不计算").append(division);
                continue;
            }
            Integer currentPlanQty=CxScheduleUtils.getCurrentClassPlanQty(currentCxScheduleResultCopy,cls);
            Integer currentShiftIndex=cls.getClassIndex();
            logDetail.append("【开始计算当前计划后续所有班次的剩余时间】，当前班次下标").append(currentShiftIndex).append(",获取到的计划量=").append(currentPlanQty).append(division);
            if(currentPlanQty>0){
                logDetail.append("【开始计算当前计划后续所有班次的剩余时间】，需要变更班次剩余时间》》》》》").append(division);
                String key=GenerageMapKeyUtils.createMapKey(machineCode,currentShiftIndex+"");
                Double shiftHour=machineShiftHourMap.get(key);
                //更新班次剩余时间
                Double remainTime=commonCacheService.getClassShiftRemainTime(shiftHour,machineQuota,currentPlanQty);
                updateMachineShiftHourMap(machineCode,currentShiftIndex,remainTime);
                logDetail.append("【开始计算当前计划后续所有班次的剩余时间】，计算的剩余时间=").append(remainTime).append(division);
            }

        }
    }*/

    /**
     * 构建日志
     * @param suppleScheduleList
     * @param taskShiftPlanQty
     * @param taskShiftAnalysis
     * @param logDetail
     */
    private void appendLogDetail(List<CxEngineScheduleResult> suppleScheduleList,Map<String,Integer> taskShiftPlanQty,Map<String,String> taskShiftAnalysis, StringBuilder logDetail) {
        logDetail.append("【计划自动增补后日志:】").append(division);
        for (CxEngineScheduleResult cxEngineScheduleResult:suppleScheduleList){
            String orderNo=cxEngineScheduleResult.getOrderNo();
            logDetail.append("工单号："+cxEngineScheduleResult.getOrderNo()).append(division);
            logDetail.append("机台编号："+cxEngineScheduleResult.getCxMachineCode()).append(division);
            logDetail.append("【==============班次计划对比================】").append(division);
            logDetail.append("原三班计划："+taskShiftPlanQty.get(GenerageMapKeyUtils.createMapKey(orderNo,ClassEnums.CLASS_THREE.getClassIndex()+""))).append(division);
            logDetail.append("增补后三班计划："+cxEngineScheduleResult.getClass3PlanQty()).append(division);
            logDetail.append("原次一班计划："+taskShiftPlanQty.get(GenerageMapKeyUtils.createMapKey(orderNo,ClassEnums.CLASS_FOUR.getClassIndex()+""))).append(division);
            logDetail.append("增补后次一班计划："+cxEngineScheduleResult.getClass4PlanQty()).append(division);
            logDetail.append("原次二班计划："+taskShiftPlanQty.get(GenerageMapKeyUtils.createMapKey(orderNo,ClassEnums.CLASS_FIVE.getClassIndex()+""))).append(division);
            logDetail.append("增补后次二班计划："+cxEngineScheduleResult.getClass5PlanQty()).append(division);
            logDetail.append("【==============原因分析比对================】").append(division);
            logDetail.append("原三班原因分析："+taskShiftAnalysis.get(GenerageMapKeyUtils.createMapKey(orderNo,ClassEnums.CLASS_THREE.getClassIndex()+""))).append(division);
            logDetail.append("增补后三班原因分析："+cxEngineScheduleResult.getClass3Analysis()).append(division);
            logDetail.append("原次一班原因分析："+taskShiftAnalysis.get(GenerageMapKeyUtils.createMapKey(orderNo,ClassEnums.CLASS_FOUR.getClassIndex()+""))).append(division);
            logDetail.append("增补后次一班原因分析："+cxEngineScheduleResult.getClass4Analysis()).append(division);
            logDetail.append("原次二班原因分析："+taskShiftAnalysis.get(GenerageMapKeyUtils.createMapKey(orderNo,ClassEnums.CLASS_FIVE.getClassIndex()+""))).append(division);
            logDetail.append("增补后次二班原因分析："+cxEngineScheduleResult.getClass5Analysis()).append(division);
        }
    }

    /**
     * 初始化各个机台的三班后班次时长
     * @param machineCode
     */
    private void initMachineClassShiftHour(String machineCode) {
        for (ClassEnums cls : ClassEnums.values()) {
            String key= GenerageMapKeyUtils.createMapKey(machineCode,cls.getClassIndex()+"");
            switch (cls){
                case CLASS_ONE:
                case CLASS_TWO:
                    continue;
            }
            if(!machineShiftHourMap.containsKey(key)){
                machineShiftHourMap.put(key, CxEngineConstants.CLASS_SHIFT_HOUR);
            }

        }
    }

    /**
     * 更新班次剩余时长
     * @param cxMachineCode
     * @param classIndex
     */
    private void updateMachineShiftHourMap(String cxMachineCode, Integer classIndex, Double remainTime) {
        String key=GenerageMapKeyUtils.createMapKey(cxMachineCode,classIndex+"");
        if(machineShiftHourMap.containsKey(key)){
            machineShiftHourMap.put(key,remainTime);
        }else{
            machineShiftHourMap.put(key,remainTime);
        }
    }

    /**
     * 根据机台编号班次序号获取班次剩余时间
     * @param cxMachineCode
     * @param classIndex
     * @return
     */
    public Double getMachineShiftHourMap(String cxMachineCode,Integer classIndex){
        String key=GenerageMapKeyUtils.createMapKey(cxMachineCode,classIndex+"");
        if(machineShiftHourMap.containsKey(key)){
            return machineShiftHourMap.get(key);
        }
        return BigDecimal.ZERO.doubleValue();
    }

    /**
     * 设置成型机台
     * @param machineTaskMap
     */
    private void setMachinePlanSort(Map<String, List<CxEngineScheduleResult>> machineTaskMap) {
        for (Map.Entry<String, List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()) {
            String machineCode=entry.getKey();
            List<CxEngineScheduleResult> sameMachineTaskList=entry.getValue();
            //同机台任务进行排序
            CxScheduleUtils.scheduleTaskMachinePlanSort(sameMachineTaskList);
        }
    }

    /**
     * 处理只保留可投产列表
     * @param lastDateScheduleList
     * @param toProductList
     */
    private void toProductList(List<CxEngineScheduleResult> lastDateScheduleList,List<CxEngineScheduleResult> toProductList){
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDateScheduleList){
            CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
            //Joran 2022-02-09 将可以进行计划安排投产的计划单独获取出来start
            if(CxEngineConstants.TO_PRODUCT_YES.equals(cxEngineScheduleResult.getToProduct())){
                toProductList.add(cxEngineScheduleResult);
            }
            //Joran 2022-02-09 将可以进行计划安排投产的计划单独获取出来end
        }
    }

    /**
     * 验证是否排序成功方法
     * @param machineTaskMap
     */
    private void testSortSuccess(Map<String, List<CxEngineScheduleResult>> machineTaskMap) {
        for (Map.Entry<String, List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()) {
            String machineCode=entry.getKey();
            List<CxEngineScheduleResult> sameMachineTaskList=entry.getValue();
            for(CxEngineScheduleResult cxEngineScheduleResult:sameMachineTaskList){
                System.out.println("【机台编号】："+machineCode+"》》【胎胚代码】："+cxEngineScheduleResult.getEmbryoCode()+"》》【生产顺序】："+cxEngineScheduleResult.getPlanSort());
            }

        }
    }


    /**
     * 自动排程前进行增补计划操作验证
     * @param scheduleDate 自动排程前进行增补计划验证
     * @return
     */
    public ValidateResult beforeAutoScheduleValidate(Date scheduleDate){
        ValidateResult result=ValidateResult.success();
        if(scheduleDate==null){
            log.warn("排程日期为空，排程日期为当前日期，传入日期{}",scheduleDate);
            scheduleDate=new Date();
        }
        //获取自动排程前一天日期
        Date lastDate=DateUtils.addDays(scheduleDate,-1);
        String suppleDateStr= DateUtils.parseDateToStr("yyyyMMdd",lastDate);
        //增补计划批次数据
        List<CxEngineSuppleBatchRecord> existList=getSuppleBatchRecordByCondition(suppleDateStr,"");

        if(StringUtils.isEmpty(existList)){
            result=ValidateResult.error(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.supple.unCreate.error"),suppleDateStr));
        }/*else{
            for(CxEngineSuppleBatchRecord record:existList){
                if(CxEngineConstants.SUPPLE_BATCH_STATUS_NO.equals(record.getStatus())){
                    result=ValidateResult.error(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.supple.unConfirm.error"),suppleDateStr));
                    break;
                }
            }
        }*/
        return result;

    }

    /**
     * 根据增补日期验证提交计划增补时是否存在未确认数据
     * @param suppleDateStr 增补日期
     * @return
     */
    private List<CxEngineSuppleBatchRecord>  getSuppleBatchRecordByCondition(String suppleDateStr,String status){
        CxEngineSuppleBatchRecord condition=new CxEngineSuppleBatchRecord();
        if(StringUtils.isNotEmpty(suppleDateStr)){
            condition.setSuppleDateStr(suppleDateStr);
        }
        if(StringUtils.isNotEmpty(status)){
            condition.setStatus(status);
        }
        List<CxEngineSuppleBatchRecord> existList=cxEngineSuppleBatchRecordService.selectCxEngineSuppleBatchRecordList(condition);
        return existList;
    }

    /**
     * 重新生成增补计划
     * @param suppleDate
     * @throws CxScheduleEngineException
     */
    @Transactional
    public synchronized void reCreateLastDaySchedule(Date suppleDate) throws CxScheduleEngineException{
        String suppleDateStr= DateUtils.parseDateToStr("yyyyMMdd",suppleDate);
        //1、删除对应的批次
        cxEngineSuppleBatchRecordService.deleteCxEngineSuppleBatchRecordBySuppleDate(suppleDateStr);
        //2、删除对应的增补计划
        cxEngineLastDaySupplePlanService.deleteCxEngineLastDaySupplePlanBySuppleDate(suppleDateStr);
        //3、重新调用生成接口
        autoMixLastDaySchedule(suppleDate);
    }

    /**
     * 重新设置同机台同胎胚不同外胎的单班硫化量汇总
     * @param cxLastDaySupplePlanDto
     */
    public void reSetSupplePlanSingleLhShiftQty(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        String embryoCode=cxLastDaySupplePlanDto.getEmbryoCode();
        Date scheduleDate=cxLastDaySupplePlanDto.getScheduleDate();
        String machineCode=cxLastDaySupplePlanDto.getCxMachineCode();
        CxEngineLastDaySupplePlan condition=new CxEngineLastDaySupplePlan();
        condition.setEmbryoCode(embryoCode);
        condition.setScheduleDate(scheduleDate);
        condition.setCxMachineCode(machineCode);
        //查询分配硫化机相同机台相同胎胚的成型排程列表
        List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList=this.cxEngineLastDaySupplePlanService.selectCxEngineLastDaySupplePlanList(condition);
        if(StringUtils.isNotEmpty(cxEngineLastDaySupplePlanList)){
            CxScheduleUtils.calcMachineSpecLhShiftCountBySupplePlan(cxEngineLastDaySupplePlanList);
            //进行更新单班硫化量和留存单班硫化量
            cxEngineLastDaySupplePlanService.updateSingleLhQtyBatch(cxEngineLastDaySupplePlanList);
        }

    }
}
