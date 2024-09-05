package com.zlt.aps.cx.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxParamCodeConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxAutoScheduleTask;
import com.zlt.aps.cx.engine.domain.CxEngineEmbryoMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineProductDimensionLimit;
import com.zlt.aps.cx.engine.domain.CxEngineProductMachineLimit;
import com.zlt.aps.cx.engine.domain.CxEngineProductStockLimit;
import com.zlt.aps.cx.engine.domain.CxEngineSapSpecMoldUse;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleLimit;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxEngineSpecifyMachine;
import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;
import com.zlt.aps.cx.engine.enums.ClassEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineEmbryoMonthPlanSurplusService;
import com.zlt.aps.cx.engine.service.CxEngineGroupMachineListService;
import com.zlt.aps.cx.engine.service.CxEngineLossSettingService;
import com.zlt.aps.cx.engine.service.CxEngineProductShiftLimitService;
import com.zlt.aps.cx.engine.service.CxEngineSapSpecMoldUseService;
import com.zlt.aps.cx.engine.service.CxEngineScheduleLimitService;
import com.zlt.aps.cx.engine.service.CxEngineSpecifyMachineService;
import com.zlt.aps.cx.engine.service.CxPlanProductStatusService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 成型工序排程引擎
 */
@Component("cxScheduleTaskService")
@Slf4j
public class CxScheduleTaskService {
    @Autowired
    private CxEngineScheduleLimitService cxEngineScheduleLimitService;

    @Autowired
    private CxEngineSpecifyMachineService cxEngineSpecifyMachineService;

    @Autowired
    private CommonCacheService commonCacheService;

    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    @Autowired
    private CxPlanProductStatusService cxPlanProductStatusService;

    @Autowired
    private CxEngineLossSettingService cxEngineLossSettingService;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private CxEngineProductShiftLimitService cxEngineProductShiftLimitService;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    //获取成型排产限制设置列表
    private Map<String, List<CxEngineScheduleLimit>> scheduleLimitMap;
    //成型工序参数
    private  Map<String,String> cxParamsMap;

    //3.加载定点机台限制作业类型
    private Map<String,List<CxEngineSpecifyMachine>> specifyMachineYesMap;

    //3.加载定点机台不可作业类型
    private Map<String,List<CxEngineSpecifyMachine>> specifyMachineNoMap;

    //所有施工信息
    private Map<String, EngineProductConstructionInfo> engineConstructionInfoMap;

    //耗损率
    private Map<String,Double> cxMachineLossRateMap;

    //待投产规格列表
    private List<CxPlanProductStatus> cxPlanProductStatusList;
    //月度计划汇总(SAP+胎胚+库存地点进行汇总)列表
    private  Map<String,List<MdmMonthProdPlan>> sapEmbryoCodeProdPlanMap;

    //记录汇总同SAP在不同机台之间月度剩余量
    private Map<String,Integer> monthRemainQtyMap=new ConcurrentHashMap<>();

    //Joran 2021-12-27 单机台列表过滤投产规格
    private Map<String,CxPlanProductStatus> autoScheduleIgnoreMap = new ConcurrentHashMap<>();

    //Joran 2021-12-28 缓存各个班次对应的班次剩余时间
    private Map<String,Double> machineShiftHourMap=new HashMap<>();

    //胎胚类型库存汇总集合
    private Map<String,Integer> embryoCodeTypeTotalMap=new HashMap<>();
    //Joran 2022-01-08 同寸口一班平均可硫化班次集合
    private Map<String,Double> sameDimensionAvailableClassOneShiftMap=new HashMap<>();

    //Joran 2022-01-08 胎胚库存班数设定集合
    private Map<String,List<CxEngineProductStockLimit>> cxEngineProductStockLimitListMap;

    //Joran 2022-01-08 同寸口一班平均班数设定调整数据集合
    private Map<Double,List<CxEngineProductDimensionLimit>> cxEngineProductDimensionLimitListMap;

    //Joran 2022-01-08 同机台平均班数设定调整数据集合
    private List<CxEngineProductMachineLimit> cxEngineProductMachineLimitList;

    /**
     * 规格使用模数初始化集合
     */
    private List<CxEngineSapSpecMoldUse> sapSpecMoldUseList;

    /**
     * 异常情况下最大的迭代层数
     */
    private Integer maxRoopCount;

    /**
     * 单机台下迭代层数
     */
    private Integer singleMachineRoopCount;

    @Autowired
    private CxEngineEmbryoMonthPlanSurplusService cxEngineEmbryoMonthPlanSurplusService;

    @Autowired
    private CxEngineGroupMachineListService cxEngineGroupMachineListService;

    @Autowired
    private CxEngineSapSpecMoldUseService cxEngineSapSpecMoldUseService;

    /**
     * 成型自动排程
     * @param machineTaskMap
     * @param cxPlanProductStatusList
     */
    /*public void autoScheduling(Map<String,List<CxEngineScheduleResult>> machineTaskMap,List<CxPlanProductStatus> cxPlanProductStatusList,List<MdmMonthProdPlan> mdmMonthProdPlanList,Map<String,Integer> embryoCodeTypeTotalMap,Map<String,Double> sameDimensionAvailableClassOneShiftMap){
        StringBuilder logDetail =new StringBuilder("开始成型自动排程开始日志：").append(division);
        //数据初始化
        initScheduleData(cxPlanProductStatusList,mdmMonthProdPlanList,logDetail);
        //Joran 2022-01-08 初始化胎胚类型对应的库存汇总信息
        this.embryoCodeTypeTotalMap=embryoCodeTypeTotalMap;
        //Joran 2022-01-08 初始化同寸口一班平均可硫化班次
        this.sameDimensionAvailableClassOneShiftMap=sameDimensionAvailableClassOneShiftMap;
        //前一天排程任务安排
        scheduleLastDaySchedule(machineTaskMap,logDetail);
        String title="【成型所有机台自动排程结果】";
        autoScheduleLogService.insertCxScheduleLog("", "", title,logDetail.toString()); //添加日志
    }*/

    /**
     * 前一天排程任务安排
     * @param machineTaskMap
     */
    /*private void scheduleLastDaySchedule(Map<String, List<CxEngineScheduleResult>> machineTaskMap,StringBuilder logDetail) {
        if(StringUtils.isEmpty(machineTaskMap)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.schedule.record.error"));
        }
        logDetail.append("======================遍历机台进行自动排程日志生成=============================").append(division);
        List<CxEngineScheduleResult> insertTaskList =new ArrayList<>();
        //遍历前一天所有机台 处理机台上所有排程任务start
        for(Map.Entry<String,List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()){
            //成型机台编号
            String  machineCode=entry.getKey();
            logDetail.append("当前机台：").append(machineCode).append("开始自动排程。").append(division);
            //Joran初始化班次可用时间
            initShiftHourMap(machineCode,logDetail);
            List<CxEngineScheduleResult> machineTaskList=entry.getValue();
            //复制一份保留，原始数据保留
            //前一天机台对应的排程结果数据
            List<CxEngineScheduleResult> lastDayScheduleResultList=new ArrayList<>(machineTaskList);
            //如果投产数据为空时则默认设置为可投产
            commonCacheService.defaultToProduct(lastDayScheduleResultList);
            //Joran 2021-12-21 当昨日去除掉收尾的规格后还剩一个规格且为不自动安排，临时将他变更为可安排start
            if(StringUtils.isNotEmpty(lastDayScheduleResultList)){
                if(lastDayScheduleResultList.size()==1&& CxEngineConstants.TO_PRODUCT_NO.equals(lastDayScheduleResultList.get(0).getToProduct())){
                    lastDayScheduleResultList.get(0).setToProduct(CxEngineConstants.TO_PRODUCT_YES);
                }
            }
            //Joran 2021-12-21 当昨日去除掉收尾的规格后还剩一个规格且为不自动安排，临时将他变更为可安排end
            //初始化各个规格的月度剩余量集合
            initMonthRemainQtyMap(lastDayScheduleResultList,logDetail);
            //TODO 预留方法，后续提供机台任务列表中班(一班)计划计算
            scheduleClass1PlanQtyCalc(machineCode,lastDayScheduleResultList);

             //Joran 2021-12-20 对机台任务列表同胎胚的单班硫化量进行合并到投产规格上start
            CxScheduleUtils.calcMachineSpecLhShiftCount(lastDayScheduleResultList);
            //Joran 2021-12-20 对机台任务列表同胎胚的单班硫化量进行合并到投产规格上end

            //所有排程任务进行二班开始的计划安排start
             scheduleByMachineScheduleResult(machineCode,lastDayScheduleResultList,insertTaskList,logDetail);
            //所有排程任务进行二班开始的计划安排end
        }
        logDetail.append("======================遍历机台进行自动排程日志结束=============================").append(division);
        //遍历前一天所有机台 处理机台上所有排程任务end
        if(StringUtils.isNotEmpty(insertTaskList)){
            //插入排程结果表
            cxScheduleEngineMapper.batchInsertCxScheduleResult(insertTaskList);
        }
    }
*/
    /**
     * 初始化班次可用时间
     * @param machineCode
     */
    private void initShiftHourMap(String machineCode,StringBuilder logDetail) {
        logDetail.append("=============初始化机台各个班次的可用时间============").append(division);
        machineShiftHourMap=new HashMap<>();
        for (ClassEnums cls : ClassEnums.values()) {
            String key=GenerageMapKeyUtils.createMapKey(machineCode,cls.getClassIndex()+"");
            machineShiftHourMap.put(key, CxEngineConstants.CLASS_SHIFT_HOUR);
        }
    }


    /**
     * 初始化各个规格的月度剩余量
     * @param lastDayScheduleResultList
     */
    /*private void initMonthRemainQtyMap(List<CxEngineScheduleResult> lastDayScheduleResultList,StringBuilder logDetail) {
        logDetail.append("【成型自动排程前进行初始化各个规格月度剩余量】").append(division);
        //Joran 2021-09-06 提示收尾数量工序参数设置值
        Integer closeOutNumber=commonCacheService.getCloseOutTipSetting(cxParamsMap);
        //Joran 2022-01-07 月度剩余量低于设定值后不进行计划安排，to_product标记为no
        Integer unProductCount= commonCacheService.getUnProductMonthRemainQty(cxParamsMap);
        for (CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            String embryoCode=cxEngineScheduleResult.getEmbryoCode();
            //月度剩余量
            Integer remainMonthQty=0;
            //前一天三班计划量
            Integer lastDayClass3PlanQty=cxEngineScheduleResult.getLastClass3PlanQty();
            if(!monthRemainQtyMap.containsKey(embryoCode)){
                remainMonthQty=cxEngineScheduleResult.getMonthRemainQty();
            }else{
                remainMonthQty=monthRemainQtyMap.get(embryoCode);
            }
            //扣除掉三班计划量
            remainMonthQty-=lastDayClass3PlanQty;
            if(remainMonthQty<=0){
                //剩余量不够时将一班计划量置0
                cxEngineScheduleResult.setClass1PlanQty(0);
                monthRemainQtyMap.put(embryoCode,0);
            }else{
                //Joran 2021-09-06 扣除三班计划收尾提示start
                if(remainMonthQty<=closeOutNumber){
                    cxEngineScheduleResult.setMarkCloseOutTip(CxEngineConstants.CLOSE_OUT_TIP_YES);
                    log.debug("标记收尾提醒SAP："+cxEngineScheduleResult.getSapCode()+"胎胚代码"+cxEngineScheduleResult.getEmbryoCode());
                    logDetail.append("月度剩余量扣除前一天三班计划量剩余计划，需要进行收尾提示标记").append(division);
                }
                //Joran 2021-09-06 扣除三班计划收尾提示end

                //Joran 2022-01-07 月度剩余量剩余数如果低于设定值后标记为不自动安排计划start
                if(remainMonthQty<=unProductCount){
                    logDetail.append("当前胎胚代码【").append(embryoCode).append("】,当前月度剩余量=").append(remainMonthQty).append("<=月度剩余量设定不投产值（").append(unProductCount+")").append("规格标记不投产").append(division);
                    cxEngineScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_NO);
                }
                //Joran 2022-01-07 月度剩余量剩余数如果低于设定值后标记为不自动安排计划end

                monthRemainQtyMap.put(embryoCode,remainMonthQty);
            }
            logDetail.append("【初始化月度剩余量】，SAP品号："+cxEngineScheduleResult.getSapCode()+",胎胚代码："+embryoCode+",月度剩余量："+remainMonthQty+",扣除前一天三班计划量："+lastDayClass3PlanQty).append(division);

        }
    }*/

    /**
     * 预留方法，可以在此进行1班计划量调整
     * @param machineCode 成型机台编号
     * @param lastDayScheduleResultList 成型机台任务
     */
    private void scheduleClass1PlanQtyCalc(String machineCode,List<CxEngineScheduleResult> lastDayScheduleResultList) {
        log.debug("机台："+machineCode+"任务列表，任务列表复制前一班排程，预留修改一班计划量方法，任务数量："+(lastDayScheduleResultList==null?0:lastDayScheduleResultList.size()));
        //TODO 重新计算计划量后 月度剩余量需要扣减掉一班的计划量
        return;
    }

    /**
     * 处理单个成型机任务列表
     * @param machineCode
     * @param lastDayScheduleResultList
     */
    private void scheduleByMachineScheduleResult(String machineCode, List<CxEngineScheduleResult> lastDayScheduleResultList,List<CxEngineScheduleResult> insertTaskList,StringBuilder logDetail) {
        if(StringUtils.isEmpty(lastDayScheduleResultList)){
            log.debug("【停止排程】成型机台："+machineCode+",前一天安排的任务数量为0,当前机台不进行自动排程");
            return;
        }
        logDetail.append("【单机台任务自动排程】").append("机台编号：").append(machineCode).append(",机台前一天任务列表：").append(toJSONString(lastDayScheduleResultList)).append(division);
        List<CxEngineScheduleResult> monthRemainQtyList=new ArrayList<>();
        //月度剩余量为0的数据
        List<CxEngineScheduleResult> lastDayRemainResultList=new ArrayList<>(lastDayScheduleResultList);
        //1班次任务顺序设置
        class1ShiftTaskSort(lastDayScheduleResultList,monthRemainQtyList,logDetail);
        if(StringUtils.isNotEmpty(monthRemainQtyList)){
            //初始化
            maxRoopCount= CxEngineConstants.AUTO_SCHEDULE_MAX_ROOP_COUNT;
            singleMachineRoopCount=0;
            lastDayRemainResultList.removeAll(monthRemainQtyList);//剩余月度剩余量为0的数据集
            //根据一班的生产顺序进行降序排序
            CxScheduleUtils.resultSortAscByClassShiftSort(monthRemainQtyList, ClassEnums.CLASS_ONE);
            //前一天机台任务预排+新规格投产
            preScheduleTask(machineCode,monthRemainQtyList,logDetail);
            insertTaskList.addAll(monthRemainQtyList);
           /* List<CxEngineScheduleResult> insertList=new ArrayList<>(monthRemainQtyList);
            insertList.addAll(lastDayRemainResultList);*/
            logDetail.append("【机台任务自动排程】").append(",自动排程结束后结果列表：").append(toJSONString(monthRemainQtyList)).append(division);
        }else{
            //插入排程结果表
            //cxScheduleEngineMapper.batchInsertCxScheduleResult(lastDayRemainResultList);
            logDetail.append("【机台任务自动排程】").append(",自动排程结束后结果列表：").append(toJSONString(lastDayRemainResultList)).append(division);
        }
        insertTaskList.addAll(lastDayRemainResultList);

    }

    /**
     * 根据前一日排程任务列表进行数据预排
     * @param monthRemainQtyList
     */
    private void preScheduleTask(String machineCode,List<CxEngineScheduleResult> monthRemainQtyList,StringBuilder logDetail) {
        logDetail.append("开始机台任务预排,当前机台："+machineCode+",开始时间:"+ DateUtils.getTime()).append(division);
        //预排任务前一天排程结果
        CxEngineScheduleResult preCxEngineScheduleResult=null;
        for(CxEngineScheduleResult cxEngineScheduleResult:monthRemainQtyList){
            if(cxEngineScheduleResult.getClass1Sort()<1||CxEngineConstants.TO_PRODUCT_NO.equals(cxEngineScheduleResult.getToProduct())){
                continue;
            }
            preCxEngineScheduleResult=cxEngineScheduleResult;
            break;//跳出循环
        }

        //Joran 2021-09-10 有计划列表，但是没有次一班有排计划的时候根据一班可硫化班次少的优先安排投产start
        List<CxEngineScheduleResult> toProductList =new ArrayList<>();
        if(StringUtils.isNotEmpty(monthRemainQtyList)&&preCxEngineScheduleResult==null){
            for(CxEngineScheduleResult cxEngineScheduleResult:monthRemainQtyList){
                CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
                //Joran 2021-12-20 将可以进行计划安排投产的计划单独获取出来start
                if(CxEngineConstants.TO_PRODUCT_YES.equals(cxEngineScheduleResult.getToProduct())){
                    toProductList.add(cxEngineScheduleResult);
                }
                //Joran 2021-12-20 将可以进行计划安排投产的计划单独获取出来end
            }
            if(StringUtils.isNotEmpty(toProductList)){
                preCxEngineScheduleResult= getPreScheduleResult(toProductList,ClassEnums.CLASS_ONE);
            }else{
                preCxEngineScheduleResult= getPreScheduleResult(monthRemainQtyList,ClassEnums.CLASS_ONE);
                preCxEngineScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_YES);
            }
            logDetail.append("【机台编号：【"+machineCode+"】】，前一天计划次一班没有计划，重新挑选规格胎胚："+preCxEngineScheduleResult.getEmbryoCode()).append(division);
        }
        //Joran 2021-09-10 有计划列表，但是没有次一班有排计划的时候根据一班可硫化班次少的优先安排投产end

        if(preCxEngineScheduleResult==null){
            logDetail.append("【机台编号：【"+machineCode+"】，没有规格安排】：").append(division);
            return;
        }

        logDetail.append("【机台编号：【"+machineCode+"】，班次自动排程】：").append(toJSONString(preCxEngineScheduleResult)).append(division);

        //任务规格还有任务剩余量，可进行班次计划排程且1班存在计划量的规格（如果没有符合条件的数据则表示该成型机台下一个班次需要进行新规格安排）
        //获取一班平均可硫化班次
        Double avgAvalableLhShift= CxScheduleUtils.calcAvgAvailableLhShift(toProductList,ClassEnums.CLASS_ONE);
        Double classOneAvgaliableLhShift=avgAvalableLhShift;
        //Joran 2021-08-07 一班任务清除重排start
        for(CxEngineScheduleResult clearClassOneQty:monthRemainQtyList){
            if(clearClassOneQty.getClass1PlanQty()>0){
                clearClassOneQty.setClass1PlanQty(0);
            }
        }
        //Joran 2021-08-07 一班任务清除重排end

        //优先安排任务顺序靠前的单个规格任务列表
        Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap=singScheduleResultClass2Plan(preCxEngineScheduleResult,avgAvalableLhShift,classOneAvgaliableLhShift,logDetail,false);
        //预排没有成功，1.月度剩余量为0； 2.连续任务量超了。
        if(StringUtils.isEmpty(cxAutoScheduleTaskListMap)){
            cxAutoScheduleTaskListMap=new HashMap<>();
            //重新挑规格或者新安排规格
            preCxEngineScheduleResult= reScheduleTask(monthRemainQtyList,preCxEngineScheduleResult,cxAutoScheduleTaskListMap,ClassEnums.CLASS_ONE,logDetail);
        }

