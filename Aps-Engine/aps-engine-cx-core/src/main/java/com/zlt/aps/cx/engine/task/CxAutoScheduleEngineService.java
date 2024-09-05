package com.zlt.aps.cx.engine.task;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleStopInfo;
import com.zlt.aps.cx.api.domain.entity.CxScheduleTaskTime;
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
import com.zlt.aps.cx.engine.service.CxScheduleTaskTimeService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 成型工序自动排程引擎重新调整
 */
@Service("cxAutoScheduleEngineService")
@Slf4j
public class CxAutoScheduleEngineService {
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

    @Autowired
    private CxScheduleTaskTimeService cxScheduleTaskTimeService;

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

    //记录汇总同胎胚月计划需求量 pancd+ 20230901
    private Map<String,Integer> embryoCodeMonthPlanQtyMap=new ConcurrentHashMap<>();

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

    //Joran 2022-03-31 自动排程时 停排列表
    private List<CxScheduleStopInfo> cxScheduleStopInfoList;

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

    /**
     * 成型机台当前在产规格胎胚
     */
    //private Map<String,String> machineInProductMap;

    @Autowired
    private CxEngineEmbryoMonthPlanSurplusService cxEngineEmbryoMonthPlanSurplusService;

    @Autowired
    private CxEngineGroupMachineListService cxEngineGroupMachineListService;

    @Autowired
    private CxEngineSapSpecMoldUseService cxEngineSapSpecMoldUseService;

    @Autowired
    private CxEngineCommonService cxEngineCommonService;

    /**
     * 每个机台自动排程的顺序
     */
    private Integer machineAutoScheduleSort=0;

    /**
     * 自动排程日期
     */
    private Date autoScheduleDate;

    /**
     * 单次自动的批次号
     */
    private String cxBatchNo;

    /**
     * 硫化外胎施工集合
     */
    private Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap;

    /**
     * 机台过滤的投产规格
     */
    private Map<String,List<CxPlanProductStatus>> machineExcludeProductStatusMap;

    /**
     * 最大挑选规格次数
     */
    private Integer maxSelectStatusCount;

    /**
     * 最大添加规格次数
     */
    private Integer maxAddSpecCount;

    /**
     * 成型自动排程
     * @param machineTaskMap
     * @param cxPlanProductStatusList
     * @param mdmMonthProdPlanList
     * @param embryoCodeTypeTotalMap
     * @param sameDimensionAvailableClassOneShiftMap
     */
    public void autoSchedule(Date scheduleDate,String cxBatchNo, Map<String,List<CxEngineScheduleResult>> machineTaskMap, List<CxPlanProductStatus> cxPlanProductStatusList, List<MdmMonthProdPlan> mdmMonthProdPlanList, Map<String,Integer> embryoCodeTypeTotalMap, Map<String,Double> sameDimensionAvailableClassOneShiftMap,Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap){
        StringBuilder logDetail =new StringBuilder("成型自动排程开始日志：").append(division);
        cxScheduleStopInfoList=new ArrayList<>();
        this.autoScheduleDate=scheduleDate;
        this.cxBatchNo=cxBatchNo;
        //自动排程数据初始化
        initScheduleData(cxPlanProductStatusList,mdmMonthProdPlanList,scheduleDate,logDetail);
        //Joran 2022-01-08 初始化胎胚类型对应的库存汇总信息
        this.embryoCodeTypeTotalMap=embryoCodeTypeTotalMap;
        //Joran 2022-01-08 初始化同寸口一班平均可硫化班次
        this.sameDimensionAvailableClassOneShiftMap=sameDimensionAvailableClassOneShiftMap;

        //Joran 2022-04-01硫化施工信息
        this.sapTireConstructionListMap=sapTireConstructionListMap;

        //2022-05-17 进行排程日期任务时间删除
        cxScheduleTaskTimeService.deleteCxScheduleTaskTimeByScheduleDate(scheduleDate);
        //前一天排程任务安排
        scheduleLastDaySchedule(machineTaskMap,logDetail);
        String title="【新成型所有机台自动排程结果】";
        autoScheduleLogService.insertCxScheduleLog("", "", title,logDetail.toString()); //添加日志

    }

    /**
     * 前一天排程任务安排
     * @param machineTaskMap
     */
    private void scheduleLastDaySchedule(Map<String, List<CxEngineScheduleResult>> machineTaskMap,StringBuilder logDetail) {
        if(StringUtils.isEmpty(machineTaskMap)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.schedule.record.error"));
        }
        logDetail.append("======================遍历机台进行自动排程日志生成=============================").append(division);
        List<CxEngineScheduleResult> insertTaskList =new ArrayList<>();
        //用来存储所有任务时间
        List<CxScheduleTaskTime> scheduleTaskTimeList =new ArrayList<>();
        //过滤集合初始化
        machineExcludeProductStatusMap=new HashMap<>();
        //遍历前一天所有机台 处理机台上所有排程任务start
        for(Map.Entry<String,List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()){

            //初始化
            maxRoopCount= CxEngineConstants.AUTO_SCHEDULE_MAX_ROOP_COUNT;
            singleMachineRoopCount=0;
            maxSelectStatusCount=0;
            maxAddSpecCount=0;

            //自动排程任务集合顺序
            machineAutoScheduleSort=0;
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

            //补充规格或寸口（注：在前日增补手工插单时，可能会为空）；
            supplementSpec2Dimension(lastDayScheduleResultList);

            //Joran 2021-12-20 对机台任务列表同胎胚的单班硫化量进行合并到投产规格上start
            CxScheduleUtils.calcMachineSpecLhShiftCount(lastDayScheduleResultList);
            //Joran 2021-12-20 对机台任务列表同胎胚的单班硫化量进行合并到投产规格上end

            //2022-02-24 单机台任务列表处理
            handleMachineTaskList(machineCode,lastDayScheduleResultList,insertTaskList,scheduleTaskTimeList,logDetail);
        }
        //Joran 2022-05-18 进行机台时间计算对象缓存释放
        commonCacheService.clearCacheData();
        logDetail.append("======================遍历机台进行自动排程日志结束=============================").append(division);

        //Joran 2022-03-31 存在自动停排列表进行停排信息存储start
        if(StringUtils.isNotEmpty(cxScheduleStopInfoList)){
            cxScheduleEngineMapper.batchInsertScheduleStopInfo(cxScheduleStopInfoList);
        }
        //Joran 2022-06-17 批量进行成型任务时间存储
        if(StringUtils.isNotEmpty(scheduleTaskTimeList)){
            cxScheduleTaskTimeService.batchInsertCxScheduleTaskTime(scheduleTaskTimeList);
        }

        //Joran 2022-03-31 存在自动停排列表进行停排信息存储end

        //遍历前一天所有机台 处理机台上所有排程任务end
        if(StringUtils.isNotEmpty(insertTaskList)){
            //插入排程结果表
            cxScheduleEngineMapper.batchInsertCxScheduleResult(insertTaskList);
        }
    }