        if(preCxEngineScheduleResult==null){
            logDetail.append("【预排失败，任务重排失败】，机台不进行投产").append(division);
            return;
        }
        logDetail.append("&【后续自动排程】：").append(division);
        //剩余规格按照二班可硫化班次进行筛选下一个投产规格
        ClassEnums cls=ClassEnums.CLASS_TWO;
        if(StringUtils.isNotEmpty(cxAutoScheduleTaskListMap)){
            List<CxAutoScheduleTask> scheduleTaskList= cxAutoScheduleTaskListMap.get(preCxEngineScheduleResult.getEmbryoCode());
            //根据安排的任务列表获取最大的班次
            CxScheduleUtils.sortDescByScheduleTaskClassShift(scheduleTaskList);
            //取到最大的班次安排量和下一个规格的更换时间扣掉
            CxAutoScheduleTask  maxClassShiftTask=scheduleTaskList.get(0);
            cls=ClassEnums.getClassEnums(maxClassShiftTask.getClassShift());
        }else{
            logDetail.append("【一班自动结束】").append(",没有符合条件的规格进行投产，机台不再进行自动排产").append(division);
            log.debug(logDetail.toString());
            return;
        }
        //Joran 2021-12-29 标注重点迭代自动排程逻辑
        setNextSpecClassShiftPlan(monthRemainQtyList,cls,cxAutoScheduleTaskListMap,preCxEngineScheduleResult,logDetail);
        if(StringUtils.isNotEmpty(monthRemainQtyList)){
            for(CxEngineScheduleResult cxEngineScheduleResult:monthRemainQtyList){
                if(StringUtils.isEmpty(cxEngineScheduleResult.getStorageLocation())){
                    setResultStorageLocation(cxEngineScheduleResult,new StringBuilder());
                }
            }
        }
        //Joran 2021-12-25 自动排程结束后根据任务进行构建更换工装原因分析start
        logDetail.append("机台:"+preCxEngineScheduleResult.getCxMachineName()+",【自动排任务列表集合】：").append(division);
        if(StringUtils.isNotEmpty(cxAutoScheduleTaskListMap)){
            for(Map.Entry<String,List<CxAutoScheduleTask>> entry:cxAutoScheduleTaskListMap.entrySet()){
                logDetail.append("胎胚代码：").append(entry.getKey()).append(division);
                List<CxAutoScheduleTask> taskList= entry.getValue();
                for (CxAutoScheduleTask cxAutoScheduleTask:taskList){
                    logDetail.append("班次：").append(cxAutoScheduleTask.getClassShift())
                            .append(";计划量=").append(cxAutoScheduleTask.getCurrentShiftPlanQty())
                            .append(";剩余时间=").append(cxAutoScheduleTask.getRemainTime()).append(division);
                }
            }
        }
        //Joran 2021-12-25 自动排程结束后根据任务进行构建更换工装原因分析end
        logDetail.append("机台任务预排结束,当前机台："+machineCode+",结束时间:"+ DateUtils.getTime()).append(division);

    }

    /**
     * 获取投产规格
     * @param cxEngineScheduleResultList
     * @param cls
     */
    private CxEngineScheduleResult getPreScheduleResult(List<CxEngineScheduleResult> cxEngineScheduleResultList, ClassEnums cls) {
       if(StringUtils.isNotEmpty(cxEngineScheduleResultList)){
           CxScheduleUtils.resultSortAscByClassShiftSort(cxEngineScheduleResultList,cls);
           for(CxEngineScheduleResult cxEngineScheduleResult:cxEngineScheduleResultList){
               switch (cls){
                   case CLASS_ONE:
                        if(cxEngineScheduleResult.getLastClass4PlanQty()>0){
                            return cxEngineScheduleResult;
                        }
                        break;
                   case CLASS_TWO:
                       if(cxEngineScheduleResult.getLastClass5PlanQty()>0){
                           return cxEngineScheduleResult;
                       }
                       break;
                   default: break;
               }
           }
       }
       return cxEngineScheduleResultList.get(0);
    }


    /**
     * 预排失败，进行其他规格挑选或者新规格安排
     * @param monthRemainQtyList
     */
    private CxEngineScheduleResult reScheduleTask(List<CxEngineScheduleResult> monthRemainQtyList,CxEngineScheduleResult preCxEngineScheduleResult,Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap,ClassEnums cls,StringBuilder logDetail) {
        List<CxEngineScheduleResult> toProductList=new ArrayList<>();
        for(CxEngineScheduleResult cxEngineScheduleResult:monthRemainQtyList){
            //重新计算各个班次的可硫化班次
            CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
            //Joran 2021-12-20 将可以进行计划安排投产的计划单独获取出来start
            if(CxEngineConstants.TO_PRODUCT_YES.equals(cxEngineScheduleResult.getToProduct())){
                toProductList.add(cxEngineScheduleResult);
            }
            //Joran 2021-12-20 将可以进行计划安排投产的计划单独获取出来end
        }
        String preOrderNo=preCxEngineScheduleResult.getOrderNo();
        CxScheduleUtils.taskSortAscByAvailableLhShift(toProductList,cls.getClassIndex());
        //下一个排产规格
        CxEngineScheduleResult nextCxEngineScheduleResult=null;
        Double avgAvalableLhShift= CxScheduleUtils.calcAvgAvailableLhShift(toProductList,cls);
        Double classOneAvgailableLhShift=CxScheduleUtils.calcAvgAvailableLhShift(toProductList,ClassEnums.CLASS_ONE);
        logDetail.append("预排失败，任务重排平均可硫化班次："+avgAvalableLhShift).append(division);
        Double minAvailableLhShift=0D;
        CxEngineScheduleResult minAvailableLhShfit=null;
        if(StringUtils.isNotEmpty(toProductList)){
            minAvailableLhShfit=toProductList.get(0);
            minAvailableLhShift=CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(minAvailableLhShfit,cls.getClassIndex());
        }
        //验证是否可以投产新规格
        boolean canAddSpecFlag=addSpecValidate(avgAvalableLhShift,minAvailableLhShift,logDetail);
        logDetail.append("预排失败，是否可投产新规格："+canAddSpecFlag).append(division);
        //进行新规格投产
        if(canAddSpecFlag){
            CxScheduleUtils.taskSortAscByAvailableLhShift(toProductList,cls.getClassIndex());
            //添加新规格
            nextCxEngineScheduleResult=addSpec(monthRemainQtyList,preCxEngineScheduleResult,minAvailableLhShift,logDetail);
            if(nextCxEngineScheduleResult==null){
                log.debug("【自动排程】任务重排投产新规格时，没有匹配到相应的新规格进行投产。机台："+preCxEngineScheduleResult.getCxMachineCode()+"自动排程结束");
                return null;
            }
            logDetail.append("投产新规格信息："+toJSONString(nextCxEngineScheduleResult)).append(division);
            monthRemainQtyList.add(nextCxEngineScheduleResult);
            //重新计算平均可硫化班数
            avgAvalableLhShift=CxScheduleUtils.calcAvgAvailableLhShift(toProductList,cls);
        }else{
            //挑选可硫化班次最小的规格
            if(StringUtils.isNotEmpty(toProductList)){
                nextCxEngineScheduleResult= getPreScheduleResult(toProductList,cls);
            }else{
                //CxScheduleUtils.resultSortAscByClassShiftSort(monthRemainQtyList,cls);
                //nextCxEngineScheduleResult=monthRemainQtyList.get(0);
                nextCxEngineScheduleResult= getPreScheduleResult(monthRemainQtyList,cls);
                nextCxEngineScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_YES);
            }
        }
        //获取班次对应的可硫化班次
        Double workShirtAvalableLhShift= CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(nextCxEngineScheduleResult,cls.getClassIndex());
        Double maxClassShifts=getMaxLhClassShifts();
        if(workShirtAvalableLhShift>maxClassShifts){
            log.debug("任务重排后获取到的规格可硫化班次超过最大可硫化班次，自动排程结束！");
            return null;
        }
        //验证是否为相同规格连续排班
        boolean sameTask=validateSameTask(preOrderNo,nextCxEngineScheduleResult.getOrderNo());
        cxAutoScheduleTaskListMap.putAll(singScheduleResultCreateTask(nextCxEngineScheduleResult,cls.getClassIndex(),nextCxEngineScheduleResult.getCxMachineCode(),avgAvalableLhShift,classOneAvgailableLhShift,logDetail,sameTask));
        return nextCxEngineScheduleResult;
    }

    /**
     * 设置下个规格投产
     * @param lastDayScheduleResultList
     * @param cls
     * @param cxAutoScheduleTaskListMap
     * @param preCxEngineScheduleResult
     */
    private  void setNextSpecClassShiftPlan(List<CxEngineScheduleResult> lastDayScheduleResultList,ClassEnums cls, Map<String, List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap,CxEngineScheduleResult preCxEngineScheduleResult,StringBuilder logDetail) {
        singleMachineRoopCount+=1;
        logDetail.append(logSplit("前规格信息："+toJSONString(preCxEngineScheduleResult),"任务集合："+toJSONString(cxAutoScheduleTaskListMap),"当前迭代层数："+singleMachineRoopCount,"最大可迭代层数："+maxRoopCount));
        if(singleMachineRoopCount>maxRoopCount){
            logDetail.append("【自动排程递归异常】当前迭代层数："+singleMachineRoopCount+",最大可迭代层数："+maxRoopCount).append(division);
            return;
        }
        //根据可硫化班次进行升序排序
        int currentClassIndex=cls.getClassIndex();
        List<CxAutoScheduleTask> preScheduleTaskList=null;
        CxEngineScheduleResult nextCxEngineScheduleResult=null;
        //获取前规格排的最后一个班次任务
        CxAutoScheduleTask preLastOneTask=null;
        //前规格胎胚代码
        String preEmbryoCode=preCxEngineScheduleResult.getEmbryoCode();
        //前规格任务获取以及最大任务班次获取和各班可硫化班次计算start
        if(StringUtils.isNotEmpty(cxAutoScheduleTaskListMap)&&cxAutoScheduleTaskListMap.containsKey(preEmbryoCode)){
            preScheduleTaskList= cxAutoScheduleTaskListMap.get(preEmbryoCode);
            if(StringUtils.isNotEmpty(preScheduleTaskList)){
                //根据安排的任务列表获取最大的班次
                CxScheduleUtils.sortDescByScheduleTaskClassShift(preScheduleTaskList);
                //取到最大的班次安排量和下一个规格的更换时间扣掉
                preLastOneTask=preScheduleTaskList.get(0);
                logDetail.append("【自动任务安排到排程任务】：胎胚").append(preEmbryoCode).append("，任务班次：").append(preLastOneTask.getClassShift()).append("，安排的任务量=").append(preLastOneTask.getCurrentShiftPlanQty());
                //将任务安排的排程结果当中
                setClassPlanQtyByAutoScheduleTask(preCxEngineScheduleResult,preScheduleTaskList,cxAutoScheduleTaskListMap,logDetail);
            }
        }
        //前规格任务获取以及最大任务班次获取和各班可硫化班次计算end

        //遍历进行计算各个班次可硫化班次数start
        List<CxEngineScheduleResult> remainQtyList=new ArrayList<>();
        //Joran 2021-12-25 进行代码拆解，将筛选过程单独抽离一个方法
        reValidateScheduleResult(remainQtyList,lastDayScheduleResultList,cls,logDetail);

       //当所有规格月度计划都安排完了，自动进行新规格安排start
        boolean emptyAddSpec=false;
       if(StringUtils.isEmpty(remainQtyList)&&preLastOneTask.getRemainTime()>0&&preLastOneTask!=null){
           emptyAddSpec=true;
           emptyScheduleAddSpec(remainQtyList,lastDayScheduleResultList,preCxEngineScheduleResult,preLastOneTask,logDetail);
       }
       //当所有规格月度计划都安排完了，自动进行新规格安排end
       if(StringUtils.isEmpty(remainQtyList)){
           log.debug("可自动安排的规格任务剩余量都为0");
           logDetail.append("可自动安排的规格任务剩余量都为0").append(division);
           return;
       }

        //遍历进行计算各个班次可硫化班次数end
        //根据可硫化班次进行升序排序，时间最短的优先安排
        CxScheduleUtils.taskSortAscByAvailableLhShift(remainQtyList,currentClassIndex);
        //挑选可硫化班次最小的规格
        nextCxEngineScheduleResult=remainQtyList.get(0);
        logDetail.append("下一个规格："+toJSONString(nextCxEngineScheduleResult)).append(division);
        if(nextCxEngineScheduleResult==null){
            log.debug("【自动排程】所有规格都执行了，没有下一个规格，单机台任务自动排程结束。");
            logDetail.append("【自动排程】所有规格都执行了，没有下一个规格，单机台任务自动排程结束。").append(division);
            return ;
        }
        if(preLastOneTask!=null){

            //Joran 2021-12-27 缓存工装更换前规格
            Double beforeRemainTime=null;
            String afterKey= GenerageMapKeyUtils.createMapKey(nextCxEngineScheduleResult.getEmbryoCode(),nextCxEngineScheduleResult.getBomDataVersion());
            String beforeKey= GenerageMapKeyUtils.createMapKey(preLastOneTask.getEmbryoCode(),preLastOneTask.getBomDataVersion());
            //Joran 2021-12-27选规格又选到自己，更换工装的时间还是需要根据实际情况来确认前规格start
            if(afterKey.equals(beforeKey)){
                //获取当前班次顺序如果大于1证明有同班次前规格需要获取前规格和当前规格的切换工装的时间考量
                beforeRemainTime=getRealBeforeSpec(nextCxEngineScheduleResult,cls,lastDayScheduleResultList,cxAutoScheduleTaskListMap);
            }
            //Joran 2021-12-27选规格又选到自己，更换工装的时间还是需要根据实际情况来确认前规格end
            //任务已经安排满了
            Double changeSpecTime=changeSpecTime(afterKey,beforeKey,logDetail);
            if(preLastOneTask.getClassShift().equals(ClassEnums.CLASS_FIVE.getClassIndex())&&preLastOneTask.getRemainTime()<=changeSpecTime){
                log.debug("【自动排程】前规格胎胚：【"+preCxEngineScheduleResult.getEmbryoCode()+"】已经将所有班次占满，不再进行产能安排");
                logDetail.append("【自动排程】前规格胎胚：【"+preCxEngineScheduleResult.getEmbryoCode()+"】已经将所有班次占满，不再进行产能安排").append(division);
                return ;
            }

            //重新计算平均可硫化班数
            ClassEnums nextTaskCls= getNextTaskCls(preLastOneTask,changeSpecTime);
            if(nextTaskCls==null){
                log.debug("【自动排程】前规格胎胚：【"+preCxEngineScheduleResult.getEmbryoCode()+"】已经将所有班次占满，不再进行产能安排");
                logDetail.append("【自动排程】前规格胎胚：【"+preCxEngineScheduleResult.getEmbryoCode()+"】已经将所有班次占满，不再进行产能安排").append(division);
                return ;
            }

            List<CxEngineScheduleResult> toProductList=new ArrayList<>();
            //Joran 2021-12-18 进行投产标记类型为是的数据筛选start
            CxScheduleUtils.addProductSourceToTarget(lastDayScheduleResultList,toProductList);
            //Joran 2021-12-18 进行投产标记类型为是的数据筛选end
            //对符合条件的任务列表进行取平均可硫化班次运算
            //Joran 2021-12-28没有因为月度收尾新增规格start
            Double avgAvalableLhShift= CxScheduleUtils.calcAvgAvailableLhShift(toProductList,nextTaskCls);//Joran 2021-12-18 只筛选投产规格
            Double classOneAvgAvailableLhShift= CxScheduleUtils.calcAvgAvailableLhShift(toProductList,ClassEnums.CLASS_ONE);//Joran 2022-01-08 获取中班可硫化班次
            if(!emptyAddSpec){
                //按班次可硫化班数进行升序排序获取到可硫化班次最小值
                CxScheduleUtils.taskSortAscByAvailableLhShift(toProductList,preLastOneTask.getClassShift());//Joran 2021-12-18 只筛选投产规格
                //拿到班次最小规格
                CxEngineScheduleResult minAvailableLhShiftResult=toProductList.get(0);
                //拿到最小可硫化班次的规格的可硫化班次
                Double minAvailableLhShift= CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(minAvailableLhShiftResult,nextTaskCls.getClassIndex());
                logDetail.append("平均可硫化班次："+avgAvalableLhShift).append(division);
                //验证是否可以投产新规格
                boolean canAddSpecFlag=addSpecValidate(avgAvalableLhShift,minAvailableLhShift,logDetail);
                logDetail.append("是否可投产新规格："+canAddSpecFlag).append(division);
                //进行新规格投产
                if(canAddSpecFlag){
                    //添加新规格
                    CxEngineScheduleResult newSpecScheduleResult=addSpec(lastDayScheduleResultList,preCxEngineScheduleResult,minAvailableLhShift,logDetail);
                    if(newSpecScheduleResult==null){
                        log.debug("【自动排程】投产新规格时，没有匹配到相应的新规格进行投产。机台："+preCxEngineScheduleResult.getCxMachineCode()+"自动排程结束");
                        logDetail.append("【自动排程】投产新规格时，没有匹配到相应的新规格进行投产。机台："+preCxEngineScheduleResult.getCxMachineCode()+"自动排程结束").append(division);
                        return;
                    }
                    logDetail.append("投产新规格信息："+toJSONString(newSpecScheduleResult)).append(division);
                    //重新计算每个班次的可硫化班次数
                    CxScheduleUtils.calcAllClassAvailableLhShift(newSpecScheduleResult);
                    //下个班次任务创建
                    nextCxEngineScheduleResult=newSpecScheduleResult;
                    //将新生成的规格结果放入投产列表中
                    lastDayScheduleResultList.add(nextCxEngineScheduleResult);
                    //重新计算平均可硫化班数
                    avgAvalableLhShift=CxScheduleUtils.calcAvgAvailableLhShift(lastDayScheduleResultList,ClassEnums.getClassEnums(preLastOneTask.getClassShift()));
                    logDetail.append("添加新规格后重新计算平均可硫化班次："+avgAvalableLhShift).append(division);
                }
            }
            //Joran 2021-12-28没有因为月度收尾新增规格end
            //验证是否为相同规格连续排班
            boolean sameTask=validateSameTask(preLastOneTask.getCxOrderNo(),nextCxEngineScheduleResult.getOrderNo());
            //相同规格验证前一班是否排满
            if(sameTask){
              if(preLastOneTask.getRemainTime()>=changeSpecTime){ //没排满 先排满
                    //获取成型机规格定额数据
                    logDetail.append("【{续作相同规格，重新获取定额}】>>>");
                    Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(nextCxEngineScheduleResult.getCxMachineCode(),nextCxEngineScheduleResult.getEmbryoCode(),nextCxEngineScheduleResult.getBomDataVersion(),logDetail);
                    BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR)); //一个小时生产多少
                    //Joran 2021-12-28 用于前置规格任务获取定额计算剩余可安排总时间量start
                    int differentPlan=0;
                    if(beforeRemainTime!=null){
                        //当前可排计划量
                        int currentPlanQty=BigDecimal.valueOf(beforeRemainTime).multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN).intValue();
                        differentPlan=currentPlanQty-preLastOneTask.getCurrentShiftPlanQty();
                    }else{
                        //任务差额
                        differentPlan=machineQuota-preLastOneTask.getCurrentShiftPlanQty();
                    }
                    //Joran 2021-12-28 用于前置规格任务获取定额计算剩余可安排总时间量end
                    if(differentPlan>0){
                        //更新规格班次任务量、任务班次剩余时间、月度剩余量、清空班次的原因分析
                        updatePreResultTask(nextCxEngineScheduleResult,preLastOneTask,differentPlan,hourCountBig);
                        //Joran 2021-12-24 前规格的最后一个班是否添加更换工装start
                        // updatePreResultTaskByChangeMoldAnalysis(nextCxEngineScheduleResult,preLastOneTask,changeSpecTime,sameTask);
                        //Joran 2021-12-24 前规格的最后一个班是否添加更换工装end
                    }

                }
            }

            //下个班次任务创建
            Map<String, List<CxAutoScheduleTask>> nextMap= nextScheduleResultCreateTask(nextCxEngineScheduleResult,preLastOneTask,avgAvalableLhShift,classOneAvgAvailableLhShift,logDetail,sameTask);
            logDetail.append("投产新规格自动排结果："+toJSONString(nextMap)).append(division);
            ClassEnums nextCls=null;
             if(StringUtils.isNotEmpty(nextMap)){
                 if(nextMap.containsKey(CxEngineConstants.AUTO_OUTOVER_TASK_QTY)){
                     log.debug("【自动排程结束】，没有任务剩余量，不再自动排程");
                     logDetail.append("【自动排程结束】，没有任务剩余量，不再自动排程").append(division);
                     return;
                 }
                 if(nextMap.containsKey(CxEngineConstants.AUTO_OUTOVER_REMAIN_MONTH_QTY)){
                     log.debug("【自动排程结束】，没有月度剩余量，不再自动排程");
                     logDetail.append("【自动排程结束】，没有任务剩余量，不再自动排程").append(division);
                     return;
                 }
                 if(nextMap.containsKey(CxEngineConstants.AUTO_OUTOVER_REMAIN_TIME)){
                     log.debug("【自动排程结束】，没有剩余班次可排，不再自动排程");
                     logDetail.append("【自动排程结束】，没有剩余班次可排，不再自动排程").append(division);
                     return;
                 }
                   cxAutoScheduleTaskListMap.putAll(nextMap);
                   //获取下一次进行排计划的班次
                   nextCls=getMaxClassShift(nextCxEngineScheduleResult,nextMap);
             }else{
                 nextCls=ClassEnums.getClassEnums(preLastOneTask.getClassShift());
             }
            setNextSpecClassShiftPlan(lastDayScheduleResultList,nextCls,cxAutoScheduleTaskListMap,nextCxEngineScheduleResult,logDetail);
        }
    }

    /**
     *  前一天计划全部收尾的情况还有剩余空间进行新规格安排
     * @param remainQtyList
     * @param lastDayScheduleResultList
     * @param preLastOneTask
     */
    private void emptyScheduleAddSpec(List<CxEngineScheduleResult> remainQtyList, List<CxEngineScheduleResult> lastDayScheduleResultList,CxEngineScheduleResult preCxEngineScheduleResult, CxAutoScheduleTask preLastOneTask,StringBuilder logDetail) {
        logDetail.append("【收尾自动安排新规格计划】》》").append(division);
        List<CxEngineScheduleResult> toProductList=new ArrayList<>();
        //Joran 2021-12-18 进行投产标记类型为是的数据筛选start
        CxScheduleUtils.addProductSourceToTarget(lastDayScheduleResultList,toProductList);
        //重新计算平均可硫化班数
        ClassEnums nextTaskCls= getNextTaskCls(preLastOneTask,0D);
        if(nextTaskCls==null){
            log.debug("【全部收尾自动排程】前规格胎胚：【"+preLastOneTask.getEmbryoCode()+"】已经将所有班次占满，不再进行产能安排");
            logDetail.append("【全部收尾自动排程】前规格胎胚：【"+preLastOneTask.getEmbryoCode()+"】已经将所有班次占满，不再进行产能安排").append(division);
            return ;
        }
        //按班次可硫化班数进行升序排序获取到可硫化班次最小值
        CxScheduleUtils.taskSortAscByAvailableLhShift(toProductList,preLastOneTask.getClassShift());//Joran 2021-12-18 只筛选投产规格
        //拿到班次最小规格
        CxEngineScheduleResult minAvailableLhShiftResult=toProductList.get(0);
        //拿到最小可硫化班次的规格的可硫化班次
        Double minAvailableLhShift= CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(minAvailableLhShiftResult,nextTaskCls.getClassIndex());
        //添加新规格
        CxEngineScheduleResult newSpecScheduleResult=addSpec(lastDayScheduleResultList,preCxEngineScheduleResult,minAvailableLhShift,logDetail);
        if(newSpecScheduleResult==null){
            log.debug("【收尾自动安排新规格计划】投产新规格时，没有匹配到相应的新规格进行投产。机台："+preCxEngineScheduleResult.getCxMachineCode()+"自动排程结束");
            logDetail.append("【收尾自动安排新规格计划】投产新规格时，没有匹配到相应的新规格进行投产。机台："+preCxEngineScheduleResult.getCxMachineCode()+"自动排程结束").append(division);
            return;
        }
        logDetail.append("【收尾自动安排新规格计划】投产新规格信息："+toJSONString(newSpecScheduleResult)).append(division);
        //重新计算每个班次的可硫化班次数
        CxScheduleUtils.calcAllClassAvailableLhShift(newSpecScheduleResult);
        //下个班次任务创建
        //将新生成的规格结果放入投产列表中
        lastDayScheduleResultList.add(newSpecScheduleResult);
        remainQtyList.add(newSpecScheduleResult);
        //重新计算平均可硫化班数
        Double avgAvalableLhShift=CxScheduleUtils.calcAvgAvailableLhShift(lastDayScheduleResultList,ClassEnums.getClassEnums(preLastOneTask.getClassShift()));
        logDetail.append("【收尾自动安排新规格计划】添加新规格后重新计算平均可硫化班次："+avgAvalableLhShift).append(division);
    }

    /**
     * 获取自动安排的前规格
     * @return
     */
    private Double getRealBeforeSpec(CxEngineScheduleResult nextCxEngineScheduleResult,ClassEnums cls,List<CxEngineScheduleResult> lastDayScheduleResultList,Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap) {
        Double beforeRemainTime=null;
        int classSort= CxScheduleUtils.getClassClassSort(nextCxEngineScheduleResult,cls,CxEngineConstants.CURRENT_SHIFT_TYPE);
        if(classSort>1){
            CxEngineScheduleResult beforeScheduleResult = CxScheduleUtils.getBeforeScheduleResultByClassSort(lastDayScheduleResultList,cls,classSort-1);
            if(beforeScheduleResult!=null){
                List<CxAutoScheduleTask> beforeScheduleTaskList=cxAutoScheduleTaskListMap.get(beforeScheduleResult.getEmbryoCode());
                //根据安排的任务列表获取最大的班次
                CxScheduleUtils.sortDescByScheduleTaskClassShift(beforeScheduleTaskList);
                //取到最大的班次安排量和下一个规格的更换时间扣掉
                CxAutoScheduleTask beforeTask=beforeScheduleTaskList.get(0);
                beforeRemainTime=beforeTask.getRemainTime();
                //Double lastUseTime=CxEngineConstants.CLASS_SHIFT_HOUR-preLastOneTask.getRemainTime();//当前规格用掉的时长
                //重新设置剩余时间
                //preLastOneTask.setRemainTime(beforeRemainTime-lastUseTime);
                //preLastOneTask.setCxOrderNo(beforeScheduleResult.getOrderNo());
                //beforeKey= GenerageMapKeyUtils.createMapKey(beforeScheduleResult.getEmbryoCode(),beforeScheduleResult.getBomDataVersion());
            }
        }
        return beforeRemainTime;
    }

    /**
     * 根据月度 最大班次进行验证保留符合条件的规格任务列表
     * @param remainQtyList 符合条件的集合
     * @param lastDayScheduleResultList 原始数据集合
     * @param cls 当前排班班次
     */
    private void reValidateScheduleResult(List<CxEngineScheduleResult> remainQtyList, List<CxEngineScheduleResult> lastDayScheduleResultList, ClassEnums cls,StringBuilder logDetail) {
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            //重新计算每个班次的可硫化班次数
            CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
            Integer monthRemainQty=monthRemainQtyMap.get(cxEngineScheduleResult.getEmbryoCode());
            //Joran 2021-12-27 如果没有月度剩余量则默认赋予0 start
            if(monthRemainQty==null){
                logDetail.append("【reValidateScheduleResult》》获取月度剩余量】,胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",获取月度剩余量为空").append(division);
                monthRemainQty=0;
            }else{
                logDetail.append("【reValidateScheduleResult》》获取月度剩余量】,胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",月度剩余量=").append(monthRemainQty).append(division);
            }
            //Joran 2021-12-27 如果没有月度剩余量则默认赋予0 end
            Double maxClassShifts=getMaxLhClassShifts();
            //Joran 2021-12-20 标记自动排程不进行自动安排计划的任务跳过start
            if(CxEngineConstants.TO_PRODUCT_NO.equals(cxEngineScheduleResult.getToProduct())){
                log.debug("【reValidateScheduleResult》》规格筛选标记不自动安排】,当前胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",最大可硫化班次："+maxClassShifts);
                logDetail.append("【reValidateScheduleResult》》规格筛选标记不自动安排】,当前胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",最大可硫化班次："+maxClassShifts).append(division);
                continue;
            }
            //Joran 2021-12-20 标记自动排程不进行自动安排计划的任务跳过end
            //获取班次对应的可硫化班次
            Double workShirtAvalableLhShift= CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(cxEngineScheduleResult,cls.getClassIndex());
            if(monthRemainQty>0&&maxClassShifts>workShirtAvalableLhShift){ //还有月度剩余量的规格
                remainQtyList.add(cxEngineScheduleResult);
            }else{
                log.debug("，月度剩余量小于0的规格,胎胚代码："+cxEngineScheduleResult.getEmbryoCode());
                logDetail.append("【自动排程规则不通过规格】,当前胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",当前月度剩余量："+monthRemainQty+",当前可硫化班次："+workShirtAvalableLhShift+",最大可硫化班次："+maxClassShifts).append(division);
                log.debug("【自动排程超过最大可硫化班次】，当前班次可硫化班次数量大于最大可硫化班次,最大可硫化班次："+maxClassShifts);
            }
        }
    }

    /**
     * 前规格的最后一个班是否添加更换工装原因分析
     * @param preCxEngineScheduleResult 前规格排程计划
     * @param preLastOneTask 前规格最大班次自动排程信息包含剩余时间
     * @param changeSpecTime 更换工装时间
     */
    private void updatePreResultTaskByChangeMoldAnalysis(CxEngineScheduleResult preCxEngineScheduleResult, CxAutoScheduleTask preLastOneTask, Double changeSpecTime,boolean sameTask) {
        Double remainTime=preLastOneTask.getRemainTime();//前规格自动排剩余时间
        //二分之一更换工装时间
        Double halfChangeSpecTime=BigDecimal.valueOf(changeSpecTime).divide(BigDecimal.valueOf(2)).doubleValue();
        //剩余时间大于一半及以上的更换工装时间时，前规格最后一个班次则进行换工装原因标注
        if(remainTime>0 && remainTime>=halfChangeSpecTime){
            String changeMoldAnalysis="";
            if(preCxEngineScheduleResult.getNewSpecFlag()){
                //换工装开班
                changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
            }else{
                //换工装
                changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
            }
            preLastOneTask.setIsChangeMoldAnalysis(true);//前规格最大班次标注换工装原因分析
            CxScheduleUtils.setClassAnalysis(preCxEngineScheduleResult,ClassEnums.getClassEnums(preLastOneTask.getClassShift()),changeMoldAnalysis);
        }
    }

    /**
     * 判定是否需要跳下一个班次
     * @param preLastOneTask
     * @return
     */
    private ClassEnums getNextTaskCls(CxAutoScheduleTask preLastOneTask,Double changeSpecTime) {
        //Joran 2021-08-19 如果是满额的话则直接进行下一个班次start
        Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(preLastOneTask.getCxMachineCode(),preLastOneTask.getEmbryoCode(),preLastOneTask.getBomDataVersion(),new StringBuilder());
        Integer nextClassIndex=preLastOneTask.getClassShift();
        if(preLastOneTask.getCurrentShiftPlanQty().equals(machineQuota)){
            nextClassIndex+=1;
        }else if(preLastOneTask.getRemainTime()<=changeSpecTime){
            nextClassIndex+=1;
        }else if(preLastOneTask.getRemainTime()<=0){
            nextClassIndex+=1;
        }
        //Joran 2021-08-19 如果是满额的话则直接进行下一个班次end
        return ClassEnums.getClassEnums(nextClassIndex);
    }

    /**
     * 更新前规格的计划量、清空原因分析、任务剩余时间、月度剩余量
     * @param nextCxEngineScheduleResult
     * @param preLastOneTask
     * @param differentPlan
     */
    private void updatePreResultTask(CxEngineScheduleResult nextCxEngineScheduleResult, CxAutoScheduleTask preLastOneTask, int differentPlan,BigDecimal hourCountBig) {
        Integer monthRemainQty=monthRemainQtyMap.get(nextCxEngineScheduleResult.getEmbryoCode());
        if(monthRemainQty<0){//没有月度剩余量则不进行重新弄
            return;
        }
        String analysis="";
        //月度剩余量小于差异量
        if(monthRemainQty<=differentPlan){
            //获取班次
            ClassEnums cls =ClassEnums.getClassEnums(preLastOneTask.getClassShift());
            //更新剩余量
            int classShiftPlan=preLastOneTask.getCurrentShiftPlanQty();
            int planQty =classShiftPlan + monthRemainQty;

            // Joran 2021-12-29 计划量往上加的时候需要更新下班次剩余时间 start
            BigDecimal planQtyBig=BigDecimal.valueOf(monthRemainQty);
            //加上这些计划量用了多少时间
            Double usedTime=planQtyBig.divide(hourCountBig,3, RoundingMode.CEILING).doubleValue();
            if(preLastOneTask.getRemainTime()>=usedTime){
                Double remainTime=preLastOneTask.getRemainTime()- usedTime;
                //更新班次剩余时长
                updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),cls.getClassIndex(), remainTime);
                preLastOneTask.setRemainTime(remainTime);
            }else{
                //更新班次剩余时长为0
                updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),cls.getClassIndex(), BigDecimal.ZERO.doubleValue());
                preLastOneTask.setRemainTime(BigDecimal.ZERO.doubleValue());//剩余时间占满
            }
            // Joran 2021-12-29 计划量往上加的时候需要更新下班次剩余时间 end

            preLastOneTask.setCurrentShiftPlanQty(planQty);
            //重设班次计划量和清空原因分析
            Integer totalTaskQty=nextCxEngineScheduleResult.getDayTotalPlanQty();
            totalTaskQty+=monthRemainQty;
            //共多少收尾原因分析
            analysis=StringUtils.format(I18nUtil.getMessage("cx.engine.auto.analysis.totalQty.title"),totalTaskQty);
           /* if(CxScheduleUtils.getBeforeClassPlanQty(nextCxEngineScheduleResult,cls)==0 && !preLastOneTask.getIsChangeMoldAnalysis()){
                analysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title")+";"+StringUtils.format(I18nUtil.getMessage("cx.engine.auto.analysis.totalQty.title"),totalTaskQty);;
            }else{
                analysis=StringUtils.format(I18nUtil.getMessage("cx.engine.auto.analysis.totalQty.title"),totalTaskQty);
            }*/
            CxScheduleUtils.reSetPlanQtyAndAnalysis(nextCxEngineScheduleResult,cls,monthRemainQty,analysis);
            monthRemainQty=0;
            //月度剩余量清0
            monthRemainQtyMap.put(nextCxEngineScheduleResult.getEmbryoCode(),monthRemainQty);
            log.debug("【同规格连续生产排产，班次有剩余量补充：】补班次：【"+cls.getClassName()+"】，差额量："+planQty+"，月度剩余量："+monthRemainQty );
        }else{
            //更新剩余量
            monthRemainQty-=differentPlan;
            monthRemainQtyMap.put(nextCxEngineScheduleResult.getEmbryoCode(),monthRemainQty);
            //获取班次
            ClassEnums cls =ClassEnums.getClassEnums(preLastOneTask.getClassShift());
           /* if(CxScheduleUtils.getBeforeClassPlanQty(nextCxEngineScheduleResult,cls)==0 && !preLastOneTask.getIsChangeMoldAnalysis()){
                analysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
            }*/
            //重设班次计划量和清空原因分析
            CxScheduleUtils.reSetPlanQtyAndAnalysis(nextCxEngineScheduleResult,cls,differentPlan,analysis);
            int classShiftPlan=preLastOneTask.getCurrentShiftPlanQty();
            preLastOneTask.setCurrentShiftPlanQty(classShiftPlan+differentPlan);
            preLastOneTask.setRemainTime(0D);//剩余时间占满
            //更新班次剩余时长为0
            updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),cls.getClassIndex(), BigDecimal.ZERO.doubleValue());
            log.debug("【同规格连续生产排产，班次有剩余量补充：】补班次：【"+cls.getClassName()+"】，差额量："+differentPlan+"，月度剩余量："+monthRemainQty   );
        }
    }

    /**
     * 获取下一个班次
     * @param newSpecScheduleResult
     * @param nextMap
     */
    private ClassEnums getMaxClassShift(CxEngineScheduleResult newSpecScheduleResult, Map<String, List<CxAutoScheduleTask>> nextMap) {
        //当前任务列表集合获取
        List<CxAutoScheduleTask> thisScheduleTaskList=nextMap.get(newSpecScheduleResult.getEmbryoCode());
        //根据安排的任务列表获取最大的班次
        CxScheduleUtils.sortDescByScheduleTaskClassShift(thisScheduleTaskList);
        //取到最大的班次安排量和下一个规格的更换时间扣掉
        CxAutoScheduleTask maxClassShiftTask=thisScheduleTaskList.get(0);
        Integer nextClassIndex=maxClassShiftTask.getClassShift();
        //Joran 2021-08-19 如果是满额的话则直接进行下一个班次start
        Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(newSpecScheduleResult.getCxMachineCode(),newSpecScheduleResult.getEmbryoCode(),newSpecScheduleResult.getBomDataVersion(),new StringBuilder());
        if(maxClassShiftTask.getCurrentShiftPlanQty().equals(machineQuota)){
            nextClassIndex+=1;
            if(nextClassIndex>ClassEnums.CLASS_FIVE.getClassIndex()){
                return ClassEnums.CLASS_FIVE;
            }
        }
        //Joran 2021-08-19 如果是满额的话则直接进行下一个班次end
        return  ClassEnums.getClassEnums(nextClassIndex);
    }

    /**
     * 添加新规格进行规格筛选,数据组装
     * @param cxEngineScheduleResult
     * @param minAvailableLhShift
     * @return
     */
    private CxEngineScheduleResult addSpec(List<CxEngineScheduleResult> lastDayScheduleResultList,CxEngineScheduleResult cxEngineScheduleResult,Double minAvailableLhShift,StringBuilder logDetail) {
        String machineCode=cxEngineScheduleResult.getCxMachineCode();
        String machineType=cxEngineScheduleResult.getCxMachineType();
        CxEngineScheduleResult newSpecPlanResult=null;
        if(StringUtils.isNotEmpty(cxPlanProductStatusList)){
            String beforeKey=GenerageMapKeyUtils.createMapKey(cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getBomDataVersion());
            //校验是否存在施工信息start
            if(!engineConstructionInfoMap.containsKey(beforeKey)){
                throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.import.embryo.construction.noFound.error"), cxEngineScheduleResult.getEmbryoCode()));
            }
            EngineProductConstructionInfo beforeSpec=engineConstructionInfoMap.get(beforeKey);
            //Joran 2021-11-18 因为一次法没有扣圈盘直径，所以扣圈盘直径不考虑start
            Double beforeFlipDiscDiameter=null;
            if(!CxEngineConstants.MACHINE_TYPE_ONCE.equals(machineType)){
                beforeFlipDiscDiameter=beforeSpec.getFlipDiscDiameter();//扣圈盘直径
                if(beforeFlipDiscDiameter==null){
                    throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.beforeFlipDiscDiameter.error"), cxEngineScheduleResult.getEmbryoCode()));
                }
            }
            //Joran 2021-11-18 因为一次法没有扣圈盘直径，所以扣圈盘直径不考虑end
            Double beforeNoseWidth=beforeSpec.getNoseWidth();//机头宽度
            if(beforeNoseWidth==null){
                throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.beforeNoseWidth.error"), cxEngineScheduleResult.getEmbryoCode()));
            }
            //寸口不一样的线跳过
            Double dimension=beforeSpec.getDimension();//规格寸口信息
            if(dimension==null){
                throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.beforeDimension.error"), cxEngineScheduleResult.getEmbryoCode()));
            }
            logDetail.append("【添加新规格】：").append("前规格寸口：").append(dimension).append("，前规格扣圈盘直径：").append(beforeFlipDiscDiameter).append("前规格机头宽度：").append(beforeNoseWidth).append(division);
            CxPlanProductStatus nextPlanProduct=null;
            //新规格施工信息
            EngineProductConstructionInfo newSpecConstructionInfo=null;
            //1.优先筛选限制作业规格列表
            //剔除掉不可作业的规格列表
            List<CxPlanProductStatus> cxPlanRemainProductStatusList=new ArrayList<>(cxPlanProductStatusList);
            logDetail.append("【可投产列表】：").append(toJSONString(cxPlanProductStatusList)).append(division);
            //剔除掉不可作业的规格
            specifyMachineProductList(machineCode,cxPlanRemainProductStatusList,CxEngineConstants.SPECIFY_JOB_TYPE_NO);
            logDetail.append("【移除不可作业后投产列表】：").append(toJSONString(cxPlanRemainProductStatusList)).append(division);
            //挑选定的规格
            nextPlanProduct=selectionPlanProduct(machineType,machineCode,beforeFlipDiscDiameter,beforeNoseWidth,cxPlanRemainProductStatusList,dimension,minAvailableLhShift,logDetail);
            if(nextPlanProduct!=null){
                //根据待投产创建排程结果
                String key =GenerageMapKeyUtils.createMapKey(nextPlanProduct.getEmbryoCode(),nextPlanProduct.getBomDataVersion());
                if(!engineConstructionInfoMap.containsKey(key)){
                    throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.import.embryo.construction.noFound.error"), nextPlanProduct.getEmbryoCode()));
                }
                newSpecConstructionInfo=engineConstructionInfoMap.get(key);
                newSpecPlanResult=createScheduleResultByPlanProductStatus(lastDayScheduleResultList,cxEngineScheduleResult,nextPlanProduct,newSpecConstructionInfo);
                // 1.更新待投产表中的投产状态 2.将待投产列表的数据移除
                cxPlanProductStatusService.updatePlanProductToProduction(nextPlanProduct);
                cxPlanProductStatusList.remove(nextPlanProduct);
                //如果新安排的规格为同胎胚的则重新进行其他规格安排start
                if(newSpecPlanResult.getIsProducted()){
                    logDetail.append("【剔除已投产规格】").append("，挑选的胎胚代码：").append(newSpecPlanResult.getEmbryoCode()).append("月度剩余量已小于等于0").append(division);
                    newSpecPlanResult=addSpec(lastDayScheduleResultList,cxEngineScheduleResult,minAvailableLhShift,logDetail);
                }else if(CxEngineConstants.TO_PRODUCT_NO.equals(newSpecPlanResult.getToProduct())){
                    //Joran 2021-12-22 新挑选的不排计划规格添加到列表，重新挑
                    logDetail.append("【新挑选的不排计划规格添加到列表】").append("，挑选的胎胚代码：").append(newSpecPlanResult.getEmbryoCode()).append("不自动安排任务，进行重新挑选").append(division);
                    lastDayScheduleResultList.add(newSpecPlanResult);
                    newSpecPlanResult=addSpec(lastDayScheduleResultList,cxEngineScheduleResult,minAvailableLhShift,logDetail);
                }
                //如果新安排的规格为同胎胚的则重新进行其他规格安排end
            }
        }else{
            log.error("未投产列表数据没有数据，请确认未投产数据是否存在数据。");
        }
        return newSpecPlanResult;
    }

    /**
     * 验证待投产规格是否有指定其他定点机台
     * @param nextPlanProudct
     * @param cxPlanRemainProductStatusList
     */
    private CxPlanProductStatus validateNexPlanProduct(String machineCode,CxPlanProductStatus nextPlanProudct, List<CxPlanProductStatus> cxPlanRemainProductStatusList,StringBuilder logDetail) {
        CxPlanProductStatus newNextPlanProduct=null;
        List<CxEngineSpecifyMachine> specifyMachineList=new ArrayList<>();
        if(StringUtils.isNotEmpty(specifyMachineYesMap)){
            for(Map.Entry<String,List<CxEngineSpecifyMachine>> entry:specifyMachineYesMap.entrySet()){
                specifyMachineList.addAll(entry.getValue());
            }
        }
        logDetail.append("【移除限定作业不在本机台规格】").append("限制作业数据集合").append(toJSONString(specifyMachineYesMap)).append(division);
        String machKey=CxScheduleUtils.getMapKeyByInputString(nextPlanProudct.getSapCode(),nextPlanProudct.getEmbryoCode());
        boolean isContinueProductPlan=true;
        for(CxEngineSpecifyMachine cxEngineSpecifyMachine:specifyMachineList){
            String specifyKey=CxScheduleUtils.getMapKeyByInputString(cxEngineSpecifyMachine.getSapCode(),cxEngineSpecifyMachine.getEmbryoCode());
            String ignoreMapKey=CxScheduleUtils.getMapKeyByInputString(machineCode,cxEngineSpecifyMachine.getSapCode(),cxEngineSpecifyMachine.getEmbryoCode());
            if(specifyKey.equals(machKey)){
                String specifyMachineCode=cxEngineSpecifyMachine.getCxMachineCode();
                if(!machineCode.equals(specifyMachineCode)){
                    cxPlanRemainProductStatusList.remove(nextPlanProudct);
                    autoScheduleIgnoreMap.put(ignoreMapKey,nextPlanProudct);
                    isContinueProductPlan=false;
                    break;//跳出循环
                }
            }
        }
        if(isContinueProductPlan){
            newNextPlanProduct=nextPlanProudct;
        }
        logDetail.append("【限定作业校验后投产规格】").append(toJSONString(newNextPlanProduct)).append(division);

        return newNextPlanProduct;
    }

    /**
     * 规格筛选业务逻辑
     * @param beforeFlipDiscDiameter 前规格扣圈盘直径
     * @param beforeNoseWidth 前规格断面宽
     * @param cxPlanProductStatusList 投产列表
     */
    private CxPlanProductStatus selectionPlanProduct(String machineType,String machineCode,Double beforeFlipDiscDiameter, Double beforeNoseWidth, List<CxPlanProductStatus> cxPlanProductStatusList,Double dimension,Double minAvailableLhShift,StringBuilder logDetail) {
        CxPlanProductStatus nextPlanProduct=null;
        List<CxPlanProductStatus> sameDimensionList=new ArrayList<>();
        String startPrefix=getEmbryoCodePrefixByMachineType(machineType);
        //优先根据寸口进行筛选start
        for(CxPlanProductStatus cxPlanProductStatus:cxPlanProductStatusList){
            String embryoCode=cxPlanProductStatus.getEmbryoCode();
            String bomDataVersion=cxPlanProductStatus.getBomDataVersion();
            if(!org.apache.commons.lang3.StringUtils.startsWithIgnoreCase(embryoCode,startPrefix)){
                log.debug("【规格筛选胎胚校验】成型机台类型为：【"+(CxEngineConstants.MACHINE_TYPE_ONCE.equals(machineType)?"一次法":"二次法】,当前胎胚代码【")+embryoCode+"】,不允许安排在成型机台上！");
                logDetail.append("【规格筛选胎胚校验】成型机台类型为：【").append(CxEngineConstants.MACHINE_TYPE_ONCE.equals(machineType)?"一次法":"二次法").append("】,当前胎胚代码【").append(embryoCode).append("】,不允许安排在成型机台上！").append(division);
                continue;
            }

            String afterKey=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
            //获取施工信息
            EngineProductConstructionInfo afterSpec=engineConstructionInfoMap.get(afterKey);
            //Joran 2021-11-16 规格施工未找到时进行错误提示报错 start
            if(afterSpec==null){
                throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.afterSpec.error"),embryoCode));
            }
            //Joran 2021-11-16 规格施工未找到时进行错误提示报错 end
            Double  afterDimension=afterSpec.getDimension();
            if(afterDimension==null){
                throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.afterDimension.error"), embryoCode));
            }
            if(afterDimension.equals(dimension)){
                sameDimensionList.add(cxPlanProductStatus);//将同寸口的待投产规格进行筛选
            }
        }
        //优先根据寸口进行筛选end
        if(StringUtils.isEmpty(sameDimensionList)){
            log.debug("挑选规格时，根据同寸口进行挑选，没有匹配得规格，机台投产结束。");
            return null;
        }
        logDetail.append("【同寸口投产列表】").append(toJSONString(sameDimensionList)).append(division);
        //根据扣圈盘直径进行筛选start
        List<CxPlanProductStatus> sameFlipDiscDiameterList=new ArrayList<>();

        //Joran 2021-11-18添加如果为一次法类型则不进行扣圈盘直径筛选start
        if(!CxEngineConstants.MACHINE_TYPE_ONCE.equals(machineType)){
            for(CxPlanProductStatus cxPlanProductStatus:sameDimensionList){
                //获取施工信息
                String embryoCode=cxPlanProductStatus.getEmbryoCode();
                String bomDataVersion=cxPlanProductStatus.getBomDataVersion();
                String afterKey=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
                EngineProductConstructionInfo afterSpec=engineConstructionInfoMap.get(afterKey);
                //判断扣圈盘直径是否相同即可
                Double afterFlipDiscDiameter=afterSpec.getFlipDiscDiameter();//扣圈盘直径
                if(afterFlipDiscDiameter==null){
                    throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.afterFlipDiscDiameter.error"), embryoCode));
                }
                //扣圈盘直径相同放一起
                if(beforeFlipDiscDiameter.equals(afterFlipDiscDiameter)){
                    sameFlipDiscDiameterList.add(cxPlanProductStatus);
                }
            }
        }else{
            sameFlipDiscDiameterList =new ArrayList<>(sameDimensionList);
        }
        //Joran 2021-11-18添加如果为一次法类型则不进行扣圈盘直径筛选end

        //根据扣圈盘直径进行筛选end
        if(StringUtils.isEmpty(sameFlipDiscDiameterList)){
            log.debug("同寸口不存在扣圈盘直径规格相同的投产规格，以机头宽度差异小的来进行安排");
            nextPlanProduct=selectionCxPlanProductSpec(machineCode,beforeNoseWidth,sameDimensionList,minAvailableLhShift,logDetail);
        }else{
            logDetail.append("【同扣圈盘直径投产列表】").append(toJSONString(sameFlipDiscDiameterList)).append(division);
            nextPlanProduct=selectionCxPlanProductSpec(machineCode,beforeNoseWidth,sameFlipDiscDiameterList,minAvailableLhShift,logDetail);
        }
        if(nextPlanProduct==null){
            return nextPlanProduct;
        }
        //验证规格是否有指定定点机台，且定点机台不是当前机台则规格剔除重新获取
        nextPlanProduct=validateNexPlanProduct(machineCode,nextPlanProduct,sameDimensionList,logDetail);
        if(nextPlanProduct==null&&StringUtils.isNotEmpty(sameDimensionList)){//挑选规格指定其他定点机台，移除重新选规格
            nextPlanProduct=selectionPlanProduct(machineType,machineCode,beforeFlipDiscDiameter,beforeNoseWidth,sameDimensionList,dimension,minAvailableLhShift,logDetail);
        }
        return nextPlanProduct;
    }


    /**
     * 待投产列表中符合寸口相同列表数据中筛选
     * @param beforeNoseWidth
     * @param cxPlanProductStatusList
     * @param minAvailableLhShift 成型机上可硫化班次最小的班次数
     * @return
     */
    private CxPlanProductStatus selectionCxPlanProductSpec(String machineCode,Double beforeNoseWidth,List<CxPlanProductStatus> cxPlanProductStatusList,Double minAvailableLhShift,StringBuilder logDetail){
        CxPlanProductStatus newCxPlanProductStatus=null;
        Double minNoseWidth=null;//机头宽度差异
        //根据最小班次限定可投产量限定班次
        String productSpecLimitShiftParams=cxParamsMap.get(CxParamCodeConstants.PRODUCT_SPEC_LIMIT_SHIFT);
        //最小班次低于限定班次可投产量配置
        String productspecLimitQtyParams=cxParamsMap.get(CxParamCodeConstants.PRODUCT_SPEC_LIMIT_QTY);
        if(StringUtils.isEmpty(productSpecLimitShiftParams)){
            throw new CxScheduleEngineException("可投产量限定班次参数为空,请先配置！");
        }
        if(StringUtils.isEmpty(productspecLimitQtyParams)){
            throw new CxScheduleEngineException("低于限定班次可投产最大量为空,请先配置！");
        }
        //最小投产限定班次
        int productSpecLimitShift=Integer.valueOf(productSpecLimitShiftParams);
        int maxPlanQty=Integer.MAX_VALUE;
        //最小计划量
        int minPlanQty=421;
        boolean limitMax=false;
        boolean limitMin=false;
        if(minAvailableLhShift<=productSpecLimitShift){//Joran 2021-11-25 如果平均班次小于等于设置的班数则进行小规格投产
            //最大可投产数量
            maxPlanQty =Integer.valueOf(productspecLimitQtyParams);
            limitMax=true;//限制可投最大计划量规格
            log.debug("【新规格筛选】最小可硫化班次：【"+minAvailableLhShift+"】，小于参数设定班次：【"+productSpecLimitShift+"】,最大可投产量限定为：【"+maxPlanQty+"】");
            logDetail.append("【新规格筛选】最小可硫化班次：【"+minAvailableLhShift+"】，小于参数设定班次：【"+productSpecLimitShift+"】,最大可投产量限定为：【"+maxPlanQty+"】").append(division);
        }else{
            //限制最小计划量
            limitMin=true;
            minPlanQty =Integer.valueOf(productspecLimitQtyParams);
        }
        //Joran 2021-11-25 任务月度量排序
        Comparator<CxPlanProductStatus> monthPlanTotalQtySort=null;
        if(limitMax){
             monthPlanTotalQtySort = Comparator.comparing(CxPlanProductStatus::getMonthPlanTotalQty);
        }else if(limitMin){
            monthPlanTotalQtySort = Comparator.comparing(CxPlanProductStatus::getMonthPlanTotalQty).reversed();
        }
        //进行任务排序
        Collections.sort(cxPlanProductStatusList,monthPlanTotalQtySort);

        for(CxPlanProductStatus cxPlanProductStatus:cxPlanProductStatusList){
            //小规格筛选
            if(limitMax&&cxPlanProductStatus.getMonthPlanTotalQty()>maxPlanQty){
                log.debug("【新规格筛选】新规格月度计划量大于可投产数量，跳过不投产,继续找下一个规格，当前规格胎胚：【"+cxPlanProductStatus.getEmbryoCode()+"】");
                logDetail.append("【新规格筛选】新规格月度计划量大于可投产数量，跳过不投产,继续找下一个规格，当前规格胎胚：【"+cxPlanProductStatus.getEmbryoCode()+"】").append(division);
                continue;
            }
            //大规格筛选
            if(limitMin&&cxPlanProductStatus.getMonthPlanTotalQty()< minPlanQty){
                log.debug("【新规格筛选】新规格月度计划量必须投大规格，跳过不投产,继续找下一个规格，当前规格胎胚：【"+cxPlanProductStatus.getEmbryoCode()+"】");
                logDetail.append("【新规格筛选】新规格月度计划量必须投大规格，跳过不投产,继续找下一个规格，当前规格胎胚：【"+cxPlanProductStatus.getEmbryoCode()+"】").append(division);
                continue;
            }
            String embryoCode=cxPlanProductStatus.getEmbryoCode();
            String bomDataVersion=cxPlanProductStatus.getBomDataVersion();
            String afterKey=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
            //获取施工信息
            EngineProductConstructionInfo afterSpec=engineConstructionInfoMap.get(afterKey);
            Double afterNoseWidth=afterSpec.getNoseWidth();//机头宽度
            //扣圈盘直径和机头宽度完全一样直接优先
            if(beforeNoseWidth.equals(afterNoseWidth)){
                newCxPlanProductStatus=cxPlanProductStatus;
                break;//跳出循环
            }
            //扣圈盘直径或者机头宽度存在不一样，直接判断机头宽度差异
            Double calcNoseWidth=Math.abs(beforeNoseWidth-afterNoseWidth); //取绝对值只管差异不管正负数
            if(minNoseWidth==null){
                minNoseWidth=calcNoseWidth;
                newCxPlanProductStatus=cxPlanProductStatus;
            }else if(calcNoseWidth< minNoseWidth){ //取最小的
                minNoseWidth=calcNoseWidth;
                newCxPlanProductStatus=cxPlanProductStatus;
            }
        }
        //Joran 2021-12-24 进行挑选但是挑不到数据就按照排序随机挑选一条投产记录进行投产start
        if(newCxPlanProductStatus==null){
            newCxPlanProductStatus=reSelectProductStatus(cxPlanProductStatusList,beforeNoseWidth);
        }
        //Joran 2021-12-24 进行挑选但是挑不到数据就按照排序随机挑选一条投产记录进行投产end
        logDetail.append("【机头宽度差异最小规格】").append("，规格数据：").append(toJSONString(newCxPlanProductStatus)).append(division);
        return newCxPlanProductStatus;
    }

    /**
     * 遍历投产列表获取投产规格
     * @param cxPlanProductStatusList
     * @return
     */
    private CxPlanProductStatus reSelectProductStatus(List<CxPlanProductStatus> cxPlanProductStatusList,Double beforeNoseWidth) {
        CxPlanProductStatus newCxPlanProductStatus=null;
        Double minNoseWidth=null;//机头宽度差异
        for(CxPlanProductStatus cxPlanProductStatus:cxPlanProductStatusList){
            String embryoCode=cxPlanProductStatus.getEmbryoCode();
            String bomDataVersion=cxPlanProductStatus.getBomDataVersion();
            String afterKey=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
            //获取施工信息
            EngineProductConstructionInfo afterSpec=engineConstructionInfoMap.get(afterKey);
            Double afterNoseWidth=afterSpec.getNoseWidth();//机头宽度
            //扣圈盘直径和机头宽度完全一样直接优先
            if(beforeNoseWidth.equals(afterNoseWidth)){
                newCxPlanProductStatus=cxPlanProductStatus;
                break;//跳出循环
            }
            //扣圈盘直径或者机头宽度存在不一样，直接判断机头宽度差异
            Double calcNoseWidth=Math.abs(beforeNoseWidth-afterNoseWidth); //取绝对值只管差异不管正负数
            if(minNoseWidth==null){
                minNoseWidth=calcNoseWidth;
                newCxPlanProductStatus=cxPlanProductStatus;
            }else if(calcNoseWidth< minNoseWidth){ //取最小的
                minNoseWidth=calcNoseWidth;
                newCxPlanProductStatus=cxPlanProductStatus;
            }
        }
        return  newCxPlanProductStatus;
    }

    /**
     * 根据定点机台限制作业筛选出优先作业规格列表
     * @param machineCode 成型机台
     * @param cxPlanProductStatusList 待投产任务列表
     * @return
     */
    private List<CxPlanProductStatus> specifyMachineProductList(String machineCode,List<CxPlanProductStatus> cxPlanProductStatusList,String jobType) {
        if(StringUtils.isEmpty(jobType)){
            jobType=CxEngineConstants.SPECIFY_JOB_TYPE_YES;
        }
        List<CxPlanProductStatus> specifyMachineProductList=new ArrayList<>(cxPlanProductStatusList.size());
        if(jobType.equals(CxEngineConstants.SPECIFY_JOB_TYPE_YES)){
            //获取限制机台作业的规格列表数据
            List<CxEngineSpecifyMachine> useMachineList=null;
            if(StringUtils.isNotEmpty(specifyMachineYesMap)&&specifyMachineYesMap.containsKey(machineCode)){
                useMachineList=specifyMachineYesMap.get(machineCode);
            }
            machSpecifyMachineSetting(useMachineList,cxPlanProductStatusList,specifyMachineProductList);
            return specifyMachineProductList;
        }else{
            //获取机台不可作业的规格列表数据
            List<CxEngineSpecifyMachine> undoMachineList=null;
            if(StringUtils.isNotEmpty(specifyMachineNoMap)&&specifyMachineNoMap.containsKey(machineCode)){
                undoMachineList=specifyMachineNoMap.get(machineCode);
            }
            //从不可作业规格中中若匹配到则返回不可投产
            machSpecifyMachineSetting(undoMachineList,cxPlanProductStatusList,specifyMachineProductList);
            if(StringUtils.isNotEmpty(specifyMachineProductList)){
                cxPlanProductStatusList.removeAll(specifyMachineProductList);
            }
            return  cxPlanProductStatusList;
        }
    }

    /**
     * 匹配定点机台数据
     * @param useMachineList
     * @param specifyMachineProductList
     */
    private void machSpecifyMachineSetting(List<CxEngineSpecifyMachine> useMachineList,List<CxPlanProductStatus> cxPlanProductStatusList, List<CxPlanProductStatus> specifyMachineProductList) {
        if(StringUtils.isNotEmpty(useMachineList)){
            for(CxPlanProductStatus cxPlanProductStatus:cxPlanProductStatusList){
                String machKey=CxScheduleUtils.getMapKeyByInputString(cxPlanProductStatus.getSapCode(),cxPlanProductStatus.getEmbryoCode());
                for (CxEngineSpecifyMachine cxEngineSpecifyMachine: useMachineList) {
                    String ignoreMapKey=CxScheduleUtils.getMapKeyByInputString(cxEngineSpecifyMachine.getCxMachineCode(),cxEngineSpecifyMachine.getSapCode(),cxEngineSpecifyMachine.getEmbryoCode());
                    if(autoScheduleIgnoreMap.containsKey(ignoreMapKey)){
                        continue;
                    }
                    String key=CxScheduleUtils.getMapKeyByInputString(cxEngineSpecifyMachine.getSapCode(),cxEngineSpecifyMachine.getEmbryoCode());
                    if(key.equals(machKey)){
                        specifyMachineProductList.add(cxPlanProductStatus);
                        //Joran 2021-12-27 匹配到无法进行投产的规格start
                        autoScheduleIgnoreMap.put(ignoreMapKey,cxPlanProductStatus);
                        cxPlanProductStatusList.remove(cxPlanProductStatus);
                        //Joran 2021-12-27 匹配到无法进行投产的规格end
                        break;//跳出内循环
                    }
                }
            }
        }
    }

    /**
     * 根据匹配到的待投产规格进行生成排程结果数据
     * @param cxEngineScheduleResult  前规格排程结果信息
     * @param nextProductPlan 未投产规格信息
     * @param newSpecConstructionInfo  新投产规格的施工信息
     * @return
     */
    private CxEngineScheduleResult createScheduleResultByPlanProductStatus(List<CxEngineScheduleResult> lastDayScheduleResultList,CxEngineScheduleResult cxEngineScheduleResult,CxPlanProductStatus nextProductPlan,EngineProductConstructionInfo newSpecConstructionInfo) {
        //Joran 2021-12-20 处理筛选可自动安排计划的任务start
        Map<String,CxEngineScheduleResult> toProductMap=new HashMap<>();
        if(StringUtils.isNotEmpty(lastDayScheduleResultList)){
            for(CxEngineScheduleResult lastDayScheduleResult:lastDayScheduleResultList){
                if(CxEngineConstants.TO_PRODUCT_YES.equals(lastDayScheduleResult.getToProduct())){
                    toProductMap.put(lastDayScheduleResult.getEmbryoCode(),lastDayScheduleResult);
                }
            }
        }
        //Joran 2021-12-20 处理筛选可自动安排计划的任务end
        CxEngineScheduleResult newSpecPlanResult= CxScheduleUtils.createNewScheduleResultTask(cxEngineScheduleResult,nextProductPlan,newSpecConstructionInfo);
        String scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",newSpecPlanResult.getScheduleDate());
        //成型工单号
        newSpecPlanResult.setOrderNo(commonCacheService.getCxSequence(CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX+scheduleDateStr, CxPrefixConstants.CX_ORDER_NO_PREFIX+scheduleDateStr));
        //Joran 2021-11-25 从胎胚汇总表查询月度剩余量start
        setNewSpecPlanMonthRemainQty(nextProductPlan,newSpecPlanResult);
        //Joran 2021-11-25 从胎胚汇总表查询月度剩余量end
        //Joran 2021-12-30 如果胎胚已经存在投产计划则不再自动安排标记start
        if(toProductMap.containsKey(nextProductPlan.getEmbryoCode())){
            CxEngineScheduleResult lastScheduleResult=toProductMap.get(nextProductPlan.getEmbryoCode());
            //同胎胚不设置自动计划安排，只单纯进行计划选择
            newSpecPlanResult.setToProduct(CxEngineConstants.TO_PRODUCT_NO);
            //同胎胚复用库存地点
            newSpecPlanResult.setStorageLocation(lastScheduleResult.getStorageLocation());
            newSpecPlanResult.setMaximumClassQty(lastScheduleResult.getMaximumClassQty());
        }else{
            newSpecPlanResult.setToProduct(CxEngineConstants.TO_PRODUCT_YES);
        }
        //Joran 2021-12-30 如果胎胚已经存在投产计划则不再自动安排标记end
        //计算最小硫化机需求数和单班硫化量
        commonCacheService.calcLeastLhMachineQty(newSpecPlanResult,nextProductPlan,sapSpecMoldUseList,null);
        //Joran 2022-01-03 设置外胎规格描述
        commonCacheService.setSpecDescBySapCode(newSpecPlanResult,null);
        return newSpecPlanResult;
    }

    /**
     * 设置新投产规格的月度剩余量
     * @param newSpecPlanResult
     */
    private void setNewSpecPlanMonthRemainQty(CxPlanProductStatus nextPlanProduct,CxEngineScheduleResult newSpecPlanResult) {
        CxEngineEmbryoMonthPlanSurplus condition=new CxEngineEmbryoMonthPlanSurplus();
        condition.setEmbryoCode(nextPlanProduct.getEmbryoCode());
        condition.setMonthPlanApsVersion(nextPlanProduct.getMonthPlanApsVersion());
        List<CxEngineEmbryoMonthPlanSurplus> cxEngineEmbryoMonthPlanSurplusList=this.cxEngineEmbryoMonthPlanSurplusService.selectCxEmbryoMonthPlanSurplusList(condition);
        //Joran 2021-12-22标记没有进行投产且未收尾的
        newSpecPlanResult.setIsProducted(false);
        if(StringUtils.isEmpty(cxEngineEmbryoMonthPlanSurplusList)){
            newSpecPlanResult.setMonthRemainQty(0);
        }else{
            CxEngineEmbryoMonthPlanSurplus engineEmbryoMonthPlanSurplus=cxEngineEmbryoMonthPlanSurplusList.get(0);
            newSpecPlanResult.setMonthRemainQty(engineEmbryoMonthPlanSurplus.getMonthRemainQty());
            newSpecPlanResult.setCxMonthFinishQty(engineEmbryoMonthPlanSurplus.getMonthFinishQty());
            newSpecPlanResult.setMonthFinishQty(engineEmbryoMonthPlanSurplus.getMonthFinishQty());
            if(engineEmbryoMonthPlanSurplus.getMonthRemainQty()<=0){
                //Joran 2021-12-22标记已投产且已收尾过的
                newSpecPlanResult.setIsProducted(true);
            }
        }
    }

    /**
     *
     * 设置库区信息
     * @param newSpecPlanResult
     */
    private void setResultStorageLocation(CxEngineScheduleResult newSpecPlanResult,StringBuilder logDetail) {
        String sapCode=newSpecPlanResult.getSapCode();
        String embryoCode=newSpecPlanResult.getEmbryoCode();
        String bomDataVersion=newSpecPlanResult.getBomDataVersion();
        String machKey=GenerageMapKeyUtils.createMapKey(sapCode,embryoCode,bomDataVersion);
        if(StringUtils.isNotEmpty(sapEmbryoCodeProdPlanMap)&&sapEmbryoCodeProdPlanMap.containsKey(machKey)){
             List<MdmMonthProdPlan> mdmMonthProdPlanList=sapEmbryoCodeProdPlanMap.get(machKey);
            //根据生产顺序升序
            Comparator<MdmMonthProdPlan> productSortAsc = Comparator.comparing(MdmMonthProdPlan::getProductSort);
            Collections.sort(mdmMonthProdPlanList,productSortAsc);
            Integer cxMonthFinishQty=newSpecPlanResult.getMonthFinishQty();
            Integer totalPlanQty=0;
            boolean markStorageLocation=false;
            for(MdmMonthProdPlan mdmMonthProdPlan:mdmMonthProdPlanList){
                totalPlanQty+=mdmMonthProdPlan.getMonthTotalPlanQty();
                if(cxMonthFinishQty<totalPlanQty){
                    log.debug("【库存地点匹配】成型完成量："+cxMonthFinishQty+"，当前计划量："+mdmMonthProdPlan.getMonthTotalPlanQty()+",胎胚代码："+embryoCode+",库存地点："+mdmMonthProdPlan.getStorageLocation());
                    logDetail.append("【库存地点匹配】成型完成量："+cxMonthFinishQty+"，当前计划量："+mdmMonthProdPlan.getMonthTotalPlanQty()+",SAP代码："+sapCode+",胎胚代码："+embryoCode+",库存地点："+mdmMonthProdPlan.getStorageLocation()).append(division);
                    newSpecPlanResult.setStorageLocation(mdmMonthProdPlan.getStorageLocation());
                    markStorageLocation=true;
                    break; //匹配到库存库区设置完毕
                }
            }
            //Joran 2021-12-28当完成量大于月度总计划量时则取顺序最大的那个库存地点start
            if(!markStorageLocation&&totalPlanQty<cxMonthFinishQty){
                MdmMonthProdPlan maxSortPlan=mdmMonthProdPlanList.get(mdmMonthProdPlanList.size()-1);
                newSpecPlanResult.setStorageLocation(maxSortPlan.getStorageLocation());
            }
            //Joran 2021-12-28当完成量大于月度总计划量时则取顺序最大的那个库存地点end


        }else{
            log.error("【库存地点设置】sap品号："+sapCode+",胎胚代码："+embryoCode+"未找到对应的月度计划明细信息" );
            logDetail.append("【库存地点设置】sap品号："+sapCode+",胎胚代码："+embryoCode+"未找到对应的月度计划明细信息").append(division);
        }
        String title="【成型机台:"+newSpecPlanResult.getCxMachineCode()+"胎胚代码："+newSpecPlanResult.getEmbryoCode()+"设置库存地点】";
        /*autoScheduleLogService.insertCxScheduleLog(newSpecPlanResult.getCxBatchNo(), newSpecPlanResult.getOrderNo(), title,
                logSplit("外胎汇总表处理集合：" + toJSONString(sapEmbryoCodeProdPlanMap), "设置库存地点后结果：" + newSpecPlanResult.getStorageLocation())); //添加日志*/
    }

    /**
     * 判断是否可以进行新规格投产
     * @param avgAvailableLhShift 机台规格平均可硫化班次
     * @param minAvailableLhShift  班次最小可硫化班次数
     * @return
     */
    private boolean addSpecValidate(Double avgAvailableLhShift,Double minAvailableLhShift,StringBuilder logDetail) {
        String addSpecAvgLhClassShifts=cxParamsMap.get(CxParamCodeConstants.ADD_SPEC_AVG_LH_CLASS_SHIFTS);
        if(StringUtils.isEmpty(addSpecAvgLhClassShifts)){
            throw new IllegalArgumentException(I18nUtil.getMessage("cx.engine.auto.add.spec.avgAvailableLhShift.param.error"));
        }
        Double addSpecAvgLhClassShift=Double.valueOf(addSpecAvgLhClassShifts);

        String addSpecLimitShifts=cxParamsMap.get(CxParamCodeConstants.ADD_SPEC_LIMIT_SHIFT);
        if(StringUtils.isEmpty(addSpecLimitShifts)){
            throw new IllegalArgumentException(I18nUtil.getMessage("cx.engine.auto.add.spec.addSpecLimitShift.param.error"));
        }
        Double addSpecLimitShift=Double.valueOf(addSpecLimitShifts);
        boolean isAdd=(minAvailableLhShift>addSpecLimitShift)&&(avgAvailableLhShift>=addSpecAvgLhClassShift);
        logDetail.append("【可投产新规格平均班数设置】").append(addSpecAvgLhClassShifts).append(",当前平均班数：").append(avgAvailableLhShift).append(",最小可硫化班数=").append(minAvailableLhShift).append(",是否可投产新规格：").append(isAdd).append(division);
        return  isAdd;
    }

    /**
     *  下个规格任务创建
     * @param nextCxEngineScheduleResult
     * @param preLastOneTask
     * @param avgAvalableLhShift
     */
    private Map<String, List<CxAutoScheduleTask>> nextScheduleResultCreateTask(CxEngineScheduleResult nextCxEngineScheduleResult, CxAutoScheduleTask preLastOneTask, Double avgAvalableLhShift,Double classOneAvgAvalableLhShift,StringBuilder logDetail,boolean sameTask) {
        Map<String, List<CxAutoScheduleTask>> nextScheduleTaskMap=null;
        logDetail.append("【下个规格任务创建】，胎胚代码："+nextCxEngineScheduleResult.getEmbryoCode()).append(division);
        nextCxEngineScheduleResult.initPlanQty();
        String embryoCode=nextCxEngineScheduleResult.getEmbryoCode();
        String machineCode=nextCxEngineScheduleResult.getCxMachineCode();
        String  specDimension=nextCxEngineScheduleResult.getSpecDimension()==null?"":nextCxEngineScheduleResult.getSpecDimension().toString();
        Integer  singleShiftLhQty=nextCxEngineScheduleResult.getSingleShiftLhQty();
        //获取成型机规格定额数据
        Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(machineCode,embryoCode,nextCxEngineScheduleResult.getBomDataVersion(),logDetail);
        logDetail.append("下个规格排产获取定额：").append(machineQuota).append(division);
        logDetail.append("下个规格【"+ClassEnums.getClassEnums(preLastOneTask.getClassShift()).getClassName()+"】平均可硫化班次数：").append(avgAvalableLhShift).append(division);
        //计算规格可以排的最大硫化班次
        Double maxLhCount=maxLhShiftCount(nextCxEngineScheduleResult,specDimension,avgAvalableLhShift,classOneAvgAvalableLhShift,logDetail);
        logDetail.append("下个规格最大安排硫化班次：").append(nextCxEngineScheduleResult.getMaximumClassQty()).append(division);
        logDetail.append("下个规格单班硫化量：").append(singleShiftLhQty).append(",");
        //根据最大可硫化班次设置，计算单次安排任务总量
        int taskQty= (int) Math.ceil(maxLhCount * singleShiftLhQty);
        logDetail.append("下个规格总计划量：").append(taskQty).append(division);
        //计划总量根据耗损率进行重新计算
        taskQty=calcLossRate(machineCode,embryoCode,taskQty,logDetail);
        //连续计划量计算
        Integer continuePlanQty=calcContinuePlanQty(nextCxEngineScheduleResult,preLastOneTask,ClassEnums.getClassEnums(preLastOneTask.getClassShift()),logDetail,sameTask);
        logDetail.append("下个规格连续计划量：").append(continuePlanQty).append(division);
        Integer remainTaskQty=taskQty - continuePlanQty; //剩余任务量
        logDetail.append("下个规格任务剩余量：").append(remainTaskQty).append(division);
        Map<String,List<CxAutoScheduleTask>> taskFail;
        if(remainTaskQty <= 0){
            logDetail.append("下个规格没有剩余任务量，单规格预排任务不进行生成，剩余任务量：").append(remainTaskQty).append(division);
            continuePlanQty=calcContinuePlanQty(nextCxEngineScheduleResult,preLastOneTask,ClassEnums.getClassEnums(preLastOneTask.getClassShift()),logDetail,true);
            logDetail.append("【为了任务继续往下自动排，连续生产量清0】,当前连续生产量=").append(continuePlanQty).append(division);
            remainTaskQty=taskQty - continuePlanQty; //剩余任务量
            logDetail.append("【为了任务继续往下自动排】,重新计算的任务剩余量：").append(remainTaskQty).append(division);
        }
        //上个规格排产最大的班次
        Integer classShift=preLastOneTask.getClassShift();
        //从map中获取实际月度剩余量
        Integer remainMonthQty=getRealMonthRemainQty(nextCxEngineScheduleResult);
        logDetail.append("下个规格月度剩余量：").append(remainMonthQty).append(division);
        if(remainMonthQty <= 0){
            logDetail.append("下个规格没有月度剩余量，规格续排任务不进行生成，月度剩余量：").append(remainMonthQty).append(division);
            log.debug(logDetail.toString());
            taskFail=new HashMap<>();
            taskFail.put(CxEngineConstants.AUTO_OUTOVER_REMAIN_MONTH_QTY,null);//没有月度剩余量
            return taskFail;
        }
        log.debug(logDetail.toString());
        //下一个班次排班的任务列表
        Map<String, List<CxAutoScheduleTask>> scheduleTaskMap=new HashMap<>();
        List<CxAutoScheduleTask> taskList=new ArrayList<>();
        int onceCloseOut=getOnceCloseOutQty();
        //是否一次性投产
        boolean onceProduct=remainMonthQty<=onceCloseOut;
        //月度剩余量如果大于任务量 则以任务余量进行排产，如果月度剩余量小于任务余量则用月度剩余量进行投产
        remainTaskQty=(remainMonthQty>remainTaskQty&&!onceProduct)?remainTaskQty:remainMonthQty;
        BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR)); //一个小时生产多少
        logDetail.append("【自动任务安排】规格小时产量：").append(hourCountBig).append(division);
        String key=GenerageMapKeyUtils.createMapKey(preLastOneTask.getCxMachineCode(),classShift+"");
        Double remainTime=machineShiftHourMap.get(key);
        if(remainTime>=CxEngineConstants.ZERO){ //还有剩余时间 需要扣除掉规格更换时间
            nextScheduleTaskMap=new HashMap<>();
            String afterKey=GenerageMapKeyUtils.createMapKey(embryoCode,nextCxEngineScheduleResult.getBomDataVersion());
            String beforeKey=GenerageMapKeyUtils.createMapKey(preLastOneTask.getEmbryoCode(),preLastOneTask.getBomDataVersion());
            Double changeSpecTime=changeSpecTime(afterKey,beforeKey,logDetail);
            //二分之一更换工装时间
            Double halfChangeSpecTime=BigDecimal.valueOf(changeSpecTime).divide(BigDecimal.valueOf(2)).doubleValue();
            //剩余时间小于更换工装的时长 直接进入下一个班次排产
            if(remainTime <= halfChangeSpecTime){
                //更新班次剩余时长为0
                updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),classShift, BigDecimal.ZERO.doubleValue());
                //前规格剩余时间小于更换工装工时一半，则下个班扣除整个更换工装时长
                classShift+=1;
                ClassEnums cls =ClassEnums.getClassEnums(classShift);
                log.debug("【自动任务安排】剩余时间:"+preLastOneTask.getRemainTime()+",二分之一更换工装时间："+changeSpecTime+"比二分之一更换工装时间短，不再进行当前班次排产，直接进入下一个班次");
                logDetail.append("【自动任务安排】剩余时间:"+preLastOneTask.getRemainTime()+",二分之一更换工装时间："+changeSpecTime+"比二分之一更换工装时间短，不再进行当前班次排产，直接进入下一个班次").append(division);
                if(cls==null){
                    log.debug("【自动任务安排】超过当天排班班次数，结束续排");
                    logDetail.append("【自动任务安排】超过当天排班班次数，结束续排").append(division);
                    taskFail=new HashMap<>();
                    taskFail.put(CxEngineConstants.AUTO_OUTOVER_REMAIN_TIME,null);//没有时间
                    return taskFail;
                }
                //Joran 2021-12-24 更换工装在前规格计划量当前规格扣减
                key=GenerageMapKeyUtils.createMapKey(preLastOneTask.getCxMachineCode(),classShift+"");
                //获取班次最新剩余时间
                remainTime=machineShiftHourMap.get(key);
                BigDecimal shiftRemainTimeBig=BigDecimal.valueOf(remainTime-changeSpecTime); //剩余时间
                logDetail.append("【自动任务安排】前规格换工装当前班扣除更换工装剩余时：").append(shiftRemainTimeBig).append(division);
                BigDecimal currentPlanQty=shiftRemainTimeBig.multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN);
                logDetail.append("【自动任务安排】规格计算可投产计划量：").append(currentPlanQty).append(division);
                Integer currentShiftPlanQty=currentPlanQty.intValue();
                if(remainTaskQty>currentShiftPlanQty) { //月度剩余量大于当班计划量
                    remainTaskQty-=currentShiftPlanQty;
                    continuePlanQty+=currentShiftPlanQty;
                    taskList.add(createClassShiftRemainQty(nextCxEngineScheduleResult,classShift,currentShiftPlanQty,taskQty,continuePlanQty,remainTime));
                    //更新班次剩余时长为0
                    updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),classShift, BigDecimal.ZERO.doubleValue());
                    if(!isBreakTask(nextCxEngineScheduleResult,classShift,currentShiftPlanQty)){
                        classShift+=1;
                        scheduleTaskMap=scheduleTaskMap(nextCxEngineScheduleResult,classShift,taskQty,continuePlanQty,remainTaskQty,machineQuota,onceProduct,1);
                        if(StringUtils.isNotEmpty(scheduleTaskMap)){
                            List<CxAutoScheduleTask> scheduleTaskList=scheduleTaskMap.get(nextCxEngineScheduleResult.getEmbryoCode());
                            taskList.addAll(scheduleTaskList);
                        }
                    }
                    nextScheduleTaskMap.put(nextCxEngineScheduleResult.getEmbryoCode(),taskList);
                }else{
                    currentShiftPlanQty=remainTaskQty;
                    continuePlanQty+=currentShiftPlanQty;
                    CxAutoScheduleTask autoScheduleTask=createClassShiftRemainQty(nextCxEngineScheduleResult,classShift,currentShiftPlanQty,taskQty,continuePlanQty,remainTime);
                    CxScheduleUtils.calcRemainTime(autoScheduleTask,machineQuota,currentShiftPlanQty);
                    taskList.add(autoScheduleTask);
                    nextScheduleTaskMap.put(nextCxEngineScheduleResult.getEmbryoCode(),taskList);
                    log.debug("【自动任务安排】续作时月度剩余量为0，直接排完计算剩余时间，当前任务为："+autoScheduleTask.toString());
                    logDetail.append("【自动任务安排】续作时月度剩余量为0，直接排完计算剩余时间，当前任务为："+autoScheduleTask.toString()).append(division);
                    //更新班次剩余时长
                    updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),classShift, autoScheduleTask.getRemainTime());
                    return nextScheduleTaskMap;
                }
            }else{
                String changeMoldAnalysis="";
                //实际剩余时间
                BigDecimal remainTimeBig=null;
                if(remainTime>=changeSpecTime){ //剩余时长如果大于更换工装的时长则扣除更换工装时长
                    remainTimeBig=BigDecimal.valueOf(remainTime-changeSpecTime); //剩余时间
                    if(nextCxEngineScheduleResult.getNewSpecFlag()&&!nextCxEngineScheduleResult.getMarkNewSpecAnalysisFlag()){
                        //有换工装开班就走换工装
                        nextCxEngineScheduleResult.setMarkNewSpecAnalysisFlag(true);
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
                    }else{
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
                    }
                    CxScheduleUtils.setClassAnalysis(nextCxEngineScheduleResult,ClassEnums.getClassEnums(classShift),changeMoldAnalysis);
                }else{ //不够则扣除二分之一的更换工装的时长
                    //时间预留来进行工装更换
                    //更新班次剩余时长为0
                    updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),classShift, BigDecimal.ZERO.doubleValue());
                    //Joran 2021-12-29前一个班次标记为更换工装，后个班次特殊处理不进行标记
                    if(nextCxEngineScheduleResult.getNewSpecFlag()&&!nextCxEngineScheduleResult.getMarkNewSpecAnalysisFlag()){
                        //有换工装开班就走换工装
                        nextCxEngineScheduleResult.setMarkNewSpecAnalysisFlag(true);
                       changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
                    }else{
                       changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
                    }
                    CxScheduleUtils.setClassAnalysis(nextCxEngineScheduleResult,ClassEnums.getClassEnums(classShift),changeMoldAnalysis);
                    classShift+=1;
                    ClassEnums nextCls =ClassEnums.getClassEnums(classShift);
                    log.debug("【班次剩余时间不足】剩余时间:"+preLastOneTask.getRemainTime()+",二分之一更换工装时间："+changeSpecTime+"比二分之一更换工装时间短，不再进行当前班次排产，直接进入下一个班次");
                    logDetail.append("【班次剩余时间不足】剩余时间:"+preLastOneTask.getRemainTime()+",二分之一更换工装时间："+changeSpecTime+"比二分之一更换工装时间短，不再进行当前班次排产，直接进入下一个班次").append(division);
                    if(nextCls==null){
                        log.debug("【班次剩余时间不足】超过当天排班班次数，结束续排");
                        logDetail.append("【班次剩余时间不足】超过当天排班班次数，结束续排").append(division);
                        taskFail=new HashMap<>();
                        taskFail.put(CxEngineConstants.AUTO_OUTOVER_REMAIN_TIME,null);//没有时间
                        return taskFail;
                    }
                    //下一个班次的话时间要扣除二分之一的更换工装时间
                    key=GenerageMapKeyUtils.createMapKey(preLastOneTask.getCxMachineCode(),classShift+"");
                    remainTime=machineShiftHourMap.get(key);
                    remainTimeBig=BigDecimal.valueOf(remainTime-halfChangeSpecTime); //剩余时间
                    //更新班次剩余时长
                    updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),classShift, remainTimeBig.doubleValue());
                }
                //更新班次剩余时长为扣除工装后的时间
                updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),classShift, remainTimeBig.doubleValue());
                key=GenerageMapKeyUtils.createMapKey(preLastOneTask.getCxMachineCode(),classShift+"");
                remainTime=machineShiftHourMap.get(key);
                logDetail.append("【自动任务安排】扣除更换工装剩余时：").append(remainTimeBig).append(division);
                BigDecimal currentPlanQty=remainTimeBig.multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN);
                logDetail.append("【自动任务安排】规格计算可投产计划量：").append(currentPlanQty).append(division);
                Integer currentShiftPlanQty=currentPlanQty.intValue();
                if(remainTaskQty>currentShiftPlanQty){ //月度剩余量大于当班计划量
                    remainTaskQty-=currentShiftPlanQty;
                    continuePlanQty+=currentShiftPlanQty;
                    taskList.add(createClassShiftRemainQty(nextCxEngineScheduleResult,classShift,currentShiftPlanQty,taskQty,continuePlanQty,remainTime));
                    //更新班次剩余时长为0
                    updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),classShift, BigDecimal.ZERO.doubleValue());
                    if(!isBreakTask(nextCxEngineScheduleResult,classShift,currentShiftPlanQty)){
                        classShift+=1;
                        scheduleTaskMap=scheduleTaskMap(nextCxEngineScheduleResult,classShift,taskQty,continuePlanQty,remainTaskQty,machineQuota,onceProduct,1);
                        if(StringUtils.isNotEmpty(scheduleTaskMap)){
                            List<CxAutoScheduleTask> scheduleTaskList=scheduleTaskMap.get(nextCxEngineScheduleResult.getEmbryoCode());
                            taskList.addAll(scheduleTaskList);
                        }
                    }

                    nextScheduleTaskMap.put(nextCxEngineScheduleResult.getEmbryoCode(),taskList);
                }else{
                    currentShiftPlanQty=remainTaskQty;
                    continuePlanQty+=currentShiftPlanQty;
                    CxAutoScheduleTask autoScheduleTask=createClassShiftRemainQty(nextCxEngineScheduleResult,classShift,currentShiftPlanQty,taskQty,continuePlanQty,remainTime);
                    CxScheduleUtils.calcRemainTime(autoScheduleTask,machineQuota,currentShiftPlanQty);
                    taskList.add(autoScheduleTask);
                    nextScheduleTaskMap.put(nextCxEngineScheduleResult.getEmbryoCode(),taskList);
                    log.debug("【自动任务安排】续作时月度剩余量为0，直接排完计算剩余时间，当前任务为："+autoScheduleTask.toString());
                    logDetail.append("【自动任务安排】续作时月度剩余量为0，直接排完计算剩余时间，当前任务为："+autoScheduleTask.toString()).append(division);
                    //更新班次剩余时长
                    updateMachineShiftHourMap(preLastOneTask.getCxMachineCode(),classShift, autoScheduleTask.getRemainTime());
                    return nextScheduleTaskMap;
                }

            }
        }
        return nextScheduleTaskMap;
    }

    /**
     * 验证规格投产规格是否和前规格相同
     * @param lastCxOrderNo 前规格工单号
     * @param nextCxOrderNo 后规格工单号
     * @return
     */
    private boolean validateSameTask(String  lastCxOrderNo, String nextCxOrderNo) {
        return nextCxOrderNo.equals(lastCxOrderNo);
    }

    /**
     * 计划总量根据耗损率重新计算
     * @param machineCode
     * @param embryoCode
     * @param taskQty
     * @return
     */
    private int calcLossRate(String machineCode, String embryoCode, int taskQty,StringBuilder logDetail) {
        int finalTaskQty=taskQty;
        String lossRateKey=CxScheduleUtils.getMapKeyByInputString(machineCode,embryoCode);
        if(StringUtils.isNotEmpty(cxMachineLossRateMap)&&cxMachineLossRateMap.containsKey(lossRateKey)){
            Double lossRate=cxMachineLossRateMap.get(lossRateKey);
            if(lossRate!=null){
               log.debug("【耗损率计算】获取到耗损率信息："+lossRate);
                finalTaskQty=(int) Math.ceil(taskQty * (1+(lossRate/100)));
                log.debug("【耗损率计算】考虑耗损率后总计划量："+finalTaskQty);
                logDetail.append("【耗损率计算】获取到耗损率信息："+lossRate).append(";").append(division);;
                logDetail.append("【耗损率计算】考虑耗损率后总计划量："+finalTaskQty).append(division);
            }
        }
        return finalTaskQty;
    }

    /**
     * 创建班次剩余量任务安排
     * @param nextCxEngineScheduleResult
     * @param currentShiftPlanQty
     */
    private CxAutoScheduleTask createClassShiftRemainQty(CxEngineScheduleResult nextCxEngineScheduleResult,int classShift,Integer currentShiftPlanQty,int taskQty,int continuePlanQty,Double remainTime) {
        CxAutoScheduleTask autoScheduleTask=CxScheduleUtils.createScheduleTask(nextCxEngineScheduleResult,classShift,taskQty,continuePlanQty,remainTime);
        autoScheduleTask.setCurrentShiftPlanQty(currentShiftPlanQty);
        autoScheduleTask.setScheduleDate(DateUtils.parseDateToStr("yyyyMMdd",nextCxEngineScheduleResult.getScheduleDate()));
        autoScheduleTask.setCxOrderNo(nextCxEngineScheduleResult.getOrderNo());
        autoScheduleTask.setRemainTime(CxEngineConstants.ZERO);
        autoScheduleTask.setClassShiftHour(remainTime);
        return  autoScheduleTask;
    }

    /**
     * 根据规格切换获取消耗的时间（自动排程只会排小换工装，大换工装在手工插单或导入需要考虑）
     * @param afterKey 当前施工key
     * @param beforeKey 上一个规格施工key
     * @return
     */
    private Double changeSpecTime(String afterKey,String beforeKey,StringBuilder logDetail) {
        if(StringUtils.isEmpty(beforeKey)){
            return CxEngineConstants.ZERO;
        }else if(afterKey.equals(beforeKey)){
            return  CxEngineConstants.ZERO;
        }
        //获取两个规格间的施工信息
        EngineProductConstructionInfo afterSpec=engineConstructionInfoMap.get(afterKey);
        EngineProductConstructionInfo beforeSpec=engineConstructionInfoMap.get(beforeKey);
        if(afterSpec==null||beforeSpec==null){
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.change.spec.time.error"), GenerageMapKeyUtils.getEmbryoCodeByCreateKey(beforeKey), GenerageMapKeyUtils.getEmbryoCodeByCreateKey(afterKey)));
        }
        Double beforeNoseWidth=beforeSpec.getNoseWidth();//机头宽度
        if(beforeNoseWidth==null){
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.beforeNoseWidth.error"),GenerageMapKeyUtils.getEmbryoCodeByCreateKey(beforeKey)));
        }
        Double beforeFlipDiscDiameter=beforeSpec.getFlipDiscDiameter();//扣圈盘直径

        Double afterNoseWidth=afterSpec.getNoseWidth();//机头宽度
        Double afterFlipDiscDiameter=afterSpec.getFlipDiscDiameter();//扣圈盘直径

        //扣圈盘直径和机头宽度完全一样，不需要进行工装更换
        if(beforeFlipDiscDiameter!=null&&beforeFlipDiscDiameter.equals(afterFlipDiscDiameter)&&beforeNoseWidth.equals(afterNoseWidth)){
            return CxEngineConstants.ZERO;
        }else if(beforeNoseWidth.equals(afterNoseWidth)){
            return CxEngineConstants.ZERO;
        }
        String minChangeSpecTime=cxParamsMap.get(CxParamCodeConstants.CX_MIN_CHANGE_SPEC_TIME);
        if(StringUtils.isEmpty(minChangeSpecTime)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.change.spec.min.time.param.error"));
        }
        //根据配置获取更换规格切换时间
        Double minChangeSpecTimeMin=Double.valueOf(minChangeSpecTime);
        Double minChangeSpecHour=BigDecimal.valueOf(minChangeSpecTimeMin/CxEngineConstants.ONE_MINUTE_SECOND).setScale(CxEngineConstants.TWO_SCALE, BigDecimal.ROUND_HALF_UP).doubleValue();
        log.debug("【计算更换工装时长】前规格胎胚："+beforeKey+",机头宽度："+beforeNoseWidth+",扣圈盘直径:"+beforeFlipDiscDiameter+"。后规格胎胚："+afterKey+",机头宽度："+afterNoseWidth+",扣圈盘直径:"+afterFlipDiscDiameter+"，更换工装时长："+minChangeSpecHour+"(小时)");
        logDetail.append("【计算更换工装时长】前规格胎胚："+beforeKey+",机头宽度："+beforeNoseWidth+",扣圈盘直径:"+beforeFlipDiscDiameter+"。后规格胎胚："+afterKey+",机头宽度："+afterNoseWidth+",扣圈盘直径:"+afterFlipDiscDiameter+"，更换工装时长："+minChangeSpecHour+"(小时)").append(division);
        return minChangeSpecHour;
    }


    /**
     *  将任务列表安排到排程结果表对应的班次计划中
     * @param cxEngineScheduleResult
     * @param preScheduleTaskList
     * @param cxAutoScheduleTaskListMap
     */
    private void setClassPlanQtyByAutoScheduleTask(CxEngineScheduleResult cxEngineScheduleResult, List<CxAutoScheduleTask> preScheduleTaskList,Map<String, List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap,StringBuilder logDetail) {
        logDetail.append("自动生成任务集合组装到排程结果中》》").append(division);
        if(StringUtils.isNotEmpty(cxAutoScheduleTaskListMap)&&StringUtils.isNotEmpty(preScheduleTaskList)){
            Map<ClassEnums,Integer> classSortMap=new HashMap<>();
            //创建班次任务顺序
            buildClassSort(cxAutoScheduleTaskListMap,classSortMap);
            for(CxAutoScheduleTask cxAutoScheduleTask:preScheduleTaskList){
                Integer classShift=cxAutoScheduleTask.getClassShift();//获取安排的班次
                ClassEnums cls =ClassEnums.getClassEnums(classShift);
                int sort=classSortMap.get(cls);
                Integer planQty=cxAutoScheduleTask.getCurrentShiftPlanQty();
                switch (cls){
                    case CLASS_ONE:
                        cxEngineScheduleResult.setClass1PlanQty(planQty);
                        cxEngineScheduleResult.setClass1Sort(sort);
                        break;
                    case CLASS_TWO:
                        cxEngineScheduleResult.setClass2PlanQty(planQty);
                        cxEngineScheduleResult.setClass2Sort(sort);
                        break;
                    case CLASS_THREE:
                        cxEngineScheduleResult.setClass3PlanQty(planQty);
                        cxEngineScheduleResult.setClass3Sort(sort);
                        break;
                    case CLASS_FOUR:
                        cxEngineScheduleResult.setClass4PlanQty(planQty);
                        cxEngineScheduleResult.setClass4Sort(sort);
                        break;
                    case CLASS_FIVE:
                        cxEngineScheduleResult.setClass5PlanQty(planQty);
                        cxEngineScheduleResult.setClass5Sort(sort);
                        break;
                    default:break;
                }
                //更新月度剩余量
                setRealMonthRemainQtyMap(cxEngineScheduleResult,cxAutoScheduleTask,logDetail);
            }
            //构建更换工装原因分析
            if(!cxEngineScheduleResult.getNewSpecFlag()){
                buildChangeMoldAnalysis(cxEngineScheduleResult,preScheduleTaskList,logDetail);
            }else{
                //构建更换工装开班
                bulidNewSpecChangeMoldAnalysis(cxEngineScheduleResult,preScheduleTaskList,logDetail);
            }

            //收尾原因分析处理
            bulidCloseOutAnalysis(cxEngineScheduleResult,preScheduleTaskList,logDetail);

            //库存地点设置
            setResultStorageLocation(cxEngineScheduleResult,logDetail);
        }
    }

    /**
     * 更换工装开班原因分析
     * @param cxEngineScheduleResult
     * @param preScheduleTaskList
     */
    private void bulidNewSpecChangeMoldAnalysis(CxEngineScheduleResult cxEngineScheduleResult, List<CxAutoScheduleTask> preScheduleTaskList,StringBuilder logDetail) {
        CxScheduleUtils.sortAscByScheduleTaskClassShift(preScheduleTaskList);
        CxAutoScheduleTask minClassShiftTask=preScheduleTaskList.get(0);
        ClassEnums cls=ClassEnums.getClassEnums(minClassShiftTask.getClassShift());
        String analysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
        logDetail.append("【更换工装开班原因标注】").append(division);
        setChangeMoldCondition(cxEngineScheduleResult,cls,analysis);
        /*switch (cls){
            case CLASS_ONE:
                if(cxEngineScheduleResult.getClass3PlannedQty()==0){
                    cxEngineScheduleResult.setClass1Analysis(analysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,analysis);
                }
                break;
            case CLASS_TWO:
                if(cxEngineScheduleResult.getClass1PlanQty()==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass1Analysis())){
                    cxEngineScheduleResult.setClass2Analysis(analysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,analysis);
                }
                break;
            case CLASS_THREE:
                if(cxEngineScheduleResult.getClass2PlanQty()==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass2Analysis())){
                    cxEngineScheduleResult.setClass3Analysis(analysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,analysis);
                }
                break;
            case CLASS_FOUR:
                if(cxEngineScheduleResult.getClass3PlanQty()==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass3Analysis())){
                    cxEngineScheduleResult.setClass4Analysis(analysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,analysis);
                }
                break;
            case CLASS_FIVE:
                if(cxEngineScheduleResult.getClass4PlanQty()==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass4Analysis())){
                    cxEngineScheduleResult.setClass5Analysis(analysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,analysis);
                }
                break;
            default:break;
        }*/
    }

    /***
     * 更新月度剩余量
     * @param cxEngineScheduleResult
     * @param cxAutoScheduleTask
     */
    private void setRealMonthRemainQtyMap(CxEngineScheduleResult cxEngineScheduleResult,CxAutoScheduleTask cxAutoScheduleTask,StringBuilder logDetail) {
        String embryoCode=cxEngineScheduleResult.getEmbryoCode();
        Integer remainMonthQty=cxEngineScheduleResult.getMonthRemainQty();
        if(monthRemainQtyMap.containsKey(embryoCode)){
            remainMonthQty=monthRemainQtyMap.get(embryoCode);
        }
        //Joran 2021-11-02 新投产规格需要扣减掉当前班次的计划量后再进行缓存
        remainMonthQty-=cxAutoScheduleTask.getCurrentShiftPlanQty();//扣掉班次计划量
        monthRemainQtyMap.put(embryoCode,remainMonthQty);//新投产规格直接缓存
        logDetail.append("【更新月度剩余量】，规格SAP品号："+cxEngineScheduleResult.getSapCode()+"胎胚代码："+embryoCode+",月度剩余量："+remainMonthQty).append(division);
        log.debug("【更新月度剩余量】，规格SAP品号："+cxEngineScheduleResult.getSapCode()+"胎胚代码："+embryoCode+",月度剩余量："+remainMonthQty);

    }

    /**
     * 更换工装原因分析
     * @param cxEngineScheduleResult
     */
    private void buildChangeMoldAnalysis(CxEngineScheduleResult cxEngineScheduleResult, List<CxAutoScheduleTask> preScheduleTaskList,StringBuilder logDetail) {
        //升序排序
        CxScheduleUtils.sortAscByScheduleTaskClassShift(preScheduleTaskList);
        String changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
        CxAutoScheduleTask minClassShiftTask=preScheduleTaskList.get(0);
        /*if(minClassShiftTask.getRemainTaskQty()<=0&&preScheduleTaskList.size()==1){
            log.debug("更换工装原因分析，本规格不处理");
            logDetail.append("更换工装原因分析，本规格不处理").append(division);
            return;
        }*/
        //最小班次的前班次如果有任务量则证明是自己则不换工装
        ClassEnums cls=ClassEnums.getClassEnums(minClassShiftTask.getClassShift());
        Integer beforeClassPlanQty=CxScheduleUtils.getBeforeClassPlanQty(cxEngineScheduleResult,cls);
        if(beforeClassPlanQty>0&&ClassEnums.CLASS_ONE.equals(cls)){//如果是1班的话直接根据前规格是否有量来判断
            log.debug("前规格是同规格不进行工装更换原因分析");
            logDetail.append("前规格是同规格不进行工装更换原因分析").append(division);
            return;
        }else if(beforeClassPlanQty>0&&!ClassEnums.CLASS_ONE.equals(cls)){
            //如果前后班次顺序一样则标识顺序来，不进行原因分析标识
            if(CxScheduleUtils.getAnalysisFlag(cxEngineScheduleResult,cls)){
                log.debug("前规格是同规格不进行工装更换原因分析");
                logDetail.append("前规格是同规格不进行工装更换原因分析").append(division);
                return;
            }
        }
        setChangeMoldCondition(cxEngineScheduleResult,cls,changeMoldAnalysis);

       // CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
    }

    /**
     * 依据条件判断是否需要进行更换工装原因分析
     * @param cxEngineScheduleResult
     * @param cls
     * @param changeMoldAnalysis
     */
    private void setChangeMoldCondition(CxEngineScheduleResult cxEngineScheduleResult, ClassEnums cls, String changeMoldAnalysis) {
        switch (cls){
            case CLASS_ONE:
                if(cxEngineScheduleResult.getClass3PlannedQty()==0){
                    cxEngineScheduleResult.setClass1Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            case CLASS_TWO:
                if(cxEngineScheduleResult.getClass1PlanQty()==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass1Analysis())){
                    cxEngineScheduleResult.setClass2Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            case CLASS_THREE:
                if(cxEngineScheduleResult.getClass2PlanQty()==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass2Analysis())){
                    cxEngineScheduleResult.setClass3Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            case CLASS_FOUR:
                if(cxEngineScheduleResult.getClass3PlanQty()==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass3Analysis())){
                    cxEngineScheduleResult.setClass4Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            case CLASS_FIVE:
                if(cxEngineScheduleResult.getClass4PlanQty()==0&&StringUtils.isEmpty(cxEngineScheduleResult.getClass4Analysis())){
                    cxEngineScheduleResult.setClass5Analysis(changeMoldAnalysis);
                    CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,cls,changeMoldAnalysis);
                }
                break;
            default:break;
        }
    }

    /**
     * 自动排程收尾原因自动分析
     * @param preScheduleTaskList
     */
    private void bulidCloseOutAnalysis(CxEngineScheduleResult cxEngineScheduleResult,List<CxAutoScheduleTask> preScheduleTaskList,StringBuilder logDetail) {
        //降序排序
        CxScheduleUtils.sortDescByScheduleTaskClassShift(preScheduleTaskList);
        CxAutoScheduleTask maxShiftTask=preScheduleTaskList.get(0);
        //扣除掉所有班次后的剩余量
        Integer monthRemainQty=getRealMonthRemainQty(cxEngineScheduleResult);
        if(monthRemainQty<0){
            log.debug("【构建收尾原因分析】"+cxEngineScheduleResult.getEmbryoCode()+"月度剩余量小于0");
            logDetail.append("【构建收尾原因分析】\"+cxEngineScheduleResult.getEmbryoCode()+\"月度剩余量小于0").append(division);
        }
        //剩余量收尾参数
        Integer analysisMarkQty=getAnalysisMarkQty();
        int currentClassShift=0;
        String analysis="";
        if(monthRemainQty>0 && monthRemainQty<=analysisMarkQty){//剩余多少原因分析
            currentClassShift=maxShiftTask.getClassShift();
            analysis=StringUtils.format(I18nUtil.getMessage("cx.engine.auto.analysis.remainQty.title"),monthRemainQty);
            logDetail.append("【构建收尾原因分析】").append("，班次：").append(currentClassShift).append(",生成剩余多少原因分析").append(analysis).append(division);
            CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,ClassEnums.getClassEnums(currentClassShift),analysis);
        }else if(monthRemainQty<=0){//Joran 2021-09-02 当月度剩余量为0时，进行共X收尾提示
            currentClassShift=maxShiftTask.getClassShift();
            Integer totalTaskQty=cxEngineScheduleResult.getDayTotalPlanQty();
            //Joran 2021-12-17 共多少收尾需要到前一天三班计划
            if(cxEngineScheduleResult.getClass3PlannedQty()>0){
                totalTaskQty+=cxEngineScheduleResult.getClass3PlannedQty();
                analysis=StringUtils.format(I18nUtil.getMessage("cx.engine.auto.analysis.continue.shift.title"),totalTaskQty);
            }else{
                analysis=StringUtils.format(I18nUtil.getMessage("cx.engine.auto.analysis.totalQty.title"),totalTaskQty);
            }
            logDetail.append("【构建收尾原因分析】").append("，班次：").append(currentClassShift).append(",生成共多少原因分析").append(analysis).append(division);
            CxScheduleUtils.setClassAnalysis(cxEngineScheduleResult,ClassEnums.getClassEnums(currentClassShift),analysis);
        }
       String title="【机台编号："+maxShiftTask.getCxMachineCode()+"；胎胚代码："+maxShiftTask.getEmbryoCode()+"收尾原因分析】";
       /*autoScheduleLogService.insertCxScheduleLog(cxEngineScheduleResult.getCxBatchNo(), cxEngineScheduleResult.getOrderNo(), title,
         logSplit("月度剩余量：" + monthRemainQty, "收尾原因分析数量设置：" + analysisMarkQty, CxScheduleUtils.getClassAnalysis(cxEngineScheduleResult,ClassEnums.getClassEnums(currentClassShift)))); //添加日志*/
    }

    /**
     * 构建班制顺序
     * @param cxAutoScheduleTaskListMap
     * @param classSortMap
     */
    private void buildClassSort(Map<String, List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap, Map<ClassEnums, Integer> classSortMap) {
        List<CxAutoScheduleTask> allTaskList=new ArrayList<>();
        //遍历将所有任务放一起
        for(Map.Entry<String, List<CxAutoScheduleTask>> entry:cxAutoScheduleTaskListMap.entrySet()){
            allTaskList.addAll(entry.getValue());
        }
        //降序排序
        CxScheduleUtils.sortDescByScheduleTaskClassShift(allTaskList);
        //遍历所有任务班次
        for(CxAutoScheduleTask cxAutoScheduleTask:allTaskList){
            int classShift=cxAutoScheduleTask.getClassShift();//拿到班次
            ClassEnums cls=ClassEnums.getClassEnums(classShift);
            //否则则判断当前班次跟重复班次是否相同
            if(classSortMap.containsKey(cls)){
                int sort=classSortMap.get(cls)+1;
                classSortMap.put(cls,sort);
            }else{
                classSortMap.put(cls,1);
            }
        }
    }

    /**
     * 排序后根据作业顺序最大的续作规格计划量安排
     * @param cxEngineScheduleResult
     */
    private Map<String,List<CxAutoScheduleTask>> singScheduleResultClass2Plan(CxEngineScheduleResult cxEngineScheduleResult,Double avgAvalableLhShift,Double classOneAvgaliableLhShift,StringBuilder logDetail,boolean sameTask) {
        Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap=null;
        StringBuilder sb=new StringBuilder();
        String machineCode=cxEngineScheduleResult.getCxMachineCode();//成型机台
        String embryoCode=cxEngineScheduleResult.getEmbryoCode();
        if(!checkCloseOut(cxEngineScheduleResult) && cxEngineScheduleResult.getClass1Sort()>0){
            sb.append("【一班排序最大规格】预排胎胚：").append(embryoCode).append(",");
            sb.append("【一班排序最大规格】预排开始班次：【").append(ClassEnums.CLASS_ONE.getClassName()).append("】,");
            logDetail.append(sb);
            //开始创建任务
            cxAutoScheduleTaskListMap=singScheduleResultCreateTask(cxEngineScheduleResult,ClassEnums.CLASS_ONE.getClassIndex(),machineCode,avgAvalableLhShift,classOneAvgaliableLhShift,logDetail,sameTask);
            log.debug(sb.toString());
        }else{
            logDetail.append(sb);
            sb.append("【一班排序最大规格】预排胎胚：").append(embryoCode).append(",月度剩余量为0，当前规格不继续排载");
        }
        return  cxAutoScheduleTaskListMap;
    }

    /**
     * 开始创建单规格任务
     * @param cxEngineScheduleResult
     * @param currentClassIndex 开始排班的班次
     * @param machineCode
     * @param avgAvalableLhShift
     */
    private Map<String,List<CxAutoScheduleTask>> singScheduleResultCreateTask(CxEngineScheduleResult cxEngineScheduleResult, Integer currentClassIndex, String machineCode, Double avgAvalableLhShift,Double classOneAvgaliableLhShift,StringBuilder logDetail,boolean sameTask) {
        Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap=new HashMap<>();
        logDetail.append(">>【一班排序最大规格】");
        //获取成型机规格定额数据
        Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(machineCode,cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getBomDataVersion(),logDetail);
        logDetail.append("预排获取定额：").append(machineQuota).append(",");
        logDetail.append("预排计算二班平均可硫化班次数：").append(avgAvalableLhShift).append(",");
        //计算规格可以排的最大硫化班次
        Double maxLhCount=maxLhShiftCount(cxEngineScheduleResult,cxEngineScheduleResult.getSpecDimension()==null?"":cxEngineScheduleResult.getSpecDimension()+"",avgAvalableLhShift,classOneAvgaliableLhShift,logDetail);
        logDetail.append("预排最大安排硫化班次：").append(cxEngineScheduleResult.getMaximumClassQty()).append(",");
        logDetail.append("预排单班硫化量：").append(cxEngineScheduleResult.getSingleShiftLhQty()).append(",");
        //根据最大可硫化班次设置，计算单次安排任务总量
        int taskQty= (int) Math.ceil(maxLhCount * cxEngineScheduleResult.getSingleShiftLhQty());
        logDetail.append("预排总计划量：").append(taskQty).append(",");
        //计划总量根据耗损率重新计算
        taskQty=calcLossRate(machineCode,cxEngineScheduleResult.getEmbryoCode(),taskQty,logDetail);
        //连续计划量计算
        Integer continuePlanQty=calcContinuePlanQty(cxEngineScheduleResult,null,ClassEnums.getClassEnums(currentClassIndex),logDetail,sameTask);
        logDetail.append("预排连续计划量：").append(continuePlanQty).append(",");
        Integer monthRemainQty=getRealMonthRemainQty(cxEngineScheduleResult);
        logDetail.append("规格月度剩余量：").append(monthRemainQty).append(",");
        Integer remainTaskQty=taskQty - continuePlanQty; //剩余任务量
        logDetail.append("规格剩余任务量：").append(remainTaskQty);
        if(remainTaskQty <= 0){
            logDetail.append("预排规格没有剩余任务量，单规格预排任务不进行生成，剩余任务量：").append(taskQty-continuePlanQty).append("\n");
            log.debug(logDetail.toString());
            return cxAutoScheduleTaskListMap;
        }
        if(monthRemainQty>0){
             int onceCloseOut=getOnceCloseOutQty();
             boolean onceProduct=monthRemainQty<=onceCloseOut;
             remainTaskQty=(monthRemainQty>remainTaskQty&&!onceProduct)?remainTaskQty:monthRemainQty;
             logDetail.append("【校验一次投产】").append(",设定一次投产参数").append(onceCloseOut).append("月度剩余量：").append(monthRemainQty).append(",是否可一次投产：").append(monthRemainQty<=onceCloseOut).append(division);
             //单规格任务组装生成规格任务集合
            cxAutoScheduleTaskListMap=scheduleTaskMap(cxEngineScheduleResult,currentClassIndex,taskQty,continuePlanQty,remainTaskQty,machineQuota,onceProduct,0);
        }else{
            logDetail.append("预排规格没有月度剩余量，单规格预排任务不进行生成，月度剩余量：").append(monthRemainQty).append("\n");
            log.debug(logDetail.toString());
            return cxAutoScheduleTaskListMap;
        }
        log.debug(logDetail.toString());
        logDetail.append(division).append("【最大顺序排产】自动排程后集合：").append(toJSONString(cxAutoScheduleTaskListMap));
        return cxAutoScheduleTaskListMap;
    }

    /**
     * 获取成型月度剩余量
     * @param cxEngineScheduleResult
     * @return
     */
    private Integer getRealMonthRemainQty(CxEngineScheduleResult cxEngineScheduleResult) {
       String embryoCode=cxEngineScheduleResult.getEmbryoCode();
       Integer monthRemainQty=cxEngineScheduleResult.calcMonthRemainQty(); //扣除掉一班计划后月度剩余量
       if(monthRemainQtyMap.containsKey(embryoCode)){
           monthRemainQty=monthRemainQtyMap.get(embryoCode);
       }
       log.debug("【获取月度剩余量】sap品号："+cxEngineScheduleResult.getSapCode()+",胎胚代码："+embryoCode+",月度剩余量："+monthRemainQty);
       return monthRemainQty;
    }

    /**
     * 任务集合构建
     * @param cxEngineScheduleResult
     * @param currentClassIndex
     * @param taskQty
     * @param continuePlanQty
     * @param remainTaskQty
     * @param machineQuota
     * @param onceProduct 一次投产
     */
    private Map<String,List<CxAutoScheduleTask>> scheduleTaskMap(CxEngineScheduleResult cxEngineScheduleResult, Integer currentClassIndex, int taskQty, Integer continuePlanQty, Integer remainTaskQty, Integer machineQuota,boolean onceProduct,int beginIndex) {
        Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap=new HashMap<>();
        List<CxAutoScheduleTask> cxAutoScheduleTaskList=new ArrayList<>();
        String specContinueProductShift=cxParamsMap.get(CxParamCodeConstants.SPEC_CONTINUE_PRODUCT_SHIFTS);
        if(currentClassIndex>ClassEnums.CLASS_FIVE.getClassIndex()){
            log.debug("【班次超过最大班次】，当前班次："+currentClassIndex);
            return cxAutoScheduleTaskListMap;
        }
        if(StringUtils.isEmpty(specContinueProductShift)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.change.continue.workshift.param.error"));
        }
        //可连续安排的班数
        Integer continueShift=Integer.valueOf(specContinueProductShift);
        String scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",cxEngineScheduleResult.getScheduleDate());
        String cxOrderNo=cxEngineScheduleResult.getOrderNo();
        //Joran 2022-01-04 是否终止
        boolean isBreak=false;
        while(remainTaskQty > machineQuota && remainTaskQty>0 && (beginIndex< continueShift||onceProduct) && currentClassIndex <= CxEngineConstants.TASK_MAX_CLASS_SHIFT ){
            String key=GenerageMapKeyUtils.createMapKey(cxEngineScheduleResult.getCxMachineCode(),currentClassIndex+"");
            Double classShiftHour=machineShiftHourMap.containsKey(key)? CxEngineConstants.CLASS_SHIFT_HOUR:machineShiftHourMap.get(key);
            CxAutoScheduleTask autoScheduleTask=CxScheduleUtils.createScheduleTask(cxEngineScheduleResult,currentClassIndex,taskQty,continuePlanQty,classShiftHour);
            autoScheduleTask.setScheduleDate(scheduleDateStr);
            //工单号生成
            autoScheduleTask.setCxOrderNo(cxOrderNo);
            if(remainTaskQty-machineQuota>0){
                autoScheduleTask.setCurrentShiftPlanQty(machineQuota); //班次计划量
                remainTaskQty -= machineQuota;//更新剩余任务量
                continuePlanQty+=machineQuota;//任务量累加
                autoScheduleTask.setRemainTaskQty(remainTaskQty);
                autoScheduleTask.setRemainTime(CxEngineConstants.ZERO);//班次没有剩余时间
                autoScheduleTask.setRemainTaskQty(taskQty-continuePlanQty);//任务剩余量
                if(isBreakTask(cxEngineScheduleResult,currentClassIndex,machineQuota)&&!onceProduct){
                    isBreak=true;
                    break;
                }
                updateMachineShiftHourMap(cxEngineScheduleResult.getCxMachineCode(),currentClassIndex,CxEngineConstants.ZERO);
                currentClassIndex++;
            }else{
                autoScheduleTask.setCurrentShiftPlanQty(remainTaskQty); //班次计划量
                continuePlanQty+=remainTaskQty;//任务量累加
                CxScheduleUtils.calcRemainTime(autoScheduleTask,machineQuota,remainTaskQty);
                remainTaskQty = 0;//更新剩余任务量
                autoScheduleTask.setRemainTaskQty(remainTaskQty);
                if(isBreakTask(cxEngineScheduleResult,currentClassIndex,remainTaskQty)&&!onceProduct){
                    isBreak=true;
                    break;
                }
                updateMachineShiftHourMap(cxEngineScheduleResult.getCxMachineCode(),currentClassIndex,autoScheduleTask.getRemainTime());
            }
            beginIndex+=1;
            cxAutoScheduleTaskList.add(autoScheduleTask);
        }

        String key=GenerageMapKeyUtils.createMapKey(cxEngineScheduleResult.getCxMachineCode(),currentClassIndex+"");
        Double classShiftHour=machineShiftHourMap.containsKey(key)? CxEngineConstants.CLASS_SHIFT_HOUR:machineShiftHourMap.get(key);
        if(remainTaskQty>0&&remainTaskQty <= machineQuota && (beginIndex< continueShift||onceProduct) && currentClassIndex <= CxEngineConstants.TASK_MAX_CLASS_SHIFT&&!isBreak){ //剩余任务量小于定额 且不超5个班次
            CxAutoScheduleTask autoScheduleTask=CxScheduleUtils.createScheduleTask(cxEngineScheduleResult,currentClassIndex,taskQty,continuePlanQty,classShiftHour);
            autoScheduleTask.setScheduleDate(scheduleDateStr);
            //工单号生成
            autoScheduleTask.setCxOrderNo(cxOrderNo);
            autoScheduleTask.setCurrentShiftPlanQty(remainTaskQty); //班次计划量
            CxScheduleUtils.calcRemainTime(autoScheduleTask,machineQuota,remainTaskQty);
            remainTaskQty = 0;//更新剩余任务量
            autoScheduleTask.setRemainTaskQty(remainTaskQty);
            if(!isBreakTask(cxEngineScheduleResult,currentClassIndex,remainTaskQty)){
                cxAutoScheduleTaskList.add(autoScheduleTask);
                updateMachineShiftHourMap(cxEngineScheduleResult.getCxMachineCode(),currentClassIndex,autoScheduleTask.getRemainTime());
            }

        }
        if(StringUtils.isNotEmpty(cxAutoScheduleTaskList)){
            cxAutoScheduleTaskListMap.put(cxEngineScheduleResult.getEmbryoCode(),cxAutoScheduleTaskList);
        }
        return  cxAutoScheduleTaskListMap;
    }

    /**
     * 用于处理中止任务排完后导致硫化时长爆掉
     * @param cxEngineScheduleResult
     * @param classIndex
     * @param planQty
     * @return
     */
    public boolean isBreakTask(CxEngineScheduleResult cxEngineScheduleResult,int classIndex,int planQty){
        ClassEnums cls =ClassEnums.getClassEnums(classIndex);
        if(cls==null){
            cls=ClassEnums.CLASS_FIVE;
        }else{
            //设置班次计划量
            CxScheduleUtils.setClassShiftPlanQty(cxEngineScheduleResult,cls,planQty);
        }
        //复制规格对象
        CxEngineScheduleResult scheduleResult= BeanConverUtil.conver(cxEngineScheduleResult,CxEngineScheduleResult.class);
        int nextClsIndex=classIndex+1;
        ClassEnums nextCls =ClassEnums.getClassEnums(nextClsIndex);
        if(nextCls==null){
            nextCls=ClassEnums.CLASS_FIVE;
        }
        boolean isBreak=isOutOfClassMax(scheduleResult,nextCls);
        if(isBreak){
            //设置班次计划量
            CxScheduleUtils.setClassShiftPlanQty(cxEngineScheduleResult,cls,0);
        }
        return isBreak;
    }

    /**
     * 判断班次是否超出最大可硫化班次
     * @param cxEngineScheduleResult
     * @param cls
     * @return
     */
    public boolean isOutOfClassMax(CxEngineScheduleResult cxEngineScheduleResult,ClassEnums cls){
        boolean isOutOf=false;
        //获取最大可硫化班次
        Double maxClassShifts=getMaxLhClassShifts();
        if(cls==null){
            cls=ClassEnums.CLASS_FIVE;
        }
        CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);//计算可硫化班次数
        Double classLhClassShift= CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(cxEngineScheduleResult,cls.getClassIndex());
        if(classLhClassShift>=maxClassShifts){
            isOutOf=true;
        }
        return isOutOf;
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
     * 单机台单规格连续计划量计算
     * 规则：从当前班次开始 前一班开始追加直到遇到0或者没有计划
     * @param cxEngineScheduleResult 下个任务对象
     * @param preLastOneTask 前任务最大班次对象
     * @param cls 当前班次
     * @return
     */
    private int calcContinuePlanQty(CxEngineScheduleResult cxEngineScheduleResult,CxAutoScheduleTask preLastOneTask,ClassEnums cls,StringBuilder logDetail,boolean sameTask) {
        StringBuilder sb=new StringBuilder("【计算连续生产量】规格任务量追加，胎胚："+cxEngineScheduleResult.getEmbryoCode()+",机台："+cxEngineScheduleResult.getCxMachineCode()).append("\n");
        Integer continuePlanQty=0;
        //续排同规格则任务量重新
        if(sameTask){
            return 0;
        }
        boolean isContinue=true;
        switch (cls){
            case CLASS_FIVE:
                if(cxEngineScheduleResult.getClass4PlanQty()>0){
                    sb.append("追加次日一班计划量：").append(cxEngineScheduleResult.getClass4PlanQty()).append("\n");
                    continuePlanQty+=cxEngineScheduleResult.getClass4PlanQty();//追加一班任务量
                }else{
                    isContinue=false;
                }
                if(cxEngineScheduleResult.getClass3PlanQty()>0&&isContinue){
                    sb.append("追加三班计划量：").append(cxEngineScheduleResult.getClass3PlanQty()).append("\n");
                    continuePlanQty+=cxEngineScheduleResult.getClass3PlanQty();//追加一班任务量
                }else{
                    isContinue=false;
                }
                if(cxEngineScheduleResult.getClass2PlanQty()>0&&isContinue){
                    sb.append("追加二班计划量：").append(cxEngineScheduleResult.getClass2PlanQty()).append("\n");
                    continuePlanQty+=cxEngineScheduleResult.getClass2PlanQty();//追加一班任务量
                }else{
                    isContinue=false;
                }
                break;
            case CLASS_FOUR:
                if(cxEngineScheduleResult.getClass3PlanQty()>0){
                    sb.append("追加三班计划量：").append(cxEngineScheduleResult.getClass3PlanQty()).append("\n");
                    continuePlanQty+=cxEngineScheduleResult.getClass3PlanQty();//追加一班任务量
                }else{
                    isContinue=false;
                }
                if(cxEngineScheduleResult.getClass2PlanQty()>0&&isContinue){
                    sb.append("追加二班计划量：").append(cxEngineScheduleResult.getClass2PlanQty()).append("\n");
                    continuePlanQty+=cxEngineScheduleResult.getClass2PlanQty();//追加一班任务量
                }else{
                    isContinue=false;
                }
                break;
            case CLASS_THREE:
                if(cxEngineScheduleResult.getClass2PlanQty()>0){
                    sb.append("追加二班计划量：").append(cxEngineScheduleResult.getClass2PlanQty()).append("\n");
                    continuePlanQty+=cxEngineScheduleResult.getClass2PlanQty();//追加一班任务量
                }else{
                    isContinue=false;
                }
                break;
            case CLASS_TWO:
                if(cxEngineScheduleResult.getClass1PlanQty()>0){
                    sb.append("追加一班计划量：").append(cxEngineScheduleResult.getClass1PlanQty()).append("\n");
                    continuePlanQty+=cxEngineScheduleResult.getClass1PlanQty();//追加一班任务量
                }else{
                    isContinue=false;
                }
                break;
            case CLASS_ONE:
                if(cxEngineScheduleResult.getLastClass3PlanQty()>0){
                    sb.append("追加前一天三班计划量：").append(cxEngineScheduleResult.getLastClass3PlanQty()).append("\n");
                    continuePlanQty+=cxEngineScheduleResult.getLastClass3PlanQty();//追加一班任务量
                }else{
                    isContinue=false;
                }
                break;
            default:break;
        }
        if(cxEngineScheduleResult.getLastClass2PlanQty()>0&&isContinue){
            sb.append("追加前一天二班计划量：").append(cxEngineScheduleResult.getLastClass2PlanQty()).append("\n");
            continuePlanQty+=cxEngineScheduleResult.getLastClass2PlanQty();//追加前一天二班任务量
        }else{
            isContinue=false;
        }

        if(cxEngineScheduleResult.getLastClass1PlanQty()>0&&isContinue){
            sb.append("追加前一天一班计划量：").append(cxEngineScheduleResult.getLastClass1PlanQty()).append("\n");
            continuePlanQty+=cxEngineScheduleResult.getLastClass1PlanQty();//追加前一天一班任务量
        }else{
            isContinue=false;
        }
        Date scheduleDate=cxEngineScheduleResult.getScheduleDate();//排程日期
        Date lastDate=DateUtils.addDays(scheduleDate,-1);//前一天排程日期，已经有冗余了，所以只需再往前追
        //遍历往前查排程进行任务追加
        while(isContinue){
            lastDate=DateUtils.addDays(lastDate,-1);//前一天排程日期，已经有冗余了，所以只需再往前追
            lastDate=CxScheduleUtils.formatDateByZero(lastDate);
            String dateStr=DateUtils.parseDateToStr("yyyy-MM-dd",lastDate);
            sb.append("遍历追加计划量，追加排程日期").append(dateStr).append("\n");
            CxEngineScheduleResult lastCxEngineScheduleResult=null;
            CxEngineScheduleResult condition=new CxEngineScheduleResult();
            condition.setEmbryoCode(cxEngineScheduleResult.getEmbryoCode());
            condition.setSapCode(cxEngineScheduleResult.getSapCode());
            condition.setCxMachineCode(cxEngineScheduleResult.getCxMachineCode());
            condition.setScheduleDate(lastDate);
            List<CxEngineScheduleResult> lastDayResultList=this.cxScheduleEngineMapper.selectCxScheduleResultList(condition);
            if(StringUtils.isEmpty(lastDayResultList)){
                isContinue=false;
                continue;
            }
            lastCxEngineScheduleResult= lastDayResultList.get(0);
            if(lastCxEngineScheduleResult.getClass3PlanQty()!=null&& lastCxEngineScheduleResult.getClass3PlanQty()>0){
                sb.append("追加"+dateStr+"三班计划量：").append(lastCxEngineScheduleResult.getClass3PlanQty()).append("\n");
                continuePlanQty+=lastCxEngineScheduleResult.getClass1PlanQty();//追加前一天三班任务量
            }else{
                isContinue=false;
            }
            if(lastCxEngineScheduleResult.getClass2PlanQty()!=null&& lastCxEngineScheduleResult.getClass2PlanQty()>0&&isContinue){
                sb.append("追加"+dateStr+"二班计划量：").append(lastCxEngineScheduleResult.getClass2PlanQty()).append("\n");
                continuePlanQty+=lastCxEngineScheduleResult.getClass2PlanQty();//追加前一天二班任务量
            }else{
                isContinue=false;
            }

            if(lastCxEngineScheduleResult.getClass1PlanQty()!=null&& lastCxEngineScheduleResult.getClass1PlanQty()>0&&isContinue){
                sb.append("追加"+dateStr+"一班计划量：").append(lastCxEngineScheduleResult.getClass1PlanQty()).append("\n");
                continuePlanQty+=lastCxEngineScheduleResult.getClass1PlanQty();//追加前一天一班任务量
            }else{
                isContinue=false;
            }

        }
        log.debug(sb.toString());
        logDetail.append(sb).append(division);
        return continuePlanQty;
    }



    /**
     * 根据成型机台编号获取对应的最大排产班次
     * @param cxEngineScheduleResult 排程任务对象
     * @param specDimension 外胎规格寸口
     * @param avgAvailableLhShift 平均可硫化班次
     * @return
     */
    public Double maxLhShiftCount(CxEngineScheduleResult cxEngineScheduleResult,String specDimension,Double avgAvailableLhShift,Double classOneAvgailableLhShift,StringBuilder logDetail){
        String key=cxEngineScheduleResult.getCxMachineCode();
        Double maxLhShift=null;
        if(cxEngineScheduleResult.getMaximumClassQty()!=null){
            //获取昨天的最大硫化班数
            maxLhShift= Double.valueOf(cxEngineScheduleResult.getMaximumClassQty());
            if(cxEngineScheduleResult.getMarkMaxLhShiftLock()){
                return maxLhShift;
            }
        }
        //Joran 2021-12-16机台设置投产班次，如果有设置直接优先取机台投产班次start
       /* Map<String,Double> machineProductShiftMap=cxEngineGroupMachineListService.getCxMachineProudctShift();
        if(StringUtils.isNotEmpty(machineProductShiftMap)&&machineProductShiftMap.containsKey(key)){
            Double machineProductShifts=machineProductShiftMap.get(key);
            logDetail.append("【获取机台设定可投产班次】，成型机台编号："+key+"寸口："+specDimension+",可投产班次数："+machineProductShifts);
            //自动排程设置最大班数
            cxEngineScheduleResult.setMaximumClassQty(machineProductShifts);
            return  machineProductShifts;
        }*/
        //Joran 2021-12-16机台设置投产班次，如果有设置直接优先取机台投产班次end

        //没有设置则取默认工序参数可硫化最大班次
        if(maxLhShift==null){
            String  defaultLhClassShifts=cxParamsMap.get(CxParamCodeConstants.DEFAULT_LH_CLASS_SHIFTS);
            if(scheduleLimitMap.containsKey(key)){
                List<CxEngineScheduleLimit> scheduleLimitList=scheduleLimitMap.get(key);
                if(StringUtils.isNotEmpty(scheduleLimitList)){
                    //遍历判断当前平均可硫化班次在什么范围区间，匹配到的直接取
                    for(CxEngineScheduleLimit cxEngineScheduleLimit:scheduleLimitList){
                        Double tireAvgLhStockMinimun=cxEngineScheduleLimit.getTireAvgLhStockMinimun();
                        Double tireAveLhStockMaximun=cxEngineScheduleLimit.getTireAveLhStockMaximun();
                        Double specDimensionDouble=cxEngineScheduleLimit.getSpecDimension();
                        Double specDimensionInput=null;
                        if(StringUtils.isNotEmpty(specDimension)){
                            specDimensionInput=Double.valueOf(specDimension);
                        }
                        if(specDimensionDouble.equals(specDimensionInput)&&avgAvailableLhShift>=tireAvgLhStockMinimun&&avgAvailableLhShift<=tireAveLhStockMaximun){
                            log.debug("【获取最大可硫化班次设置参数】匹配设置获取到最大班次,成型机台编号："+key+"寸口："+specDimension+",最大可硫化班次："+cxEngineScheduleLimit.getMaxLhClass());
                            logDetail.append("【获取最大可硫化班次设置参数】匹配设置获取到最大班次,成型机台编号："+key+"寸口："+specDimension+",最大可硫化班次："+cxEngineScheduleLimit.getMaxLhClass()).append(division);
                            //cxEngineScheduleResult.setMaximumClassQty(cxEngineScheduleLimit.getMaxLhClass());
                            defaultLhClassShifts = String.valueOf(cxEngineScheduleLimit.getMaxLhClass());
                        }
                    }
                }
            }
            log.debug("【获取最大可硫化班次设置参数】未匹配到设置，取默认值,成型机台编号："+key+"寸口："+specDimension+",最大可硫化班次："+defaultLhClassShifts);
            logDetail.append("【获取最大可硫化班次设置参数】未匹配到设置，取默认值,成型机台编号："+key+"寸口："+specDimension+",最大可硫化班次："+defaultLhClassShifts).append(division);
            //自动排程设置最大班数
            if(StringUtils.isEmpty(defaultLhClassShifts)){
                logDetail.append("【获取最大可硫化班次设置参数】未匹配到设置").append(division);
                throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.param.exception.error"));
            }
            maxLhShift=Double.valueOf(defaultLhClassShifts);
        }

        //Joran 2022-01-08 进行3轮参数比对验证最终获取到的可硫化班次start
        //1.第一轮轮胎类型库存获取调整量
        maxLhShift=adjustLhShiftByEmbryoType(cxEngineScheduleResult,maxLhShift,logDetail);
       //2.第二轮根据寸口一班可硫化班次进行调整量
        maxLhShift=adjustLhShiftByDimensionShift(cxEngineScheduleResult,maxLhShift,logDetail);
       //3.第三轮进行同机台一班可硫化班次进行调整量
        maxLhShift=adjustLhShiftByMachineShift(classOneAvgailableLhShift,maxLhShift,logDetail);
        //Joran 2022-01-08 进行3轮参数比对验证最终获取到的可硫化班次end
        if(maxLhShift<=BigDecimal.ZERO.doubleValue()){ //当计算出来的班次小于0时默认给1班
            maxLhShift=BigDecimal.ONE.doubleValue();
        }else if(maxLhShift>=getMaxLhClassShifts()){ //当超过设置的最大班次时默认给最大班次
            maxLhShift=getMaxLhClassShifts();
        }
        cxEngineScheduleResult.setMaximumClassQty(maxLhShift);
        //标记计算后当天档次锁定不再重复计算
        cxEngineScheduleResult.setMarkMaxLhShiftLock(true);
        return Double.valueOf(maxLhShift);
    }

    /**
     * 第一轮根据轮胎类型进行班数调整
     * @param cxEngineScheduleResult
     * @param maxLhShift
     * @return
     */
    private Double adjustLhShiftByEmbryoType(CxEngineScheduleResult cxEngineScheduleResult, Double maxLhShift,StringBuilder logDetail) {
        logDetail.append("【根据第一轮轮胎类型进行投产班数调整】:").append(division);
        String prefix="";
        String embryoCode=cxEngineScheduleResult.getEmbryoCode();
        //一次法
        String oncePrefix= commonCacheService.getEmbryoCodePrefix(cxParamsMap, CxParamCodeConstants.ONCE_EMBRYOCODE_PREFIX);
        //二次法
        String twicePrefix= commonCacheService.getEmbryoCodePrefix(cxParamsMap, CxParamCodeConstants.TWICE_EMBRYOCODE_PREFIX);
        List<CxEngineProductStockLimit> productStockLimitList=new ArrayList<>();
        if(StringUtils.startsWithIgnoreCase(embryoCode,oncePrefix)){
            logDetail.append("当前轮胎类型为【一次法】").append(division);
            prefix=oncePrefix;
            if(StringUtils.isNotEmpty(cxEngineProductStockLimitListMap)&&cxEngineProductStockLimitListMap.containsKey(CxEngineConstants.MACHINE_TYPE_ONCE)){
                productStockLimitList=cxEngineProductStockLimitListMap.get(CxEngineConstants.MACHINE_TYPE_ONCE);
                logDetail.append("获取到的一次法库存限制设置").append(toJSONString(productStockLimitList)).append(division);
            }

        }else if(StringUtils.startsWithIgnoreCase(embryoCode,twicePrefix)){
            logDetail.append("当前轮胎类型为【一次法】").append(division);
            prefix=twicePrefix;
            if(StringUtils.isNotEmpty(cxEngineProductStockLimitListMap)&&cxEngineProductStockLimitListMap.containsKey(CxEngineConstants.MACHINE_TYPE_TWICE)){
                productStockLimitList=cxEngineProductStockLimitListMap.get(CxEngineConstants.MACHINE_TYPE_TWICE);
                logDetail.append("获取到的二次法库存限制设置").append(toJSONString(productStockLimitList)).append(division);
            }
        }
        logDetail.append("获取轮胎类型对应的库存汇总信息").append(toJSONString(embryoCodeTypeTotalMap)).append(division);
        Double adjustShift=cxEngineProductShiftLimitService.adjustLhShiftCountByStock(prefix,embryoCodeTypeTotalMap,cxParamsMap,productStockLimitList,logDetail);
        if(adjustShift!=null&&adjustShift!=BigDecimal.ZERO.doubleValue()){
            maxLhShift+=adjustShift;
        }
        return  maxLhShift;
    }

    /**
     * 第二轮进行寸口一班平均可硫化班次获取调整量
     * @param cxEngineScheduleResult
     * @param maxLhShift
     * @return
     */
    private Double adjustLhShiftByDimensionShift(CxEngineScheduleResult cxEngineScheduleResult, Double maxLhShift,StringBuilder logDetail) {
        logDetail.append("【根据第二轮同寸口平均可硫化班次进行投产班数调整】:").append(division);
        String embryoCode=cxEngineScheduleResult.getEmbryoCode();
        //一次法
        String oncePrefix= commonCacheService.getEmbryoCodePrefix(cxParamsMap, CxParamCodeConstants.ONCE_EMBRYOCODE_PREFIX);
        //二次法
        String twicePrefix= commonCacheService.getEmbryoCodePrefix(cxParamsMap, CxParamCodeConstants.TWICE_EMBRYOCODE_PREFIX);
        //获取到的寸口
        Double specDimension=cxEngineScheduleResult.getSpecDimension();
        String key="";
        if(StringUtils.startsWithIgnoreCase(embryoCode,oncePrefix)){
            key=GenerageMapKeyUtils.createMapKey(oncePrefix,specDimension+"");
        }else{
            key=GenerageMapKeyUtils.createMapKey(twicePrefix,specDimension+"");
        }


        logDetail.append("当前寸口=").append(specDimension).append(division);
        Double dimensionAvgLhShift=BigDecimal.ZERO.doubleValue();
        List<CxEngineProductDimensionLimit> dimensionLimitList=null;
        if(sameDimensionAvailableClassOneShiftMap.containsKey(key)){
            dimensionAvgLhShift=sameDimensionAvailableClassOneShiftMap.get(key);
            logDetail.append("寸口一班平均可硫化班次=").append(dimensionAvgLhShift).append(division);
        }
        //同寸口设定列表
        if(cxEngineProductDimensionLimitListMap.containsKey(specDimension)){
           dimensionLimitList=cxEngineProductDimensionLimitListMap.get(specDimension);
            logDetail.append("同寸口平均值设定信息：").append(toJSONString(dimensionLimitList)).append(division);
        }
        Double adjustShift=cxEngineProductShiftLimitService.adjustLhShiftCountByDimension(specDimension,dimensionAvgLhShift,dimensionLimitList,logDetail);
        if(adjustShift!=null&&adjustShift!=BigDecimal.ZERO.doubleValue()){
            maxLhShift+=adjustShift;
        }
        return  maxLhShift;
    }

    /**
     * 第三轮进行寸口一班平均可硫化班次获取调整量
     * @param avgAvailableLhShift 机台一班平均可硫化班次
     * @param maxLhShift
     * @return
     */
    private Double adjustLhShiftByMachineShift(Double avgAvailableLhShift, Double maxLhShift,StringBuilder logDetail) {
        Double adjustShift=cxEngineProductShiftLimitService.adjustLhShiftCountByMachine(avgAvailableLhShift,cxEngineProductMachineLimitList,logDetail);
        if(adjustShift!=null&&adjustShift!=BigDecimal.ZERO.doubleValue()){
            maxLhShift+=adjustShift;
        }
        return  maxLhShift;
    }



    /**
     * 判断当前任务是否收尾(月度剩余量扣掉一班的计划量)
     * @param cxEngineScheduleResult
     * @return
     */
    private boolean checkCloseOut(CxEngineScheduleResult cxEngineScheduleResult){
        String embryoCode=cxEngineScheduleResult.getEmbryoCode();
        Integer monthRemainQty=cxEngineScheduleResult.getMonthRemainQty();
        if(monthRemainQtyMap.containsKey(embryoCode)){
            monthRemainQty=monthRemainQtyMap.get(embryoCode);
        }else{
            Integer lastClass3PlanQty=cxEngineScheduleResult.getLastClass3PlanQty();
            monthRemainQty-=lastClass3PlanQty;
            if(monthRemainQty<=0){
                monthRemainQty=0;
            }
            monthRemainQtyMap.put(embryoCode,monthRemainQty);
        }
        boolean isCloseOut=monthRemainQty<=0;
        if(isCloseOut){
            log.debug("【验证收尾】胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",月度剩余量："+cxEngineScheduleResult.getMonthRemainQty()+",表示收尾，不进行二班任务排产");
        }
       return isCloseOut;
    }

    /**
     * 给机台任务列表的一班任务进行任务顺序设置
     * @param lastDayScheduleResultList
     */
    private void class1ShiftTaskSort(List<CxEngineScheduleResult> lastDayScheduleResultList,List<CxEngineScheduleResult> monthRemainQtyList,StringBuilder logDetail) {

        //遍历所有任务根据规则进行生产任务顺序设置
        logDetail.append("【成型机台："+lastDayScheduleResultList.get(0).getCxMachineCode()+"一班任务排序】").append(division);
        Map<String,CxEngineScheduleResult> class1PlanMap=new HashMap<>();
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);//计算可硫化班次数
            //存在月度剩余量的排程结果单独出来
            if(getRealMonthRemainQty(cxEngineScheduleResult)>0){
                monthRemainQtyList.add(cxEngineScheduleResult);
            }else{
                log.debug("【一班任务排序】胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",不进行排序，原因：月度剩余量为0");
                logDetail.append("【一班任务排序】胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",不进行排序，原因：月度剩余量为0").append(division);
                if(cxEngineScheduleResult.getClass3PlannedQty()>0){//接白班收尾原因分析
                    String analysis=StringUtils.format(I18nUtil.getMessage("cx.engine.auto.analysis.continue.shift.title"),cxEngineScheduleResult.getClass3PlannedQty());
                    cxEngineScheduleResult.setClass1Analysis(analysis);
                }
                continue;
            }
            //获取所有中班(一班)有计划量的规格
            if(cxEngineScheduleResult.getClass1PlanQty()>0){
                class1PlanMap.put(cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult);
            }else{
                cxEngineScheduleResult.setClass1Sort(0);//默认为0
            }
        }
        logDetail.append("任务排序前列表：").append(toJSONString(lastDayScheduleResultList)).append(division);
        if(StringUtils.isEmpty(class1PlanMap)){
            //throw new CxScheduleEngineException("在任务列表中未找到相应的一班计划量信息!");
            log.error("【一班任务排序】中班（一班）任务排序规则，任务列表中没有存在一班计划量的任务数据,无法排序。返回");
            logDetail.append("【一班任务排序】中班（一班）任务排序规则，任务列表中没有存在一班计划量的任务数据,无法排序。返回").append(division);
            return;
        }

        //如果前一天已经排完序了就不需要重新进行排序
        boolean class1SortNeed=validateClass1SortNeed(class1PlanMap);
        if(class1SortNeed){
            //胎胚和任务顺序
            Map<String,Integer> sortMinMap=new HashMap<>();
            Map<String,Integer> sortMaxMap=new HashMap<>();
            Map<String,Integer> otherSortMap=new HashMap<>();
            Integer min=1;
            Integer max=class1PlanMap.size();
            //1.其中一个 有前任务量 则顺序为小
            for (Map.Entry<String, CxEngineScheduleResult> entry : class1PlanMap.entrySet()) {
                String embryoCode = entry.getKey();
                CxEngineScheduleResult cxEngineScheduleResult = entry.getValue();
                if (cxEngineScheduleResult.getLastClass3PlanQty() > 0) {
                    sortMinMap.put(embryoCode, min);
                    continue;
                }
            }
            //2.其中一个有后任务量 则顺序为大
            for (Map.Entry<String, CxEngineScheduleResult> entry : class1PlanMap.entrySet()) {
                String embryoCode = entry.getKey();
                CxEngineScheduleResult cxEngineScheduleResult = entry.getValue();
                if (cxEngineScheduleResult.getLastClass5PlanQty() > 0) {
                    sortMaxMap.put(embryoCode, max);
                    continue;
                }
            }
            //3.两个都没有前/后任务，则对比任务余量 任务余量小的先做
            CxEngineScheduleResult lastCxEngineScheduleResult=null;
            for (Map.Entry<String, CxEngineScheduleResult> entry : class1PlanMap.entrySet()) {
                String embryoCode = entry.getKey();
                CxEngineScheduleResult cxEngineScheduleResult=entry.getValue();
                if (sortMinMap.containsKey(embryoCode)||sortMaxMap.containsKey(embryoCode)) {
                    min=sortMinMap.containsKey(embryoCode)?1:0;
                    continue;
                }
                if(lastCxEngineScheduleResult==null){
                    lastCxEngineScheduleResult=cxEngineScheduleResult;
                    min+=1;
                    otherSortMap.put(embryoCode,min);
                }else if(lastCxEngineScheduleResult.getMonthRemainQty()<cxEngineScheduleResult.getMonthRemainQty()) {
                    min+=1;
                    otherSortMap.put(embryoCode,min);
                }else{
                    int lastSort=otherSortMap.get(lastCxEngineScheduleResult.getEmbryoCode());
                    min+=1;
                    otherSortMap.put(lastCxEngineScheduleResult.getEmbryoCode(),min);
                    otherSortMap.put(embryoCode,lastSort);
                }
            }

            for (CxEngineScheduleResult cxEngineScheduleResult : lastDayScheduleResultList) {
                String embryoCode = cxEngineScheduleResult.getEmbryoCode();
                if (sortMinMap.containsKey(embryoCode)) {
                    cxEngineScheduleResult.setClass1Sort(sortMinMap.get(embryoCode)); //设置顺序
                }
                if (sortMaxMap.containsKey(embryoCode)) {
                    cxEngineScheduleResult.setClass1Sort(sortMaxMap.get(embryoCode)); //设置顺序
                }
                if (otherSortMap.containsKey(embryoCode)) {
                    cxEngineScheduleResult.setClass1Sort(otherSortMap.get(embryoCode)); //设置顺序
                }
            }
            logDetail.append("任务排序后列表：").append(toJSONString(lastDayScheduleResultList)).append(division);
        }
    }

    /**
     * 验证是否需要重新进行排序
     * @param class1PlanMap
     * @return
     */
    private boolean validateClass1SortNeed(Map<String, CxEngineScheduleResult> class1PlanMap) {
        boolean needSort=false;
        for (Map.Entry<String, CxEngineScheduleResult> entry : class1PlanMap.entrySet()) {
            CxEngineScheduleResult cxEngineScheduleResult=entry.getValue();
            if(cxEngineScheduleResult.getClass1Sort()==null||cxEngineScheduleResult.getClass1Sort()<1){
                needSort=true;
                break;
            }
        }
        log.debug("【验证一班需要排序】："+needSort);
        return needSort;
    }
    /**
     * 月度量小于一次性收尾的数量时 需要连续进行安排投产
     * @return
     */
    public Integer getOnceCloseOutQty(){
        String onceCloseOutQtyParam=cxParamsMap.get(CxParamCodeConstants.ONCE_CLOSE_OUT_QTY);
        if(StringUtils.isEmpty(onceCloseOutQtyParam)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.once.product.param.error"));
        }
        return  Integer.valueOf(onceCloseOutQtyParam);
    }
    /**
     * 获取原因分析标注剩余X收尾参数
     * @return
     */
    public Integer getAnalysisMarkQty(){
        String analysisMarkQtyParam=cxParamsMap.get(CxParamCodeConstants.ANALYSIS_MARK_QTY);
        if(StringUtils.isEmpty(analysisMarkQtyParam)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.analysis.mark.param.error"));
        }
        return  Integer.valueOf(analysisMarkQtyParam);
    }
    /**
     * 获取最大可硫化班次参数
     * @return
     */
    public Double getMaxLhClassShifts(){
        String maxLhClassShiftsParam=cxParamsMap.get(CxParamCodeConstants.MAX_LH_CLASS_SHIFTS);
        if(StringUtils.isEmpty(maxLhClassShiftsParam)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.max.lh.class.param.error"));
        }
        return  Double.valueOf(maxLhClassShiftsParam);
    }


    /**
     * 排程限制
     * 成型工序
     * 成型机台定额
     * 信息初始化
     */
    private void initScheduleData(List<CxPlanProductStatus> cxPlanProductStatusList,List<MdmMonthProdPlan> mdmMonthProdPlanList,StringBuilder logDetail) {
        logDetail.append("==================自动排程数据初始化过程开始==================").append(division);
        scheduleLimitMap=cxEngineScheduleLimitService.getCxScheduleLimitMachineCodeMap();
        logDetail.append("初始化获取排产限制数据集：").append(toJSONString(scheduleLimitMap)).append(division);
        cxParamsMap=commonCacheService.loadCxParamsMap();
        logDetail.append("初始化获取成型工序参数数据集：").append(toJSONString(cxParamsMap)).append(division);
        //规格限制作业机台
        specifyMachineYesMap=cxEngineSpecifyMachineService.getAllCxSpecifyMachineInfo(CxEngineConstants.SPECIFY_JOB_TYPE_YES);
        logDetail.append("初始化获取规格限制作业机台数据集：").append(toJSONString(specifyMachineYesMap)).append(division);
        //规格不可作业机台
        specifyMachineNoMap=cxEngineSpecifyMachineService.getAllCxSpecifyMachineInfo(CxEngineConstants.SPECIFY_JOB_TYPE_NO);
        logDetail.append("初始化获取规格不可作业机台数据集：").append(toJSONString(specifyMachineNoMap)).append(division);
        //加载全部胎胚的施工信息
        engineConstructionInfoMap=commonCacheService.loadEngineConstructionMapFromRedis();

        //加载耗损率相关数据
        cxMachineLossRateMap=cxEngineLossSettingService.loadCxMachineLossRateMap();
        //待投产规格列表
        this.cxPlanProductStatusList=cxPlanProductStatusList;
        //月度计划汇总明细数据
        if(StringUtils.isNotEmpty(mdmMonthProdPlanList)){
            initSapEmbryoCodeMap(mdmMonthProdPlanList,logDetail);
        }
        monthRemainQtyMap=new ConcurrentHashMap<>();//重新初始化
        //Joran 2022-01-08 初始化投产班次调整设定相关数据start
        //1.轮胎类型库存限制设定
        List<CxEngineProductStockLimit> productStockLimitList=cxEngineProductShiftLimitService.selectCxProductShiftStockLimitList(new CxEngineProductStockLimit());
        if(StringUtils.isNotEmpty(productStockLimitList)){//按轮胎类型来进行区分
            cxEngineProductStockLimitListMap=productStockLimitList.stream().collect(Collectors.groupingBy(CxEngineProductStockLimit::getType));
        }else{
            cxEngineProductStockLimitListMap =new HashMap<>();
        }
        //2.同寸口一班平均可硫化班次限制设定
        List<CxEngineProductDimensionLimit> cxEngineProductDimensionLimitList=cxEngineProductShiftLimitService.selectCxEngineProductDimensionLimitList(new CxEngineProductDimensionLimit());
        if(StringUtils.isNotEmpty(cxEngineProductDimensionLimitList)){//按轮胎类型来进行区分
            cxEngineProductDimensionLimitListMap=cxEngineProductDimensionLimitList.stream().collect(Collectors.groupingBy(CxEngineProductDimensionLimit::getSpecDimension));
        }else{
            cxEngineProductDimensionLimitListMap=new HashMap<>();
        }
        //3、同机台平均可硫化班数班次限制设定
        cxEngineProductMachineLimitList=cxEngineProductShiftLimitService.selectCxEngineProductMachineLimitList(new CxEngineProductMachineLimit());
        //Joran 2022-01-08 初始化投产班次调整设定相关数据end

        //Joran 2022-01-18初始化话规格投产模数信息start
        sapSpecMoldUseList=cxEngineSapSpecMoldUseService.selectSapSpecMoldUseList(new CxEngineSapSpecMoldUse());
        //Joran 2022-01-18
        logDetail.append("==================自动排程数据初始化过程结束====================").append(division);
    }

    /**
     * 初始化月度计划汇总根据sap+胎胚代码区分
     * @param mdmMonthProdPlanList
     */
    private void initSapEmbryoCodeMap(List<MdmMonthProdPlan> mdmMonthProdPlanList,StringBuilder logDetail) {
        sapEmbryoCodeProdPlanMap=new ConcurrentHashMap<>();
        //根据组合键进行分组
        sapEmbryoCodeProdPlanMap = mdmMonthProdPlanList.stream().collect(
                Collectors.groupingBy(
                                        mdmMonthProdPlan -> (GenerageMapKeyUtils.createMapKey(
                                        mdmMonthProdPlan.getMaterialCode(),
                                        mdmMonthProdPlan.getEmbryoCode(),
                                        mdmMonthProdPlan.getBomDataVersion())
                                      )
                                    )
        );
        logDetail.append("初始化月度计划汇总分组后集合").append(toJSONString(sapEmbryoCodeProdPlanMap)).append(division);
    }

    /**
     * 根据机台类型确定胎胚代码前缀
     * @param machineType
     * @return
     */
    private String getEmbryoCodePrefixByMachineType(String machineType){
        return commonCacheService.getEmbryoCodePrefix(machineType,cxParamsMap);
    }

    /**
     * 根据机台当天自动安排的任务进行任务重排
     * @param cxMachineCode 重排机台编号
     * @param machineAutoPlanList 机台当天自动排程列表
     * @param lastTaskMap 昨日任务
     * @param cxPlanProductStatusList 待投产列表
     * @param mdmMonthProdPlanList 月度计划列表
     */
  /*  public  void reScheduleByMachine(String cxMachineCode,List<CxEngineScheduleResult> machineAutoPlanList,Map<String,CxEngineScheduleResult> lastTaskMap, List<CxPlanProductStatus> cxPlanProductStatusList,List<MdmMonthProdPlan> mdmMonthProdPlanList)throws CxScheduleEngineException{
        StringBuilder logDetail=new StringBuilder("单机台重排计算日志记录：").append(division);
        //数据初始化
        initScheduleData(cxPlanProductStatusList,mdmMonthProdPlanList,logDetail);
        //Joran初始化班次可用时间
        initShiftHourMap(cxMachineCode,logDetail);
        List<CxEngineScheduleResult> reScheduleResultList=new ArrayList<>(machineAutoPlanList);
        //如果投产数据为空时则默认设置为可投产
        commonCacheService.defaultToProduct(reScheduleResultList);
        //初始化各个规格的月度剩余量集合
        initMonthRemainQtyMap(reScheduleResultList,logDetail);

        //Joran 2021-12-20 对机台任务列表同胎胚的单班硫化量进行合并到投产规格上start
        CxScheduleUtils.calcMachineSpecLhShiftCount(reScheduleResultList);
        //Joran 2021-12-20 对机台任务列表同胎胚的单班硫化量进行合并到投产规格上end

        List<CxEngineScheduleResult> insertTaskList=new ArrayList<>();
        //所有排程任务开始计划安排start
        scheduleByMachineScheduleResult(cxMachineCode,reScheduleResultList,insertTaskList,logDetail);
        //所有排程任务开始计划安排end
        if(StringUtils.isNotEmpty(insertTaskList)){
            //插入排程结果表
            cxScheduleEngineMapper.batchInsertCxScheduleResult(insertTaskList);
        }

        String title="【成型单机台自动重排结果】";
        autoScheduleLogService.insertCxScheduleLog("", "", title,logDetail.toString()); //添加日志

    }*/

}