    /**
     * 补充规格名称和寸口
     * @param lastDayScheduleResultList
     */
    private void supplementSpec2Dimension(List<CxEngineScheduleResult> lastDayScheduleResultList) {
        //如果规格名称的则进行补充
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            String key= GenerageMapKeyUtils.createMapKey(cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getBomDataVersion());
            if(StringUtils.isEmpty(cxEngineScheduleResult.getSpecDesc())){
                EngineProductConstructionInfo constructionInfo = engineConstructionInfoMap.get(key);
                if (constructionInfo != null){
                    cxEngineScheduleResult.setSpecDesc(constructionInfo.getSpecDesc());
                    cxEngineScheduleResult.setSpecDimension(constructionInfo.getDimension());
                }
            }
        }
    }
    /**
     * 处理单机台排程
     * @param machineCode 当前机台编号
     * @param lastDayScheduleResultList 产生于昨天计划的增补列表结果
     * @param insertTaskList 新日期排程结果
     * @param logDetail 日志
     */
    private void handleMachineTaskList(String machineCode, List<CxEngineScheduleResult> lastDayScheduleResultList, List<CxEngineScheduleResult> insertTaskList,List<CxScheduleTaskTime> scheduleTaskTimeList,StringBuilder logDetail) {
        if(StringUtils.isEmpty(lastDayScheduleResultList)){
            String msg="【停止排程】成型机台："+machineCode+",前一天安排的任务数量为0,当前机台不进行自动排程";
            logDetail.append(msg).append(division);
            addMachineStopInfo(machineCode,null,null,msg);
            return;
        }
        logDetail.append("【单机台任务自动排程】").append("机台编号：").append(machineCode).append(",机台前一天任务列表记录数：").append(lastDayScheduleResultList.size()).append(division);

        //单机台胎胚任务集合
        Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap=new HashMap<>();

        //昨日三班和次一次二班存在计划量需要优先进行计划安排
        List<CxEngineScheduleResult> suppleList=new ArrayList<>();

        //昨日三班和次一次二班没有计划量
        List<CxEngineScheduleResult> remainList=new ArrayList<>();

        //月度剩余量为0的数据
        List<CxEngineScheduleResult> lastDayRemainResultList=new ArrayList<>(lastDayScheduleResultList);

        //还有月度剩余量的规格
        List<CxEngineScheduleResult> monthRemainList=new ArrayList<>();

        //Joran 2022-02-25 处理月度剩余量为0的集合
        handleRemainMonthQtyList(lastDayScheduleResultList,lastDayRemainResultList,monthRemainList);

        //保留月度剩余量为0的数据集合
        insertTaskList.addAll(lastDayRemainResultList);

        //根据三个班的计划量来分组
        groupListByPlanQty(monthRemainList,suppleList,remainList);

        //库存对应的平均库存
        Double class3PlannedAvgAvailableLhShift= CxScheduleUtils.calcAvgAvailableLhShiftIndex(monthRemainList,BigDecimal.ZERO.intValue());//Joran 2022-01-08 获取中班可硫化班次

        /**
         * 优先进行计划增补重排的规格任务
         */
        Integer beginClassIndex= BigDecimal.ZERO.intValue();//昨日三班下标

        //用来记录最后排程的规格
        Map<String,CxEngineScheduleResult> machineLastSpecMap=new HashMap<>();

        //1、进行昨日计划增补
        if(StringUtils.isNotEmpty(suppleList)){
            //需要进行昨日计划增补
            beginClassIndex= suppleScheduleTask(machineCode,suppleList,beginClassIndex,cxAutoScheduleTaskListMap,machineLastSpecMap,class3PlannedAvgAvailableLhShift,logDetail);
        }

        //原有剩余任务集合先进行清空
        monthRemainList.clear();

        //重新将自动增补后的计划回写
        monthRemainList.addAll(suppleList);

        //添加剩余任务
        monthRemainList.addAll(remainList);

        logDetail.append("【计划增补后自动排程】").append("机台编号：").append(machineCode).append(",自动排程开始班次：").append(beginClassIndex).append(division);

        //机台全部任务进行自动排程
        nextSpecAutoSchedule(machineCode,monthRemainList,machineLastSpecMap,cxAutoScheduleTaskListMap,beginClassIndex,logDetail);

        //将自动安排的计划放到inertList当中
        if(StringUtils.isNotEmpty(monthRemainList)){
            //Joran 2022-05-17 自动排程结束后按照机台的任务信息进行任务时间生成
            commonCacheService.calcMachineTaskTime(machineCode,monthRemainList,scheduleTaskTimeList, CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_AUTO,engineConstructionInfoMap,cxParamsMap);
            insertTaskList.addAll(monthRemainList);
        }

    }


    /**
     * 成型机台中可排产的任务进行自动排程
     * @param machineCode 成型机台
     * @param monthRemainList 当前剩余可排总任务量
     * @param machineLastSpecMap 全一个规格
     * @param cxAutoScheduleTaskListMap 全部规格任务列表
     * @param beginClassIndex 当前开始班次
     * @param logDetail
     */
    private void nextSpecAutoSchedule(String machineCode, List<CxEngineScheduleResult> monthRemainList, Map<String, CxEngineScheduleResult> machineLastSpecMap, Map<String, List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap, Integer beginClassIndex, StringBuilder logDetail) {
        logDetail.append("【自动排载】机台【"+machineCode+"】开始对机台所有任务进行自动排程").append(division);
        String key =GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");
        Double remainTime=machineShiftHourMap.get(key);
        String msg="";
        if(beginClassIndex>ClassEnums.CLASS_FIVE.getClassIndex()){
            logDetail.append(StringUtils.format("当前班次下标：{}，超过最大班次，不再进行自动排程",beginClassIndex)).append(division);
            return;
        }else if(beginClassIndex==ClassEnums.CLASS_FIVE.getClassIndex() && remainTime<=BigDecimal.ZERO.doubleValue()){
            msg=StringUtils.format("当前班次下标：{}，班次已经全部占满，不再进行自动排程",beginClassIndex);
            logDetail.append(msg).append(division);
            return;
        }

        singleMachineRoopCount+=1;
        logDetail.append(logSplit("任务集合："+toJSONString(cxAutoScheduleTaskListMap),"当前迭代层数："+singleMachineRoopCount,"最大可迭代层数："+maxRoopCount));
        if(singleMachineRoopCount>maxRoopCount){
            msg="【自动排程递归异常】当前迭代层数："+singleMachineRoopCount+",最大可迭代层数："+maxRoopCount;
            logDetail.append(msg).append(division);
            //Joran 2022-03-31 添加停排信息
            addMachineStopInfo(machineCode,"",beginClassIndex,msg);
            return;
        }

        CxEngineScheduleResult nextCxEngineScheduleResult=null;

        List<CxEngineScheduleResult> autoScheduleTaskList=new ArrayList<>();
        //筛选规格
        reValidateScheduleResult(autoScheduleTaskList,monthRemainList,beginClassIndex,logDetail);

        //获取前规格
        CxEngineScheduleResult preCxEngineScheduleResult =null;
        if(StringUtils.isNotEmpty(machineLastSpecMap)&&machineLastSpecMap.containsKey(machineCode)){
            preCxEngineScheduleResult=machineLastSpecMap.get(machineCode);
        }else{
            msg="自动排程前规格未找到异常，停止自动排程";
            logDetail.append(msg).append(division);
            //Joran 2022-03-31 添加停排信息
            addMachineStopInfo(machineCode,"",beginClassIndex,msg);
            return;
        }

        //当所有规格月度计划都安排完了，自动进行新规格安排start
        boolean emptyAddSpec=false;
        if(StringUtils.isEmpty(autoScheduleTaskList) && remainTime>0 && preCxEngineScheduleResult!=null){
            emptyAddSpec=true;
            emptyScheduleAddSpec(autoScheduleTaskList,monthRemainList,preCxEngineScheduleResult,beginClassIndex,logDetail);
        }
        //当所有规格月度计划都安排完了，自动进行新规格安排end
        if(StringUtils.isEmpty(autoScheduleTaskList)){
            msg="可自动安排的规格任务剩余量都为0";
            logDetail.append(msg).append(division);
            //Joran 2022-03-31 添加停排信息
            addMachineStopInfo(machineCode,preCxEngineScheduleResult.getOrderNo(),beginClassIndex,msg);
            return;
        }

        //根据可硫化班次进行升序排序，时间最短的优先安排
        CxScheduleUtils.taskSortAscByAvailableLhShift(autoScheduleTaskList,beginClassIndex);
        //挑选可硫化班次最小的规格
        nextCxEngineScheduleResult=autoScheduleTaskList.get(0);
        logDetail.append("下一个规格："+toJSONString(nextCxEngineScheduleResult)).append(division);
        if(nextCxEngineScheduleResult==null){
            msg="【自动排程】所有规格都执行了，没有下一个规格，单机台任务自动排程结束。";
            logDetail.append(msg).append(division);
            //Joran 2022-03-31 添加停排信息
            addMachineStopInfo(machineCode,preCxEngineScheduleResult.getOrderNo(),beginClassIndex,msg);
            return ;
        }

        List<CxEngineScheduleResult> toProductList=new ArrayList<>();

        //Joran 2021-12-18 进行投产标记类型为是的数据筛选start
        CxScheduleUtils.addProductSourceToTarget(monthRemainList,toProductList);
        //Joran 2021-12-18 进行投产标记类型为是的数据筛选end

        //对符合条件的任务列表进行取平均可硫化班次运算
        //Joran 2021-12-28没有因为月度收尾新增规格start
        Double avgAvailableLhShift= CxScheduleUtils.calcAvgAvailableLhShiftIndex(toProductList,beginClassIndex);//Joran 2021-12-18 只筛选投产规格
        //前一天三班也就是八点的可硫化班次
        Double classOneAvgAvailableLhShift= CxScheduleUtils.calcAvgAvailableLhShiftIndex(toProductList,BigDecimal.ZERO.intValue());//Joran 2022-01-08 获取中班可硫化班次

        if(!emptyAddSpec){
            //按班次可硫化班数进行升序排序获取到可硫化班次最小值
            CxScheduleUtils.taskSortAscByAvailableLhShift(toProductList,beginClassIndex);//Joran 2021-12-18 只筛选投产规格
            //拿到班次最小规格
            CxEngineScheduleResult minAvailableLhShiftResult=toProductList.get(0);
            //拿到最小可硫化班次的规格的可硫化班次
            Double minAvailableLhShift= CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(minAvailableLhShiftResult,beginClassIndex);
            logDetail.append("平均可硫化班次："+avgAvailableLhShift).append(division);
            //验证是否可以投产新规格
            boolean canAddSpecFlag=addSpecValidate(avgAvailableLhShift,minAvailableLhShift,logDetail);
            logDetail.append("是否可投产新规格："+canAddSpecFlag).append(division);
            //进行新规格投产
            //addSpecFlag:
              if(canAddSpecFlag){
                  StringBuilder addSpecLog=new StringBuilder();
                 //Joran 2022-03-29 获取机台的总的单班硫化总量
                 Integer totalSingleLhShiftQty=CxScheduleUtils.sumSingleLhShiftQty(toProductList,machineCode,addSpecLog,division);
                //添加新规格
                CxEngineScheduleResult newSpecScheduleResult=addSpec(monthRemainList,preCxEngineScheduleResult,minAvailableLhShift,totalSingleLhShiftQty,addSpecLog);
                if(newSpecScheduleResult==null){
                    addSpecLog.append("【自动排程】投产新规格时，没有匹配到相应的新规格进行投产。机台："+machineCode+"，自动排程结束");
                    logDetail.append(addSpecLog).append(division);
                    //Joran 2022-03-31 添加停排信息
                    addMachineStopInfo(machineCode,preCxEngineScheduleResult.getOrderNo(),beginClassIndex,msg);
                    return;
                    //logDetail.append("【自动排程】投产新规格时，没有匹配到相应的新规格进行投产。机台："+machineCode+"，调整为继续接规格切换排班").append(division);
                    //break addSpecFlag;
                }
                logDetail.append(addSpecLog).append("投产新规格信息："+toJSONString(newSpecScheduleResult)).append(division);
                //重新计算每个班次的可硫化班次数
                CxScheduleUtils.calcAllClassAvailableLhShift(newSpecScheduleResult);
                //下个班次任务创建
                nextCxEngineScheduleResult=newSpecScheduleResult;
                //将新生成的规格结果放入投产列表中
                monthRemainList.add(nextCxEngineScheduleResult);
                //重新计算平均可硫化班数
                if(beginClassIndex>0){
                    ClassEnums cls=ClassEnums.getClassEnums(beginClassIndex);
                    avgAvailableLhShift= CxScheduleUtils.calcAvgAvailableLhShift(toProductList,cls);//Joran 2021-12-18 只筛选投产规格
                }
                logDetail.append("添加新规格后重新计算平均可硫化班次："+avgAvailableLhShift).append(division);
            }
        }

        //验证是否为相同规格连续排班
        boolean sameTask=validateSameTask(preCxEngineScheduleResult.getOrderNo(),nextCxEngineScheduleResult.getOrderNo());

        if(sameTask){
            //1、当前班已经排了，但是没有占满（处理的时候，满班的话beginClassIndex会自动往下）
            if(remainTime < CxEngineConstants.CLASS_SHIFT_HOUR){
                logDetail.append("【{续作相同规格，重新获取定额}】>>>");
                Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(nextCxEngineScheduleResult.getCxMachineCode(),nextCxEngineScheduleResult.getEmbryoCode(),nextCxEngineScheduleResult.getBomDataVersion(),logDetail);

                //一个小时生产多少
                BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR));

                //Integer currentShiftPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(nextCxEngineScheduleResult,beginClassIndex);
                //任务差额(根据实际时间算出差额)
                Integer differentPlan=BigDecimal.valueOf(remainTime).multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN).intValue();
                if(differentPlan>0){
                    //更新前规格
                    beginClassIndex=updatePreCxScheduleResult(machineCode,preCxEngineScheduleResult,beginClassIndex,differentPlan,hourCountBig,remainTime);
                }
            }
        }

        //开始后续规格自动排程
        beginClassIndex=nextScheduleResultCreateTask(machineCode,nextCxEngineScheduleResult,preCxEngineScheduleResult,cxAutoScheduleTaskListMap,avgAvailableLhShift,classOneAvgAvailableLhShift,beginClassIndex,logDetail,sameTask);

        //将当前自动排程的规格置为下个一个的前规格
        machineLastSpecMap.put(machineCode,nextCxEngineScheduleResult);

        //递归进行规格筛选 前后规格记录
        nextSpecAutoSchedule(machineCode,monthRemainList,machineLastSpecMap,cxAutoScheduleTaskListMap,beginClassIndex,logDetail);


    }

    /**
     * 自动排程逻辑
     * @param nextCxEngineScheduleResult 下一个规格计划
     * @param preCxEngineScheduleResult 前一个规格计划
     * @param cxAutoScheduleTaskListMap 机台全部自动安排集合
     * @param avgAvailableLhShift 班次平均可硫化班次
     * @param classOneAvgAvailableLhShift 前日三班的平均可硫化班次
     * @param beginClassIndex 当前班次下标
     * @param logDetail
     * @param sameTask 是否相同规格
     * @return
     */
    private Integer nextScheduleResultCreateTask(String machineCode,CxEngineScheduleResult nextCxEngineScheduleResult, CxEngineScheduleResult preCxEngineScheduleResult, Map<String, List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap, Double avgAvailableLhShift, Double classOneAvgAvailableLhShift, Integer beginClassIndex, StringBuilder logDetail, boolean sameTask) {
        String embryoCode=nextCxEngineScheduleResult.getEmbryoCode();
        logDetail.append("【新自动添加下个规格任务创建】，胎胚代码："+embryoCode).append(division);
        nextCxEngineScheduleResult.initPlanQty();
        String  specDimension=nextCxEngineScheduleResult.getSpecDimension()==null?"":nextCxEngineScheduleResult.getSpecDimension().toString();
        Integer  singleShiftLhQty=nextCxEngineScheduleResult.getSingleShiftLhQty();
        //获取成型机规格定额数据
        Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(machineCode,embryoCode,nextCxEngineScheduleResult.getBomDataVersion(),logDetail);
        logDetail.append("新自动添加下个规格排产获取定额：").append(machineQuota).append(division);
        //计算规格可以排的最大硫化班次
        Double maxLhCount=maxLhShiftCount(nextCxEngineScheduleResult,specDimension,avgAvailableLhShift,classOneAvgAvailableLhShift,logDetail);
        logDetail.append("新自动添加下个规格最大安排硫化班次：").append(nextCxEngineScheduleResult.getMaximumClassQty()).append(division);
        logDetail.append("新自动添加下个规格单班硫化量：").append(singleShiftLhQty).append(division);
        //根据最大可硫化班次设置，计算单次安排任务总量
        int taskQty= (int) Math.ceil(maxLhCount * singleShiftLhQty);
        logDetail.append("新自动添加下个规格总计划量：").append(taskQty).append(division);
        //计划总量根据耗损率进行重新计算
        taskQty=calcLossRate(machineCode,embryoCode,taskQty,logDetail);
        //连续计划量计算
        Integer continuePlanQty=calcContinuePlanQty(nextCxEngineScheduleResult,beginClassIndex,logDetail,sameTask);
        logDetail.append("新自动添加下个规格连续计划量：").append(continuePlanQty).append(division);
        Integer remainTaskQty=taskQty - continuePlanQty; //剩余任务量
        logDetail.append("新自动添加下个规格任务剩余量：").append(remainTaskQty).append(division);
        if(remainTaskQty <= 0){
            logDetail.append("新自动添加下个规格没有剩余任务量，单规格预排任务不进行生成，剩余任务量：").append(remainTaskQty).append(division);
            continuePlanQty=calcContinuePlanQty(nextCxEngineScheduleResult,beginClassIndex,logDetail,true);
            logDetail.append("【新自动添加为了任务继续往下自动排，连续生产量清0】,当前连续生产量=").append(continuePlanQty).append(division);
            remainTaskQty=taskQty - continuePlanQty; //剩余任务量
            logDetail.append("【新自动添加为了任务继续往下自动排】,重新计算的任务剩余量：").append(remainTaskQty).append(division);
        }

        //从map中获取实际月度剩余量
        Integer remainMonthQty=getRealMonthRemainQty(nextCxEngineScheduleResult);
        logDetail.append("新自动添加下个规格月度剩余量：").append(remainMonthQty).append(division);
        if(remainMonthQty <= 0){
            logDetail.append("新自动添加下个规格没有月度剩余量，规格续排任务不进行生成，月度剩余量：").append(remainMonthQty).append(division);
            return beginClassIndex;
        }

        int onceCloseOut=getOnceCloseOutQty();

        //是否一次性投产
        boolean onceProduct=remainMonthQty<=onceCloseOut;

        //月度剩余量如果大于任务量 则以任务余量进行排产，如果月度剩余量小于任务余量则用月度剩余量进行投产
        remainTaskQty=(remainMonthQty>remainTaskQty&&!onceProduct)?remainTaskQty:remainMonthQty;
        BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR)); //一个小时生产多少
        logDetail.append("【新自动添加自动任务安排】规格小时产量：").append(hourCountBig).append(division);

        //当前机台规格任务班次计划安排
        beginClassIndex=machineShiftTaskAutoSchedule(machineCode,taskQty,remainTaskQty,continuePlanQty,onceProduct,preCxEngineScheduleResult,nextCxEngineScheduleResult,cxAutoScheduleTaskListMap,beginClassIndex,sameTask,logDetail);

        return beginClassIndex;
    }


    /**
     * 机台规格剩余班次安排
     * @param machineCode 机台编号
     * @param remainTaskQty 剩余任务总量
     * @param onceProduct 是否一次性投产
     * @param preCxEngineScheduleResult 前规格
     * @param nextCxEngineScheduleResult 后规格
     * @param cxAutoScheduleTaskListMap 自动排程任务集合
     * @param beginClassIndex 当前班次
     * @param sameTask 前后规格是否相同规格
     * @param logDetail
     * @return
     */
    private int machineShiftTaskAutoSchedule(String machineCode,Integer taskQty,Integer remainTaskQty,Integer continuePlanQty,Boolean onceProduct,CxEngineScheduleResult preCxEngineScheduleResult,CxEngineScheduleResult nextCxEngineScheduleResult,Map<String, List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap,int beginClassIndex,Boolean sameTask, StringBuilder logDetail) {
        logDetail.append("【机台规格剩余班次安排】机台【"+machineCode+"】开始班次自动排程。").append(division);
        String key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");

        //获取最大可硫化班次
        Double maxClassShifts=getMaxLhClassShifts();

        //规格班次自动排程
        List<CxAutoScheduleTask> shiftAutoScheduleList=new ArrayList<>();
        //胎胚代码
        String embryoCode=nextCxEngineScheduleResult.getEmbryoCode();
        Integer remainMonthQty=getRealMonthRemainQty(nextCxEngineScheduleResult);
        logDetail.append("【机台规格剩余班次安排】下个规格月度剩余量：").append(remainMonthQty).append(division);
        if(remainMonthQty <= 0){
            logDetail.append("【机台规格剩余班次安排】下个规格没有月度剩余量，当前规格不进行自动增补，月度剩余量：").append(remainMonthQty).append(division);
            return beginClassIndex;
        }

        if(remainTaskQty <= BigDecimal.ZERO.intValue()){
            logDetail.append("【机台规格剩余班次安排】没有获取到计划总量，当前计划总量=").append(remainTaskQty).append(division);
            return beginClassIndex;
        }

        if(beginClassIndex>ClassEnums.CLASS_FIVE.getClassIndex()){
            logDetail.append("【机台规格剩余班次安排】没有班次可以进行自动安排，当前班次下标").append(beginClassIndex).append(division);
            return beginClassIndex;
        }

        //获取当前班次剩余时长
        Double remainTime=machineShiftHourMap.get(key);
        logDetail.append("【机台规格剩余班次安排】当前开始排载的班次可用时长=").append(remainTime).append(division);
        //还有剩余时间 需要扣除掉规格更换时间 start
        if(remainTime >= BigDecimal.ZERO.doubleValue()){
            //获取新规格定额信息
            Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(machineCode,embryoCode,nextCxEngineScheduleResult.getBomDataVersion(),logDetail);
            logDetail.append("【机台规格剩余班次安排】获取到的定额数=").append(machineQuota).append(division);
            //一个小时生产多少
            BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR));
            logDetail.append("【机台规格剩余班次安排】获取小时产量=").append(hourCountBig).append(division);

            Double changeSpecTime=BigDecimal.ZERO.doubleValue();
            if(!sameTask){
                //后规格胎胚+施工版本
                String afterKey=GenerageMapKeyUtils.createMapKey(embryoCode,nextCxEngineScheduleResult.getBomDataVersion());

                //前规格胎胚+施工版本
                String beforeKey=GenerageMapKeyUtils.createMapKey(preCxEngineScheduleResult.getEmbryoCode(),preCxEngineScheduleResult.getBomDataVersion());

                //前后规格更换工装时长
                changeSpecTime=changeSpecTime(afterKey,beforeKey,logDetail);
                logDetail.append("【机台规格剩余班次安排】前后规格不相同，更换工装时长=").append(changeSpecTime).append(division);
            }

            //二分之一更换工装时间
            Double halfChangeSpecTime=BigDecimal.valueOf(changeSpecTime).divide(BigDecimal.valueOf(2)).doubleValue();
            logDetail.append("【机台规格剩余班次安排】前后规格不相同，更换工装(二分之一)时长=").append(halfChangeSpecTime).append(division);
            //班次剩余时间与二分之一的工装更换时间逻辑 start
            Integer begin=0; //自动安排的班次数
            if(remainTime <= halfChangeSpecTime){
                logDetail.append("【机台规格剩余班次安排】剩余时长 <= 更换工装(二分之一)时长").append(halfChangeSpecTime).append(division);
                //更新班次剩余时长为0
                updateMachineShiftHourMap(machineCode,beginClassIndex, BigDecimal.ZERO.doubleValue());

                //前规格剩余时间小于更换工装工时一半，则下个班扣除整个更换工装时长
                beginClassIndex+=1;
                ClassEnums cls =ClassEnums.getClassEnums(beginClassIndex);

                logDetail.append("【自动挑选规格后班次自动排程】剩余时间:"+remainTime+",二分之一更换工装时间："+changeSpecTime+"比二分之一更换工装时间短，不再进行当前班次排产，直接进入下一个班次").append(division);
                if(cls==null){
                    return beginClassIndex;
                }

                //Joran 2021-12-24 更换工装在前规格计划量当前规格扣减
                key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");

                //获取班次最新班次剩余时间(是新班次的话等于班次总时长)
                remainTime=machineShiftHourMap.get(key);

                //剩余时间(扣除工装更换时长)
                BigDecimal shiftRemainTimeBig=BigDecimal.valueOf(remainTime-changeSpecTime);

                logDetail.append("【自动挑选规格后班次自动排程】前规格换工装当前班扣除更换工装剩余时：").append(shiftRemainTimeBig).append(division);

                BigDecimal currentPlanQty=shiftRemainTimeBig.multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN);

                logDetail.append("【自动挑选规格后班次自动排程】规格计算可投产计划量：").append(currentPlanQty).append(division);

                Integer currentShiftPlanQty=currentPlanQty.intValue();

                //自动排程前先验证是否超出最大可硫化班数，如果超过将不再进行自动班次排程 start
                CxScheduleUtils.calcAllClassAvailableLhShift(nextCxEngineScheduleResult);
                Double currentShiftAvailableLhShift=CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(nextCxEngineScheduleResult,beginClassIndex);
                if( currentShiftAvailableLhShift >= maxClassShifts){
                    logDetail.append("【自动挑选规格后班次自动排程】自动排程结束，当前：").append(currentPlanQty).append(division);
                    return beginClassIndex;
                }
                //自动排程前先验证是否超出最大可硫化班数，如果超过将不再进行自动班次排程 end

                //增补总量与班次可安排计划量逻辑校验 start
                if(remainTaskQty >= currentShiftPlanQty) { //月度剩余量大于当班计划量
                    logDetail.append("【自动挑选规格后班次自动排程】月度剩余量大于当班计划量，计划量=").append(currentShiftPlanQty).append(division);
                    remainTaskQty -= currentShiftPlanQty;
                    continuePlanQty += currentShiftPlanQty;//记录安排的计划量
                    //创建自动处理任务
                    CxAutoScheduleTask autoSuppleTask=commonCacheService.createClassShiftRemainQty(nextCxEngineScheduleResult,beginClassIndex,currentShiftPlanQty,remainTaskQty,continuePlanQty,shiftRemainTimeBig.doubleValue());
                    machineAutoScheduleSort+=1;
                    autoSuppleTask.setMachineAutoScheduleSort(machineAutoScheduleSort);
                    shiftAutoScheduleList.add(autoSuppleTask);
                    //更新计划量 月度剩余量 班次计划领 原因分析等
                    updatePlanQtyAndRemainTimeAndQty(machineCode,nextCxEngineScheduleResult,beginClassIndex,BigDecimal.ZERO.doubleValue(),currentShiftPlanQty,"",logDetail);
                    //继续下一个班次
                    beginClassIndex+=1;
                    //自动排程次数
                    begin+=1;
                    //自动排程前先验证是否超出最大可硫化班数，如果超过将不再进行自动班次排程 start
                    CxScheduleUtils.calcAllClassAvailableLhShift(nextCxEngineScheduleResult);
                    currentShiftAvailableLhShift=CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(nextCxEngineScheduleResult,beginClassIndex);
                    if( currentShiftAvailableLhShift >= maxClassShifts){
                        logDetail.append("【自动挑选规格后班次自动排程】自动排程结束，当前：").append(currentPlanQty).append(division);
                        return beginClassIndex;
                    }
                    //自动排程前先验证是否超出最大可硫化班数，如果超过将不再进行自动班次排程 end

                    //开启剩余任务量进行自动安排直到班次排满或者剩余任务量排完
                    beginClassIndex=remainTaskNextShiftAuto(machineCode,nextCxEngineScheduleResult,taskQty,continuePlanQty,remainTaskQty,beginClassIndex,machineQuota,shiftAutoScheduleList,begin,onceProduct,maxClassShifts,logDetail);

                }else{
                    logDetail.append("【自动挑选规格后班次自动排程】月度剩余量小于当班计划量，直接排完，班次计划量=").append(currentShiftPlanQty).append(division);
                    currentShiftPlanQty=remainTaskQty;
                    remainTaskQty=0;//一次性安排完
                    continuePlanQty+=currentShiftPlanQty;//记录安排的计划量
                    //重新计算班次剩余时间
                    Double shiftRemainTime=commonCacheService.getClassShiftRemainTime(remainTime,machineQuota,currentShiftPlanQty);
                    logDetail.append(StringUtils.format("【自动挑选规格后班次自动排程】,当前班次下标：{}，班次剩余可用时长：{}",beginClassIndex,shiftRemainTime)).append(division);
                    CxAutoScheduleTask autoSuppleTask=commonCacheService.createClassShiftRemainQty(nextCxEngineScheduleResult,beginClassIndex,currentShiftPlanQty,remainTaskQty,continuePlanQty,shiftRemainTime);
                    machineAutoScheduleSort+=1;
                    autoSuppleTask.setMachineAutoScheduleSort(machineAutoScheduleSort);
                    shiftAutoScheduleList.add(autoSuppleTask);
                    logDetail.append("【自动挑选规格后班次自动排程】续作时月度剩余量为0，直接排完计算剩余时间，当前任务为："+nextCxEngineScheduleResult.toString()).append(division);

                    //更新计划量 月度剩余量 班次计划领 原因分析等
                    updatePlanQtyAndRemainTimeAndQty(machineCode,nextCxEngineScheduleResult,beginClassIndex,shiftRemainTime,currentShiftPlanQty,"",logDetail);

                    return beginClassIndex;
                }
                //增补总量与班次可安排计划量逻辑校验 end

            }else{
                logDetail.append("【机台规格剩余班次安排】剩余时长 > 更换工装(二分之一)时长").append(halfChangeSpecTime).append(division);

                String changeMoldAnalysis="";
                //实际剩余时间
                BigDecimal remainTimeBig=null;

                //剩余时长大于一半更换工装时长，是否大于更换工装时长逻辑 start
                if(remainTime>=changeSpecTime) {
                    logDetail.append(StringUtils.format("【自动挑选规格后班次自动排程】,剩余时长大于一半更换工装时长,直接班次【{}】更换工装",beginClassIndex)).append(division);
                    remainTimeBig=BigDecimal.valueOf(remainTime-changeSpecTime); //剩余时间
                    if(nextCxEngineScheduleResult.getNewSpecFlag()&&!nextCxEngineScheduleResult.getMarkNewSpecAnalysisFlag()){
                        //有换工装开班就走换工装
                        nextCxEngineScheduleResult.setMarkNewSpecAnalysisFlag(true);
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
                    }else{
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
                    }
                    CxScheduleUtils.setChangeMoldConditionByClassIndex(nextCxEngineScheduleResult,beginClassIndex,changeMoldAnalysis,logDetail);
                }else{ //不够则扣除二分之一的更换工装的时长
                    //时间预留来进行工装更换
                    //更新班次剩余时长为0
                    updateMachineShiftHourMap(machineCode,beginClassIndex, BigDecimal.ZERO.doubleValue());

                    //Joran 2021-12-29前一个班次标记为更换工装，后个班次特殊处理不进行标记
                    if(nextCxEngineScheduleResult.getNewSpecFlag()&&!nextCxEngineScheduleResult.getMarkNewSpecAnalysisFlag()){
                        //有换工装开班就走换工装
                        nextCxEngineScheduleResult.setMarkNewSpecAnalysisFlag(true);
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
                    }else{
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
                    }
                    CxScheduleUtils.setChangeMoldConditionByClassIndex(nextCxEngineScheduleResult,beginClassIndex,changeMoldAnalysis,logDetail);
                    //继续往下一个班次
                    beginClassIndex+=1;
                    ClassEnums nextCls =ClassEnums.getClassEnums(beginClassIndex);
                    if(nextCls==null){
                        logDetail.append("【班次剩余时间不足】超过当天排班班次数，结束续排").append(division);
                        return beginClassIndex;
                    }
                    //下一个班次的话时间要扣除二分之一的更换工装时间
                    key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");
                    remainTime=machineShiftHourMap.get(key);
                    remainTimeBig=BigDecimal.valueOf(remainTime-halfChangeSpecTime); //剩余时间
                }
                //剩余时长大于一半更换工装时长，是否大于更换工装时长逻辑 end

                //更新班次剩余时长
                updateMachineShiftHourMap(machineCode,beginClassIndex, remainTimeBig.doubleValue());

                //开启剩余任务量进行自动安排直到班次排满或者剩余任务量排完
                beginClassIndex=remainTaskNextShiftAuto(machineCode,nextCxEngineScheduleResult,taskQty,continuePlanQty,remainTaskQty,beginClassIndex,machineQuota,shiftAutoScheduleList,begin,onceProduct,maxClassShifts,logDetail);

            }
            //班次剩余时间与二分之一的工装更换时间逻辑 end
        }else{
            logDetail.append("【机台规格剩余班次安排】班次时长小于0，时长=").append(remainTime).append(division);
        }
        //还有剩余时间 需要扣除掉规格更换时间 end

        if(StringUtils.isNotEmpty(shiftAutoScheduleList)){
            cxAutoScheduleTaskListMap.put(embryoCode,shiftAutoScheduleList);
            //补充自动排程的原因分析库存地点，任务顺序等
            handleTaskList(nextCxEngineScheduleResult,cxAutoScheduleTaskListMap,shiftAutoScheduleList,logDetail);
        }

        return  beginClassIndex;
    }

    /**
     * 自动班次计划安排
     * @param machineCode 机台编号
     * @param nextCxEngineScheduleResult 当前排程规格
     * @param taskQty 总任务量
     * @param continuePlanQty 已经安排的总计划量
     * @param remainTaskQty 剩余任务量
     * @param beginClassIndex 当前安排的班次
     * @param machineQuota 机台定额
     * @param shiftAutoScheduleList 自动安排的任务列表
     * @param begin 开始安排的计划班次
     * @param logDetail
     * @return
     */
    private int remainTaskNextShiftAuto(String machineCode, CxEngineScheduleResult nextCxEngineScheduleResult, Integer taskQty, Integer continuePlanQty, Integer remainTaskQty, int beginClassIndex, Integer machineQuota, List<CxAutoScheduleTask> shiftAutoScheduleList, Integer begin,Boolean onceProduct,Double maxClassShifts, StringBuilder logDetail) {
        String embryoCode=nextCxEngineScheduleResult.getEmbryoCode();
        logDetail.append(StringUtils.format("【自动班次计划安排】机台【{}】，当前规格胎胚：{}，规格定额：{}，剩余任务量：{}，自动进行增补安排",machineCode,embryoCode,machineQuota,remainTaskQty)).append(division);
        String specContinueProductShift=cxParamsMap.get(CxParamCodeConstants.SPEC_CONTINUE_PRODUCT_SHIFTS);
        if(beginClassIndex>ClassEnums.CLASS_FIVE.getClassIndex()){
            logDetail.append("【自动班次计划安排】，当前班次："+beginClassIndex).append(division);
            return beginClassIndex;
        }
        if(StringUtils.isEmpty(specContinueProductShift)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.change.continue.workshift.param.error"));
        }
        //可连续安排的班数
        Integer continueShift=Integer.valueOf(specContinueProductShift);
        //遍历进行后续排次任务自动增补逻辑 start
        String cxOrderNo=nextCxEngineScheduleResult.getOrderNo();
        String scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",nextCxEngineScheduleResult.getScheduleDate());
        while((begin < continueShift||onceProduct) && beginClassIndex <= CxEngineConstants.TASK_MAX_CLASS_SHIFT && remainTaskQty > 0){
            //自动排程前先验证是否超出最大可硫化班数，如果超过将不再进行自动班次排程 start
            CxScheduleUtils.calcAllClassAvailableLhShift(nextCxEngineScheduleResult);
            Double currentShiftAvailableLhShift=CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(nextCxEngineScheduleResult,beginClassIndex);
            if( currentShiftAvailableLhShift >= maxClassShifts){
                logDetail.append("【自动挑选规格后班次自动排程】自动排程结束，当前班次下标：").append(beginClassIndex).append(division);
                return beginClassIndex;
            }
            //安排班次+1
            begin+=1;
            //自动排程前先验证是否超出最大可硫化班数，如果超过将不再进行自动班次排程 end


            String key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");
            //获取当前班次剩余时长
            Double shiftRemainHour= machineShiftHourMap.get(key);
            //一个小时生产多少
            BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR));
            logDetail.append(StringUtils.format("【自动班次计划安排】,当前班次下标：{}，获取到的班次剩余时长：{}",beginClassIndex,shiftRemainHour)).append(division);
            BigDecimal currentPlanQty=BigDecimal.valueOf(shiftRemainHour).multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN);
            //当前可安排计划量
            Integer currentShiftPlanQty=currentPlanQty.intValue();
            CxAutoScheduleTask autoScheduleTask=CxScheduleUtils.createScheduleTask(nextCxEngineScheduleResult,beginClassIndex,taskQty,continuePlanQty,shiftRemainHour);
            //机台任务顺序递增
            machineAutoScheduleSort+=1;
            autoScheduleTask.setMachineAutoScheduleSort(machineAutoScheduleSort);
            autoScheduleTask.setScheduleDate(scheduleDateStr);
            //工单号生成
            autoScheduleTask.setCxOrderNo(cxOrderNo);
            //剩余任务量大于机台定额逻辑 start
            if(remainTaskQty >= currentShiftPlanQty ){ //剩余任务总量大于单班次的计划量
                logDetail.append(StringUtils.format("【自动班次计划安排】,任务剩余量 >= 班次剩余可排计划量，剩余任务量：{}，班次可排计划量：{}",remainTaskQty,currentShiftPlanQty)).append(division);
                autoScheduleTask.setCurrentShiftPlanQty(currentShiftPlanQty); //班次计划量
                remainTaskQty -= currentShiftPlanQty;//更新剩余任务量
                continuePlanQty+=currentShiftPlanQty;//任务量累加
                autoScheduleTask.setRemainTaskQty(remainTaskQty);
                autoScheduleTask.setRemainTime(CxEngineConstants.ZERO);//班次没有剩余时间
                autoScheduleTask.setRemainTaskQty(remainTaskQty-continuePlanQty);//任务剩余量
                //更新班次剩余时间、计划量、原因分析、月度剩余量等
                updatePlanQtyAndRemainTimeAndQty(machineCode,nextCxEngineScheduleResult,beginClassIndex,BigDecimal.ZERO.doubleValue(),currentShiftPlanQty,"",logDetail);
                beginClassIndex++;
            }else{
                logDetail.append(StringUtils.format("【自动班次计划安排】,任务剩余量 < 班次剩余可排计划量，剩余任务量：{}，班次可排计划量：{}",remainTaskQty,currentShiftPlanQty)).append(division);
                currentShiftPlanQty=remainTaskQty;
                autoScheduleTask.setCurrentShiftPlanQty(currentShiftPlanQty); //班次计划量
                continuePlanQty+=currentShiftPlanQty;//任务量累加
                remainTaskQty =0 ; //安排完毕
                //重新计算班次剩余时间
                Double shiftRemainTime=commonCacheService.getClassShiftRemainTime(shiftRemainHour,machineQuota,currentShiftPlanQty);
                logDetail.append(StringUtils.format("【自动班次计划安排】,任务剩余量小于机台定额，剩余任务量：{}，定额量：{}，班次剩余时长：{}",remainTaskQty,machineQuota,shiftRemainTime)).append(division);
                //更新计划量 月度剩余量 班次计划领 原因分析等
                updatePlanQtyAndRemainTimeAndQty(machineCode,nextCxEngineScheduleResult,beginClassIndex,shiftRemainTime,currentShiftPlanQty,"",logDetail);
            }
            shiftAutoScheduleList.add(autoScheduleTask);
            //剩余任务量大于机台定额逻辑 end

        }
        //遍历进行后续排次任务自动增补逻辑 end
        return beginClassIndex;
    }


    /**
     * 处理自动挑选规格的时候 挑到自身规格处理补班计划
     * @param preCxEngineScheduleResult
     * @param differentPlan
     * @param hourCountBig
     */
    private Integer updatePreCxScheduleResult(String machineCode,CxEngineScheduleResult preCxEngineScheduleResult,Integer beginClassIndex, Integer differentPlan, BigDecimal hourCountBig,Double shiftRemainTime) {
        String embryoCode=preCxEngineScheduleResult.getEmbryoCode();
        Integer monthRemainQty=BigDecimal.ZERO.intValue();
        if(monthRemainQtyMap.containsKey(embryoCode)){
            monthRemainQty= monthRemainQtyMap.get(embryoCode);
        }
        if(monthRemainQty<0){//没有月度剩余量则不进行重新弄
            return beginClassIndex;
        }
        String analysis="";
        //月度剩余量小于差异量
        if(monthRemainQty<=differentPlan){

            // Joran 2021-12-29 计划量往上加的时候需要更新下班次剩余时间 start
            BigDecimal planQtyBig=BigDecimal.valueOf(monthRemainQty);

            //加上这些计划量用了多少时间
            Double usedTime=planQtyBig.divide(hourCountBig,3, RoundingMode.CEILING).doubleValue();
            if(shiftRemainTime>=usedTime){
                Double remainTime=shiftRemainTime - usedTime;

                //更新班次剩余时长
                updateMachineShiftHourMap(machineCode,beginClassIndex, remainTime);
            }else{

                //更新班次剩余时长为0
                updateMachineShiftHourMap(machineCode,beginClassIndex, BigDecimal.ZERO.doubleValue());

                //班次往下移动
                beginClassIndex+=1;
            }
            //重设班次计划量和清空原因分析(只有不是昨日三班的计划才需要收尾原因分析) start
            if(beginClassIndex>0){
                Integer totalTaskQty=preCxEngineScheduleResult.getDayTotalPlanQty();
                totalTaskQty+=monthRemainQty;
                //共多少收尾原因分析
                analysis=StringUtils.format(I18nUtil.getMessage("cx.engine.auto.analysis.totalQty.title"),totalTaskQty);
            }
            //重设班次计划量和清空原因分析(只有不是昨日三班的计划才需要收尾原因分析) end

            CxScheduleUtils.reSetPlanQtyAndAnalysisByShiftIndex(preCxEngineScheduleResult,beginClassIndex,monthRemainQty,analysis);
            monthRemainQty=0;
            //月度剩余量清0
            monthRemainQtyMap.put(embryoCode,monthRemainQty);
            log.debug("【同规格连续生产排产，班次有剩余量补充：】补班次：【"+beginClassIndex+"】，差额量："+differentPlan+"，月度剩余量："+monthRemainQty );
            // Joran 2021-12-29 计划量往上加的时候需要更新下班次剩余时间 end

        }else{
            //更新剩余量
            monthRemainQty-=differentPlan;
            monthRemainQtyMap.put(embryoCode,monthRemainQty);
            CxScheduleUtils.reSetPlanQtyAndAnalysisByShiftIndex(preCxEngineScheduleResult,beginClassIndex,differentPlan,analysis);

            //更新班次剩余时长为0
            updateMachineShiftHourMap(machineCode,beginClassIndex, BigDecimal.ZERO.doubleValue());

            //班次占满往下个班次移动
            beginClassIndex+=1;
        }
        return beginClassIndex;
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
     *  前一天计划全部收尾的情况还有剩余空间进行新规格安排
     * @param remainQtyList
     * @param lastDayScheduleResultList
     */
    private void emptyScheduleAddSpec(List<CxEngineScheduleResult> remainQtyList, List<CxEngineScheduleResult> lastDayScheduleResultList,CxEngineScheduleResult preCxEngineScheduleResult,Integer beginClassIndex,StringBuilder logDetail) {
        logDetail.append("【收尾自动安排新规格计划】》》").append(division);
        List<CxEngineScheduleResult> toProductList=new ArrayList<>();
        //Joran 2021-12-18 进行投产标记类型为是的数据筛选start
        CxScheduleUtils.addProductSourceToTarget(lastDayScheduleResultList,toProductList);

        //重新计算平均可硫化班数
        if(beginClassIndex>0){
            ClassEnums nextTaskCls= ClassEnums.getClassEnums(beginClassIndex);
            if(nextTaskCls==null){
                logDetail.append("【全部收尾自动排程】前规格胎胚：【"+preCxEngineScheduleResult.getEmbryoCode()+"】已经将所有班次占满，不再进行产能安排").append(division);
                return ;
            }
        }
        //按班次可硫化班数进行升序排序获取到可硫化班次最小值
        CxScheduleUtils.taskSortAscByAvailableLhShift(toProductList,beginClassIndex);//Joran 2021-12-18 只筛选投产规格
        //拿到班次最小规格
        CxEngineScheduleResult minAvailableLhShiftResult=toProductList.get(0);
        //拿到最小可硫化班次的规格的可硫化班次
        Double minAvailableLhShift= CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(minAvailableLhShiftResult,beginClassIndex);

        //Joran 2022-03-29 获取机台的总的单班硫化总量
        Integer totalSingleLhShiftQty=CxScheduleUtils.sumSingleLhShiftQty(toProductList,preCxEngineScheduleResult.getCxMachineCode(),logDetail,division);
        //添加新规格
        CxEngineScheduleResult newSpecScheduleResult=addSpec(lastDayScheduleResultList,preCxEngineScheduleResult,minAvailableLhShift,totalSingleLhShiftQty,logDetail);
        if(newSpecScheduleResult==null){
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
        Double avgAvalableLhShift=BigDecimal.ZERO.doubleValue();
        if(beginClassIndex>0){
            avgAvalableLhShift=CxScheduleUtils.calcAvgAvailableLhShift(lastDayScheduleResultList,ClassEnums.getClassEnums(beginClassIndex));
        }
        logDetail.append("【收尾自动安排新规格计划】添加新规格后重新计算平均可硫化班次："+avgAvalableLhShift).append(division);
    }




    /**
     * 处理规格根据是否有月度剩余量进行区分
     * @param lastDayScheduleResultList
     * @param lastDayRemainResultList
     * @param monthRemainList
     */
    private void handleRemainMonthQtyList(List<CxEngineScheduleResult> lastDayScheduleResultList, List<CxEngineScheduleResult> lastDayRemainResultList, List<CxEngineScheduleResult> monthRemainList) {
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            Integer monthRemainQty=getRealMonthRemainQty(cxEngineScheduleResult);
            if(CxEngineConstants.TO_PRODUCT_NO.equals(cxEngineScheduleResult.getToProduct())){
                lastDayRemainResultList.add(cxEngineScheduleResult);
            }else if(monthRemainQty<=0){
                lastDayRemainResultList.add(cxEngineScheduleResult);
            }else{
                monthRemainList.add(cxEngineScheduleResult);
            }
        }
    }

    /**
     * 昨日三班、次一班、次二班存在计划的规格优先进行安排增补
     * @param suppleList
     */
    private Integer suppleScheduleTask(String machineCode,List<CxEngineScheduleResult> suppleList,int beginClassIndex,Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap, Map<String,CxEngineScheduleResult> machineLastSpecMap,Double class3PlannedAvgAvailableLhShift,StringBuilder logDetail) {
        logDetail.append("机台【"+machineCode+"】开始进行昨日计划优先安排逻辑》》》").append(division);
        //Joran 2022-02-24 从昨天计划白班开始进行安排
       // CxEngineScheduleResult  cxEngineScheduleResult= schdeuleTaskBeginClass3Planned(machineCode,suppleList,logDetail);

        //没有获取到当前在产规格，先进行三班计划排序
    /*    if(cxEngineScheduleResult==null){
            logDetail.append("没有获取到机台在产规格胎胚，根据昨日三班的可硫化班次进行筛选。").append(division);
            cxEngineScheduleResult=getLastDayClass3MinAvailableShift(machineCode,suppleList,logDetail);
        }

        cxEngineScheduleResult=getLastDayClass3MinAvailableShift(machineCode,suppleList,logDetail);
*/

        //计划增补时根据增补计划设置的顺序进行排序
        CxScheduleUtils.sortByPlanSort(suppleList);

        //获取生产顺序最小
        CxEngineScheduleResult  cxEngineScheduleResult=suppleList.get(0);

        //增补总量设定,根据单班硫化量
        calcTaskTotalQty(machineCode,suppleList,cxEngineScheduleResult,class3PlannedAvgAvailableLhShift,logDetail);

        //用于存储处理过的工单号
        Set<String> handleOrderNoSet= new HashSet<>();

        //从昨日三班开始进行计划增补
        beginClassIndex=scheduleTaskBeginLastDayClass3Plan(cxAutoScheduleTaskListMap,cxEngineScheduleResult,beginClassIndex,machineCode,logDetail);
        logDetail.append(StringUtils.format("【自动增补】，下个规格开始的班次序号：{}",beginClassIndex)).append(division);

        //前规格工单号
        String beforeOrderNo=cxEngineScheduleResult.getOrderNo();

        handleOrderNoSet.add(beforeOrderNo);
        //前规格信息缓存
        CxEngineScheduleResult beforeSpec=cxEngineScheduleResult;
        //Joran 2022-02-25 其他规格挑选
        if(suppleList.size()>1){ //还有剩余的规格需要进行增补
          /*  //计划增补时根据增补计划设置的顺序进行排序
            CxScheduleUtils.sortByPlanSort(suppleList);*/

            //遍历需要增补计划进行自动班次计划安排 start
            for(CxEngineScheduleResult scheduleResult:suppleList){
                if(handleOrderNoSet.contains(scheduleResult.getOrderNo())){
                    continue;
                }
                String orderNo=scheduleResult.getOrderNo();
                handleOrderNoSet.add(orderNo);
                beginClassIndex=nextAutoScheduleTaskSupple(machineCode,beforeSpec,cxAutoScheduleTaskListMap,scheduleResult,beginClassIndex,logDetail);

               //将当前的计划置为前置规格
                beforeSpec=scheduleResult;
            }
            //遍历需要增补计划进行自动班次计划安排 end
        }

        //获取机台最后一个规格
        machineLastSpecMap.put(machineCode,beforeSpec);
        return beginClassIndex;
    }

    /**
     *  先计算所有增补规格的计划总量
     * @param suppleList 增补计划列表
     * @param currentScheduleResult 在产规格
     */
    private void calcTaskTotalQty(String machineCode,List<CxEngineScheduleResult> suppleList, CxEngineScheduleResult currentScheduleResult,Double class3PlannedAvgAvailableLhShift,StringBuilder logDetail) {
        logDetail.append("机台【"+machineCode+"】开始进行增补计划列表的增补总量处理》》》").append(division);
        //获取默认设定单次安排班数
        String  specDimension=currentScheduleResult.getSpecDimension()==null?"":currentScheduleResult.getSpecDimension().toString();
        Double maxLhCount=maxLhShiftCount(currentScheduleResult,specDimension,class3PlannedAvgAvailableLhShift,class3PlannedAvgAvailableLhShift,logDetail);
        logDetail.append(StringUtils.format("当前获取到的默认硫化安排班数：{}",maxLhCount)).append(division);
        //执行增补量
        boolean isSupple=true;
        for(CxEngineScheduleResult cxEngineScheduleResult:suppleList){
            //如果是在产规格，则加总昨日三班+次一班+次二班总计划量
            if(currentScheduleResult.getOrderNo().equals(cxEngineScheduleResult.getOrderNo())){
                //排序为1的规格是直接获取白班计划量
                cxEngineScheduleResult.setSuppleTotalQty(cxEngineScheduleResult.getAfterClass3PlannedQty());
                logDetail.append(StringUtils.format("在产规格增补计划总量：{}",cxEngineScheduleResult.getAfterClass3PlannedQty())).append(division);
                continue;
            }

            if(isSupple){
                Double lhMachineQty=cxEngineScheduleResult.getLhMachineQty();
                isSupple=!(lhMachineQty>BigDecimal.valueOf(2).doubleValue());
                logDetail.append(StringUtils.format("后续规格是否需要进行增补：{}",isSupple)).append(division);

            }
            //不是增补则取默认安排班数乘以单班硫化量算总计划量
            if(!isSupple){
                //单班硫化量
                Integer singleShiftLhQty=cxEngineScheduleResult.getSingleShiftLhQty();
                int taskQty= (int) Math.ceil(maxLhCount * singleShiftLhQty);
                cxEngineScheduleResult.setSuppleTotalQty(taskQty);
                logDetail.append(StringUtils.format("结合班数和单班硫化量算出来的计划总量：{}",taskQty)).append(division);
            }else if(cxEngineScheduleResult.getClass1PlanQty()>0){ //原次一班有计划量
                //增补总量 = 白班计划量+原次1班计划量+原次2班计划量
                cxEngineScheduleResult.setSuppleTotalQty(cxEngineScheduleResult.getAfterClass3PlannedQty());
                logDetail.append(StringUtils.format("原次一班有计划安排，所以增补总量=：{}",cxEngineScheduleResult.getAfterClass3PlannedQty())).append(division);
            }else{//原次一班没有计划量
                //若原次1班没有计划量，则原次2班也是没有的；增补总量=白班计划量+原次1班计划量；
                cxEngineScheduleResult.setSuppleTotalQty(cxEngineScheduleResult.getAfterClass3PlannedTwoShiftQty());
                logDetail.append(StringUtils.format("原次一班没有计划安排，所以增补总量=：{}",cxEngineScheduleResult.getAfterClass3PlannedTwoShiftQty())).append(division);
            }

        }
    }

    /**
     * 自动增补后续不是当前机台在产规格，根据昨日三班可硫化班次最小的规格进行排载
     * @param machineCode 机台编号
     * @param beforeScheduleResult 前规格排程
     * @param scheduleResult 当前成型计划
     * @param logDetail 自动排程日志
     */
    private Integer nextAutoScheduleTaskSupple(String machineCode, CxEngineScheduleResult beforeScheduleResult,Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap, CxEngineScheduleResult scheduleResult,int beginClassIndex,StringBuilder logDetail) {
        logDetail.append("机台【"+machineCode+"】自动增补计划，非当前机台在产规格排程。").append(division);

        //规格更换自动增补任务量规格计划
         beginClassIndex=changeSpecAutoScheduleSupple(machineCode,cxAutoScheduleTaskListMap,beforeScheduleResult,scheduleResult,beginClassIndex,logDetail);

        return beginClassIndex;

    }

    /**
     * 规格切换后自动增补
     * @param machineCode 机台编号
     * @param cxAutoScheduleTaskListMap 前规格任务集合
     * @param scheduleResult 当前成型计划
     * @param beginClassIndex 开始班次
     * @param logDetail 自动排程日志
     * @return
     */
    private int changeSpecAutoScheduleSupple(String machineCode, Map<String, List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap, CxEngineScheduleResult beforeScheduleResult, CxEngineScheduleResult scheduleResult, int beginClassIndex, StringBuilder logDetail) {
        logDetail.append("【更换规格增补计划】机台【"+machineCode+"】硫化机小于1台，新成型计划自动增补开始。").append(division);
        String key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");

        //清空昨日三班、次一班、次二班计划量、原因分析
        CxScheduleUtils.cleanFromClass3PlannedQty(scheduleResult);
        logDetail.append("【更换规格增补计划】,增补规格计划中，将昨日三班、一班、二班、计划先清空。").append(division);
        //规格更换时自动增补任务集合
        List<CxAutoScheduleTask> changeSpecAutoSuppleList=new ArrayList<>();
        //胎胚代码
        String embryoCode=scheduleResult.getEmbryoCode();
        Integer remainMonthQty=getRealMonthRemainQty(scheduleResult);
        logDetail.append("【更换规格增补计划】下个规格月度剩余量：").append(remainMonthQty).append(division);
        if(remainMonthQty <= 0){
            logDetail.append("【更换规格增补计划】下个规格没有月度剩余量，当前规格不进行自动增补，月度剩余量：").append(remainMonthQty).append(division);
            return beginClassIndex;
        }
        //获取到增补计划总量
        Integer taskTotalQty= scheduleResult.getSuppleTotalQty();

        if(taskTotalQty <= BigDecimal.ZERO.intValue()){
            logDetail.append("【更换规格增补计划】没有获取到增补计划总量，当前计划总量=").append(taskTotalQty).append(division);
            return beginClassIndex;
        }

        //跟月度剩余量比对，如果比月度剩余量小的话直接安排总任务量
        taskTotalQty=(remainMonthQty>taskTotalQty?taskTotalQty:remainMonthQty);
        logDetail.append("【更换规格增补计划】当前计划总量=").append(taskTotalQty).append(division);

        Double remainTime=machineShiftHourMap.get(key);
        logDetail.append(StringUtils.format("当前机台编号：{}，当前班次下标：{}，获取到的剩余时长={}",machineCode,beginClassIndex,remainTime)).append(division);

        //记录已经安排的计划量
        Integer continuePlanQty=0;

        //还有剩余时间 需要扣除掉规格更换时间 start
        if(remainTime >= BigDecimal.ZERO.doubleValue()){
            //获取新规格定额信息
            Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(machineCode,embryoCode,scheduleResult.getBomDataVersion(),logDetail);

            //一个小时生产多少
            BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR));
            logDetail.append(StringUtils.format("【更换规格增补计划】，计算小时产量={}",hourCountBig)).append(division);

            //后规格胎胚+施工版本
            String afterKey=GenerageMapKeyUtils.createMapKey(embryoCode,scheduleResult.getBomDataVersion());

            //前规格胎胚+施工版本
            String beforeKey=GenerageMapKeyUtils.createMapKey(beforeScheduleResult.getEmbryoCode(),beforeScheduleResult.getBomDataVersion());

            //前后规格更换工装时长
            Double changeSpecTime=changeSpecTime(afterKey,beforeKey,logDetail);

            //二分之一更换工装时间
            Double halfChangeSpecTime=BigDecimal.valueOf(changeSpecTime).divide(BigDecimal.valueOf(2)).doubleValue();

            //班次剩余时间与二分之一的工装更换时间逻辑 start
            if(remainTime <= halfChangeSpecTime){
                //更新班次剩余时长为0
                updateMachineShiftHourMap(machineCode,beginClassIndex, BigDecimal.ZERO.doubleValue());

                //前规格剩余时间小于更换工装工时一半，则下个班扣除整个更换工装时长
                beginClassIndex+=1;
                ClassEnums cls =ClassEnums.getClassEnums(beginClassIndex);

                logDetail.append("【更换规格增补计划】剩余时间:"+remainTime+",二分之一更换工装时间："+changeSpecTime+"比二分之一更换工装时间短，不再进行当前班次排产，直接进入下一个班次").append(division);
                if(cls==null){
                    return beginClassIndex;
                }

                //Joran 2021-12-24 更换工装在前规格计划量当前规格扣减
                key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");

                //获取班次最新班次剩余时间(是新班次的话等于班次总时长)
                remainTime=machineShiftHourMap.get(key);

                //剩余时间(扣除工装更换时长)
                BigDecimal shiftRemainTimeBig=BigDecimal.valueOf(remainTime-changeSpecTime);

                logDetail.append("【更换规格增补计划】前规格换工装当前班扣除更换工装剩余时：").append(shiftRemainTimeBig).append(division);

                BigDecimal currentPlanQty=shiftRemainTimeBig.multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN);

                logDetail.append("【更换规格增补计划】规格计算可投产计划量：").append(currentPlanQty).append(division);

                Integer currentShiftPlanQty=currentPlanQty.intValue();

                //增补总量与班次可安排计划量逻辑校验 start
                if(taskTotalQty >= currentShiftPlanQty) { //月度剩余量大于当班计划量
                    taskTotalQty -= currentShiftPlanQty;
                    continuePlanQty += currentShiftPlanQty;//记录安排的计划量

                    //创建自动处理任务
                    CxAutoScheduleTask autoSuppleTask=commonCacheService.createClassShiftRemainQty(scheduleResult,beginClassIndex,currentShiftPlanQty,taskTotalQty,continuePlanQty,shiftRemainTimeBig.doubleValue());
                    machineAutoScheduleSort+=1;
                    autoSuppleTask.setMachineAutoScheduleSort(machineAutoScheduleSort);
                    changeSpecAutoSuppleList.add(autoSuppleTask);

                    //更新计划量 月度剩余量 班次计划领 原因分析等
                    updatePlanQtyAndRemainTimeAndQty(machineCode,scheduleResult,beginClassIndex,BigDecimal.ZERO.doubleValue(),currentShiftPlanQty,"",logDetail);

                    //继续下一个班次
                    beginClassIndex+=1;

                    //开启剩余任务量进行自动安排直到班次排满或者剩余任务量排完
                    beginClassIndex=autoSuppleRemainTask(machineCode,scheduleResult,taskTotalQty,continuePlanQty,beginClassIndex,machineQuota,changeSpecAutoSuppleList,logDetail);

                }else{
                    currentShiftPlanQty=taskTotalQty;
                    taskTotalQty=0;//一次性安排完
                    continuePlanQty+=currentShiftPlanQty;//记录安排的计划量
                    //重新计算班次剩余时间
                    Double shiftRemainTime=commonCacheService.getClassShiftRemainTime(remainTime,machineQuota,currentShiftPlanQty);
                    CxAutoScheduleTask autoSuppleTask=commonCacheService.createClassShiftRemainQty(scheduleResult,beginClassIndex,currentShiftPlanQty,taskTotalQty,continuePlanQty,shiftRemainTime);
                    machineAutoScheduleSort+=1;
                    autoSuppleTask.setMachineAutoScheduleSort(machineAutoScheduleSort);
                    changeSpecAutoSuppleList.add(autoSuppleTask);
                    logDetail.append("【更换规格增补计划】续作时月度剩余量为0，直接排完计算剩余时间，当前任务为："+scheduleResult.toString()).append(division);

                    //更新计划量 月度剩余量 班次计划领 原因分析等
                    updatePlanQtyAndRemainTimeAndQty(machineCode,scheduleResult,beginClassIndex,shiftRemainTime,currentShiftPlanQty,"",logDetail);

                    return beginClassIndex;
                }
                //增补总量与班次可安排计划量逻辑校验 end

            }else{
                String changeMoldAnalysis="";
                //实际剩余时间
                BigDecimal remainTimeBig=null;

                //剩余时长大于一半更换工装时长，是否大于更换工装时长逻辑 start
                if(remainTime>=changeSpecTime) {
                    remainTimeBig=BigDecimal.valueOf(remainTime-changeSpecTime); //剩余时间
                    if(scheduleResult.getNewSpecFlag()&&!scheduleResult.getMarkNewSpecAnalysisFlag()){
                        //有换工装开班就走换工装
                        scheduleResult.setMarkNewSpecAnalysisFlag(true);
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
                    }else{
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
                    }
                    CxScheduleUtils.setChangeMoldConditionByClassIndex(scheduleResult,beginClassIndex,changeMoldAnalysis,logDetail);
                }else{ //不够则扣除二分之一的更换工装的时长
                    //更新班次剩余时长为0
                    updateMachineShiftHourMap(machineCode,beginClassIndex, BigDecimal.ZERO.doubleValue());

                    //Joran 2021-12-29前一个班次标记为更换工装，后个班次特殊处理不进行标记
                    if(scheduleResult.getNewSpecFlag()&&!scheduleResult.getMarkNewSpecAnalysisFlag()){
                        //有换工装开班就走换工装
                        scheduleResult.setMarkNewSpecAnalysisFlag(true);
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
                    }else{
                        changeMoldAnalysis=I18nUtil.getMessage("cx.engine.auto.analysis.changeMold.title");
                    }
                    CxScheduleUtils.setChangeMoldConditionByClassIndex(scheduleResult,beginClassIndex,changeMoldAnalysis,logDetail);

                    //继续往下一个班次
                    beginClassIndex+=1;
                    ClassEnums nextCls =ClassEnums.getClassEnums(beginClassIndex);
                    if(nextCls==null){
                        logDetail.append("【班次剩余时间不足】超过当天排班班次数，结束续排").append(division);
                        return beginClassIndex;
                    }
                    //下一个班次的话时间要扣除二分之一的更换工装时间
                    key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");
                    remainTime=machineShiftHourMap.get(key);
                    remainTimeBig=BigDecimal.valueOf(remainTime-halfChangeSpecTime); //剩余时间
                    //剩余时长如果大于更换工装的时长则扣除更换工装时长
                }

                //更新班次剩余时长
                updateMachineShiftHourMap(machineCode,beginClassIndex, remainTimeBig.doubleValue());

                //开启剩余任务量进行自动安排直到班次排满或者剩余任务量排完
                beginClassIndex=autoSuppleRemainTask(machineCode,scheduleResult,taskTotalQty,continuePlanQty,beginClassIndex,machineQuota,changeSpecAutoSuppleList,logDetail);
            }
            //班次剩余时间与二分之一的工装更换时间逻辑 end
        }
        //还有剩余时间 需要扣除掉规格更换时间 end
        //Joran 2022-03-15 其他增补规格计算可硫化班次
        CxScheduleUtils.calcAllClassAvailableLhShift(scheduleResult);
        if(StringUtils.isNotEmpty(changeSpecAutoSuppleList)){
            cxAutoScheduleTaskListMap.put(embryoCode,changeSpecAutoSuppleList);
            //补充自动排程的原因分析库存地点，任务顺序等
            handleTaskList(scheduleResult,cxAutoScheduleTaskListMap,changeSpecAutoSuppleList,logDetail);
        }

        return  beginClassIndex;
    }

    /**
     * 机台尚有任务剩余量，进行后续班次任务增补
     * @param machineCode 当前机台
     * @param scheduleResult 当前成型计划
     * @param taskTotalQty 剩余增补的计划任务量
     * @param beginClassIndex 开始班次下标
     * @param machineQuota 当前规格在机台定额数据
     * @param changeSpecAutoSuppleList 自动增补任务列表
     * @return
     */
    private int autoSuppleRemainTask(String machineCode, CxEngineScheduleResult scheduleResult, Integer taskTotalQty,Integer continuePlanQty, int beginClassIndex, Integer machineQuota, List<CxAutoScheduleTask> changeSpecAutoSuppleList,StringBuilder logDetail) {
        logDetail.append(StringUtils.format("【更换规格增补计划】机台【{}】，当前规格胎胚：{}，规格定额：{}，剩余任务量：{}，自动进行增补安排",machineCode,scheduleResult.getEmbryoCode(),machineQuota,taskTotalQty)).append(division);
        //遍历进行后续排次任务自动增补逻辑 start
        String cxOrderNo=scheduleResult.getOrderNo();

        //一个小时生产多少
        BigDecimal hourCountBig=BigDecimal.valueOf(machineQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR));

        String scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",scheduleResult.getScheduleDate());
        while(beginClassIndex <= CxEngineConstants.TASK_MAX_CLASS_SHIFT && taskTotalQty > 0){

            String key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");
            //获取当前班次剩余时长
            Double shiftRemainHour= machineShiftHourMap.get(key);

            //当前可排计划量
            int currentPlanQty=BigDecimal.valueOf(shiftRemainHour).multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN).intValue();
            Integer currentShiftPlanQty=currentPlanQty;
            CxAutoScheduleTask autoScheduleTask=CxScheduleUtils.createScheduleTask(scheduleResult,beginClassIndex,taskTotalQty,continuePlanQty,shiftRemainHour);
            //机台任务顺序递增
            machineAutoScheduleSort+=1;
            autoScheduleTask.setMachineAutoScheduleSort(machineAutoScheduleSort);
            autoScheduleTask.setScheduleDate(scheduleDateStr);

            //工单号生成
            autoScheduleTask.setCxOrderNo(cxOrderNo);
            //剩余任务量大于机台定额逻辑 start
            if(taskTotalQty >= currentShiftPlanQty){
              autoScheduleTask.setCurrentShiftPlanQty(currentShiftPlanQty); //班次计划量
              taskTotalQty -= currentShiftPlanQty;//更新剩余任务量
              continuePlanQty+=currentShiftPlanQty;//任务量累加
              autoScheduleTask.setRemainTaskQty(taskTotalQty);
              autoScheduleTask.setRemainTime(CxEngineConstants.ZERO);//班次没有剩余时间
              autoScheduleTask.setRemainTaskQty(taskTotalQty-continuePlanQty);//任务剩余量
              //更新班次剩余时间、计划量、原因分析、月度剩余量等
              updatePlanQtyAndRemainTimeAndQty(machineCode,scheduleResult,beginClassIndex,BigDecimal.ZERO.doubleValue(),currentShiftPlanQty,"",logDetail);
              beginClassIndex++;
            }else{
              currentShiftPlanQty = taskTotalQty;
              autoScheduleTask.setCurrentShiftPlanQty(currentShiftPlanQty); //班次计划量
              continuePlanQty+=currentShiftPlanQty;//任务量累加
              taskTotalQty =0 ; //增补完毕
              //重新计算班次剩余时间
              Double shiftRemainTime=commonCacheService.getClassShiftRemainTime(shiftRemainHour,machineQuota,currentShiftPlanQty);
              //更新计划量 月度剩余量 班次计划领 原因分析等
              updatePlanQtyAndRemainTimeAndQty(machineCode,scheduleResult,beginClassIndex,shiftRemainTime,currentShiftPlanQty,"",logDetail);
            }
            changeSpecAutoSuppleList.add(autoScheduleTask);
            //剩余任务量大于机台定额逻辑 end

        }
        //遍历进行后续排次任务自动增补逻辑 end
        return beginClassIndex;
    }

    /**
     *
     * @param machineCode 机台编号
     * @param scheduleResult 当前成型计划
     * @param shiftRemainTime 当前班次剩余时间
     * @param currentShiftPlanQty 当前班次计划量
     * @param currentShiftAnalysis 当前班次原因分析
     */
    private void updatePlanQtyAndRemainTimeAndQty(String machineCode, CxEngineScheduleResult scheduleResult,Integer beginClassIndex, Double shiftRemainTime, Integer currentShiftPlanQty, String currentShiftAnalysis,StringBuilder logDetail) {
        //设置班次计划量
        CxScheduleUtils.setClassShiftPlanQtyByShiftIndex(scheduleResult,beginClassIndex,currentShiftPlanQty);
        logDetail.append(StringUtils.format("【设置排程】,设置班次计划量，班次下标：{}，计划量：{}",beginClassIndex,currentShiftPlanQty)).append(division);
        //更新班次剩余时间
        updateMachineShiftHourMap(machineCode,beginClassIndex,shiftRemainTime);
        logDetail.append(StringUtils.format("【设置排程】,更新剩余时长，班次下标：{}，剩余时长：{}",beginClassIndex,shiftRemainTime)).append(division);

        //设置班次原因分析
        ClassEnums cls=ClassEnums.getClassEnums(beginClassIndex);
        if(cls!=null&&StringUtils.isNotEmpty(currentShiftAnalysis)){
            CxScheduleUtils.setClassAnalysis(scheduleResult,cls,currentShiftAnalysis);
            logDetail.append(StringUtils.format("【设置排程】,设置原因分析，班次下标：{}，原因分析内容：{}",beginClassIndex,currentShiftAnalysis)).append(division);
        }
        //更新月度剩余量信息
        updateRealMonthRemainQtyMap(scheduleResult,currentShiftPlanQty,logDetail);
    }

    /**
     * 没有找到在产的机台规格从所有计划中计算前日三班的可硫化班次，获取最小的优先进行投产
     * @param machineCode
     * @param suppleList
     * @param logDetail
     * @return
     */
    private CxEngineScheduleResult getLastDayClass3MinAvailableShift(String machineCode, List<CxEngineScheduleResult> suppleList, StringBuilder logDetail) {
        logDetail.append("机台【"+machineCode+"】没有在产规格，进行计算可硫化班数，获取前日三班可硫化班数最小的进行投产。").append(division);
        for (CxEngineScheduleResult cxEngineScheduleResult:suppleList){
            //计算各个班可硫化班次
            CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
            logDetail.append(StringUtils.format("当前工单号：{}，获取到的前日三班的可硫化班数：{}。",cxEngineScheduleResult.getOrderNo(),cxEngineScheduleResult.getClass3PlannedAvailableLhShift())).append(division);
        }
        //根据昨日三班的可硫化班次进行排序
        CxScheduleUtils.taskSortAscByAvailableLhShift(suppleList,BigDecimal.ZERO.intValue());
        CxEngineScheduleResult  cxEngineScheduleResult=suppleList.get(0);
        logDetail.append(StringUtils.format("获取到的进行优先投产的规格信息：【{}】",JSON.toJSONString(cxEngineScheduleResult))).append(division);
        return cxEngineScheduleResult;
    }


    /**
     * 挑选到三班规格进行昨日三班计划调整(主要是昨日计划增补)
     * @param cxAutoScheduleTaskListMap 自动排程任务结果集合
     * @param currentSpec 当前规格
     * @param machineCode 机台编号
     */
    private Integer scheduleTaskBeginLastDayClass3Plan(Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap,CxEngineScheduleResult currentSpec,int beginClassIndex,String machineCode,StringBuilder logDetail){
        List<CxAutoScheduleTask> cxAutoScheduleTaskList=new ArrayList<>();
        //从前日三班开始进行安排

        //获取到需要增补的总计划量
        int taskTotalQty=currentSpec.getSuppleTotalQty();

        //胎胚代码
        String embryoCode=currentSpec.getEmbryoCode();

        //施工版本信息
        String bomDataVersion=currentSpec.getBomDataVersion();

        //获取工单号
        String cxOrderNo=currentSpec.getOrderNo();

        //获取排程日期
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",currentSpec.getScheduleDate());

        //获取机台定额
        Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(machineCode,embryoCode,bomDataVersion,logDetail);
        logDetail.append("【处理昨日三班计划】机台编号定额：【"+machineQuota+"】.").append(division);

        //获取规格月度剩余量
        int monthRemainQty=getRealMonthRemainQty(currentSpec);

        //清空昨日三班、次一班、次二班计划量、原因分析
        CxScheduleUtils.cleanFromClass3PlannedQty(currentSpec);

        if(monthRemainQty<=0){
            logDetail.append(StringUtils.format("【处理昨日三班计划】增补机台前日三班计划时，当前胎胚代码：{}，月度剩余量为0，当前规格跳过不自动排程",embryoCode)).append(division);
            return beginClassIndex;
        }

        //如果计划量小于月度剩余量时，则只能按最小的月度剩余量进行排产
        if(monthRemainQty<taskTotalQty){
            taskTotalQty=monthRemainQty;
        }
        while(beginClassIndex <= ClassEnums.CLASS_FIVE.getClassIndex() && taskTotalQty>0){
            String key=GenerageMapKeyUtils.createMapKey(machineCode,beginClassIndex+"");
            Double shiftHour=machineShiftHourMap.get(key);
            CxAutoScheduleTask autoScheduleTask=CxScheduleUtils.createScheduleTask(currentSpec,beginClassIndex,taskTotalQty,0,shiftHour);
            //同一个机台所有任务自动排程任务顺序
            machineAutoScheduleSort+=1;
            autoScheduleTask.setScheduleDate(scheduleDateStr);
            autoScheduleTask.setCxOrderNo(cxOrderNo);

            //设置同一个机台任务的顺序
            autoScheduleTask.setMachineAutoScheduleSort(machineAutoScheduleSort);
            //从前日三班开始进行规格设置计划量start

            //机台定额数大于总任务量时，直接安排完计算更新班次剩余时间
            int currentPlanQty=BigDecimal.ZERO.intValue();
            if(machineQuota > taskTotalQty){
                currentPlanQty=taskTotalQty;
                taskTotalQty=0;
                logDetail.append("【机台定额大于增补总量】当前班次可排计划量=：【"+currentPlanQty+"】").append(division);

                //更新班次剩余时间
                Double remainTime=commonCacheService.getClassShiftRemainTime(shiftHour,machineQuota,currentPlanQty);
                logDetail.append("【机台定额大于增补总量】班次剩余时长=：【"+remainTime+"】").append(division);

                //更新班次剩余时间、计划量、原因分析、月度剩余量等
                updatePlanQtyAndRemainTimeAndQty(machineCode,currentSpec,beginClassIndex,remainTime,currentPlanQty,"",logDetail);
                autoScheduleTask.setCurrentShiftPlanQty(currentPlanQty); //班次计划量
                autoScheduleTask.setRemainTaskQty(taskTotalQty);//剩余任务量
                autoScheduleTask.setRemainTime(remainTime);//班次剩余时间
                cxAutoScheduleTaskList.add(autoScheduleTask);
            }else{
                currentPlanQty=machineQuota;

                //计划量=定额
                logDetail.append("【机台定额小于增补总量】当前班次可排计划量=：【"+currentPlanQty+"】").append(division);
                taskTotalQty-=machineQuota;//任务量扣掉定额

                //计划量=定额
                logDetail.append("【机台定额小于增补总量】剩余可排总计划量=：【"+taskTotalQty+"】").append(division);

                //班次占满
                //更新班次剩余时间、计划量、原因分析、月度剩余量等
                updatePlanQtyAndRemainTimeAndQty(machineCode,currentSpec,beginClassIndex,BigDecimal.ZERO.doubleValue(),currentPlanQty,"",logDetail);

                beginClassIndex+=1;//班次往下移动
                autoScheduleTask.setCurrentShiftPlanQty(currentPlanQty); //班次计划量
                autoScheduleTask.setRemainTaskQty(taskTotalQty);//剩余任务量
                autoScheduleTask.setRemainTime(CxEngineConstants.ZERO);//班次没有剩余时间
                cxAutoScheduleTaskList.add(autoScheduleTask);

            }
            //从前日三班开始进行规格设置计划量end
        }

        //重新计算可硫化班次
        CxScheduleUtils.calcAllClassAvailableLhShift(currentSpec);
        if(StringUtils.isNotEmpty(cxAutoScheduleTaskList)){
            cxAutoScheduleTaskListMap.put(embryoCode,cxAutoScheduleTaskList);
            //补充自动排程的原因分析库存地点，任务顺序等
            handleTaskList(currentSpec,cxAutoScheduleTaskListMap,cxAutoScheduleTaskList,logDetail);
        }
        return beginClassIndex;

    }

    /**
     * 处理自动排程补充原因分析以及收尾处理等
     * @param currentSpec
     * @param cxAutoScheduleTaskListMap
     * @param logDetail
     */
    private void handleTaskList(CxEngineScheduleResult currentSpec,Map<String,List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap,List<CxAutoScheduleTask> currentAutoTaskList, StringBuilder logDetail) {
        Map<ClassEnums, Integer> classSortMap = new HashMap<>();

        //创建班次任务顺序
        commonCacheService.buildClassSort(cxAutoScheduleTaskListMap, classSortMap);

        //根据安排的任务列表获取最大的班次
        CxScheduleUtils.sortDescByScheduleTaskClassShift(currentAutoTaskList);
        for (CxAutoScheduleTask cxAutoScheduleTask : currentAutoTaskList) {
            Integer classShift = cxAutoScheduleTask.getClassShift();//获取安排的班次
            ClassEnums cls = ClassEnums.getClassEnums(classShift);
            if(cls==null){
                continue;
            }
            int sort = classSortMap.get(cls);
            switch (cls) {
                case CLASS_ONE:
                    currentSpec.setClass1Sort(sort);
                    break;
                case CLASS_TWO:
                    currentSpec.setClass2Sort(sort);
                    break;
                case CLASS_THREE:
                    currentSpec.setClass3Sort(sort);
                    break;
                case CLASS_FOUR:
                    currentSpec.setClass4Sort(sort);
                    break;
                case CLASS_FIVE:
                    currentSpec.setClass5Sort(sort);
                    break;
                default:
                    break;
            }
        }

        //构建更换工装原因分析
        if(!currentSpec.getNewSpecFlag()){
            buildChangeMoldAnalysis(currentSpec,currentAutoTaskList,logDetail);
        }else{
            //构建更换工装开班
            bulidNewSpecChangeMoldAnalysis(currentSpec,currentAutoTaskList,logDetail);
        }

        //收尾原因分析处理
        buildCloseOutAnalysis(currentSpec,currentAutoTaskList,logDetail);
        //库存地点设置
        setResultStorageLocation(currentSpec,logDetail);
    }

    /**
     * 根据昨天白班、次一、次二班的计划量 来分组
     * @param lastDayRemainResultList
     * @param suppleList
     * @param remainList
     */
    private void groupListByPlanQty(List<CxEngineScheduleResult> lastDayRemainResultList, List<CxEngineScheduleResult> suppleList, List<CxEngineScheduleResult> remainList) {
        //计划增补时根据增补计划设置的顺序进行排序
        CxScheduleUtils.sortByPlanSort(lastDayRemainResultList);
        for (CxEngineScheduleResult cxEngineScheduleResult:lastDayRemainResultList){
            if(cxEngineScheduleResult.getClass3PlannedQty()>0){ //白班计划量存在才进行计划增补列表
                suppleList.add(cxEngineScheduleResult);
            }else if(cxEngineScheduleResult.getPlanSort()==1){//Joran 2022-03-18 顺序为第一的规格且白班的计划量为0时也放入增补计划列表中
                suppleList.add(cxEngineScheduleResult);
            } else{
                //清空昨日三班、次一班、次二班计划量、原因分析
                CxScheduleUtils.cleanFromClass3PlannedQty(cxEngineScheduleResult);
                remainList.add(cxEngineScheduleResult);
            }
        }
    }

    /**
     * 从昨天三班计划（class3PlannedQty）开始安排
     * @param lastDayRemainResultList
     */
   /* private CxEngineScheduleResult schdeuleTaskBeginClass3Planned(String machineCode,List<CxEngineScheduleResult> lastDayRemainResultList,StringBuilder logDetail) {
        logDetail.append("成型机台编号【").append(machineCode).append("】,开始进行昨日白班计划重新安排。").append(division);

        //1、根据mes中获取的在产规格进行优先安排昨日三班计划
        CxEngineScheduleResult class3PlannedTask=getSchedulePlanByFirst(machineCode,lastDayRemainResultList,logDetail);
        if(class3PlannedTask!=null){
            return class3PlannedTask;
        }
        return null;

    }*/

    /**
     *  挑选第一个投产规格
     //* @param lastDayRemainResultList
     * @return
     */
   /* private CxEngineScheduleResult getSchedulePlanByFirst(String machineCode,List<CxEngineScheduleResult> lastDayRemainResultList,StringBuilder logDetail) {
        logDetail.append("成型机台编号【").append(machineCode).append("】,开始进行优先排产规格选择。").append(division);
        CxEngineScheduleResult firstScheduleTask=null;
        //1.从在产规格中选取优先获取
        logDetail.append("第一步获取所有机台在产的规格集合").append(JSON.toJSONString(machineInProductMap)).append(division);
        if(StringUtils.isNotEmpty(machineInProductMap)&&machineInProductMap.containsKey(machineCode)){
            //获取到在产的胎胚代码
             String inProductEmbryoCode=machineInProductMap.get(machineCode);
            logDetail.append("当前机台获取到的在产胎胚代码【").append(inProductEmbryoCode).append("】").append(division);
             for(CxEngineScheduleResult cxEngineScheduleResult:lastDayRemainResultList){
                 if(CxEngineConstants.TO_PRODUCT_NO.equals(cxEngineScheduleResult.getToProduct())){
                     continue;
                 }
                 if(StringUtils.equals(cxEngineScheduleResult.getEmbryoCode(),inProductEmbryoCode)){
                     firstScheduleTask=cxEngineScheduleResult;
                     logDetail.append("获取到第一个安排的规格为").append(JSON.toJSONString(firstScheduleTask)).append(division);
                     break;
                 }
             }
        }
        return firstScheduleTask;
    }*/


    private void initScheduleData(List<CxPlanProductStatus> cxPlanProductStatusList,List<MdmMonthProdPlan> mdmMonthProdPlanList,Date scheduleDate,StringBuilder logDetail) {
        logDetail.append("==================自动排程数据初始化过程开始==================").append(division);
        scheduleLimitMap = cxEngineScheduleLimitService.getCxScheduleLimitMachineCodeMap();
        //logDetail.append("初始化获取排产限制数据集：").append(toJSONString(scheduleLimitMap)).append(division);
        cxParamsMap = commonCacheService.loadCxParamsMap();
        logDetail.append("初始化获取成型工序参数数据集：").append(toJSONString(cxParamsMap)).append(division);
        //规格限制作业机台
        specifyMachineYesMap = cxEngineSpecifyMachineService.getAllCxSpecifyMachineInfo(CxEngineConstants.SPECIFY_JOB_TYPE_YES);
        //logDetail.append("初始化获取规格限制作业机台数据集：").append(toJSONString(specifyMachineYesMap)).append(division);
        //规格不可作业机台
        specifyMachineNoMap = cxEngineSpecifyMachineService.getAllCxSpecifyMachineInfo(CxEngineConstants.SPECIFY_JOB_TYPE_NO);
        //logDetail.append("初始化获取规格不可作业机台数据集：").append(toJSONString(specifyMachineNoMap)).append(division);
        //加载全部胎胚的施工信息
        engineConstructionInfoMap = commonCacheService.loadEngineConstructionMapFromRedis();

        //加载耗损率相关数据
        cxMachineLossRateMap = cxEngineLossSettingService.loadCxMachineLossRateMap();
        //待投产规格列表
        this.cxPlanProductStatusList = cxPlanProductStatusList;
        //月度计划汇总明细数据
        if (StringUtils.isNotEmpty(mdmMonthProdPlanList)) {
            initSapEmbryoCodeMap(mdmMonthProdPlanList, logDetail);

            //初始化同胎胚月计划需求量
            initEmbryoCodeMonthPlanMap(mdmMonthProdPlanList);
        }
        monthRemainQtyMap = new ConcurrentHashMap<>();//重新初始化
        //Joran 2022-01-08 初始化投产班次调整设定相关数据start
        //1.轮胎类型库存限制设定
        List<CxEngineProductStockLimit> productStockLimitList = cxEngineProductShiftLimitService.selectCxProductShiftStockLimitList(new CxEngineProductStockLimit());
        if (StringUtils.isNotEmpty(productStockLimitList)) {//按轮胎类型来进行区分
            cxEngineProductStockLimitListMap = productStockLimitList.stream().collect(Collectors.groupingBy(CxEngineProductStockLimit::getType));
        } else {
            cxEngineProductStockLimitListMap = new HashMap<>();
        }
        //2.同寸口一班平均可硫化班次限制设定
        List<CxEngineProductDimensionLimit> cxEngineProductDimensionLimitList = cxEngineProductShiftLimitService.selectCxEngineProductDimensionLimitList(new CxEngineProductDimensionLimit());
        if (StringUtils.isNotEmpty(cxEngineProductDimensionLimitList)) {//按轮胎类型来进行区分
            cxEngineProductDimensionLimitListMap = cxEngineProductDimensionLimitList.stream().collect(Collectors.groupingBy(CxEngineProductDimensionLimit::getSpecDimension));
        } else {
            cxEngineProductDimensionLimitListMap = new HashMap<>();
        }
        //3、同机台平均可硫化班数班次限制设定
        cxEngineProductMachineLimitList = cxEngineProductShiftLimitService.selectCxEngineProductMachineLimitList(new CxEngineProductMachineLimit());
        //Joran 2022-01-08 初始化投产班次调整设定相关数据end

        //Joran 2022-01-18初始化话规格投产模数信息start
        sapSpecMoldUseList = cxEngineSapSpecMoldUseService.selectSapSpecMoldUseList(new CxEngineSapSpecMoldUse());

        //Joran 2022-02-24 成型机台在产规格初始化获取
        //machineInProductMap=cxEngineCommonService.cxMachineInProductSpecMap(scheduleDate);
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
       // logDetail.append("初始化月度计划汇总分组后集合").append(toJSONString(sapEmbryoCodeProdPlanMap)).append(division);
    }

    /**
     * 初始化同胎胚月计划需求量
     * @param mdmMonthProdPlanList
     */
    private void initEmbryoCodeMonthPlanMap(List<MdmMonthProdPlan> mdmMonthProdPlanList) {
        embryoCodeMonthPlanQtyMap=new ConcurrentHashMap<>();
        //根据组合键进行分组
        embryoCodeMonthPlanQtyMap = mdmMonthProdPlanList.stream().collect(Collectors.groupingBy(m->m.getEmbryoCode(),Collectors.collectingAndThen(
                Collectors.toList(),m1->{return m1.stream().mapToInt(MdmMonthProdPlan::getMonthTotalPlanQty).sum();})
        ));
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
     * 初始化班次可用时间
     * @param machineCode
     */
    private void initShiftHourMap(String machineCode,StringBuilder logDetail) {
        logDetail.append("=============初始化机台各个班次的可用时间============").append(division);
        machineShiftHourMap=new HashMap<>();
        //Joran 2022-02-25 进行前日三班班次时长初始化start
        String key=GenerageMapKeyUtils.createMapKey(machineCode,BigDecimal.ZERO.toString());
        if(!machineShiftHourMap.containsKey(key)){
            machineShiftHourMap.put(key, CxEngineConstants.CLASS_SHIFT_HOUR);
        }
        //Joran 2022-02-25 进行前日三班班次时长初始化end
        for (ClassEnums cls : ClassEnums.values()) {
            key=GenerageMapKeyUtils.createMapKey(machineCode,cls.getClassIndex()+"");
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
     * 初始化各个规格的月度剩余量
     * @param lastDayScheduleResultList
     */
    private void initMonthRemainQtyMap(List<CxEngineScheduleResult> lastDayScheduleResultList,StringBuilder logDetail) {
        logDetail.append("【新成型自动排程前进行初始化各个规格月度剩余量】").append(division);
        //Joran 2021-09-06 提示收尾数量工序参数设置值
        Integer closeOutNumber=commonCacheService.getCloseOutTipSetting(cxParamsMap);
        //Joran 2022-01-07 月度剩余量低于设定值后不进行计划安排，to_product标记为no
        for (CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            String embryoCode=cxEngineScheduleResult.getEmbryoCode();
            Integer monthQty = embryoCodeMonthPlanQtyMap.get(embryoCode);
            Integer unProductCount= commonCacheService.getUnProductMonthRemainQty(monthQty==null ? 0:monthQty,cxParamsMap);
            //月度剩余量
            Integer remainMonthQty=0;

            if(!monthRemainQtyMap.containsKey(embryoCode)){
                remainMonthQty=cxEngineScheduleResult.getMonthRemainQty();
            }else{
                remainMonthQty=monthRemainQtyMap.get(embryoCode);
            }

            if(remainMonthQty<=0){
                monthRemainQtyMap.put(embryoCode,0);
            }else{
                //Joran 2021-09-06 扣除三班计划收尾提示start
                if(remainMonthQty<=closeOutNumber){
                    cxEngineScheduleResult.setMarkCloseOutTip(CxEngineConstants.CLOSE_OUT_TIP_YES);
                    logDetail.append(StringUtils.format("月度剩余量，需要进行收尾提示标记,胎胚代码：【{}】",cxEngineScheduleResult.getEmbryoCode())).append(division);
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
            logDetail.append("【初始化月度剩余量】，SAP品号："+cxEngineScheduleResult.getSapCode()+",胎胚代码："+embryoCode+",月度剩余量："+remainMonthQty).append(division);

        }
    }

    /***
     * 更新月度剩余量
     * @param cxEngineScheduleResult
     * @param currentPlanQty
     */
    private void updateRealMonthRemainQtyMap(CxEngineScheduleResult cxEngineScheduleResult,Integer currentPlanQty,StringBuilder logDetail) {
        String embryoCode=cxEngineScheduleResult.getEmbryoCode();
        Integer remainMonthQty=cxEngineScheduleResult.getMonthRemainQty();
        if(monthRemainQtyMap.containsKey(embryoCode)){
            remainMonthQty=monthRemainQtyMap.get(embryoCode);
        }
        //Joran 2021-11-02 新投产规格需要扣减掉当前班次的计划量后再进行缓存
        remainMonthQty-=currentPlanQty;//扣掉班次计划量
        monthRemainQtyMap.put(embryoCode,remainMonthQty);//新投产规格直接缓存
        logDetail.append("【更新月度剩余量】，规格SAP品号："+cxEngineScheduleResult.getSapCode()+"胎胚代码："+embryoCode+",月度剩余量："+remainMonthQty).append(division);
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
        //最小班次的前班次如果有任务量则证明是自己则不换工装
        ClassEnums cls=ClassEnums.getClassEnums(minClassShiftTask.getClassShift());
        if(cls==null){
            logDetail.append("当前处理的是昨日三班，不需要进行更换工装原因分析").append(division);
            return;
        }
        Integer beforeClassPlanQty=CxScheduleUtils.getBeforeClassPlanQty(cxEngineScheduleResult,cls);
        if(beforeClassPlanQty>0&&ClassEnums.CLASS_ONE.equals(cls)){//如果是1班的话直接根据前规格是否有量来判断
            logDetail.append("前规格是同规格不进行工装更换原因分析").append(division);
            return;
        }else if(beforeClassPlanQty>0&&!ClassEnums.CLASS_ONE.equals(cls)){
            //如果前后班次顺序一样则标识顺序来，不进行原因分析标识
            if(CxScheduleUtils.getAnalysisFlag(cxEngineScheduleResult,cls)){
                logDetail.append("前规格是同规格不进行工装更换原因分析").append(division);
                return;
            }
        }
        setChangeMoldCondition(cxEngineScheduleResult,cls,changeMoldAnalysis,logDetail);
    }

    /**
     * 依据条件判断是否需要进行更换工装原因分析
     * @param cxEngineScheduleResult
     * @param cls
     * @param changeMoldAnalysis
     */
    private void setChangeMoldCondition(CxEngineScheduleResult cxEngineScheduleResult, ClassEnums cls, String changeMoldAnalysis,StringBuilder logDetail) {
        logDetail.append(StringUtils.format("当前规格胎胚：【{}】，进行更换工装原因分析。班次下标：【{}】，原因分析：【{}】",cxEngineScheduleResult.getEmbryoCode(),cls.getClassIndex(),changeMoldAnalysis)).append(division);
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
     * 更换工装开班原因分析
     * @param cxEngineScheduleResult
     * @param preScheduleTaskList
     */
    private void bulidNewSpecChangeMoldAnalysis(CxEngineScheduleResult cxEngineScheduleResult, List<CxAutoScheduleTask> preScheduleTaskList,StringBuilder logDetail) {
        CxScheduleUtils.sortAscByScheduleTaskClassShift(preScheduleTaskList);
        CxAutoScheduleTask minClassShiftTask=preScheduleTaskList.get(0);
        ClassEnums cls=ClassEnums.getClassEnums(minClassShiftTask.getClassShift());
        if(cls==null){
            logDetail.append("当前处理的是昨日三班，不需要进行更换工装开班原因分析").append(division);
            return;
        }
        String analysis=I18nUtil.getMessage("cx.engine.auto.analysis.newSpec.title");
        logDetail.append("【更换工装开班原因标注】").append(division);
        setChangeMoldCondition(cxEngineScheduleResult,cls,analysis,logDetail);
    }

    /**
     * 自动排程收尾原因自动分析
     * @param preScheduleTaskList
     */
    private void buildCloseOutAnalysis(CxEngineScheduleResult cxEngineScheduleResult,List<CxAutoScheduleTask> preScheduleTaskList,StringBuilder logDetail) {
        //降序排序
        CxScheduleUtils.sortDescByScheduleTaskClassShift(preScheduleTaskList);
        CxAutoScheduleTask maxShiftTask=preScheduleTaskList.get(0);
        //扣除掉所有班次后的剩余量
        Integer monthRemainQty=getRealMonthRemainQty(cxEngineScheduleResult);
        if(monthRemainQty<0){
            logDetail.append("【构建收尾原因分析】"+cxEngineScheduleResult.getEmbryoCode()+"月度剩余量小于0").append(division);
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
            Integer cxMonthFinishQty=newSpecPlanResult.getMonthFinishQty()==null?0:newSpecPlanResult.getMonthFinishQty();
            Integer totalPlanQty=0;
            boolean markStorageLocation=false;
            for(MdmMonthProdPlan mdmMonthProdPlan:mdmMonthProdPlanList){
                totalPlanQty+=mdmMonthProdPlan.getMonthTotalPlanQty();
                if(cxMonthFinishQty<totalPlanQty){
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
            logDetail.append("【库存地点设置】sap品号："+sapCode+",胎胚代码："+embryoCode+"未找到对应的月度计划明细信息").append(division);
        }
    }

    /**
     * 获取成型月度剩余量
     * @param cxEngineScheduleResult
     * @return
     */
    private Integer getRealMonthRemainQty(CxEngineScheduleResult cxEngineScheduleResult) {
        String embryoCode=cxEngineScheduleResult.getEmbryoCode();
        Integer monthRemainQty=cxEngineScheduleResult.getMonthRemainQty();
        if(monthRemainQtyMap.containsKey(embryoCode)){
            monthRemainQty=monthRemainQtyMap.get(embryoCode);
        }
        log.debug("【获取月度剩余量】sap品号："+cxEngineScheduleResult.getSapCode()+",胎胚代码："+embryoCode+",月度剩余量："+monthRemainQty);
        return monthRemainQty;
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
        logDetail.append("【计算更换工装时长】前规格胎胚："+beforeKey+",机头宽度："+beforeNoseWidth+",扣圈盘直径:"+beforeFlipDiscDiameter+"。后规格胎胚："+afterKey+",机头宽度："+afterNoseWidth+",扣圈盘直径:"+afterFlipDiscDiameter+"，更换工装时长："+minChangeSpecHour+"(小时)").append(division);
        return minChangeSpecHour;
    }

    /**
     * 根据月度 最大班次进行验证保留符合条件的规格任务列表
     * @param remainQtyList 符合条件的集合
     * @param lastDayScheduleResultList 原始数据集合
     * @param clsIndex 当前排班班次
     */
    private void reValidateScheduleResult(List<CxEngineScheduleResult> remainQtyList, List<CxEngineScheduleResult> lastDayScheduleResultList, Integer clsIndex,StringBuilder logDetail) {
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
                logDetail.append("【reValidateScheduleResult》》规格筛选标记不自动安排】,当前胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",最大可硫化班次："+maxClassShifts).append(division);
                continue;
            }
            //Joran 2021-12-20 标记自动排程不进行自动安排计划的任务跳过end
            //获取班次对应的可硫化班次
            Double workShirtAvailableLhShift= CxScheduleUtils.getAvailableLhShiftByClassShiftIndex(cxEngineScheduleResult,clsIndex);
            if(monthRemainQty>0&&maxClassShifts>workShirtAvailableLhShift){ //还有月度剩余量的规格
                remainQtyList.add(cxEngineScheduleResult);
            }else{
                logDetail.append("【自动排程规则不通过规格】,当前胎胚代码："+cxEngineScheduleResult.getEmbryoCode()+",当前月度剩余量："+monthRemainQty+",当前可硫化班次："+workShirtAvailableLhShift+",最大可硫化班次："+maxClassShifts).append(division);
            }
        }
    }


    /**
     * 添加新规格进行规格筛选,数据组装
     * @param cxEngineScheduleResult
     * @param minAvailableLhShift 最小平均可硫化班数
     * @return
     */
    private CxEngineScheduleResult addSpec(List<CxEngineScheduleResult> lastDayScheduleResultList,CxEngineScheduleResult cxEngineScheduleResult,Double minAvailableLhShift,Integer totalSingleLhShiftQty,StringBuilder logDetail) {
        String machineCode=cxEngineScheduleResult.getCxMachineCode();
        String machineType=cxEngineScheduleResult.getCxMachineType();
        maxAddSpecCount+=1;
        int maxAddCount=10;
        if(StringUtils.isNotEmpty(cxPlanProductStatusList)){
            maxAddCount=cxPlanProductStatusList.size();
        }
        if(maxAddSpecCount > maxAddCount){
            logDetail.append(StringUtils.format("当前机台：{}，新增规格结束。原因超过最大添加次数：{}",machineCode,maxAddSpecCount)).append(division);
            return null;
        }
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
            List<CxPlanProductStatus> machineExcludeProductStatusList=new ArrayList<>();
            if(StringUtils.isNotEmpty(machineExcludeProductStatusMap)&&machineExcludeProductStatusMap.containsKey(machineCode)){
                machineExcludeProductStatusList=machineExcludeProductStatusMap.get(machineCode);
            }

            if(StringUtils.isNotEmpty(machineExcludeProductStatusList)){
                cxPlanRemainProductStatusList.removeAll(machineExcludeProductStatusList);
            }

            logDetail.append("【可投产列表】记录数：").append(StringUtils.isEmpty(cxPlanProductStatusList)?0:cxPlanProductStatusList.size()).append(division);
            //剔除掉不可作业的规格
            specifyMachineProductList(machineCode,cxPlanRemainProductStatusList,CxEngineConstants.SPECIFY_JOB_TYPE_NO);
            logDetail.append("【移除不可作业后投产列表】记录数：").append(StringUtils.isEmpty(cxPlanRemainProductStatusList)?0:cxPlanRemainProductStatusList.size()).append(division);

            if(StringUtils.isEmpty(cxPlanRemainProductStatusList)){
                logDetail.append("添加规格结束，可进行筛选的投产记录数为0").append(division);
                return null;
            }

            //挑选定的规格
            nextPlanProduct=selectionPlanProduct(machineType,machineCode,beforeFlipDiscDiameter,beforeNoseWidth,cxPlanRemainProductStatusList,dimension,minAvailableLhShift,logDetail);
            if(nextPlanProduct!=null){
                maxSelectStatusCount=0;
                //根据待投产创建排程结果
                String key =GenerageMapKeyUtils.createMapKey(nextPlanProduct.getEmbryoCode(),nextPlanProduct.getBomDataVersion());
                if(!engineConstructionInfoMap.containsKey(key)){
                    throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.import.embryo.construction.noFound.error"), nextPlanProduct.getEmbryoCode()));
                }
                newSpecConstructionInfo=engineConstructionInfoMap.get(key);
                newSpecPlanResult=createScheduleResultByPlanProductStatus(lastDayScheduleResultList,cxEngineScheduleResult,nextPlanProduct,newSpecConstructionInfo);

                //如果新安排的规格为同胎胚的则重新进行其他规格安排start
                if(newSpecPlanResult.getIsProducted()){
                    logDetail.append("【剔除已投产规格】").append("，挑选的胎胚代码：").append(newSpecPlanResult.getEmbryoCode()).append("月度剩余量已小于等于0").append(division);
                    //Joran 2022-04-06 机台投产规格过滤
                    cxPlanProductStatusService.updatePlanProductToProduction(nextPlanProduct);
                    cxPlanProductStatusList.remove(nextPlanProduct);
                    newSpecPlanResult=addSpec(lastDayScheduleResultList,cxEngineScheduleResult,minAvailableLhShift,totalSingleLhShiftQty,logDetail);
                }else if(CxEngineConstants.TO_PRODUCT_NO.equals(newSpecPlanResult.getToProduct())){
                    //Joran 2021-12-22 新挑选的不排计划规格添加到列表，重新挑
                    logDetail.append("【新挑选的不排计划规格添加到列表】").append("，挑选的胎胚代码：").append(newSpecPlanResult.getEmbryoCode()).append("不自动安排任务，进行重新挑选").append(division);
                    lastDayScheduleResultList.add(newSpecPlanResult);
                    cxPlanProductStatusService.updatePlanProductToProduction(nextPlanProduct);
                    cxPlanProductStatusList.remove(nextPlanProduct);
                    newSpecPlanResult=addSpec(lastDayScheduleResultList,cxEngineScheduleResult,minAvailableLhShift,totalSingleLhShiftQty,logDetail);
                }
                //如果新安排的规格为同胎胚的则重新进行其他规格安排end

                if(newSpecPlanResult!=null){
                    //Joran 2022-04-06 验证是否重新选择规格 start
                    //nick 2024-05-23 这个单班硫化总量与定额差额的允许范围判断是否重新选择投产的规格之前一直未开启，新增需求变动单（编号是20240411-01）要求开启改成True
                     boolean isChange = true;
                     if(isChange){
                         boolean isReSelect=validateLhShift(cxEngineScheduleResult,newSpecPlanResult,totalSingleLhShiftQty,logDetail);
                         if(isReSelect){
                             machineExcludeProductList(machineCode,nextPlanProduct,logDetail);
                             newSpecPlanResult=addSpec(lastDayScheduleResultList,cxEngineScheduleResult,minAvailableLhShift,totalSingleLhShiftQty,logDetail);
                         }
                     }
                   //Joran 2022-04-06 验证是否重新选择规格 end

                // 1.更新待投产表中的投产状态 2.将待投产列表的数据移除
                    cxPlanProductStatusService.updatePlanProductToProduction(nextPlanProduct);
                    cxPlanProductStatusList.remove(nextPlanProduct);
                    maxAddSpecCount=0;
                }
            }else{
                logDetail.append("【nextPlanProduct is null】没有筛选到待投产规格，规格添加失败。").append(division);
            }
        }else{
            log.error("未投产列表数据没有数据，请确认未投产数据是否存在数据。");
        }
        return newSpecPlanResult;
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
        maxSelectStatusCount+=1;
        int maxCount=10;
        if(StringUtils.isNotEmpty(cxPlanProductStatusList)){
            maxCount=cxPlanProductStatusList.size();
        }
        if(maxSelectStatusCount > maxCount){
            logDetail.append("筛选规格任务结束，超出最大次数，终止筛选规格,当前总次数："+maxSelectStatusCount).append(dimension);
            return null;
        }
        //优先根据寸口进行筛选start
        for(CxPlanProductStatus cxPlanProductStatus:cxPlanProductStatusList){
            String embryoCode=cxPlanProductStatus.getEmbryoCode();
            String bomDataVersion=cxPlanProductStatus.getBomDataVersion();
            if(!org.apache.commons.lang3.StringUtils.startsWithIgnoreCase(embryoCode,startPrefix)){
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
            logDetail.append("挑选规格时，根据同寸口进行挑选，没有匹配得规格，机台投产结束。").append(division);
            return null;
        }
        logDetail.append("【同寸口投产列表】记录数").append(sameDimensionList.size()).append(division);
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
            logDetail.append("同寸口不存在扣圈盘直径规格相同的投产规格，以机头宽度差异小的来进行安排").append(division);
            nextPlanProduct=selectionCxPlanProductSpec(machineCode,beforeNoseWidth,sameDimensionList,minAvailableLhShift,logDetail);
        }else{
            logDetail.append("【同扣圈盘直径投产列表】记录数=").append(sameFlipDiscDiameterList.size()).append(division);
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
        String productSpecLimitQtyParams=cxParamsMap.get(CxParamCodeConstants.PRODUCT_SPEC_LIMIT_QTY);
        if(StringUtils.isEmpty(productSpecLimitShiftParams)){
            throw new CxScheduleEngineException("可投产量限定班次参数为空,请先配置！");
        }
        if(StringUtils.isEmpty(productSpecLimitQtyParams)){
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
            maxPlanQty =Integer.valueOf(productSpecLimitQtyParams);
            limitMax=true;//限制可投最大计划量规格
            logDetail.append("【新规格筛选】机台编号：【"+machineCode+"】,最小可硫化班次：【"+minAvailableLhShift+"】，小于参数设定班次：【"+productSpecLimitShift+"】,最大可投产量限定为：【"+maxPlanQty+"】").append(division);
        }else{
            //限制最小计划量
            limitMin=true;
            minPlanQty =Integer.valueOf(productSpecLimitQtyParams);
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
                logDetail.append("【新规格筛选】机台编号：【"+machineCode+"】,新规格月度计划量大于可投产数量，跳过不投产,继续找下一个规格，当前规格胎胚：【"+cxPlanProductStatus.getEmbryoCode()+"】").append(division);
                continue;
            }
            //大规格筛选
            if(limitMin&&cxPlanProductStatus.getMonthPlanTotalQty()< minPlanQty){
                logDetail.append("【新规格筛选】机台编号：【"+machineCode+"】,新规格月度计划量必须投大规格，跳过不投产,继续找下一个规格，当前规格胎胚：【"+cxPlanProductStatus.getEmbryoCode()+"】").append(division);
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
        logDetail.append("【机头宽度差异最小规格】机台编号：【"+machineCode+"】,").append("，规格数据：").append(toJSONString(newCxPlanProductStatus)).append(division);
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
        //logDetail.append("【移除限定作业不在本机台规格】").append("限制作业数据集合").append(toJSONString(specifyMachineYesMap)).append(division);
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
        commonCacheService.calcLeastLhMachineQty(newSpecPlanResult,nextProductPlan,sapSpecMoldUseList,sapTireConstructionListMap);
        //Joran 2022-01-03 设置外胎规格描述
        commonCacheService.setSpecDescBySapCode(newSpecPlanResult,sapTireConstructionListMap);
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

        //没有设置则取默认工序参数可硫化最大班次
        if(maxLhShift==null){
            // 2024-05-23 nick 改数可硫化最大班次 需要根据单双模配置（没有配置默认按照双模计算）
            String  defaultLhClassShifts=getDefaultLhClassShifts(cxEngineScheduleResult);
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
                            logDetail.append("【获取最大可硫化班次设置参数】匹配设置获取到最大班次,成型机台编号："+key+"寸口："+specDimension+",最大可硫化班次："+cxEngineScheduleLimit.getMaxLhClass()).append(division);
                            //cxEngineScheduleResult.setMaximumClassQty(cxEngineScheduleLimit.getMaxLhClass());
                            defaultLhClassShifts = String.valueOf(cxEngineScheduleLimit.getMaxLhClass());
                        }
                    }
                }
            }
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
        Double  maxLhClassShifts = getMaxLhClassShifts();
        //Joran 2022-01-08 进行3轮参数比对验证最终获取到的可硫化班次end
        if(maxLhShift<=BigDecimal.ZERO.doubleValue()){ //当计算出来的班次小于0时默认给1班
            maxLhShift=BigDecimal.ONE.doubleValue();
        }else if(maxLhShift >= maxLhClassShifts){ //当超过设置的最大班次时默认给最大班次
            maxLhShift=maxLhClassShifts;
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
                //logDetail.append("获取到的一次法库存限制设置").append(toJSONString(productStockLimitList)).append(division);
            }

        }else if(StringUtils.startsWithIgnoreCase(embryoCode,twicePrefix)){
            logDetail.append("当前轮胎类型为【一次法】").append(division);
            prefix=twicePrefix;
            if(StringUtils.isNotEmpty(cxEngineProductStockLimitListMap)&&cxEngineProductStockLimitListMap.containsKey(CxEngineConstants.MACHINE_TYPE_TWICE)){
                productStockLimitList=cxEngineProductStockLimitListMap.get(CxEngineConstants.MACHINE_TYPE_TWICE);
                //logDetail.append("获取到的二次法库存限制设置").append(toJSONString(productStockLimitList)).append(division);
            }
        }
        //logDetail.append("获取轮胎类型对应的库存汇总信息").append(toJSONString(embryoCodeTypeTotalMap)).append(division);
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
           // logDetail.append("同寸口平均值设定信息：").append(toJSONString(dimensionLimitList)).append(division);
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
                finalTaskQty=(int) Math.ceil(taskQty * (1+(lossRate/100)));
                logDetail.append("【耗损率计算】获取到耗损率信息："+lossRate).append(";").append(division);;
                logDetail.append("【耗损率计算】考虑耗损率后总计划量："+finalTaskQty).append(division);
            }
        }
        return finalTaskQty;
    }

    /**
     * 单机台单规格连续计划量计算
     * 规则：从当前班次开始 前一班开始追加直到遇到0或者没有计划
     * @param cxEngineScheduleResult 下个任务对象
     * @param classIndex 当前班次
     * @return
     */
    private int calcContinuePlanQty(CxEngineScheduleResult cxEngineScheduleResult,Integer classIndex,StringBuilder logDetail,boolean sameTask) {
        StringBuilder sb=new StringBuilder("【新版计算连续生产量】规格任务量追加，胎胚："+cxEngineScheduleResult.getEmbryoCode()+",机台："+cxEngineScheduleResult.getCxMachineCode()).append("\n");
        Integer continuePlanQty=0;
        //续排同规格则任务量重新
        if(sameTask){
            return 0;
        }
        boolean isContinue=true;

        //当前班次为昨日三班班次时不走本日计划累加 start
        if(classIndex>0){
            ClassEnums cls= ClassEnums.getClassEnums(classIndex);
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
        }
        //当前班次为昨日三班班次时不走本日计划累加 end

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
        logDetail.append(sb).append(division);
        return continuePlanQty;
    }

    /**
     * 添加停排信息记录
     * @param machineCode
     * @param msg
     */
    private void addMachineStopInfo(String machineCode,String orderNo,Integer classShift, String msg) {
        CxScheduleStopInfo cxScheduleStopInfo=new CxScheduleStopInfo();
        cxScheduleStopInfo.setCxMachineCode(machineCode);
        cxScheduleStopInfo.setCxBatchNo(cxBatchNo);
        cxScheduleStopInfo.setOrderNo(orderNo);
        cxScheduleStopInfo.setScheduleDate(autoScheduleDate);
        cxScheduleStopInfo.setClassShift(classShift);
        cxScheduleStopInfo.setStopReason(msg);
        if(StringUtils.isEmpty(cxScheduleStopInfoList)){
            cxScheduleStopInfoList=new ArrayList<>();
        }
        cxScheduleStopInfoList.add(cxScheduleStopInfo);
    }

    /**
     * 验证挑选完规格后，是否需要重新选择，主要校验单班硫化总量与成型定额
     * @param cxEngineScheduleResult 前规格
     * @param newSpecPlanResult 新投产规格
     * @param totalSingleLhShiftQty 现有总量（未包含新投产规格）
     * @param logDetail 日志
     * @return
     */
    private Boolean validateLhShift(CxEngineScheduleResult cxEngineScheduleResult, CxEngineScheduleResult newSpecPlanResult, Integer totalSingleLhShiftQty, StringBuilder logDetail) {
        Boolean isReSelect=false;
        //获取当前规格的机台定额数据
        Integer machineQuota=commonCacheService.getQuotaByMachineEmbryoCode(newSpecPlanResult.getCxMachineCode(),newSpecPlanResult.getEmbryoCode(),newSpecPlanResult.getBomDataVersion(),logDetail);
        logDetail.append("【添加规格验证机台总硫化数】，投产新规格获取到的定额=").append(machineQuota).append(division);
        Integer newSpecSingleLhShift=newSpecPlanResult.getSingleShiftLhQty()==null?BigDecimal.ZERO.intValue():newSpecPlanResult.getSingleShiftLhQty();
        logDetail.append("【添加规格验证机台总硫化数】，新规格单班硫化量=").append(newSpecSingleLhShift).append(division);
        //加上新规格单班硫化量后的总量
        Integer sumSingleLhShiftQty=totalSingleLhShiftQty+newSpecSingleLhShift;
        logDetail.append("【添加规格验证机台总硫化数】，机台加上新规格后的总硫化量=").append(sumSingleLhShiftQty).append(division);
        //最大差额
        String shiftQuotaDiffMax=cxParamsMap.get(CxParamCodeConstants.SHIFT_QUOTA_DIFF_MAX);
        if(StringUtils.isEmpty(shiftQuotaDiffMax)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.shift.quota.diff.max.value.empty.error"));
        }
        Double shiftDiffMax = Double.valueOf(shiftQuotaDiffMax);
        logDetail.append("【添加规格验证机台总硫化数】，差额范围最大值=").append(shiftDiffMax).append(division);
        //最小差额
        String shiftQuotaDiffMin=cxParamsMap.get(CxParamCodeConstants.SHIFT_QUOTA_DIFF_MIN);
        if(StringUtils.isEmpty(shiftQuotaDiffMax)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.shift.quota.diff.min.value.empty.error"));
        }
        Double shiftDiffMin = Double.valueOf(shiftQuotaDiffMin);
        logDetail.append("【添加规格验证机台总硫化数】，差额范围最小值=").append(shiftDiffMin).append(division);
        Integer diffQty= machineQuota - sumSingleLhShiftQty;
        if(machineQuota < sumSingleLhShiftQty){
            isReSelect=true;
        }else if(diffQty < shiftDiffMin || diffQty > shiftDiffMax){
            isReSelect=true;
        }

        logDetail.append("【添加规格验证机台总硫化数】，是否重新进行选择=").append(isReSelect).append(division);

        return isReSelect;
    }

    /**
     * 机台投产规格过滤
     * @param machineCode 机台编号
     * @param nextPlanProduct 当前挑选的规格
     * @param logDetail
     */
    private void machineExcludeProductList(String machineCode, CxPlanProductStatus nextPlanProduct,StringBuilder logDetail) {
        logDetail.append(StringUtils.format("机台编号：{}，过滤规格",machineCode)).append(division);
        List<CxPlanProductStatus> machineExcludeProductStatusList=new ArrayList<>();
        if(StringUtils.isEmpty(machineExcludeProductStatusMap)|| !machineExcludeProductStatusMap.containsKey(machineCode)){
            machineExcludeProductStatusList=new ArrayList<>();
            machineExcludeProductStatusList.add(nextPlanProduct);
            machineExcludeProductStatusMap.put(machineCode,machineExcludeProductStatusList);
            logDetail.append("第一次进行规格筛选").append(division);
        }else{
            machineExcludeProductStatusList =machineExcludeProductStatusMap.get(machineCode);
            machineExcludeProductStatusList.add(nextPlanProduct);
        }
    }

    /**
     * 2024-05-23 默认可硫化班次获取
     *
     *  默认可硫化班次原方案：
     * 不分单双摸，只有一个默认可硫化班次参数设置
     *
     *
     * 变更方案：
     * 默认可硫化班次 分单双模设置参数
     * 单模默认可硫化班次设置 默认值 12
     * 双模默认可硫化班次设置 默认值 8
     */
    public String getDefaultLhClassShifts(CxEngineScheduleResult cxEngineScheduleResult) {
        if (cxEngineScheduleResult == null || cxEngineScheduleResult.getLhMachineQty() == null || cxEngineScheduleResult.getLhMachineQty() >= 2 || cxEngineScheduleResult.getLhMachineQty() == 0) {
            //没有模数也按照双模计算
            return cxParamsMap.get(CxParamCodeConstants.DEFAULT_DOUBLE_LH_CLASS_SHIFTS);
        }
        // 不然则用单模返回
        return cxParamsMap.get(CxParamCodeConstants.DEFAULT_LH_CLASS_SHIFTS);
    }

}
