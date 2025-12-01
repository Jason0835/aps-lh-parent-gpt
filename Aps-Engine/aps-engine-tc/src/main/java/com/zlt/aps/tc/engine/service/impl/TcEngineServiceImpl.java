package com.zlt.aps.tc.engine.service.impl;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.engine.mapper.TcEngineMapper;
import com.zlt.aps.tc.engine.mapper.TcEngineStockMapper;
import com.zlt.aps.tc.engine.service.*;
import com.zlt.aps.tc.engine.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.*;

@Slf4j
@Service
public class TcEngineServiceImpl implements TcEngineService {

    @Resource
    private TcEngineMapper tcEngineMapper;
    @Resource
    private TcEngineGlueService tcEngineGlueService;
    @Resource
    private TcEngineStockService tcEngineStockService;
    @Resource
    private TcEngineMachineService tcEngineMachineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private TcEngineLossService tcEngineLossService;
    @Resource
    private TcEngineMonthSurplusService tcEngineMonthSurplusService;
    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    @Resource
    private TcEngineCurlRollService tcEngineCurlRollService;
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "12"; // 保库存供应时长
    private final static BigDecimal HOUR24 = new BigDecimal("24"); // 24小时
    private final static String DEFAULT_ONE_SPEC_LARGE_DEMAND = "16"; // 单规格大需求量卷数
    @Resource
    private TcEngineStockMapper tcEngineStockMapper;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 排程参数预设值，参数设置取不到值时使用这些预设值
     */
    private final static String DEFAULT_STANDARD_CRIMP_LENGTH = "50"; // 卷曲标准长度
    private final static String DEFAULT_CURL_DECIMAL_ROUNDING = "0.3"; // 卷曲数小数取整值
    private final static String DEFAULT_CLOSE_OUT_DAYS = "1"; // 共用规格收尾判断天数
	/**
	 * 生产阶段校验开关状态：打开
	 */
	private final static String PRODUCTION_STAGE_ON = "1";

    private final static BigDecimal TWO = new BigDecimal("2"); // 用于计算平分

    /**
     * 胎侧胶自动排程
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    @Transactional(rollbackFor=Exception.class)
    public void autoTcSchedule(String scheduleDate) {
        String username = SecurityUtils.getUsername();
        String cxBatchNo = "";  //成型批次号
        String batchNo = this.createBatchNo(scheduleDate);  //胎侧排程批次号
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        Map<String, String> mapAssistSpec = this.mapAssistSpec(); //获得外协规格Map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        BigDecimal standardCurlLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        standardCurlLength = standardCurlLength.compareTo(BigDecimal.ZERO) > 0? standardCurlLength: new BigDecimal(DEFAULT_STANDARD_CRIMP_LENGTH); // 卷曲标准长度防错处理，不合法的配置都按默认值处理
        BigDecimal curlDecimalRounding = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CURL_DECIMAL_ROUNDING, DEFAULT_CURL_DECIMAL_ROUNDING)); // 卷曲数小数取整值，小数部分大于等于该值的进位，否则舍弃
        BigDecimal closeOutDays = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CLOSE_OUT_DAYS, DEFAULT_CLOSE_OUT_DAYS)); // 共用规格收尾判断天数
        BigDecimal oneSpecLargeDemand = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND, DEFAULT_ONE_SPEC_LARGE_DEMAND));
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
        double mergeThreshold = getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD));
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        double productStockDay = productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue();
        // 查询机台维修计划，如果有，则需扣减机台对应生产定额，
        // K：GenerageMapKeyUtils.createMapKey(机台ID, 停机班次)，V：机台需扣减的生产定额
        Map<String, BigDecimal> machineSubQuotaMap = tcEngineMachineService.selectMachineSubQuota(scheduleDate);
        List<TcScheduleResultVo> scheduleList = tcEngineMapper.statTcScheduleBase(scheduleDate, productionStage);  //根据成型排程记录 统计出 胎侧胶排程记录基础数据
        if (scheduleList == null || scheduleList.isEmpty()) {
            log.info("根据成型排程记录 统计出 胎侧胶排程记录基础数据 为空");
            autoScheduleLogService.insertTcScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料"); //添加日志
            throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
        }
        //过滤掉成型2个班的计划量都为0的数据
//        scheduleList = scheduleList.stream().filter(s -> (s.getCxClass2Plan()+s.getCxClass3Plan())>0).collect(Collectors.toList());
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "根据成'型排程记录'统计出胎侧胶排程记录基础数据",  toJSONString(scheduleList));
//        this.ValidatedConstruction(scheduleDate, batchNo, productionStage, mapAssistSpec);   //证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
        Map<String, String> glueSeqMap = tcEngineGlueService.getGlueSeqMap();  //获取胶料序号map
        Map<String, String> mouthPlateMachineMap = tcEngineMachineService.getMouthPlateMachineMap(); //获得口型板代码和定点机台的map
        Map<String, String> disableMouthPlateMachineMap = tcEngineMachineService.getDisableMouthPlateMachineMap(); //获得已禁用口型板代码和定点机台的map
        Map<String, String> specifyCanMachineMap = tcEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN); //获得胎侧代码和定点机台的限制作业map
        Map<String, String> specifyNotMachineMap = tcEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT); //获得胎侧代码和定点机台的不可作业map
        Map<String, Double> planStockMap = tcEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算胎侧16点预计库存
        Map<String, String> lastDayGlueMachineMap = this.loadLastDayMidPlan4Glue(scheduleDate); // 加载昨日胶料对应机台
        Map<String, Double> stockMap = this.loadTcStock(scheduleDate); // 加载库存
        Map<String, Double> lastDayMidPlanMap = this.loadLastDayMidPlan(scheduleDate); // 加载昨日早班计划
        Map<String, Double> lossRateMap = tcEngineLossService.getLossRateMap();   //损耗率map
        Map<String, TcMonthSurplusVo> monthSurplus = tcEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        List<String> closeOutSpecList = this.getCloseOutSpecList(scheduleDate, closeOutDays, productionStage); // 获取当天的收尾规格列表
        Map<String, BigDecimal> tcCurlLengthMap = tcEngineCurlRollService.getTcCurlLengthMap(); // 胎侧卷曲设置
        this.baseDataLog(batchNo, glueSeqMap, mouthPlateMachineMap, specifyCanMachineMap, specifyNotMachineMap, planStockMap, lossRateMap, monthSurplus, paramsMap); //把基础数据假如到日志中
        Map<String, TcTotalPlanQtyVo> totalPlanQtyMap = new HashMap<>();  //每个生产线的计划量汇总MAP
        List<TcMachineInfo> allMachineList = tcEngineMachineService.listTcMachine();
        TcTotalPlanQtyVo totalPlanQtyVo = new TcTotalPlanQtyVo();  //胎面中班和夜班总计划量Vo
        for (TcScheduleResultVo scheduleVo : scheduleList) {
            cxBatchNo = scheduleVo.getCxBatchNo();
            scheduleVo.setBatchNo(batchNo);    //批次号
            scheduleVo.setOrderNo(this.createOrderNo(batchNo));  //工单号
            String orderNo = this.createOrderNo(batchNo);   //创建工单号
            scheduleVo.setOrderNo(orderNo);
            scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE); // 初始未收尾
            BigDecimal curlLength = tcCurlLengthMap.getOrDefault(scheduleVo.getSidewallCode(), standardCurlLength); // 卷曲长度

            scheduleVo.setGlueSeq(glueSeqMap.get(scheduleVo.getGlueCode()));  //胶料序号
            autoScheduleLogService.insertTcScheduleLog(batchNo, orderNo, "根据'胶料顺序集合'设置胶料序号",
                    logSplit("胶料顺序集合：" + toJSONString(glueSeqMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

//            this.chooseMachine(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap);  //选择生产线
            scheduleVo.setPlanStockQty(planStockMap.getOrDefault(scheduleVo.getSidewallCode(), 0D));  //16点预计库存
            scheduleVo.setStockQty(stockMap.getOrDefault(scheduleVo.getSidewallCode(), 0D));  // 库存
            scheduleVo.setLastMidPlanQty(lastDayMidPlanMap.getOrDefault(scheduleVo.getSidewallCode(), 0D)); // 上一天早班库存
            scheduleVo.setSurplusQty(Optional.ofNullable(monthSurplus.get(scheduleVo.getSidewallCode())).map(TcMonthSurplusVo::getMonthRemainQty).orElse(0D)); // 剩余量
            autoScheduleLogService.insertTcScheduleLog(batchNo, orderNo, "根据'16点预计库存集合'设置库存",
                    logSplit("16点预计库存集合：" + toJSONString(planStockMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.newComputeSupplyTime(scheduleVo, scheduleVo.getPlanStockQty());  //库存供应时长
            this.computeTcPlanQty(scheduleVo, totalPlanQtyVo, lossRateMap, paramLossRate, mergeThreshold, curlLength, productStockDay, paramsMap);  //计算胎侧中班和夜班计划量
//            this.computeTcCurlRoll(scheduleVo, tcCurlLengthMap, standardCurlLength, closeOutSpecList, curlDecimalRounding, totalPlanQtyMap); // 计算卷曲数
            this.setStatusAndCloseTip(scheduleVo, monthSurplus.get(scheduleVo.getSidewallCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段

            if(BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan(), scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan()) == 0D) {
                //判断成型前4个班是否有计划量，若无计划，判断为新投产规格，半部件计划计划量放在“预计划”栏位中，中班和夜班计划都显示为0；若有计划，半部件计划正常排产
                scheduleVo.setPrePlanQty(BigDecimalUtil.add(scheduleVo.getDayPlanQty(), scheduleVo.getNightPlanQty()));
                scheduleVo.setDayPlanQty(0D);
                scheduleVo.setNightPlanQty(0D);
            }
            scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleVo.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            scheduleVo.setCreateTime(new Date());
            scheduleVo.setCreateBy(username);
        }
//        this.equilibrium(scheduleList, paramsMap, totalPlanQtyMap);  //中班和夜班计排程计划量均衡处理
//        double midPlanQtyReference = BigDecimalUtil.div(totalPlanQtyVo.getTotalPlanQty(), 3, 0); // 中班计划参考值，总量的三分之一
//        this.equilibriumDay1(scheduleList, totalPlanQtyVo, tcCurlLengthMap, standardCurlLength);
//        this.equilibriumDay2(scheduleList, totalPlanQtyVo, new BigDecimal(paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD)));  //中班和夜班计排程计划量均衡处理
//        this.glueMerge(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX));  //同胶料合并生产
//        this.glueMerge1(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX), paramsMap.get(EngineConstants.MERGE_MAX_ROLL), tcCurlLengthMap, standardCurlLength);
        this.chooseMachineByCapacity(scheduleList, allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, disableMouthPlateMachineMap, lastDayGlueMachineMap, machineSubQuotaMap);  //选择生产线
//        this.equilibriumMachineQuota(scheduleList, paramsMap, allMachineList, tcCurlLengthMap, standardCurlLength);
//        this.equalShare(batchNo, scheduleList, totalPlanQtyVo, new BigDecimal(paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD)));  //单规格排产数量达到设定值时，中夜班数量对半分
        this.allocationPlanQty(scheduleList, allMachineList, tcCurlLengthMap, paramsMap, machineSubQuotaMap);
        this.setProduceOrder(scheduleList);  //设置白班和夜班的生产顺序

        List<TcScheduleResultVo> existScheduleList = this.tcEngineMapper.listTcEnginSchedule(scheduleDate);  //查询当天已经存在的排产记录
        this.syncTcScheduleToLog(scheduleDate);  //把排程数据同步到log表
        this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);  //创建自动排程记录

        List<TcScheduleResultVo> assistScheduleList = scheduleList.stream().filter(r -> mapAssistSpec.containsKey(r.getSidewallCode())).collect(Collectors.toList()); //过滤出外协排程数据
        scheduleList = scheduleList.stream().filter(r -> !mapAssistSpec.containsKey(r.getSidewallCode())).collect(Collectors.toList());  //过滤出非外协的排产数据
        if(StringUtils.isNotEmpty(assistScheduleList)) {
            tcEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);   //批量新增外协排程结果数据
        }

        scheduleList = this.mergeExistSchedule(batchNo, scheduleList, existScheduleList);  //如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
        if(StringUtils.isNotEmpty(scheduleList)) {
            tcEngineMapper.batchCreateScheduleResult(scheduleList);   //批量新增非外协排程结果数据
        }
    }

    /**
     * 重新分配计划量到各班次
     *
     * @param scheduleList   排产计划
     * @param allMachineList 机台
     * @param curlLengthMap  标准长度
     * @param paramsMap      标准卷曲长度
     *
     */
    private void allocationPlanQty(List<TcScheduleResultVo> scheduleList, List<TcMachineInfo> allMachineList,
                                   Map<String, BigDecimal> curlLengthMap, Map<String, String> paramsMap, Map<String, BigDecimal> machineSubQuotaMap) {
        Map<String, List<TcScheduleResultVo>> scheduleMachineMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineId()))
                .collect(Collectors.groupingBy(TcScheduleResultVo::getMachineId)); // 将排产计划按机台分组
        Map<String, Map<OpenMachineClassEnums, BigDecimal>> classMachinePlanQtyMap = this
                .initClassMachinePlanQtyMap(scheduleMachineMap); // 获取各班已排计划
        Map<String, BigDecimal> machineQuata = allMachineList.stream()
                .collect(Collectors.toMap(m -> String.valueOf(m.getId()), TcMachineInfo::getQuata)); // 各机台单班产能
        BigDecimal standardCurlLength = new BigDecimal(
                paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        double productStockDay = productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue();
        Map<String, Map<OpenMachineClassEnums, BigDecimal>> limitQuataMap = this.initLimitQuataMap(machineQuata,
                scheduleList, allMachineList, productStockDay, machineSubQuotaMap); // 各机台各班的产能限制列表

        // 遍历所有机台每个班次的计划
        for (Entry<String, Map<OpenMachineClassEnums, BigDecimal>> entry : classMachinePlanQtyMap.entrySet()) {
            String machineId = entry.getKey();
            Map<OpenMachineClassEnums, BigDecimal> classPlanQtyMap = entry.getValue();
            List<TcScheduleResultVo> scheduleMachineList = scheduleMachineMap.get(machineId);
            BigDecimal quata = machineQuata.get(machineId); // 机台产能
            BigDecimal planQtyReference; // 平衡基准值与产能一致（计划量往最大产能靠）
            // 机台各班已排计划量
            OpenMachineClassEnums currentClass = OpenMachineClassEnums.CLASS_TWO;
            while (currentClass != null && currentClass.getClassIndex() < OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) { // 遍历2个班的计划
                planQtyReference = limitQuataMap.get(machineId).getOrDefault(currentClass, quata); // 如果有产能限制，按经过限制的产能计算
                this.equalShareMachineClass(scheduleMachineList, curlLengthMap, paramsMap, currentClass, classPlanQtyMap); // 调整计划量前先均分处理
                OpenMachineClassEnums previousClass = currentClass.getPreviousClass();
                OpenMachineClassEnums nextClass = currentClass.getNextClass();
                BigDecimal classTotalPlanQty = classPlanQtyMap.getOrDefault(currentClass, BigDecimal.ZERO); // 本班计划量统计
                BigDecimal nextClassTotalPlanQty = classPlanQtyMap.getOrDefault(nextClass, BigDecimal.ZERO); // 下一个班计划量统计
                BigDecimal diff = classTotalPlanQty.subtract(planQtyReference); // 与基准值比较
                if (diff.compareTo(BigDecimal.ZERO) == 0) { // 一致则处理下一个班次
                    currentClass = currentClass.getNextClass();
                    continue;
                }
                boolean isPlanQtyPass = diff.compareTo(BigDecimal.ZERO) > 0; // 是否超产能
                // 只处理本机台的计划
                OpenMachineClassEnums sortClass = currentClass;
                List<TcScheduleResultVo> filterList = scheduleMachineList.stream().sorted((s1, s2) -> {
                    int result;
                    // 第一次序，按成型需求顺位排序，如果是产能没满，优先将第一顺位往前提
//                    Double sort1 = this.getCxClassSort(s1, nextClass);
//                    Double sort2 = this.getCxClassSort(s2, nextClass);
//                    result = isPlanQtyPass ? sort2.compareTo(sort1) : sort1.compareTo(sort2); // 超量，倒序；产能没满，顺序
//                    if (result != 0) {
//                        return result;
//                    }
                    // 第二次序，库存缺口， 下个班为止成型总需求量 - 上一个班为止的库存量
                    Double cxPlanQty1 = this.getCxClassPlanCumulative(s1, nextClass);
                    Double cxPlanQty2 = this.getCxClassPlanCumulative(s2, nextClass);
                    Double tmPlanQty1 = this.getTcClassPlanCumulative(s1, previousClass);
                    Double tmPlanQty2 = this.getTcClassPlanCumulative(s2, previousClass);
                    Double stock1 = s1.getStockQty();
                    Double stock2 = s2.getStockQty();
                    Double lackStock1 = BigDecimalUtil.sub(cxPlanQty1, BigDecimalUtil.add(stock1, tmPlanQty1));
                    Double lackStock2 = BigDecimalUtil.sub(cxPlanQty2, BigDecimalUtil.add(stock2, tmPlanQty2));
                    result = isPlanQtyPass ? lackStock1.compareTo(lackStock2) : lackStock2.compareTo(lackStock1); // 超量，库存缺口顺序（缺口小的先推迟）；缺量，库存缺口倒序（缺口大的先提前）
                    return result;
                }).collect(Collectors.toList());
                for (TcScheduleResultVo scheduleVo : filterList) {
                    if (scheduleVo.getIsDayProductSpec()) { // 固定早班生产规格跳过
                        continue;
                    }
                    if (scheduleVo.getIsEqualShare()) { // 已均分的规格跳过
                        continue;
                    }

                    BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getSidewallCode(), standardCurlLength); // 标准长度
                    // 计划量低于基准值，将下个班的部分计划提前
                    if (diff.abs().compareTo(curlLength) <= 0) { // 差异在一卷以内，则直接结束
                        break;
                    }

                    Double planQtyCumulative = this.getTcClassPlanCumulative(scheduleVo, previousClass); // 上个班为止的累计已排计划量
                    Double cxPlanQtyCumulative = this.getCxClassPlanCumulative(scheduleVo, currentClass); // 本班为止的成型累计需求
                    Double nextCxPlanQtyCumulative = this.getCxClassPlanCumulative(scheduleVo, nextClass); // 下一个班为止的成型累计需求
                    Double classStock = BigDecimalUtil.add(planQtyCumulative, scheduleVo.getStockQty()); // 累计入库量 = 累计排产量 + 库存
                    Double currentClassPlanQty = this.getPlanQty(scheduleVo, currentClass); // 本班计划量
                    if (diff.compareTo(BigDecimal.ZERO) > 0) {
                        if (currentClassPlanQty <= 0) { // 本班没有排计划的跳过
                            continue;
                        }
                        boolean isForceFlag = currentClassPlanQty > planQtyReference.doubleValue(); // 单规格计划量比产能基准值还大，不需要做其他判断直接推迟（通常是当前班次不关机）
                        if (!isForceFlag) { // 非强制推迟情况，再判断其他情况是否可推迟

                            if (classStock < cxPlanQtyCumulative) { // 本班的需求量无法满足则不能推迟
                                continue;
                            }
                            if (classStock < nextCxPlanQtyCumulative) { // 下个班的需求量不满足
                                if (this.getCxClassSort(scheduleVo, nextClass) <= 2) { // 下个班成型需求顺位1的不能推迟
                                    continue;
                                }
//                                if (BigDecimalUtil.sub(nextCxPlanQtyCumulative, classStock) > curlLength.doubleValue()) { // 库存缺口超过1卷的不能推迟
//                                    continue;
//                                }
                            }
                        }

                        // 符合条件的，将计划推迟到下个班
                        Double nextClassPlanQty = this.getPlanQty(scheduleVo, nextClass);
                        this.setPlanQty(scheduleVo, currentClass, 0D);
                        this.setPlanQty(scheduleVo, nextClass,
                                BigDecimalUtil.add(nextClassPlanQty, currentClassPlanQty));
                        classTotalPlanQty = BigDecimalUtils.sub(classTotalPlanQty, currentClassPlanQty); // 本班总计划量扣减预计推迟到下个班的量;
                        diff = classTotalPlanQty.subtract(planQtyReference); // 与基准值比较
                        nextClassTotalPlanQty = BigDecimalUtils.add(nextClassTotalPlanQty, currentClassPlanQty);
                        if (diff.abs().compareTo(curlLength) <= 0
                                || classTotalPlanQty.compareTo(planQtyReference) <= 0) { // 新差异在一卷以内、或者总计划量已经低于基准值，则直接结束
                            break;
                        }
                    } else {
                        // 尝试把下个班成型有需求但交接班库存不足的计划提前
                        Double nextClassPlanQty = this.getPlanQty(scheduleVo, nextClass);
                        if (nextClassPlanQty <= 0) { // 下个班无计划量的跳过
                            continue;
                        }
                        boolean isPreviousProduct = this.getPlanQty(scheduleVo, previousClass) > 0; // 上一个班有排产
                        if (classStock >= nextCxPlanQtyCumulative && isPreviousProduct) { // 如果交接班库存已经满足两个班的需求量，且上个班也有排计划，则不要提前
                            continue;
                        }

                        BigDecimal newCLassTotalPlanQty = BigDecimalUtils.add(classTotalPlanQty, nextClassPlanQty); // 本班计划添加上下个班的计划量
                        BigDecimal newDiff = newCLassTotalPlanQty.subtract(planQtyReference); // 与基准值比较
                        if (newCLassTotalPlanQty.compareTo(quata) > 0) { // 处理后超产能则不处理
                            continue;
                        }
                        // 符合条件的，下个班的计划提前到本班
                        this.setPlanQty(scheduleVo, currentClass,
                                BigDecimalUtil.add(currentClassPlanQty, nextClassPlanQty));
                        this.setPlanQty(scheduleVo, nextClass, 0D);
                        diff = newDiff;
                        classTotalPlanQty = newCLassTotalPlanQty;
                        nextClassTotalPlanQty = BigDecimalUtils.sub(nextClassTotalPlanQty, nextClassPlanQty);
                        if (diff.abs().compareTo(curlLength) <= 0
                                || newCLassTotalPlanQty.compareTo(planQtyReference) >= 0) { // 新差异在一卷以内、或者总计划量已经达到基准值，则直接结束
                            break;
                        }
                    }
                }
                classPlanQtyMap.put(currentClass, classTotalPlanQty);
                classPlanQtyMap.put(nextClass, nextClassTotalPlanQty);
                currentClass = currentClass.getNextClass();
            }
        }
    }

    /**
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     *
     * @param scheduleList    排程列表
     * @param curlLengthMap   标准长度
     * @param paramsMap       排产参数
     * @param currentClass    当前班次
     * @param classPlanQtyMap 各班计划量统计
     */
    private void equalShareMachineClass(List<TcScheduleResultVo> scheduleList, Map<String, BigDecimal> curlLengthMap,
            Map<String, String> paramsMap, OpenMachineClassEnums currentClass,
            Map<OpenMachineClassEnums, BigDecimal> classPlanQtyMap) {
        BigDecimal bisectThreshold = BigDecimalUtils.valueOf(paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD)); // 均分阈值，超过的要平分
        BigDecimal tcVmMinRollNum = BigDecimalUtils.valueOf(paramsMap.get(EngineConstants.TC_VM_MIN_ROLL_NUM)); // 二次法
        BigDecimal standardCurlLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        OpenMachineClassEnums previousClass = currentClass.getPreviousClass();
        OpenMachineClassEnums nextClass = currentClass.getNextClass();
        BigDecimal classTotalPlanQty = classPlanQtyMap.getOrDefault(currentClass, BigDecimal.ZERO); // 本班计划量统计
        BigDecimal nextClassTotalPlanQty = classPlanQtyMap.getOrDefault(nextClass, BigDecimal.ZERO); // 下一个班计划量统计

        // 次日早夜班总计划量超过阈值的平分中夜班计划量
        for (TcScheduleResultVo scheduleVo : scheduleList) {
            if (scheduleVo.getIsDayProductSpec()) { // 固定早班生产规格跳过
                continue;
            }
            BigDecimal stockQty = BigDecimalUtils.valueOf(scheduleVo.getStockQty());
            Double planQtyCumulative = this.getTcClassPlanCumulative(scheduleVo, previousClass); // 上个班为止的累计已排计划量
            Double cxPlanQtyCumulative = this.getCxClassPlanCumulative(scheduleVo, currentClass); // 本班为止的成型累计需求
            Double nextCxPlanQtyCumulative = this.getCxClassPlanCumulative(scheduleVo, nextClass); // 下个班为止的成型累计需求

            BigDecimal currentPlanQty = BigDecimalUtils.valueOf(this.getPlanQty(scheduleVo, currentClass)); // 当前班次计划
            BigDecimal nextPlanQty = BigDecimalUtils.valueOf(this.getPlanQty(scheduleVo, nextClass)); // 下一班次计划
            BigDecimal newCurrentPlanQty = currentPlanQty;
            BigDecimal newNextPlanQty = nextPlanQty;
            BigDecimal twoClassPlanQty = BigDecimalUtils.add(currentPlanQty, nextPlanQty);
            Double nextCxClassSort = this.getCxClassSort(scheduleVo, nextClass); // 下一班成型需求顺位

            BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getSidewallCode(), standardCurlLength); // 卷长
            BigDecimal toolCapacity = this.checkIsVMI(scheduleVo)? curlLength: tcVmMinRollNum.multiply(curlLength); // 满工装长度

            boolean isEqualShare = false;
            if (twoClassPlanQty.compareTo(bisectThreshold) > 0) { // 超过指定计划量，则以工装的为单位平分
                BigDecimal totalStockQty = BigDecimalUtils.add(stockQty, planQtyCumulative);
                BigDecimal newPlanQty = BigDecimalUtils.ceil(BigDecimalUtils.half(twoClassPlanQty), toolCapacity); // 平分后的计划量，先换算成工装数，平分后再换算成米数
                BigDecimal lackStock = BigDecimalUtils.sub(nextCxClassSort <= 1? nextCxPlanQtyCumulative: cxPlanQtyCumulative, totalStockQty); // 均分后的库存缺口，如果下个班成型需求是第一顺位，则库存缺口要算到下一个班
                if (lackStock.compareTo(BigDecimal.ZERO) > 0) { // 如果有库存缺口，则均分后本班计划不能低于缺口
                    newPlanQty = BigDecimalUtils.greatest(BigDecimalUtils.ceil(lackStock, toolCapacity), newPlanQty); // 用库存缺口取整后的量比较
                }
                // 如果均分后会造成早班有库存缺口，则不做均分
                newCurrentPlanQty = BigDecimalUtils.least(newPlanQty, twoClassPlanQty); // 取整后的量不能超过总量
                newNextPlanQty = twoClassPlanQty.subtract(newCurrentPlanQty); // 夜班计划 = 总计划 - 早班计划
                isEqualShare = true;
            }
            if (!isEqualShare){ // 没有达到阈值的，合并计划
//              if (!this.checkIsVMI(scheduleVo) && !isEqualShare){ // 没有达到阈值的，合并计划（一次法规格除外）
                if (newCurrentPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    newCurrentPlanQty = twoClassPlanQty;
                    newNextPlanQty = BigDecimal.ZERO;
                }
            }
            scheduleVo.setIsEqualShare(isEqualShare);
            this.setPlanQty(scheduleVo, currentClass, newCurrentPlanQty.doubleValue());
            this.setPlanQty(scheduleVo, nextClass, newNextPlanQty.doubleValue());

            classTotalPlanQty = classTotalPlanQty.subtract(currentPlanQty).add(newCurrentPlanQty);
            nextClassTotalPlanQty = nextClassTotalPlanQty.subtract(nextPlanQty).add(newNextPlanQty);
        }
        classPlanQtyMap.put(currentClass, classTotalPlanQty);
        classPlanQtyMap.put(nextClass, nextClassTotalPlanQty);
    }

    /**
     * 设定计划量到排产计划指定班次中
     * @param scheduleVo
     * @param currentClass
     * @param planQty
     * @return
     */
    private void setPlanQty(TcScheduleResultVo scheduleVo, OpenMachineClassEnums currentClass, Double planQty) {
        if (currentClass == OpenMachineClassEnums.CLASS_TWO) {
            scheduleVo.setDayPlanQty(planQty);
        } else if (currentClass == OpenMachineClassEnums.CLASS_THREE) {
            scheduleVo.setNightPlanQty(planQty);
        } else if (currentClass == OpenMachineClassEnums.CLASS_FOUR) {
            scheduleVo.setNextDayPlanQty(planQty);
        }
    }

    /**
     * 初始化各机台各班的产能限制列表
     *
     * @param machineQuata   机台产能
     * @param scheduleList   排产计划
     * @param allMachineList 机台列表
     * @param productStockDay 预生产库存天数
     * @return
     */
    private Map<String, Map<OpenMachineClassEnums, BigDecimal>> initLimitQuataMap(Map<String, BigDecimal> machineQuata,
                                                                                  List<TcScheduleResultVo> scheduleList, List<TcMachineInfo> allMachineList, double productStockDay, Map<String, BigDecimal> machineSubQuotaMap) {
        Map<String, Map<OpenMachineClassEnums, BigDecimal>> limitQuataMap = new HashMap<>();
        // 限制各班次的产能（符合条件的情况下）
        for (TcMachineInfo machine : allMachineList) {
            String openMachineClass = machine.getOpenMachineClass();
            String machineId = String.valueOf(machine.getId());
            Map<OpenMachineClassEnums, BigDecimal> classQuataMap = new HashMap<>();
            OpenMachineClassEnums currentClass = OpenMachineClassEnums.CLASS_TWO; // 从夜班开始
            while (currentClass.getClassIndex() < OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) { // 只统计2、3班
                BigDecimal quata = machine.getQuata();
                if (productStockDay < 1) { // 根据生产天数限制产能，只有少于一天的时候要限制
                    quata = quata.multiply(BigDecimalUtils.valueOf(productStockDay)).setScale(0, RoundingMode.DOWN);
                }
                if (StringUtils.isEmpty(openMachineClass) || !openMachineClass.contains(String.valueOf(currentClass.getClassIndex()))) {
                    quata = BigDecimal.ZERO; // 检查如果本班不开机则产能归0
                }
                // 机台维修时间扣减对应生产定额
                String mapKey = GenerageMapKeyUtils.createMapKey(machineId, String.valueOf(currentClass.getClassIndex()));
                if (machineSubQuotaMap.containsKey(mapKey)) {
                    BigDecimal subQuota = machineSubQuotaMap.get(mapKey);
                    quata = BigDecimal.ZERO.compareTo(BigDecimalUtils.sub(quata, subQuota)) > 0 ? BigDecimal.ZERO : quata.subtract(subQuota);
                }
                classQuataMap.put(currentClass, quata);
                currentClass = currentClass.getNextClass();
            }

            if (!classQuataMap.isEmpty()) {
                limitQuataMap.put(machineId, classQuataMap);
            }
        }
        return limitQuataMap;
    }

    /**
     * 初始化各班各机台计划列表
     *
     * @param scheduleMachineMap 按机台分组后的排产计划
     * @return <机台，<班次，计划量>>
     */
    private Map<String, Map<OpenMachineClassEnums, BigDecimal>> initClassMachinePlanQtyMap(
            Map<String, List<TcScheduleResultVo>> scheduleMachineMap) {
        Map<String, Map<OpenMachineClassEnums, BigDecimal>> classMachinePlanQtyMap = new HashMap<>();
        OpenMachineClassEnums currentClass = OpenMachineClassEnums.CLASS_TWO;
        while (currentClass.getClassIndex() <= OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) { // 遍历3个班的计划
            for (Entry<String, List<TcScheduleResultVo>> entry : scheduleMachineMap.entrySet()) {
                String machineId = entry.getKey();
                List<TcScheduleResultVo> scheduleMachineList = entry.getValue();
                Map<OpenMachineClassEnums, BigDecimal> classPlanQtyMap = classMachinePlanQtyMap.get(machineId);
                if (classPlanQtyMap == null) {
                    classPlanQtyMap = new HashMap<>();
                    classMachinePlanQtyMap.put(machineId, classPlanQtyMap);
                }
                for (TcScheduleResultVo scheduleVo : scheduleMachineList) {
                    Double planQty = this.getPlanQty(scheduleVo, currentClass);
                    BigDecimal machinePlanQty = classPlanQtyMap.get(currentClass);
                    classPlanQtyMap.put(currentClass, BigDecimalUtils.add(planQty, machinePlanQty));
                }
            }
            currentClass = currentClass.getNextClass();
            if (currentClass == null) {
                break;
            }
        }
        return classMachinePlanQtyMap;
    }

    /**
     * 获取排产计划指定班次的计划量
     *
     * @param scheduleVo
     * @param currentClass
     * @return
     */
    private Double getPlanQty(TcScheduleResultVo scheduleVo, OpenMachineClassEnums currentClass) {
        if (currentClass == OpenMachineClassEnums.CLASS_ONE) {
            return scheduleVo.getLastMidPlanQty();
        } else if (currentClass == OpenMachineClassEnums.CLASS_TWO) {
            return scheduleVo.getDayPlanQty();
        } else if (currentClass == OpenMachineClassEnums.CLASS_THREE) {
            return scheduleVo.getNightPlanQty();
        } else if (currentClass == OpenMachineClassEnums.CLASS_FOUR) {
            return scheduleVo.getNextDayPlanQty();
        }
        return 0D;
    }

    /**
     * 获取成型排产顺序
     *
     * @param scheduleVo
     * @param currentClass
     */
    private Double getCxClassSort(TcScheduleResultVo scheduleVo, OpenMachineClassEnums currentClass) {
        if (currentClass == OpenMachineClassEnums.CLASS_TWO) {
            return scheduleVo.getClass2Sort();
        } else if (currentClass == OpenMachineClassEnums.CLASS_THREE) {
            return scheduleVo.getClass3Sort();
        } else if (currentClass == OpenMachineClassEnums.CLASS_FOUR) {
            return scheduleVo.getClass4Sort();
        }
        return 9D;
    }

    /**
     * 判断是否一次法规格
     * @param scheduleVo
     * @return
     */
    private boolean checkIsVMI(TcScheduleResultVo scheduleVo) {
        return checkIsVMI(scheduleVo.getSidewallCode());
    }

    /**
     * 判断是否一次法规格
     * @param scheduleVo
     * @return
     */
    private boolean checkIsVMI(String sidewallCode) {
        if (StringUtils.isEmpty(sidewallCode)) {
            return false;
        }
        return sidewallCode.contains("VM");
    }

    /**
     * 更新计划量统计对象
     * @param scheduleList
     * @param totalPlanQtyVo
     */
    private Map<String, TcTotalPlanQtyVo> initMachineTotalPlanQtyMap(List<TcScheduleResultVo> scheduleList) {
        Map<String, TcTotalPlanQtyVo> machineTotalPlanQtyMap = new HashMap<>();
        Map<String, List<TcScheduleResultVo>> machineScheduleMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineId()))
                .collect(Collectors.groupingBy(TcScheduleResultVo::getMachineId));
        for (Entry<String, List<TcScheduleResultVo>> entry: machineScheduleMap.entrySet()) {
            String machineId = entry.getKey();
            List<TcScheduleResultVo> machineScheduleList = entry.getValue();
            Double totalDayPlanQty = machineScheduleList.stream().mapToDouble(TcScheduleResultVo::getDayPlanQty).sum();
            Double totalNightPlanQty = machineScheduleList.stream().mapToDouble(TcScheduleResultVo::getNightPlanQty).sum();
            Double totalNextDayPlanQty = machineScheduleList.stream().mapToDouble(TcScheduleResultVo::getNextDayPlanQty).sum();
            TcTotalPlanQtyVo totalPlanQtyVo = new TcTotalPlanQtyVo();
            totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty);
            totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty);
            totalPlanQtyVo.setTotalNextDayPlanQty(totalNextDayPlanQty);
            totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalDayPlanQty, totalNightPlanQty, totalNextDayPlanQty));
            machineTotalPlanQtyMap.put(machineId, totalPlanQtyVo);
        }
        return machineTotalPlanQtyMap;
    }

    private Map<String, String> loadLastDayMidPlan4Glue(String scheduleDate) {
        return tcEngineStockMapper.listLastDayMidPlan4Glue(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getGlueCode()))
                .collect(Collectors.toMap(TcGlueOrderVo::getGlueCode, TcGlueOrderVo::getMachineId,
                        (v1, v2) -> String.join(",", v1, v2)));
    }

    /**
     * 加载当天库存
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadTcStock(String scheduleDate) {
        return tcEngineStockMapper.listTcStock(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getSidewallCode()))
                .collect(Collectors.toMap(TcStockVo::getSidewallCode, TcStockVo::getStockNum));
    }

    /**
     * 加载上一天的早班计划
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadLastDayMidPlan(String scheduleDate) {
        return tcEngineStockMapper.listLastDayMidPlan(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getSidewallCode()))
                .collect(Collectors.toMap(TcStockConsumeVo::getSidewallCode, TcStockConsumeVo::getConsume));
    }

    /**
     * 如果有富余产能，将计划提前生产
     *
     * @param scheduleList       排程结果列表
     * @param paramsMap          参数
     * @param allMachineList     机台列表
     * @param curlLengthMap      卷曲长度
     * @param standardCurlLength 标准卷曲长度
     */
    private void equilibriumMachineQuota(List<TcScheduleResultVo> scheduleList, Map<String, String> paramsMap, List<TcMachineInfo> allMachineList, Map<String, BigDecimal> curlLengthMap, BigDecimal standardCurlLength) {
        List<TcScheduleResultVo> machineNullList = scheduleList.stream().filter(item -> item.getMachineId() == null).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(machineNullList)) {
            log.error("机台为空的列表:{}", JSON.toJSONString(machineNullList));
            return;
        }
        Map<String, Double> machineDayPlanQtyMap = scheduleList.stream().collect(Collectors.groupingBy(TcScheduleResultVo::getMachineId, Collectors.summingDouble(TcScheduleResultVo::getDayPlanQty)));
        Map<String, Double> machineNightPlanQtyMap = scheduleList.stream().collect(Collectors.groupingBy(TcScheduleResultVo::getMachineId, Collectors.summingDouble(TcScheduleResultVo::getNightPlanQty)));
        Map<String, Boolean> machineDayCapacityMoreMap = new HashMap<>(16);
        Map<String, Boolean> machineNightCapacityMoreMap = new HashMap<>(16);
        for (TcMachineInfo machineInfo : allMachineList) {
            String id = String.valueOf(machineInfo.getId());
            BigDecimal quata = ObjectUtils.defaultIfNull(machineInfo.getQuata(), BigDecimal.ZERO);
            if (machineDayPlanQtyMap.containsKey(id)) {
                Double sumDayPlanQty = machineDayPlanQtyMap.get(id);
                if (sumDayPlanQty > quata.doubleValue()) {
                    machineDayCapacityMoreMap.put(id, true);
                }
            }
            if (machineNightPlanQtyMap.containsKey(id)) {
                Double sumNightPlanQty = machineNightPlanQtyMap.get(id);
                if (sumNightPlanQty > quata.doubleValue()) {
                    machineNightCapacityMoreMap.put(id, true);
                }
            }
        }
        Map<Long, TcMachineInfo> machineQuotaMap = allMachineList.stream().collect(Collectors.toMap(TcMachineInfo::getId, Function.identity()));

        String mergeMaxRoll = paramsMap.get(EngineConstants.MERGE_MAX_ROLL);
        Integer cxMergeMaxSort = Integer.valueOf(paramsMap.getOrDefault(EngineConstants.CX_MERGE_MAX_SORT, "3"));
        // 根据胶料分组，如果成型二班顺序大于等于2，将计划量从夜班移到早班
        Map<String, List<TcScheduleResultVo>> groupMap1 = scheduleList.stream().collect(Collectors.groupingBy(TcScheduleResultVo::getMachineId));
        Set<Map.Entry<String, List<TcScheduleResultVo>>> entrySet = groupMap1.entrySet();
        for (Map.Entry<String, List<TcScheduleResultVo>> entry : entrySet) {
            String machineId = entry.getKey();
            BigDecimal quota = machineQuotaMap.get(Long.valueOf(machineId)).getQuata();
            Double dayTotalPlanQty = machineDayPlanQtyMap.get(machineId);
            Double nightTotalPlanQty = machineNightPlanQtyMap.get(machineId);
            if (machineDayCapacityMoreMap.containsKey(machineId)) {
                List<TcScheduleResultVo> value = entry.getValue();
                List<TcScheduleResultVo> sortedList = value.stream().sorted(Comparator.comparing(TcScheduleResultVo::getDayPlanQty).reversed()).collect(Collectors.toList());
                for (TcScheduleResultVo scheduleVo : sortedList) {
                    // 通过计划量小于机台定额了，则直接跳出
                    if (quota == null || BigDecimal.valueOf(dayTotalPlanQty).compareTo(quota) <= 0) {
                        break;
                    }
                    Double dayPlanQty = scheduleVo.getDayPlanQty();
                    Double nightPlanQty = scheduleVo.getNightPlanQty();

                    BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getSidewallCode(), standardCurlLength);
                    BigDecimal dayRollNum = BigDecimalUtils.valueOf(dayPlanQty).divide(curlLength, 0, RoundingMode.CEILING);

                    Double stockQty = scheduleVo.getStockQty();
                    Double lastMidPlanQty = scheduleVo.getLastMidPlanQty();
                    double totalStock = stockQty + lastMidPlanQty;
                    Double cxClass1Plan = scheduleVo.getCxClass1Plan();
                    Double cxClass2Plan = scheduleVo.getCxClass2Plan();
                    double cxTotalPlan = cxClass1Plan + cxClass2Plan;

                    if (scheduleVo.getClass2Sort() != null
                            && scheduleVo.getClass2Sort() >= cxMergeMaxSort
                            && dayRollNum.compareTo(new BigDecimal(mergeMaxRoll)) <= 0
                            && totalStock >= cxTotalPlan) {
                        scheduleVo.setDayPlanQty(0D);
                        scheduleVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                        dayTotalPlanQty -= dayPlanQty;
                        nightTotalPlanQty += dayPlanQty;
                    }
                }
            }
            if (machineNightCapacityMoreMap.containsKey(machineId)) {
                List<TcScheduleResultVo> value = entry.getValue();
                List<TcScheduleResultVo> sortedList = value.stream().sorted(Comparator.comparing(TcScheduleResultVo::getNightPlanQty).reversed()).collect(Collectors.toList());
                for (TcScheduleResultVo scheduleVo : sortedList) {
                    // 通过计划量小于机台定额了，则直接跳出
                    if (quota == null || BigDecimal.valueOf(nightTotalPlanQty).compareTo(quota) <= 0) {
                        break;
                    }
                    Double dayPlanQty = scheduleVo.getDayPlanQty();
                    Double nightPlanQty = scheduleVo.getNightPlanQty();

                    BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getSidewallCode(), standardCurlLength);
                    BigDecimal dayRollNum = BigDecimalUtils.valueOf(dayPlanQty).divide(curlLength, 0, RoundingMode.CEILING);

                    Double stockQty = scheduleVo.getStockQty();
                    Double lastMidPlanQty = scheduleVo.getLastMidPlanQty();
                    double totalStock = stockQty + lastMidPlanQty + scheduleVo.getDayPlanQty();
                    Double cxClass1Plan = scheduleVo.getCxClass1Plan();
                    Double cxClass2Plan = scheduleVo.getCxClass2Plan();
                    Double cxClass3Plan = scheduleVo.getCxClass3Plan();
                    Double cxClass4Plan = scheduleVo.getCxClass4Plan();
                    double cxTotalPlan = cxClass1Plan + cxClass2Plan + cxClass3Plan + cxClass4Plan;

                    if (scheduleVo.getClass3Sort() != null
                            && scheduleVo.getClass3Sort() >= cxMergeMaxSort
                            && totalStock >= cxTotalPlan) {
                        scheduleVo.setNightPlanQty(0D);
                        scheduleVo.setNextDayPlanQty(BigDecimalUtil.add(scheduleVo.getNextDayPlanQty(), nightPlanQty));
                        nightTotalPlanQty -= nightPlanQty;
                    }
                }
            }
            machineDayPlanQtyMap.put(machineId, dayTotalPlanQty);
            machineNightPlanQtyMap.put(machineId, nightTotalPlanQty);
        }

        double cxMergeMinSort = Double.parseDouble(paramsMap.getOrDefault(EngineConstants.CX_MERGE_MIN_SORT, "-1"));
        // 看夜班还有没有富余产能，如果还有，就把早班的量从大到小的顺序移到早班生产，直到产能满足
        List<TcScheduleResultVo> nightPlanSortList = scheduleList.stream().sorted(Comparator.comparing(TcScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
        for (TcScheduleResultVo scheduleResultVo : nightPlanSortList) {
            String machineId = scheduleResultVo.getMachineId();
            Double dayTotalPlanQty = machineDayPlanQtyMap.get(machineId);
            TcMachineInfo machineInfo = machineQuotaMap.get(Long.valueOf(machineId));
            BigDecimal quota = machineInfo.getQuata();
            if (quota == null) {
                continue;
            }
            if (dayTotalPlanQty >= quota.doubleValue()) {
                // 夜班产能超定额了，如果成型顺序大于等于2的，且计划总量超过定额360，将夜班移到早班
                double moreThanPlan = dayTotalPlanQty - quota.doubleValue();
                if (scheduleResultVo.getClass3Sort() != null && scheduleResultVo.getClass3Sort() > cxMergeMinSort && moreThanPlan >= 360) {
                    Double dayPlanQty = scheduleResultVo.getDayPlanQty();
                    Double nightPlanQty = scheduleResultVo.getNightPlanQty();
                    double addResultPlan = dayPlanQty + nightPlanQty;
                    scheduleResultVo.setDayPlanQty(0D);
                    scheduleResultVo.setNightPlanQty(addResultPlan);
                    machineDayPlanQtyMap.put(machineId, dayTotalPlanQty - dayPlanQty);
                    machineNightPlanQtyMap.put(machineId, machineNightPlanQtyMap.get(machineId) + nightPlanQty);
                }
                continue;
            }
            Double dayPlanQty = scheduleResultVo.getDayPlanQty();
            Double nightPlanQty = scheduleResultVo.getNightPlanQty();
            double addResultPlan = dayPlanQty + nightPlanQty;
            scheduleResultVo.setDayPlanQty(addResultPlan);
            scheduleResultVo.setNightPlanQty(0D);
            machineDayPlanQtyMap.put(machineId, dayTotalPlanQty + nightPlanQty);
            machineNightPlanQtyMap.put(machineId, machineNightPlanQtyMap.get(machineId) - nightPlanQty);
        }

        // 看早班还有没有富余产能，如果还有，就把次日夜班的量从大到小的顺序移到早班生产，直到产能满足
        List<TcScheduleResultVo> nextDayPlanSortList = scheduleList.stream().sorted(Comparator.comparing(TcScheduleResultVo::getNextDayPlanQty)).collect(Collectors.toList());
        for (TcScheduleResultVo scheduleResultVo : nextDayPlanSortList) {
            String machineId = scheduleResultVo.getMachineId();
            Double nightTotalPlanQty = machineNightPlanQtyMap.get(machineId);
            TcMachineInfo machineInfo = machineQuotaMap.get(Long.valueOf(machineId));
            BigDecimal quota = machineInfo.getQuata();
            if (quota == null) {
                continue;
            }
            if (nightTotalPlanQty >= quota.doubleValue()) {
                continue;
            }
            Double nightPlanQty = scheduleResultVo.getNightPlanQty();
            Double nextDayPlanQty = scheduleResultVo.getNextDayPlanQty();
            BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleResultVo.getSidewallCode(), standardCurlLength);
            BigDecimal nextDayRollNum = BigDecimal.valueOf(nextDayPlanQty).divide(curlLength, 0, RoundingMode.CEILING);
            // 次日夜班的计划大于2卷，才将计划量移到早班生产
            if (nextDayRollNum.compareTo(BigDecimal.valueOf(2)) > 0) {
                double addResultPlan = nightPlanQty + nextDayPlanQty;
                scheduleResultVo.setNightPlanQty(addResultPlan);
                scheduleResultVo.setNextDayPlanQty(0D);
                machineNightPlanQtyMap.put(machineId, nightTotalPlanQty + nextDayPlanQty);
            }
        }
    }

    /**
     * 均衡第一天夜班与第二天的计划
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 成型中班和夜班总计划量Vo
     */
    private void equilibriumDay1(List<TcScheduleResultVo> scheduleList, TcTotalPlanQtyVo totalPlanQtyVo,
                                 Map<String, BigDecimal> curlLengthMap, BigDecimal standardCurlLength) {
        double totalDayPlanQty = totalPlanQtyVo.getTotalDayPlanQty(); // 夜班总计划量
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划量
        double totalNextDayPlanQty = totalPlanQtyVo.getTotalNextDayPlanQty(); // 次日夜班总计划量
        double midPlanQtyReference = Arrays.asList(totalDayPlanQty, totalNightPlanQty, totalNextDayPlanQty).stream().mapToDouble(Double::doubleValue).average().getAsDouble(); // 计划平均值
        midPlanQtyReference = Math.ceil(midPlanQtyReference);
        double difNum = BigDecimalUtil.sub(totalDayPlanQty, midPlanQtyReference); // 早班和平均值的差值
        if (difNum == 0) {
            return;
        }
        boolean isNightClassPass = difNum > 0; // 夜班是否超量
        scheduleList = scheduleList.stream().sorted((r1, r2) -> {
            BigDecimal classStock1 = BigDecimalUtils.sub(r1.getClassStock(), r1.getCxClass3Plan());
            BigDecimal classStock2 = BigDecimalUtils.sub(r2.getClassStock(), r2.getCxClass3Plan());
            if (isNightClassPass) {
                // 夜班超量，将交接班库存较充足的转移到早班（倒序）
                return classStock2.compareTo(classStock1);
            } else {
                // 早班超量，将交接班库存较充低的转移到夜班（顺序）
                return classStock1.compareTo(classStock2);
            }
        }).collect(Collectors.toList());

        for (TcScheduleResultVo scheduleVo : scheduleList) {
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) {
                continue; // 收尾规格不处理
            }
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal nextDayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
            BigDecimal toolCapacity = curlLengthMap.getOrDefault(scheduleVo.getSidewallCode(), standardCurlLength); // 满工装长度
            BigDecimal cxNextAllDayPlanQty = BigDecimalUtils.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan()); // 隔天成型需求总量
            double classStock2 = scheduleVo.getClassStock();

            // 二次法最少4卷起
            if (!this.checkIsVMI(scheduleVo)) {
                toolCapacity = toolCapacity.multiply(BigDecimal.valueOf(4));
            }

            BigDecimal dayAddPlan = BigDecimal.ZERO; // 夜班增加量
            BigDecimal nightAddPlan = BigDecimal.ZERO; // 早班增加量
            BigDecimal nextDayAddPlan = BigDecimal.ZERO; // 次日夜班增加量
            if (isNightClassPass) { // 夜班超量，则从夜班转移到隔天早班
                if (dayPlanQty.compareTo(BigDecimal.ZERO) <= 0) { // 一个工装以上才处理
                    continue;
                }
                if (classStock2 <= toolCapacity.doubleValue()) { // 交接班库存不足一个工装的也不处理
                    continue;
                }
                nightAddPlan = BigDecimalUtils.least(toolCapacity, dayPlanQty);
                dayAddPlan = nightAddPlan.negate();
            } else if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) { // 隔天超量，且早班大于0，则从早班转移到夜班
                if (cxNextAllDayPlanQty.compareTo(toolCapacity) < 0) { // 成型一整天的需求量不足一个工装的，不移动
                    continue;
                }
                dayAddPlan = BigDecimalUtils.least(toolCapacity, nightPlanQty);
                nightAddPlan = dayAddPlan.negate();
            } else {
                continue;
            }
            // 先算一下是否调整后差异反而更大
            double newTotalDayPlanQty = BigDecimalUtils.add(totalDayPlanQty, dayAddPlan).doubleValue();
            double newDifNum = BigDecimalUtil.sub(newTotalDayPlanQty, midPlanQtyReference); // 早班和平均值的差值
            if (Math.abs(newDifNum) > Math.abs(difNum)) { // 如果更大跳过该规格
                continue;
            }
            // 更新各班计划量
            scheduleVo.setDayPlanQty(dayPlanQty.add(dayAddPlan).doubleValue());
            scheduleVo.setNightPlanQty(nightPlanQty.add(nightAddPlan).doubleValue());
            scheduleVo.setNextDayPlanQty(nextDayPlanQty.add(nextDayAddPlan).doubleValue());
            totalDayPlanQty = newTotalDayPlanQty;
            totalNightPlanQty = BigDecimalUtils.add(totalNightPlanQty, nightAddPlan).doubleValue();
            totalNextDayPlanQty = BigDecimalUtils.add(totalNextDayPlanQty, nextDayAddPlan).doubleValue();
            difNum = newDifNum;
            if (isNightClassPass ^ difNum > 0) { // 如果计算前后差值符号相反则直接结束
                break;
            }
        }
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalNextDayPlanQty(totalNextDayPlanQty); // 次日夜班总计划量
    }

    /**
     * 均衡第二天早夜班库存
     *
     * @param scheduleList    排程列表
     * @param totalPlanQtyVo  中班和夜班总计划量Vo
     * @param bisectThreshold 中夜班平分阈值，超过该数值的计划中夜班平分
     */
    private void equilibriumDay2(List<TcScheduleResultVo> scheduleList, TcTotalPlanQtyVo totalPlanQtyVo, BigDecimal bisectThreshold) {
//        TcScheduleResultVo firstScheduleVo = CollectionUtil.firstElement(scheduleList);
//        if (firstScheduleVo != null) {//单规格排产数量达到设定值时，中夜班数量对半分
//            this.equalShare(firstScheduleVo.getBatchNo(), scheduleList, totalPlanQtyVo, bisectThreshold);
//        }
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划里量
        double totalNextDayPlanQty = totalPlanQtyVo.getTotalNextDayPlanQty(); // 次日夜班总计划量
        double difNum = BigDecimalUtil.sub(totalNextDayPlanQty, totalNightPlanQty); //早班和次日夜班的计划量差额
        if (difNum == 0) {
            return;
        }

        boolean isDayClassPass = difNum < 0;  //true：早班超量，false：次日夜班超量
        if (isDayClassPass) {
            // 早班超量，说明库存不足，需要从供需比例较大的（库存比较足的）开始调整
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(TcScheduleResultVo::getSupplyDemandRatio, Comparator.reverseOrder())).collect(Collectors.toList());
        } else {
            // 次日夜班超量，说明库存充足，都再提前做隔天的，需要从供需比例较小的（库存比较小的）开始调整
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(TcScheduleResultVo::getSupplyDemandRatio)).collect(Collectors.toList());
        }

        for (TcScheduleResultVo scheduleVo : scheduleList) {
            boolean isCloseOutSpec = ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag()); // 是否收尾规格
            double nightPlanQty = scheduleVo.getNightPlanQty();
            double nextDayPlanQty = scheduleVo.getNextDayPlanQty();
            if (isCloseOutSpec) { // 收尾规格不调整
                continue;
            }
            if (nightPlanQty == nextDayPlanQty) { // 中夜班计划量相等的不调整
                continue;
            }
            // 尝试平衡第二天早夜半的计划量
            double classStock2 = scheduleVo.getClassStock();
            double cxPlanQty2 = BigDecimalUtil.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan());// 第二天成型两个班消耗量
            boolean isNightPlanQtyLarger = nightPlanQty > nextDayPlanQty; // 本规格早班计划量较大
            boolean isTotalNightPlanQtyLarger = totalNightPlanQty > totalNextDayPlanQty; // 合计值早班计划量较大
            double diffPlanQty = BigDecimalUtil.sub(nextDayPlanQty, nightPlanQty); // 本计划的差异值，次日夜班 - 早班
            /*if (new BigDecimal(scheduleVo.getSpecSize()).compareTo(BIG_SIZE_SPEC) >= 0) { // 大尺寸，要同时判断大尺寸规格的总计划量
                boolean isBigSizeNightPlanQtyLarger = bigSizeNgintPlanQty > bigSizeDayPlanQty; // 大规格夜班总计划量较大
                if (isNightPlanQtyLarger != isBigSizeNightPlanQtyLarger) { // 本规格计划量较高的班次与大规格的相同才有必要调换
                    continue;
                }
            } else */
            if (isNightPlanQtyLarger != isTotalNightPlanQtyLarger) { // 本规格计划量较高的班次与总计划的相同才有必要调换
                continue;
            } else if (classStock2 < cxPlanQty2 && nextDayPlanQty <= 0) { // 如果交接班库存不足，且夜班计划量为0，则不动
                continue;
            } else if (Math.abs(diffPlanQty) > Math.abs(difNum)) { // 如果差异值超过了总差异，则不处理
                continue;
            }
            double tempNightPlanQty = nightPlanQty;
            nightPlanQty = nextDayPlanQty;
            nextDayPlanQty = tempNightPlanQty;
            scheduleVo.setNightPlanQty(nightPlanQty);
            scheduleVo.setNextDayPlanQty(nextDayPlanQty);
            totalNightPlanQty = BigDecimalUtil.add(totalNightPlanQty, diffPlanQty); // 总早班更新为：总早班 + (次日夜班 - 早班)
            totalNextDayPlanQty = BigDecimalUtil.sub(totalNextDayPlanQty, diffPlanQty); // 总夜班更新为：总夜班 - (次日夜班 - 早班)
            difNum = BigDecimalUtil.sub(totalNextDayPlanQty, totalNightPlanQty); // 重算差异
            if (isDayClassPass ^ difNum < 0) { // 如果计算前后差值符号相反则直接结束
                break;
            }
        }
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalNextDayPlanQty(totalNextDayPlanQty); // 次日夜班总计划量
    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleList         排程列表
     * @param specifyCanMachineMap 定点机台中限制作业map
     * @param specifyNotMachineMap 定点机台中不可作业
     * @param mouthPlateMachineMap 口型板代码map
     * @param disableMouthPlateMachineMap 已禁用口型板代码map
     * @param lastDayGlueMachineMap 昨日早班胶料与机台信息
     */
    private void chooseMachineByCapacity(List<TcScheduleResultVo> scheduleList, List<TcMachineInfo> allMachineList,
            Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
            Map<String, String> mouthPlateMachineMap, Map<String, String> disableMouthPlateMachineMap,
                                         Map<String, String> lastDayGlueMachineMap, Map<String, BigDecimal> machineSubQuotaMap) {
        // 机台夜班已占用产能
        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(16);
        // 机台白班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(16);

        // 获取规格仅可选择一个机台的 map:<班次, List<规格>>
        Map<String, List<String>> classCodeMap = new HashMap<>(16);
        for (TcScheduleResultVo scheduleVo : scheduleList) {
            // 胎侧代码
            String beadCode = scheduleVo.getSidewallCode();
            // 口型板code
            String mouthPlateCode = scheduleVo.getMouthPlateCode();
            // 定点机台ID列表
            String specifyMachineIds = specifyCanMachineMap.get(beadCode);
            String mouthPlateMachineIds = mouthPlateMachineMap.getOrDefault(mouthPlateCode, StringUtils.EMPTY);
//            String disableMouthPlateMachineIds = disableMouthPlateMachineMap.getOrDefault(mouthPlateCode, StringUtils.EMPTY);
            List<String> machineIds;
            // 如果有设置定点机台，需要把非定点全部过滤掉
            if (StringUtils.isNotEmpty(specifyMachineIds)) {
                machineIds = Arrays.asList(specifyMachineIds.split(","));
            } else {
                machineIds = new ArrayList<>(0);
            }
            // 可选机台
            List<TcMachineInfo> oneFilterList = allMachineList.stream().filter(m -> {
                        // 排除定点不可生产机台
                        String machineId = String.valueOf(m.getId());
                        String notMachine = specifyNotMachineMap.get(beadCode);
                        if (StringUtils.isEmpty(notMachine)) {
                            return true;
                        }
                        String[] notMachineIds = notMachine.split(",");
                        for (String notMachineId : notMachineIds) {
                            return Objects.equals(machineId, notMachineId);
                        }
                        return true;
                    }).filter(m -> {
                        // 如果有设置定点机台，则仅选中定点机台
                        if (CollectionUtils.isNotEmpty(machineIds)) {
                            return machineIds.contains(String.valueOf(m.getId()));
                        }
                        return true;
                    }).filter(m -> {
                        // 如果机台支持全口型，则不受口型影响
                        if (ApsConstant.STATUS_ENABLE.equals(m.getSupportsAllMouthFlag())) {
                            return true;
                        }
                        // 不是支持所有口型的机台，则必须要配置口型板机台
                        return mouthPlateMachineIds.contains(String.valueOf(m.getId()));
                    })
                    .collect(Collectors.toList());
            List<TcMachineInfo> nightClassMachineList = oneFilterList.stream().filter(item -> item.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))).collect(Collectors.toList());
            List<TcMachineInfo> dayClassMachineList = oneFilterList.stream().filter(item -> item.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))).collect(Collectors.toList());
            if (nightClassMachineList.size() == 1) {
                List<String> nightClassCodeList = classCodeMap.getOrDefault(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()), new ArrayList<>());
                nightClassCodeList.add(beadCode);
                classCodeMap.put(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()), nightClassCodeList);
            }
            if (dayClassMachineList.size() == 1) {
                List<String> dayClassCodeList = classCodeMap.getOrDefault(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()), new ArrayList<>());
                dayClassCodeList.add(beadCode);
                classCodeMap.put(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()), dayClassCodeList);
            }
        }

        // 先对排产计划
        // 夜班
        String classCode = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex());
        List<String> nightClassOneMachineList = classCodeMap.getOrDefault(classCode, new ArrayList<>());
        List<TcScheduleResultVo> chooseMachineScheduleList = scheduleList.stream()
                .sorted((o1, o2) -> {
                    // 一次法只能在1号线生产，优先分一次法的
                    Integer oneMethodFlag1 = this.checkIsVMI(o1) ? 1 : 2;
                    Integer oneMethodFlag2 = this.checkIsVMI(o2) ? 1 : 2;
                    if (oneMethodFlag1.compareTo(oneMethodFlag2) != 0) {
                        return oneMethodFlag1.compareTo(oneMethodFlag2);
                    }
                    // 哪个班次只有一个机台，只有一个机台的先选机台
                    Integer oneMachineFlag1 = nightClassOneMachineList.contains(o1.getSidewallCode()) ? 1 : 2;
                    Integer oneMachineFlag2 = nightClassOneMachineList.contains(o2.getSidewallCode()) ? 1 : 2;
                    if (oneMachineFlag1.compareTo(oneMachineFlag2) != 0) {
                        return oneMachineFlag1.compareTo(oneMachineFlag2);
                    }

                    Integer flag1 = specifyCanMachineMap.containsKey(o1.getSidewallCode()) ? 1 : 2;
                    Integer flag2 = specifyCanMachineMap.containsKey(o2.getSidewallCode()) ? 1 : 2;
                    if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                        return flag1.compareTo(flag2);
                    }
                    // 如果定点机台设置一样，则按计划量从大到小
                    BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o1.getNightPlanQty());
                    BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
                    return planQty2.compareTo(planQty1);
                }).collect(Collectors.toList());

        // 根据夜班计划分配机台
        for (TcScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            Double midPlanQty = scheduleVo.getDayPlanQty();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            List<TcMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            TcMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果早班不作业，则把计划量都转移到夜班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))) {
                scheduleVo.setDayPlanQty(BigDecimalUtil.add(midPlanQty, scheduleVo.getNightPlanQty()));
                scheduleVo.setNightPlanQty(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty())));
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap); // 添加日志
        }

        // 早班
        classCode = String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex());
        List<String> dayClassOneMachineList = classCodeMap.getOrDefault(classCode, new ArrayList<>());
        chooseMachineScheduleList = chooseMachineScheduleList.stream()
                .sorted((o1, o2) -> {
                    // 一次法只能在1号线生产，优先分一次法的
                    Integer oneMethodFlag1 = this.checkIsVMI(o1) ? 1 : 2;
                    Integer oneMethodFlag2 = this.checkIsVMI(o2) ? 1 : 2;
                    if (oneMethodFlag1.compareTo(oneMethodFlag2) != 0) {
                        return oneMethodFlag1.compareTo(oneMethodFlag2);
                    }
                    // 哪个班次只有一个机台，只有一个机台的先选机台
                    Integer oneMachineFlag1 = dayClassOneMachineList.contains(o1.getSidewallCode()) ? 1 : 2;
                    Integer oneMachineFlag2 = dayClassOneMachineList.contains(o2.getSidewallCode()) ? 1 : 2;
                    if (oneMachineFlag1.compareTo(oneMachineFlag2) != 0) {
                        return oneMachineFlag1.compareTo(oneMachineFlag2);
                    }

                    Integer flag1 = specifyCanMachineMap.containsKey(o1.getSidewallCode()) ? 1 : 2;
                    Integer flag2 = specifyCanMachineMap.containsKey(o2.getSidewallCode()) ? 1 : 2;
                    if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                        return flag1.compareTo(flag2);
                    }
                    // 如果定点机台设置一样，则按计划量从大到小
                    BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o1.getNightPlanQty());
                    BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
                    return planQty2.compareTo(planQty1);
                }).collect(Collectors.toList());

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (TcScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                continue;
            }
            List<TcMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    // 检索当班可选机台
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap);
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            TcMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果夜班不作业，则把计划量都转移到早班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))) {
                scheduleVo.setNightPlanQty(BigDecimalUtil.add(scheduleVo.getDayPlanQty(), scheduleVo.getNightPlanQty()));
                scheduleVo.setDayPlanQty(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty())));
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap); // 添加日志
        }

        for (TcScheduleResultVo scheduleVo : scheduleList) {
            Double supplyTime = scheduleVo.getSupplyTime();
            // 只有供应时长超过12小时的才能提前
            if (supplyTime >= Integer.parseInt(DEFAULT_PRODUCT_STOCK_HOUR)) {
                String glueCode = scheduleVo.getGlueCode();
                // 昨日早班相同胶料的夜班优先
                if (lastDayGlueMachineMap.containsKey(glueCode)) {
                    String machineIdStr = scheduleVo.getMachineId();
                    // 机台也要匹配，因为14535可以在两个机台上生产，可能两个机台最后生产都是相同胶料
                    if (StringUtils.isNotBlank(machineIdStr) &&
                            machineIdStr.contains(lastDayGlueMachineMap.get(glueCode))) {
                        scheduleVo.setDayProduceOrderFlag(ApsConstant.PRODUCT_ORDER_FLAG);
                    }
                }
            }
        }
    }
    /**
     * 获取当天的收尾规格列表
     * @param scheduleDate	排程日期
     * @param closeOutDays	判断收尾的天数
     * @return
     */
	private List<String> getCloseOutSpecList(String scheduleDate, BigDecimal closeOutDays, String productionStage) {
		BigDecimal queryCloseOutDays = closeOutDays.compareTo(BigDecimal.ONE) >= 0? closeOutDays.subtract(BigDecimal.ONE): BigDecimal.ZERO; // 判断天数需要减1，0才是查1天的数据进行判断
		boolean isProductionStage = PRODUCTION_STAGE_ON.equals(productionStage); // 判断是否只看投产规格
		return tcEngineMapper.listCloseOutSpec(DateUtils.parseDate(scheduleDate), queryCloseOutDays.intValue(), isProductionStage);
	}

	/**
	 * 计算胎面卷曲长度
     *
	 * @param scheduleVo          胎面排程
	 * @param tmCurlLengthMap     各规格卷曲长度配置
	 * @param standardCurlLength  卷曲标准长度
	 * @param closeOutSpecList    收尾规格列表
	 * @param curlDecimalRounding 卷曲数小数取整值
	 * @param totalPlanQtyMap     每个生产线的计划量汇总MAP
	 */
	private void computeTcCurlRoll(TcScheduleResultVo scheduleVo, Map<String, BigDecimal> tmCurlLengthMap,
			BigDecimal standardCurlLength, List<String> closeOutSpecList, BigDecimal curlDecimalRounding,
			Map<String, TcTotalPlanQtyVo> totalPlanQtyMap) {
		String sidewallCode = scheduleVo.getSidewallCode();
		if (closeOutSpecList.contains(sidewallCode)) { // 收尾规格，则直接返回
	        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_ENABLE); // 打上收尾标记
			return;
		}
		BigDecimal curlLength = tmCurlLengthMap.get(sidewallCode); // 本规格的卷曲长度
		if (curlLength == null || curlLength.compareTo(BigDecimal.ZERO) <= 0) { // 不合法的配置都按默认值为准
			curlLength = standardCurlLength;
		}
		BigDecimal dayPlanQty = BigDecimalUtil.getValue(scheduleVo.getDayPlanQty());
		BigDecimal nightPlanQty = BigDecimalUtil.getValue(scheduleVo.getNightPlanQty());
		BigDecimal totalPlanQty = dayPlanQty.add(nightPlanQty); // 本规格胎面的总计划量

		BigDecimal planNum = totalPlanQty.divide(curlLength, 1, RoundingMode.UP); // 卷数，保留1位小数
		// 卷数小数部分处理
		if (planNum.subtract(planNum.setScale(0, RoundingMode.DOWN)).compareTo(curlDecimalRounding) >= 0) {
			planNum = planNum.setScale(0, RoundingMode.UP); // 如果小数部分大于等于卷曲数小数取整值，直接进位
		} else if (planNum.compareTo(curlDecimalRounding) < 0) {
			planNum = planNum.setScale(0, RoundingMode.UP); // 如果原计划卷数比最小取整卷数少，也直接进位
		} else {
			planNum = planNum.setScale(0, RoundingMode.DOWN); // 其余情况舍去小数部分
		}
		BigDecimal newPlanQty = planNum.multiply(curlLength).setScale(0, RoundingMode.UP); // 新计划量
		BigDecimal planQtyDifference = newPlanQty.subtract(totalPlanQty);

		if (planQtyDifference.compareTo(BigDecimal.ZERO) == 0) {
			return;
		}
		boolean isDay = dayPlanQty.compareTo(BigDecimal.ZERO) > 0; // 是否安排在中班
		if (isDay) {
			dayPlanQty = newPlanQty;
			scheduleVo.setDayPlanQty(dayPlanQty.doubleValue());
		} else {
			nightPlanQty = newPlanQty;
			scheduleVo.setNightPlanQty(nightPlanQty.doubleValue());
		}

		// 将增加的量补到汇总值中
		String key = scheduleVo.getMachineId(); // 机台id作为Map的key
		key = StringUtils.isBlank(key) ? "" : key;
		TcTotalPlanQtyVo totalPlanQtyVo = totalPlanQtyMap.getOrDefault(key, new TcTotalPlanQtyVo()); // 取出对应生产线的计划量汇总对象
		if (isDay) {
			totalPlanQtyVo.setTotalDayPlanQty(
					BigDecimalUtil.getValue(totalPlanQtyVo.getTotalDayPlanQty()).add(planQtyDifference).doubleValue());
		} else {
			totalPlanQtyVo.setTotalNightPlanQty(
					BigDecimalUtil.getValue(totalPlanQtyVo.getTotalNightPlanQty()).add(planQtyDifference).doubleValue());
		}
		totalPlanQtyVo.setTotalPlanQty(
				BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()));
		totalPlanQtyMap.put(key, totalPlanQtyVo);
	}

    /**
     * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
     * @param scheduleDate 排程日志
     * @param batchNo 批次号
     * @param productionStage 仅投产阶段规格排产标识
     */
    private void ValidatedConstruction(String scheduleDate, String batchNo, String productionStage, Map<String, String> mapAssistSpec) {
        List<EngineConstructionInfo> list = tcEngineMapper.listTcNeedConstruction(scheduleDate, productionStage);
        list = list.stream().filter(r -> !mapAssistSpec.containsKey(r.getSidewallCode())).collect(Collectors.toList());  //校验忽略掉 外协规格，只校验 不是外协的规格
        for(EngineConstructionInfo construction : list) {
            List<String> errorColumns = new ArrayList<>();
            String embryoCode = construction.getEmbryoCode().split(",")[0];  //成型排程结果对应的胎胚代码
            String[] versionArray = construction.getBomDataVersion().split(",");
            String embryoVersion = versionArray.length > 0 ? versionArray[0] : "";  //施工版本
            if(construction.getEmbryoCode().split(",").length < 2) {
                //施工表胎胚代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoCode") + "\"");
            }
            if(versionArray.length < 2) {
                //施工表版本为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoVersion") + "\"");
            }
            if(StringUtils.isBlank(construction.getSidewallCode())) {
                //施工表胎侧代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.sidewallCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getSidewallRubber())) {
                //施工表胎侧胶料为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.sidewallRubber") + "\"");
            }
            if(StringUtils.isBlank(construction.getSidewallMouthPlate())) {
                //施工表胎侧口型板为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.sidewallMouthPlate") + "\"");
            }
            if(construction.getSidewallLength() == null || construction.getSidewallLength() == 0) {
                //施工表胎侧长为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.sidewallLength") + "\"");
            }
            if(!errorColumns.isEmpty()) {
                String tip = StringUtils.format(I18nUtil.getMessage("engine.auto.scheule.construction.validate"), embryoCode, embryoVersion, String.join(",", errorColumns));
                autoScheduleLogService.insertTcScheduleLog(batchNo, "", "自动排程失败", tip); //添加日志
                throw new RuntimeException(tip);
            }
        }
    }

    /**
     * 把外协规格列表转成Map
     * @return
     */
    private Map<String, String> mapAssistSpec() {
        Map<String, String> map = new HashMap<>();
        List<String> listAssistSpec = this.tcEngineMapper.listAssistSpec();
        if(listAssistSpec == null || listAssistSpec.size() == 0) {
            return map;
        }
        for(String assistSpec : listAssistSpec) {
            map.put(assistSpec, "1");
        }
        return map;
    }

    /**
     * 胎侧插单
     * @param scheduleVo
     */
    public int inertTcOrder(TcScheduleResultVo scheduleVo) {
        String scheduleDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleVo.getScheduleDate()); //排程日期
        List<TcScheduleResultVo> scheduleList = new ArrayList<>();
        scheduleList.add(scheduleVo);
        return this.batchSaveTcSchedule(scheduleDate, scheduleList, true);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    public int batchSaveTcSchedule(String scheduleDate, List<TcScheduleResultVo> scheduleList) {
        return this.batchSaveTcSchedule(scheduleDate, scheduleList, false);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     * @param isUpdate 相同唯一键是否做更新操作。true：是
     */
    @Transactional(rollbackFor=Exception.class)
    public int batchSaveTcSchedule(String scheduleDate, List<TcScheduleResultVo> scheduleList, boolean isUpdate) {
        if(scheduleList == null || scheduleList.isEmpty()) {
            return -1;
        }
        String batchNo = "";
        if(isUpdate) {
            batchNo = tcEngineMapper.getTcCurrentBatchNo(scheduleDate);  //查询当前排程的批次号
        }
        if(StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“，那么自己生成一个排程批次号
            batchNo = this.createBatchNo(scheduleDate);  //胎侧排程批次号
            this.createScheduleRecord(scheduleDate, "", batchNo);  //创建自动排程记录
            this.syncTcScheduleToLog(scheduleDate);  //把排程数据同步到log表
        }
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "插单或批量导入初始数据", toJSONString(scheduleList));  //添加日志

        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        List<String> sidewallCodes = scheduleList.stream().map(TcScheduleResultVo::getSidewallCode).collect(Collectors.toList());
        Map<String, TcScheduleBaseInfoVo> scheduleBaseInfoMap = getScheduleBaseInfoMap(scheduleDate, sidewallCodes, productionStage);  //根据胎侧代码查询对应的胎侧基础信息
        Map<String, String> glueSeqMap = tcEngineGlueService.getGlueSeqMap();  //获取胶料序号map
        Map<String, Double> planStockMap = tcEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算胎侧16点预计库存
        Map<String, TcMonthSurplusVo> monthSurplus = tcEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "插单或批量导入基础数据", logSplit("半部件基础数据信息:" + toJSONString(scheduleBaseInfoMap),
                "胶料序号map：" + glueSeqMap, "16点预计库存：" + planStockMap, "月度计划剩余量、完成量：" + monthSurplus, "工序参数map：" + paramsMap));  //添加日志

        for(TcScheduleResultVo schedule : scheduleList) {
            schedule.setBatchNo(batchNo);  //批次号
            String orderNo = this.createOrderNo(batchNo); //工单号
            schedule.setOrderNo(orderNo);  //工单号
            TcScheduleBaseInfoVo baseInfoVo = scheduleBaseInfoMap.get(schedule.getSidewallCode());
            if(baseInfoVo != null) {
                BeanUtils.copyProperties(baseInfoVo, schedule);
            }
            Double dayPlanQty = schedule.getDayPlanQty();  //中班计划量
            schedule.setDayPlanQty(dayPlanQty == null ? 0D : dayPlanQty);
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            schedule.setNightPlanQty(nightPlanQty == null ? 0D : nightPlanQty);

            schedule.setGlueSeq(glueSeqMap.get(schedule.getGlueCode()));  //胶料序号
            schedule.setStockQty(planStockMap.getOrDefault(schedule.getSidewallCode(), 0D));  //16点预计库存
            this.newComputeSupplyTime(schedule, schedule.getStockQty());  //库存供应时长
            this.setStatusAndCloseTip(schedule, monthSurplus.get(schedule.getSidewallCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段
            schedule.setIsRelease(ApsConstant.NO_RELEASE);
            schedule.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            schedule.setCreateTime(new Date());
            schedule.setCreateBy(SecurityUtils.getUsername());
            schedule.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE);
        }
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "插单或批量导入最终数据", toJSONString(scheduleList));  //添加日志
        return tcEngineMapper.mergeTcScheduleResult(scheduleList);  //批量合并排程结果表（根据唯一字段，做更新或新增）
    }

    /**
     * 根据胎侧代码查询对应的胎侧基础信息
     * @param scheduleDate
     * @return
     */
    private Map<String, TcScheduleBaseInfoVo> getScheduleBaseInfoMap(String scheduleDate, List<String> sidewallCodes, String productionStage) {
//        Map<String, TcScheduleBaseInfoVo> map = new HashMap<>();
//        List<TcScheduleResultVo> list = tcEngineMapper.statTcScheduleBase(scheduleDate, productionStage);
//        for(TcScheduleResultVo info : list) {
//            TcScheduleBaseInfoVo baseInfoVo = new TcScheduleBaseInfoVo();
//            BeanUtils.copyProperties(info, baseInfoVo);
//            map.put(info.getSidewallCode(), baseInfoVo);
//        }
//        return map;

        Map<String, TcScheduleBaseInfoVo> map = new HashMap<>();
        List<TcScheduleBaseInfoVo> list = tcEngineMapper.listTcScheduleBaseInfo(sidewallCodes, ""); //查询出胎面在施工表的基础信息
        if(!StringUtils.isEmpty(list)) {
            map = list.stream().collect(Collectors.toMap(TcScheduleBaseInfoVo::getSidewallCode, baseInfoVo->baseInfoVo));
        }

        Map<String, TcScheduleBaseInfoVo> hasCxMap = new HashMap<>();
        List<TcScheduleResultVo> hasCxlist = tcEngineMapper.statTcScheduleBase(scheduleDate, productionStage); //查询出在有对应成型排程的胎面基础信息
        for(TcScheduleResultVo info : hasCxlist) {
            TcScheduleBaseInfoVo baseInfoVo = new TcScheduleBaseInfoVo();
            BeanUtils.copyProperties(info, baseInfoVo);
            hasCxMap.put(info.getSidewallCode(), baseInfoVo);
        }

        map.putAll(hasCxMap);  //有对应成型排程的胎面基础信息 覆盖掉，没有成型排程的胎面基础信息
        return map;
    }

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    public void changeTcMachine(String oldMachineIds, TcScheduleResult scheduleResult) {
//        String batchNo = scheduleResult.getBatchNo();  //批次号
//        String orderNo = scheduleResult.getOrderNo();  //工单号
//        autoScheduleLogService.insertTcScheduleLog(batchNo, orderNo, "转机台初始数据", logSplit("转机台前的机台ID：" + oldMachineIds, "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
//        Map<String, Double> lossRateMap = tcEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
//        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
//
//        //转机台后，不同机台的损耗率不一样，需要重新计算计划量
//        double oldLossRate = tcEngineLossService.getLossRate(scheduleResult.getSidewallCode(), oldMachineIds, lossRateMap, paramLossRate);  //计算出转机台前的耗损率
//        double lossRate = tcEngineLossService.getLossRate(scheduleResult.getSidewallCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);
//        autoScheduleLogService.insertTcScheduleLog(batchNo, orderNo, "转机台需要根据不同机台耗损率重新计算计划量",
//                logSplit("重新计算计划量规则：先要根据之前机台的耗损率推算出之前在没有加上耗损率之前的计划量A，然后再用计划量A * 当前机台对应的耗损率，计算出最终的计划量",
//                        "转机台前的耗损率：" + oldLossRate + "转机台后的耗损率：" + lossRate));  //添加日志
//
//        Double dayPlanQty = scheduleResult.getDayPlanQty();  //中班计划量
//        if(dayPlanQty != null) {
//            dayPlanQty = BigDecimalUtil.div(dayPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
//            scheduleResult.setDayPlanQty(dayPlanQty);
//        }
//        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
//        if(nightPlanQty != null) {
//            nightPlanQty = BigDecimalUtil.div(nightPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
//            scheduleResult.setNightPlanQty(nightPlanQty);
//        }
//        autoScheduleLogService.insertTcScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
//    }

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    public void confirmTcMachine(TcScheduleResult scheduleResult) {
        String batchNo = scheduleResult.getBatchNo();  //批次号
        String orderNo = scheduleResult.getOrderNo();  //工单号
        autoScheduleLogService.insertTcScheduleLog(batchNo, orderNo, "确认机台初始数据", logSplit( "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
        Map<String, Double> lossRateMap = tcEngineLossService.getLossRateMap();   //损耗率map
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));

        //耗损率
        double lossRate = tcEngineLossService.getLossRate(scheduleResult.getSidewallCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
        autoScheduleLogService.insertTcScheduleLog(batchNo, orderNo, "确认机台耗损率", "耗损率：" + lossRate);  //添加日志

        Double dayPlanQty = scheduleResult.getDayPlanQty();  //中班计划量
        if(dayPlanQty != null) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            scheduleResult.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty, 0));
        }
        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
        if(nightPlanQty != null) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            scheduleResult.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty, 0));
        }
        autoScheduleLogService.insertTcScheduleLog(batchNo, orderNo, "确认机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
    }

    /**
     * 手动均衡和重新设置生产顺序
     * @param scheduleDate 排程日期,格式：yyyy-mm-dd
     */
    public void handEquilibriumAndProduceOrder(String scheduleDate) {
        List<TcScheduleResultVo> scheduleList = tcEngineMapper.listTcEnginSchedule(scheduleDate);
        Map<String, BigDecimal> tcCurlLengthMap = tcEngineCurlRollService.getTcCurlLengthMap(); // 胎侧卷曲设置
        if(StringUtils.isEmpty(scheduleList)) {
            return;
        }

        String batchNo = "";
        Map<String, TcTotalPlanQtyVo> totalPlanQtyMap = new HashMap<>();  //每个生产线的计划量汇总MAP
        for(TcScheduleResultVo schedule : scheduleList ) {
            Double dayPlanQty = schedule.getDayPlanQty();  //中班计划量
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            //如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（不二次排程逻辑处理）
            if (dayPlanQty > 0) {
                dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
                nightPlanQty = 0D;
                schedule.setDayPlanQty(dayPlanQty);
                schedule.setNightPlanQty(nightPlanQty);
            }
            if(schedule.getMachineId() == null) {
                schedule.setMachineId("");
            }
            if(schedule.getSupplyTime() == null) {
                schedule.setSupplyTime(0D);
            }
            //计算中班总计划量 和 夜班总计划量
            this.groupTotalPlanQtyMap(schedule, totalPlanQtyMap);
        }

        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        this.equilibrium(scheduleList, paramsMap, totalPlanQtyMap);  //均衡
        this.equalShare(scheduleList, tcCurlLengthMap, paramsMap);  //单规格排产数量达到设定值时，中夜班数量对半分
        this.setProduceOrder(scheduleList);  //生产顺序重新计算
        tcEngineMapper.createTempTable();
        tcEngineMapper.insertTempTable(scheduleList);
        tcEngineMapper.batchUpdateProduceOrder(scheduleDate, scheduleList);  //批量更新各班的计划量和生产顺序
//        tcEngineMapper.dropTempTable();
    }

    /**
     * 手动 同胶料合并生产
     * @param scheduleDate
     */
    public void handGlueMerge(String scheduleDate) {
        List<TcScheduleResultVo> scheduleList = tcEngineMapper.listTcEnginSchedule(scheduleDate);
        if(StringUtils.isEmpty(scheduleList)) {
            return;
        }
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String batchNo = scheduleList.get(0).getBatchNo();  //批次号
        this.glueMerge(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX));  //同胶料合并生产
        tcEngineMapper.createTempTable();
        tcEngineMapper.insertTempTable(scheduleList);
        tcEngineMapper.batchUpdatePlanQty(scheduleDate, scheduleList);  //批量更新各班的计划量
//        tcEngineMapper.dropTempTable();
    }

    /**
     * 中班和夜班计排程计划量均衡处理(根据生产线进行分组均衡)
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyMap 每个生产线的计划量汇总MAP
     */
    private void equilibrium(List<TcScheduleResultVo> scheduleList, Map<String, String> paramsMap, Map<String, TcTotalPlanQtyVo> totalPlanQtyMap) {
        Map<String, List<TcScheduleResultVo>> map = scheduleList.stream().collect(Collectors.groupingBy(s->s.getMachineId()));
        scheduleList.clear();
        map.forEach((key, valueList) -> {
            this.equilibriumOne(valueList, paramsMap, totalPlanQtyMap.get(key));
            scheduleList.addAll(valueList);
        });
    }

    /**
     * 中班和夜班计排程计划量均衡处理
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 胎侧中班和夜班总计划量Vo
     */
    private void equilibriumOne(List<TcScheduleResultVo> scheduleList, Map<String, String> paramsMap, TcTotalPlanQtyVo totalPlanQtyVo) {
        String batchNo = "";  //批次号
        String oldScheduleList = toJSONString(scheduleList);
        double difRate = getDoubleOrDefault(paramsMap.get(EngineConstants.PLAN_DIFFERENCE_RATE) ,100D);  //参数配置：中班总量和夜班总量差额百分比
        double supplyTimePass = getDoubleOrDefault(paramsMap.get(EngineConstants.SUPPLY_TIME_PASS),12D);;  //参数配置：库存供应时长小时数
        double difNum = BigDecimalUtil.sub(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()); //中班和夜班计划量差额
        double actualDifRate = Math.abs(difNum) / totalPlanQtyVo.getTotalPlanQty() * 100;  //实际中班和夜班总计划量差额百分比
        if (actualDifRate > difRate) {
            //中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理
            boolean isDayClassPass = (difNum > 0);  //true：中班超量，false：夜班超量
            if (isDayClassPass) {
                //中班超量，排程结果按中班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(TcScheduleResultVo::getDayPlanQty)).collect(Collectors.toList());
            } else {
                //夜班超量，排程结果按夜班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(TcScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
            }
            //开始计划量均衡处理
            double lastDifRate = actualDifRate;  //上一次的中班总量和夜班总量的差额百分比
            for (TcScheduleResultVo resultVo : scheduleList) {
                batchNo = resultVo.getBatchNo();
                double supplyTime = resultVo.getSupplyTime() == null ? 0D : resultVo.getSupplyTime(); //库存供应时长
                double dayPlanQty = resultVo.getDayPlanQty();    //中班计划量
                double nightPlanQty = resultVo.getNightPlanQty();  //夜班计划量

                if (isDayClassPass) {  //中班超量，中班移到夜班
                    if (dayPlanQty == 0 || supplyTime <= supplyTimePass) {
                        //库存供应时长 超过supplyTimePass的， 才允许拆到夜班生产
                        continue;
                    }
                    double totalDayPlan = BigDecimalUtil.sub(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty);
                    double totalNightPlan = BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), dayPlanQty);
                    double newDifNum = BigDecimalUtil.sub(totalDayPlan, totalNightPlan); //中班和夜班计划量差额
                    double newDifRate = Math.abs(newDifNum) / totalPlanQtyVo.getTotalPlanQty() * 100;   //计算新的差额率
                    if(newDifRate >= lastDifRate) {
                        //如果调整后的差额率，比上次调整的高，那上次调整的数据是最均衡的。均衡处理全部结束
                        break;
                    }

                    resultVo.setDayPlanQty(0D);
                    resultVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    //重新计算中班和夜班的总计划量
                    totalPlanQtyVo.setTotalDayPlanQty(totalDayPlan);
                    totalPlanQtyVo.setTotalNightPlanQty(totalNightPlan);
                    lastDifRate = newDifRate;
                } else {  //夜班超量，夜班移到中班
                    if (nightPlanQty == 0) {
                        continue;
                    }
                    double totalDayPlan =  BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), nightPlanQty);
                    double totalNightPlan = BigDecimalUtil.sub(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty);
                    double newDifNum = BigDecimalUtil.sub(totalDayPlan, totalNightPlan); //中班和夜班计划量差额
                    double newDifRate = Math.abs(newDifNum) / totalPlanQtyVo.getTotalPlanQty() * 100;   //计算新的差额率
                    if(newDifRate >= lastDifRate) {
                        //如果调整后的差额率，比上次调整的高，那上次调整的数据是最均衡的。均衡处理全部结束
                        break;
                    }

                    resultVo.setDayPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    resultVo.setNightPlanQty(0D);
                    //重新计算中班和夜班的总计划量
                    totalPlanQtyVo.setTotalDayPlanQty(totalDayPlan);
                    totalPlanQtyVo.setTotalNightPlanQty(totalNightPlan);
                    lastDifRate = newDifRate;
                }
            }
        }
        this.equilibriumLog(batchNo, oldScheduleList, scheduleList, paramsMap, totalPlanQtyVo);  //添加日志
    }

    /**
     * 选择排程对应机台列表
     *
     * @param scheduleVo           排程
     * @param classCode            班制
     * @param capacityMap          机台产能map
     * @param allMachineList       所有机台
     * @param specifyCanMachineMap 定点机台
     * @param specifyNotMachineMap 不可作业机台
     * @param mouthPlateMachineMap 口型板机台
     * @return 机台列表
     */
    private List<TcMachineInfo> searchOptionalMachineList(TcScheduleResultVo scheduleVo, String classCode,
                                                          Map<Long, BigDecimal> capacityMap,
                                                          List<TcMachineInfo> allMachineList,
                                                          Map<String, String> specifyCanMachineMap,
                                                          Map<String, String> specifyNotMachineMap,
                                                          Map<String, String> mouthPlateMachineMap) {
//        BigDecimal dimension = BigDecimalUtils.valueOf(scheduleVo.getDimension()); // 寸口
        String beadCode = scheduleVo.getSidewallCode(); // 胎侧代码
        String mouthPlateCode = scheduleVo.getMouthPlateCode(); // 口型板code
        // 定点机台ID列表
        String specifyMachineIds = specifyCanMachineMap.get(beadCode);
        String mouthPlateMachineIds = mouthPlateMachineMap.getOrDefault(mouthPlateCode, StringUtils.EMPTY);
//        specifyMachineIds = StringUtils.isBlank(specifyMachineIds) ? mouthPlateMachineIds
//                : specifyMachineIds; // 从口型板中找机台
        List<String> machineIds;
        // 如果有设置定点机台，需要把非定点全部过滤掉
        if (StringUtils.isNotEmpty(specifyMachineIds)) {
            machineIds = Arrays.asList(specifyMachineIds.split(","));
        } else {
            machineIds = new ArrayList<>(0);
        }
        String glueCode = scheduleVo.getGlueCode();
        // 可选机台
        List<TcMachineInfo> optionalMachineList = allMachineList.stream()
                .filter(m -> {
                    if (this.checkIsVMI(beadCode)) { // 一次法规格，机台必须支持一次法
                        return ApsConstant.STATUS_ENABLE.equals(m.getSupportsVmiFlag());
                    }
                    return true;
//                    if (this.checkIsVMI(beadCode)) {
//                        // 1号线做一次法
//                        return m.getMachineName().contains("1号线");
//                    } else {
//                        // 二号线做二次法。除非口型板不支持2号线，安排在1号线
//                        if (StringUtils.isNotBlank(mouthPlateMachineIds) && !mouthPlateMachineIds.contains("29393")) {
//                            return m.getMachineName().contains("1号线");
//                        } else {
//                            return m.getMachineName().contains("2号线");
//                        }
//                    }
                })
                .filter(m -> {// 排除定点不可生产机台
                    String machineId = String.valueOf(m.getId());
                    String notMachine = specifyNotMachineMap.get(beadCode);
                    if (StringUtils.isEmpty(notMachine)) {
                        return true;
                    }
                    String[] notMachineIds = notMachine.split(",");
                    for (String notMachineId : notMachineIds) {
                        return Objects.equals(machineId, notMachineId);
                    }
                    return true;
                }).filter(m -> { // 如果有设置定点机台，则仅选中定点机台
                    if (CollectionUtils.isNotEmpty(machineIds)) {
                        return machineIds.contains(String.valueOf(m.getId()));
                    }
                    return true;
                }).filter(m -> {
                    // 如果机台支持全口型，则不受口型影响
                    if (ApsConstant.STATUS_ENABLE.equals(m.getSupportsAllMouthFlag())) {
                        return true;
                    }
                    // 不是支持所有口型的机台，则必须要配置口型板机台
                    return mouthPlateMachineIds.contains(String.valueOf(m.getId()));
                }).filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode)) // 对应班次可用
                .filter(item -> {
                    // 机台产能小于定额
//                    if (item.getQuata() != null) {
//                        return capacityMap.getOrDefault(item.getId(), BigDecimal.ZERO).compareTo(item.getQuata()) <= 0;
//                    }
                    return true;
                }).sorted(new Comparator<TcMachineInfo>() {// 按剩余产能升序排序
                    @Override
                    public int compare(TcMachineInfo m1, TcMachineInfo m2) {
                        int result = 0;
                        if (!checkIsVMI(beadCode)) { // 二次法规格。优先安排在二次法机台
                            Integer supportsVmiFlag1 = ApsConstant.STATUS_DISABLE.equals(m1.getSupportsVmiFlag())? 0: 1;
                            Integer supportsVmiFlag2 = ApsConstant.STATUS_DISABLE.equals(m2.getSupportsVmiFlag())? 0: 1;
                            result = supportsVmiFlag1.compareTo(supportsVmiFlag2);
                            if (result != 0) {
                                return result;
                            }
                        }
                        // 比较产能
                        result = capacityMap.getOrDefault(m1.getId(), BigDecimal.ZERO)
                                .compareTo(capacityMap.getOrDefault(m2.getId(), BigDecimal.ZERO));
                        return result;
                    }
                }).collect(Collectors.toList());
        return optionalMachineList;
    }

    /**
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     * @param scheduleList 排程列表
     * @param bisectThreshold  各班计划量均分阈值
     */
    private void equalShare(List<TcScheduleResultVo> scheduleList, Map<String, BigDecimal> curlLengthMap, Map<String, String> paramsMap) {
        BigDecimal bisectThreshold = BigDecimalUtils.valueOf(paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD)); // 均分阈值，超过的要平分
        BigDecimal tcVmMinRollNum = BigDecimalUtils.valueOf(paramsMap.get(EngineConstants.TC_VM_MIN_ROLL_NUM)); // 二次法
        BigDecimal standardCurlLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        // 次日早夜班总计划量超过阈值的平分中夜班计划量
        for (TcScheduleResultVo scheduleVo : scheduleList) {
            if (scheduleVo.getIsDayProductSpec()) { // 固定早班生产规格跳过
                continue;
            }
            BigDecimal stockQty = BigDecimalUtils.valueOf(scheduleVo.getStockQty());
            BigDecimal lastMidPlanQty = BigDecimalUtils.valueOf(scheduleVo.getLastMidPlanQty());
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal nextDayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
            BigDecimal totalPlanQty1 = nightPlanQty.add(dayPlanQty);
            BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getSidewallCode(), standardCurlLength);
            BigDecimal toolCapacity = this.checkIsVMI(scheduleVo)? curlLength: tcVmMinRollNum.multiply(curlLength);
            boolean isEqualShare = false;
            if (totalPlanQty1.compareTo(bisectThreshold) > 0) { // 超过指定计划量，则以工装的为单位平分
                BigDecimal newDayPlanQty = BigDecimalUtils.ceil(BigDecimalUtils.half(totalPlanQty1), toolCapacity); // 夜班平分后的计划量，先换算成工装数，平分后再换算成米数
                // 如果均分后会造成早班有库存缺口，则不做均分
                BigDecimal cxPlanQty = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan(), scheduleVo.getCxClass3Plan());
                BigDecimal day1StockQty = BigDecimalUtils.add(stockQty, lastMidPlanQty, newDayPlanQty);
                BigDecimal lackStock = cxPlanQty.subtract(day1StockQty); // 均分后的库存缺口
                if (lackStock.compareTo(BigDecimal.ZERO) <= 0 || scheduleVo.getClass3Sort() > 1) {
                    dayPlanQty = BigDecimalUtils.ceil(BigDecimalUtils.half(totalPlanQty1), toolCapacity); // 夜班平分后的计划量，先换算成工装数，平分后再换算成米数
                    dayPlanQty = BigDecimalUtils.least(dayPlanQty, totalPlanQty1); // 取整后的量不能超过总量
                    nightPlanQty = totalPlanQty1.subtract(dayPlanQty); // 夜班计划 = 总计划 - 早班计划
                    isEqualShare = true;
                }
            }
//            BigDecimal totalPlanQty2 = nightPlanQty.add(nextDayPlanQty);
//            if (totalPlanQty2.compareTo(bisectThreshold) > 0) { // 超过指定计划量，则以工装的为单位平分
//                nightPlanQty = BigDecimalUtils.ceil(BigDecimalUtils.half(totalPlanQty2), toolCapacity); // 夜班平分后的计划量，先换算成工装数，平分后再换算成米数
//                nightPlanQty = BigDecimalUtils.least(nightPlanQty, totalPlanQty2); // 取整后的量不能超过总量
//                nextDayPlanQty = totalPlanQty2.subtract(nightPlanQty); // 夜班计划 = 总计划 - 早班计划
//                isEqualShare = true;
//            }
            if (!isEqualShare){ // 没有达到阈值的，合并计划（一次法规格除外）
//            if (!this.checkIsVMI(scheduleVo) && !isEqualShare){ // 没有达到阈值的，合并计划（一次法规格除外）
                if (dayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    dayPlanQty = totalPlanQty1;
                    nightPlanQty = BigDecimal.ZERO;
//                } else if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
//                    dayPlanQty = BigDecimal.ZERO;
//                    nightPlanQty = totalPlanQty2;
                }
            }
            scheduleVo.setIsEqualShare(isEqualShare);
            scheduleVo.setDayPlanQty(dayPlanQty.doubleValue());
            scheduleVo.setNightPlanQty(nightPlanQty.doubleValue());
            scheduleVo.setNextDayPlanQty(nextDayPlanQty.doubleValue());
        }
    }

    /**
     * 同一个机台，胶料一样的排程记录，供应时长有一个小于等于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到中班;
     * 反之如果供应时长都大于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到夜班，在此情况下其中要是有记录的供应时长又大于{GLUE_MERGE_THRESHOLD_MAX}参数，则把计划量归并到【预计划】字段中，中班和夜班计划量变0
     *
     * @param scheduleList 排程列表
     * @param glueMergethreshold  同胶料合并生产预计库存可供应时长参数
     * @param glueMergethresholdMax  同胶料归并生产可供应时长(MAX)
     */
    private void glueMerge(String batchNo, List<TcScheduleResultVo> scheduleList, String glueMergethreshold, String glueMergethresholdMax) {
        Double threshold = 12D;
        Double thresholdMax = 28D;
        try {
            threshold = Double.parseDouble(glueMergethreshold);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长参数转换错误");
        }
        try {
            thresholdMax = Double.parseDouble(glueMergethresholdMax);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长(Max)参数转换错误");
        }

        //根据机台+胶料进行分组
        Map<String, List<TcScheduleResultVo>> groupMap = scheduleList.stream().collect(Collectors.groupingBy(v -> v.getGlueCode() + v.getMachineId()));
        scheduleList.clear();

        for(List<TcScheduleResultVo> list : groupMap.values()) {
            boolean isPassParam = this.compareSupplyTime(list, threshold);  //判断集合中的库存供应时长 是否 有小于参数值的

            for(TcScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();
                if(isPassParam) {
                    //库存供应时长 有小于 {GLUE_MERGE_THRESHOLD}参数值.计划量全归并到中班
                    scheduleVo.setDayPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    scheduleVo.setNightPlanQty(0D);
                } else {
                    //如果供应时长都大于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到夜班，在此情况下其中要是有记录的供应时长又大于{GLUE_MERGE_THRESHOLD_MAX}参数，则把计划量归并到【预计划】字段中，中班和夜班计划量变0
                    Double supplyTime = scheduleVo.getSupplyTime();
                    if(supplyTime > thresholdMax ) {
                        //则把计划量归并到【预计划】字段中，中班和夜班计划量变0
                        if(scheduleVo.getDayPlanQty() > 0 || scheduleVo.getNightPlanQty() > 0) {
                            scheduleVo.setPrePlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));  //设置预计库存
                            scheduleVo.setDayPlanQty(0D);
                            scheduleVo.setNightPlanQty(0D);
                        }
                    } else {
                        //计划量都归并到夜班
                        scheduleVo.setDayPlanQty(0D);
                        scheduleVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    }
                }
            }
            scheduleList.addAll(list);
        }
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "同胶料合并生产", logSplit("同胶料合并生产预计库存可供应时长参数:" + glueMergethreshold,
                "同胶料合并生产后排程数据：" + toJSONString(scheduleList)));  //添加日志
    }

    /**
     * 同一个机台，胶料一样的排程记录，供应时长有一个小于等于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到中班;
     * 反之如果供应时长都大于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到夜班，在此情况下其中要是有记录的供应时长又大于{GLUE_MERGE_THRESHOLD_MAX}参数，则把计划量归并到【预计划】字段中，中班和夜班计划量变0
     *
     * @param scheduleList          排程列表
     * @param glueMergethreshold    同胶料合并生产预计库存可供应时长参数
     * @param glueMergethresholdMax 同胶料归并生产可供应时长(MAX)
     */
    private void glueMerge1(String batchNo, List<TcScheduleResultVo> scheduleList, String glueMergethreshold,
                            String glueMergethresholdMax, String mergeMaxRoll, Map<String, BigDecimal> curlLengthMap,
                            BigDecimal standardCurlLength) {
        Double threshold = 12D;
        Double thresholdMax = 28D;
        try {
            threshold = Double.parseDouble(glueMergethreshold);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长参数转换错误");
        }
        try {
            thresholdMax = Double.parseDouble(glueMergethresholdMax);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长(Max)参数转换错误");
        }

        //根据机台+胶料进行分组
        Map<String, List<TcScheduleResultVo>> groupMap = scheduleList.stream().collect(Collectors.groupingBy(v -> v.getGlueCode() + v.getMachineId()));
        scheduleList.clear();

        for (List<TcScheduleResultVo> list : groupMap.values()) {
            boolean isPassParam = this.compareSupplyTime(list, threshold);  //判断集合中的库存供应时长 是否 有小于参数值的

            for (TcScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();

                BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getSidewallCode(), standardCurlLength);
                BigDecimal nightRollNum = BigDecimalUtils.valueOf(nightPlanQty).divide(curlLength, 0, RoundingMode.CEILING);

                if (isPassParam && dayPlanQty > 0 && nightRollNum.compareTo(new BigDecimal(mergeMaxRoll)) <= 0) {
                    //库存供应时长 有小于 {GLUE_MERGE_THRESHOLD}参数值.计划量全归并到中班
                    scheduleVo.setDayPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    scheduleVo.setNightPlanQty(0D);
                }
            }
            scheduleList.addAll(list);
        }
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "同胶料合并生产",
                logSplit("同胶料合并生产预计库存可供应时长参数:" + glueMergethreshold,
                        "卷长：" + toJSONString(curlLengthMap),
                        "合并最大卷数:" + mergeMaxRoll,
                        "同胶料合并生产后排程数据：" + toJSONString(scheduleList)));  //添加日志
    }

    /**
     * 判断集合中是否有 库存供应时长 小于 参数值
     * @param list
     * @param equalShareThreshold 同胶料合并生产预计库存可供应时长参数
     * @return
     */
    private boolean compareSupplyTime(List<TcScheduleResultVo> list, Double equalShareThreshold) {
        for(TcScheduleResultVo schedule : list) {
            if(schedule.getSupplyTime() <= equalShareThreshold) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算每个生产线的中班总计划量、夜班总计划量，以及总计划量
     * @param scheduleVo 排程数据
     * @param totalPlanQtyMap 每个生产线的计划量汇总MAP
     */
    private void groupTotalPlanQtyMap(TcScheduleResultVo scheduleVo, Map<String, TcTotalPlanQtyVo> totalPlanQtyMap) {
        String key = scheduleVo.getMachineId();  //机台id作为Map的key
        key = StringUtils.isBlank(key) ? "" : key;
        TcTotalPlanQtyVo totalPlanQtyVo = totalPlanQtyMap.getOrDefault(key, new TcTotalPlanQtyVo());  //取出对应生产线的计划量汇总对象

        Double dayPlanQty = scheduleVo.getDayPlanQty();  //中班计划量
        Double nightPlanQty = scheduleVo.getNightPlanQty();  //夜班计划量
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()));
        totalPlanQtyMap.put(key, totalPlanQtyVo);  //计算完毕后，把生产线的计划量汇总对象重新存入map中
    }

    /**
     * 均衡日志
     * @param scheduleList
     * @param paramsMap
     * @param totalPlanQtyVo
     */
    private void equilibriumLog(String batchNo, String oldScheduleList, List<TcScheduleResultVo> scheduleList, Map<String, String> paramsMap, TcTotalPlanQtyVo totalPlanQtyVo) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，" +
                "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。").append(division);
        logDetail.append("各班总计划量：" + toJSONString(totalPlanQtyVo)).append(division);
        logDetail.append("参数配置集合，这里要用到‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’和‘SUPPLY_TIME_PASS（库存供应时长小时数）’：" + toJSONString(paramsMap)).append(division);
        logDetail.append("均衡前的排程数据列表：" + oldScheduleList).append(division);
        logDetail.append("均衡后的排产数据列表：" + toJSONString(scheduleList));
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "计划量均衡处理", logDetail.toString());
    }


    /**
     * 根据机台+胶料进行分组，然后在根据库存供应时长，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）
     * @param scheduleList
     */
    private void setProduceOrder(List<TcScheduleResultVo> scheduleList) {
        //根据机台+胶料进行分组
        Map<String, List<TcScheduleResultVo>> groupMap = scheduleList.stream().filter(item -> StringUtils.isNotBlank(item.getMachineId()))
                .collect(Collectors.groupingBy(TcScheduleResultVo::getMachineId));
        scheduleList.clear();

        for(List<TcScheduleResultVo> list : groupMap.values()) {
            int dayProduceOrder = 1; //白班生产顺序
            int nightProduceOrder = 1;  //夜班生产顺序
            //根据库存供应时长升序排序
            list = list.stream().sorted(Comparator.comparing(TcScheduleResultVo::getSupplyTime)).collect(Collectors.toList());
            for(TcScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();
                if(dayPlanQty > 0) {
                    scheduleVo.setDayProduceOrder(dayProduceOrder++);
                }
                if(nightPlanQty > 0) {
                    scheduleVo.setNightProduceOrder(nightProduceOrder++);
                }
                autoScheduleLogService.insertTcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产顺序字段",
                        logSplit("根据机台+胶料进行分组，然后在根据库存供应时长(从小到大)，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）", "设置后的排程数据：" + toJSONString(scheduleVo)));  //添加日志
            }
            scheduleList.addAll(list);
        }
    }

    /**
     * 设置收尾提示标识 和 生产状态字段
     * @param scheduleResultVo
     * @param monthSurplusVo
     * @param closeOutNum  参数配置表设置的 提示收尾阈值
     */
    private void setStatusAndCloseTip(TcScheduleResultVo scheduleResultVo, TcMonthSurplusVo monthSurplusVo, Double closeOutNum) {
        if(monthSurplusVo == null) {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
            return;
        }
        Double monthFinishQty = monthSurplusVo.getMonthFinishQty();  //月度计划完成量
        Double monthRemainQty = monthSurplusVo.getMonthRemainQty();  //月度计划剩余量
        if(monthRemainQty < closeOutNum) {
            //剩余量小宇等于“临近收尾阈值”，设置收尾提示
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NEED);
        } else {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
        }
        autoScheduleLogService.insertTcScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "设置收尾提示标识markCloseOutTip",
                logSplit("剩余量小宇等于“临近收尾阈值”，设置收尾提示","月度计划剩余量：" + monthRemainQty + ",提示收尾阈值：" + closeOutNum, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志

        if(monthFinishQty == 0D) {
            //没有完成量
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
        } else if(monthFinishQty > 0D && monthRemainQty > 0) {
            //完成量大于0，月度计划量也大于0，说明出于生产中
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_ING);
        } else if(monthRemainQty <= 0) {
            //月度计划量小于等于0，说明出于生产完成
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_FINISH);
        }
        autoScheduleLogService.insertTcScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "修改生产状态productionStatus",
                logSplit("①完成量为0，对应生产状态：未生产;②完成量大于0，月度计划量也大于0，说明出于生产中;③月度计划量小于等于0，说明出于生产完成",
                        "月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
     * @param batchNo   批次号
     * @param autoScheduleList   自动排程列表
     * @param existScheduleList  当天已经存在的排产记录
     */
    private List<TcScheduleResultVo> mergeExistSchedule(String batchNo, List<TcScheduleResultVo> autoScheduleList, List<TcScheduleResultVo> existScheduleList) {
        if(StringUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }
        List<TcScheduleResultVo> mergeList = new ArrayList<>();

        Map<String, List<TcScheduleResultVo>> existScheduleMap = existScheduleList.stream().filter(s->s.getPublishSuccessCount()>0)
                .collect(Collectors.groupingBy(TcScheduleResultVo::getSidewallCode)); //拿到重排前，已经有发布给MES的排产数据。key为 半部件规格代码

        for(TcScheduleResultVo autoSchedule : autoScheduleList) {
            List<TcScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getSidewallCode());

            if(existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                //对应规格重排前已经发布，并且此规格重排前只有一条排程记录（只对应了一个机台）
                TcScheduleResultVo existSchedule = existScheduleGroupList.get(0);
                //重排前的数据如果已经发布过，在重新排程后仍有相应的生产需求，计划量按照重新自动排程的计划量安排；订单号需要和之前发布个mes的订单号一致
                autoSchedule.setOrderNo(existSchedule.getOrderNo());  //订单号
                autoSchedule.setPublishSuccessCount(existSchedule.getPublishSuccessCount());
                autoSchedule.setNewestPublishTime(existSchedule.getNewestPublishTime());
                autoSchedule.setIsRelease(ApsConstant.WAIT_RELEASING);  //发布状态修改
                autoSchedule.setMachineId(existSchedule.getMachineId());  //机台沿用重排前的机台
                mergeList.add(autoSchedule);
            } else if(existScheduleGroupList != null && existScheduleGroupList.size() > 1) {
                //对应规格重排前已经发布，并且此规格重排前只有多条排程记录（对应了多个机台）。那需要保留重排之前的排产，并且要把此规格重排后的各班的计划量，拼接到备注中
                String remarkTip = I18nUtil.getMessage("reschedule.double.spec.remark");
                remarkTip = StringUtils.format(remarkTip, stripZeros(autoSchedule.getDayPlanQty()), stripZeros(autoSchedule.getNightPlanQty()));
                for(TcScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                //对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getSidewallCode());
        }

        //重排前的已发布的规格如果没有在重排后的列表中，则需要把对应的规格也加入到最新的排程列表中
        for(List<TcScheduleResultVo> list : existScheduleMap.values()) {
            list.forEach(r->r.setBatchNo(batchNo));
            mergeList.addAll(list);
        }
        return mergeList;
    }

    /**
     * 创建自动排程记录
     *
     * @param scheduleDate 排程日期
     * @param cxBatchNo    对成型批次号
     * @param batchNo      胎侧批次号
     */
    private void createScheduleRecord(String scheduleDate, String cxBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("cxBatchNo", cxBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());  //用户名
        tcEngineMapper.createScheduleRecord(params);
    }

    /**
     * 把排程数据同步到log表
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    private void syncTcScheduleToLog(String scheduleDate) {
        tcEngineMapper.syncTcScheduleToLog(scheduleDate);
        tcEngineMapper.deleteTcSchedule(scheduleDate);
        tcEngineMapper.deleteTcAssistSchedule(scheduleDate);
    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleVo
     * @param specifyCanMachineMap  定点机台中限制作业map
     * @param specifyNotMachineMap  定点机台中
     * @param mouthPlateMachineMap
     */
    private void chooseMachine(TcScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap, Map<String, String> mouthPlateMachineMap) {
        String sidewallCode = scheduleVo.getSidewallCode();  //胎侧代码
        String mouthPlateCode = scheduleVo.getMouthPlateCode();  //口型板code

        String machineIds = specifyCanMachineMap.get(sidewallCode);
        machineIds = StringUtils.isBlank(machineIds) ? mouthPlateMachineMap.get(mouthPlateCode) : machineIds;
        //过滤掉 定点机台中 设置的不可作业的机台
        String notMachineIds = specifyNotMachineMap.get(sidewallCode);  //定点机台中不可作业的机台
        if(StringUtils.isNotBlank(machineIds) && StringUtils.isNotBlank(notMachineIds)) {
            List<String> machineList = new ArrayList<>();
            String[] machineArray = machineIds.split(",");
            List<String> notMachineIdList = Arrays.asList(notMachineIds.split(","));
            for(int i = 0;i < machineArray.length; i++) {
                if(!notMachineIdList.contains(machineArray[i])) {
                    machineList.add(machineArray[i]);  //此机台不在 定点机台的 不可作业集合中
                }
            }
            machineIds = String.join(",", machineList);
        }
        scheduleVo.setMachineId(machineIds == null ? "" : machineIds);
        chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap);  //添加日志
    }

    /**
     * 设置生产线日志
     * @param scheduleVo
     * @param specifyCanMachineMap
     * @param specifyNotMachineMap
     * @param mouthPlateMachineMap
     */
    private void chooseMachineLog(TcScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap, Map<String, String> mouthPlateMachineMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“口型板与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
        logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("口型板与机台对应关系集合：" + toJSONString(mouthPlateMachineMap)).append(division);
        logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
        autoScheduleLogService.insertTcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
    }

    /**
     * （新）计算并设置供成型库存供应时长（小时）。
     * 具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；
     *         预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）
     * @param scheduleVo
     * @param stockQty
     */
    private void newComputeSupplyTime(TcScheduleResultVo scheduleVo,  Double stockQty) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型一班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型二班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型三班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日一班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日一班的计划量
        autoScheduleLogService.insertTcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长前数据",
                logSplit("具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+12小时；预计库存-1班计划-2班计划大于等于0时，供应时长+24小时；预计库存-1班计划-2班计划-3班计划小于0，供应时长=24个小时+（((预计库存-1班计划-2班计划)/3班计划)*12）；以此类推到第5班",
                        "物料编号：" + scheduleVo.getSidewallCode() + ",7点预计库存：" + stockQty + "，对应成型一班的计划量：" + 0 + "，对应成型二班的计划量：" + cxClass2Plan + "，对应成型三班的计划量：" + cxClass3Plan + "，对应成型次日一班的计划量：" + cxClass4Plan + "，对应成型次日二班的计划量：" + cxClass5Plan));

        //根据1班计算库存供应时长
        double remnantStock = stockQty;    //剩余库存
//        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass1Plan)) {
//            return;
//        }

        //根据2班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, 0);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass2Plan)) {
            return;
        }

        //根据3班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass2Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass3Plan)) {
            return;
        }

        //根据次日1班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass3Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass4Plan)) {
            return;
        }

        //根据次日2班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass4Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass5Plan)) {
            return;
        }
        autoScheduleLogService.insertTcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getSidewallCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }

    /**
     * 根据对应成型每班计划量，计算库存供应时长
     * @param scheduleVo  排程实体
     * @param remnantStock 剩余库存
     * @param classPlan 对应成型的计划量
     * @return false：不需要再根据其他班在计算了。 true：还需要根据其他班计划量，继续计算库存供应时长
     */
    private boolean oneComputeSupplyTime(TcScheduleResultVo scheduleVo,Double remnantStock, Double classPlan) {
        Double supplyTime = scheduleVo.getSupplyTime();
        supplyTime = (supplyTime == null ? 0D : supplyTime);
        if(BigDecimalUtil.sub(remnantStock, classPlan) >= 0) {
            //如果剩余库存 大于 对应班次库存，则库存供应时长直接+12小时
            scheduleVo.setSupplyTime(BigDecimalUtil.add(supplyTime, 12));  //库存供应时长加12小时
            return true;
        } else {
            //如果剩余库存 小宇 对应班次库存，则库存供应时长在加上：((剩余库存)/对应班班计划)*12小时
            double classSupplyTime = BigDecimalUtil.mul(BigDecimalUtil.div(remnantStock, classPlan), 12);
            supplyTime = supplyTime + BigDecimalUtil.roundDown(classSupplyTime, 1);  //设置库存供应时长向下保留1位小数
            scheduleVo.setSupplyTime(supplyTime);
            autoScheduleLogService.insertTcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getSidewallCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
            return false;
        }
    }

    /**
     * 计算库存供应时长（小时）= 库存/(成型定额*单耗)*8小时
     * @param quotaKeys 成型机台code和胎胚代码，格式：成型机台code$胎胚代码
     * @param stockQty 16点预计库存
     * @param unitConsume 单耗
     */
    private void computeSupplyTime(TcScheduleResultVo scheduleVo, String quotaKeys, Double stockQty, Double unitConsume) {
        if(StringUtils.isBlank(quotaKeys)) {
            scheduleVo.setSupplyTime(0D);
            autoScheduleLogService.insertTcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长", "库存供应时长为空，原因：没找到对应的成型排程记录");
            return;
        }
        String [] quotaKeyArray = quotaKeys.split(",");
        Integer cxQuota = cxEngineQuotaCommonService.getCxMachineQuota(quotaKeyArray);  //成型定额
        unitConsume = BigDecimalUtil.div(unitConsume, 1000);   //单耗把毫米转成米
        Double quota = BigDecimalUtil.mul(cxQuota, unitConsume);   //定额
        if(quota == 0) {
            scheduleVo.setSupplyTime(0D);
        } else {
            Double supplyTime = stockQty / quota * 8;  //库存可供成型连续生产的时长
            supplyTime = BigDecimalUtil.add(supplyTime, addComputeSupplyTime(scheduleVo)); //如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
            scheduleVo.setSupplyTime(BigDecimalUtil.roundDown(supplyTime, 1)); //设置困存公用时长向下保留2位小数
        }
        autoScheduleLogService.insertTcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长",
                logSplit("库存供应时长supplyTime（小时）= 库存/(成型定额*单耗)*8小时;其中成型定额取成型定额的平均值，单耗也是取平均单耗", "成型定额：" + cxQuota + "，半制品平均单耗：" + unitConsume,
                        "计算后的结果数据：" + toJSONString(scheduleVo)));
    }

    /**
     * 如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
     * @param scheduleVo
     * @return
     */
    private int addComputeSupplyTime(TcScheduleResultVo scheduleVo) {
        int count = 0;
        int addTime = 12;  //每班8小时
        if(scheduleVo.getCxClass1Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass2Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass3Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass4Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass5Plan() == 0) {
            count++;
        }
        return count * addTime;
    }

    /**
     * 计算胎面中班和夜班计划量
     * @param scheduleVo
     * @param totalPlanQtyVo 计划量总计VO
     * @param lossMap        耗损率map
     * @param paramLossRate  工序参数中配置的耗损率
     * @param mergeThreshold 往前一班合并计划量阈值
     * @param toolCapacity   取整数
     * @param productStockDay   预生产库存天数
     */
    private void computeTcPlanQty(TcScheduleResultVo scheduleVo, TcTotalPlanQtyVo totalPlanQtyVo,
                                  Map<String, Double> lossMap, double paramLossRate, double mergeThreshold, BigDecimal toolCapacity,
                                  double productStockDay, Map<String, String> paramsMap) {
        BigDecimal productStockHourOneMethod = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR_ONE_METHOD, DEFAULT_PRODUCT_STOCK_HOUR));
        double productStockDayOneMethod = productStockHourOneMethod.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue();

        String tcVmMinRollNum = paramsMap.get(EngineConstants.TC_VM_MIN_ROLL_NUM); // 二次法取整卷数
        String oldScheduleResult = toJSONString(scheduleVo); // 没计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); // 库存
        Double lastMidPlanQty = scheduleVo.getLastMidPlanQty(); // 前日白班计划
        String sidewallCode = scheduleVo.getSidewallCode();
        Double totalConsumeQty = scheduleVo.getSurplusQty(); // 总需求量，前四个班
        // 如果是一次法的话，使用一次法的预生产库存天数
        double supplyClass = this.checkIsVMI(scheduleVo) ? productStockDayOneMethod : productStockDay; // 预生产库存天数
        double lossRate = tcEngineLossService.getLossRate(sidewallCode, null, lossMap, paramLossRate); // 按规格取出损耗率

        // 每个早班计算交接班库存 = 上一天交接班库存 + 上一天胎圈计划量总量 - 上一天成型两个班的消耗量
        // 交接班库存要按生产几个小时库存算，例如预生产12小时库存，则交接班库存要 > 当天成型需求量 / 2，最多超过一车（110个）
        // 上一天胎圈计划总量原则上平均分配给两个班，但是早班的计划量要 > 上一天成型两个班的需求量 - 上一天交接班库存
        double cxPlanQty1 = BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());// 第一天成型两个班消耗量
        double cxPlanQty2 = BigDecimalUtil.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan());// 第二天成型两个班消耗量
        double cxPlanQty3 = cxPlanQty2;// 第三天成型两个班消耗量（成型没有，如果未收尾暂时先预计与第二天一样）
        double classStock1 = stockQty; // 第一天交接班库存，初始为当天库存
        double classStock2 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty2, supplyClass), 0); // 第二天交接班库存，第二天成型两个班的消耗量 * 预生产天数
        // 计算第一天相关数值
        double planQty1 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock2, classStock1), cxPlanQty1);// 第一天胎圈计划量 = 第二天交接班库存 - 第一天交接班库存 + 第一天成型两个班的消耗量
        planQty1 = planQty1 > 0 ? planQty1 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double class1PlanQty1 = lastMidPlanQty;// 第一天早班计划 = 前日早班计划
        double class2PlanQty1 = BigDecimalUtil.sub(planQty1, class1PlanQty1);// 第一天夜班计划 = 等于第一天胎圈计划 - 第一天早班计划
        class2PlanQty1 = this.addLossRate(class2PlanQty1, lossRate); // 计算损耗率
        class2PlanQty1 = this.planQtyRounding(scheduleVo, class2PlanQty1, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_TWO, tcVmMinRollNum); // 整车取整
        double dayPlanQty = class2PlanQty1; // 夜班计划
        scheduleVo.setDayPlanQty(dayPlanQty);
        // 根据排好的计划量重算相关数值
        planQty1 = BigDecimalUtil.add(class1PlanQty1, class2PlanQty1); // 刷新第一天胎圈计划量
        classStock2 = BigDecimalUtil.sub(BigDecimalUtil.add(planQty1, classStock1), cxPlanQty1);// 刷新第二天交接班库存
        scheduleVo.setClassStock(classStock2); // 保存交接班库存，用于均衡计算
        scheduleVo.setSupplyDemandRatio(BigDecimalUtil.div(classStock2, cxPlanQty2, 4)); // 计算交接班库存供需比率，第二天交接班库存 / 成型第二天需求量，用于均衡计算

        // 计算第二天相关数值
        double classStock3 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty3, supplyClass), 0); // 第三天交接班库存，第三天成型两个班的消耗量 * 预生产天数
        double planQty2 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock3, classStock2), cxPlanQty2);// 第二天胎圈计划量 = 第三天交接班库存 - 第二天交接班库存 + 第二天成型两个班的消耗量
        planQty2 = planQty2 > 0 ? planQty2 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double class1PlanQty2;// 第二天早班计划，“第二天胎圈计划量的一半” 与 “(第二天成型两个班的需求量 - 第二天交接班库存)的一半”的较大值
        double lackPlanQty = BigDecimalUtil.sub(cxPlanQty2, classStock2); // 早班先补交接班库存缺口
        lackPlanQty = this.addLossRate(lackPlanQty, lossRate); // 计算损耗率
        class1PlanQty2 = this.planQtyRounding(scheduleVo, lackPlanQty, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_THREE, tcVmMinRollNum); // 整车取整
        double nightPlanQty = class1PlanQty2; // 早班计划
        scheduleVo.setNightPlanQty(nightPlanQty);
        double class2PlanQty2 = BigDecimalUtil.sub(planQty2, class1PlanQty2);// 第二天夜班计划 = 等于第二天胎圈计划 - 第二天早班计划
        // 如果次日早班备库量小于1卷，忽略不计，避免仅差一小部分就生产4卷的情况
        if (class2PlanQty2 < toolCapacity.doubleValue()) {
            class2PlanQty2 = 0D;
        }
        class2PlanQty2 = this.addLossRate(class2PlanQty2, lossRate); // 计算损耗率
        double nextDayPlanQty = this.planQtyRounding(scheduleVo, class2PlanQty2, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_FOUR, tcVmMinRollNum); // 次日夜班计划 = 第二天夜班计划整车取整
        scheduleVo.setNextDayPlanQty(nextDayPlanQty);

        // 收尾规格计划如果不足一车，合并到上一个班
        if (nextDayPlanQty > 0 && nextDayPlanQty < toolCapacity.doubleValue()) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, nextDayPlanQty);
            nextDayPlanQty = 0D;
        }
        if (nightPlanQty > 0 && nightPlanQty < toolCapacity.doubleValue()) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
            nightPlanQty = 0D;
        }

        /*String oldScheduleResult = toJSONString(scheduleVo); //没看是计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); //库存
//        double unitConsume = BigDecimalUtil.div(scheduleVo.getUnitConsume(), 1000D, 3);
        //计算夜班计划量 = 成型三班消耗胎面计划量（次日成型早班计划）
        double dayPlanQty = scheduleVo.getCxClass3Plan();
        double initDayPlanQty = dayPlanQty;
        //计算早班计划量 = 成型四班消耗胎面计划量（次日成型夜班计划）
        double nightPlanQty = scheduleVo.getCxClass4Plan();

        //根据库存重新计算中班计划量：（原中班计划量>库存） ？（ 原中班计划量-库存） ： 0
        dayPlanQty = (initDayPlanQty > stockQty) ? BigDecimalUtil.sub(dayPlanQty, stockQty) : 0;
        //根据库存重新计算夜班计划量：（原中班计划量>库存） ？原夜班计划量 ： （原中班计划量+原夜班计划量 - 库存）
        nightPlanQty = (initDayPlanQty > stockQty) ? nightPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initDayPlanQty, nightPlanQty), stockQty);
        nightPlanQty = (nightPlanQty < 0) ? 0D : nightPlanQty;*/

        //如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）
        /*if (dayPlanQty > 0) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
            nightPlanQty = 0D;
        }*/

        /*// 如果成型的夜班最小的顺序小于参数值，且计划量小于参数值，则将早班计划量移到夜班
        String cxMergeMinSortStr = paramsMap.get(EngineConstants.CX_MERGE_MIN_SORT);
        String mergeMaxRollStr = paramsMap.get(EngineConstants.MERGE_MAX_ROLL);
        BigDecimal nightRollNum = BigDecimalUtils.valueOf(nightPlanQty).divide(toolCapacity, 0, RoundingMode.CEILING);
        if (scheduleVo.getClass3Sort() != null &&
                scheduleVo.getClass3Sort() <= Integer.parseInt(cxMergeMinSortStr) &&
                nightRollNum.intValue() != 0 &&
                nightRollNum.intValue() <= Integer.parseInt(mergeMaxRollStr)) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
            nightPlanQty = 0D;
        }*/
//        double cxMergeMinSort = Double.parseDouble(paramsMap.getOrDefault(EngineConstants.CX_MERGE_MIN_SORT, "-1"));
//        double cxSortMergeRollNum = Double.parseDouble(paramsMap.getOrDefault(EngineConstants.CX_SORT_MERGE_ROLL_NUM, "5"));
//
//        // 成型顺序小于等于1，夜班计划量小于5卷的，从早班移到夜班
//        // 成型顺序大于1，夜班早班计划合计小于5卷的，从夜班移到早班
//        double totalPlan = nightPlanQty != 0 ? dayPlanQty + nightPlanQty : dayPlanQty + nextDayPlanQty;
//        BigDecimal totalRollNum = BigDecimalUtils.div(totalPlan, toolCapacity);
//        if (totalRollNum.compareTo(BigDecimal.valueOf(cxSortMergeRollNum)) < 0) {
//            if ((scheduleVo.getClass3Sort() != null && scheduleVo.getClass3Sort() <= cxMergeMinSort)
////                    || scheduleVo.getSupplyTime() <= 12
//            ) {
//                if (nightPlanQty == 0) {
//                    dayPlanQty = dayPlanQty + nextDayPlanQty;
//                    nextDayPlanQty = 0;
//                } else if (dayPlanQty == 0) {
//                    nightPlanQty = nightPlanQty + nextDayPlanQty;
//                    nextDayPlanQty = 0;
//                } else {
//                    dayPlanQty = dayPlanQty + nightPlanQty;
//                    nightPlanQty = 0;
//                }
//            } else if ((scheduleVo.getClass3Sort() != null && scheduleVo.getClass3Sort() > cxMergeMinSort)
////                    || scheduleVo.getSupplyTime() > 12
//            ) {
//                if (nightPlanQty != 0) {
//                    nightPlanQty = dayPlanQty + nightPlanQty;
//                    dayPlanQty = 0;
//                }
//            }
//        }

        // 如果限制早班生产规格前缀，将夜班的计划量移到早班
        boolean isDayProductSpec = false;
        String dayProductCodePrefixParamValue = paramsMap.getOrDefault(EngineConstants.DAY_PRODUCT_CODE_PREFIX, "");
        if (Arrays.stream(dayProductCodePrefixParamValue.split(",")).anyMatch(s -> scheduleVo.getSidewallCode().startsWith(s))) {
            // 把计划量移到早班，夜班计划量清0
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, dayPlanQty);
            dayPlanQty = 0D;
            isDayProductSpec = true;
        }

        //计划量向上取整
        dayPlanQty = BigDecimalUtil.roundUp(dayPlanQty, 0);
        nightPlanQty = BigDecimalUtil.roundUp(nightPlanQty, 0);
        nextDayPlanQty = BigDecimalUtil.roundUp(nextDayPlanQty, 0);
        scheduleVo.setDayPlanQty(dayPlanQty);
        scheduleVo.setNightPlanQty(nightPlanQty);
        scheduleVo.setNextDayPlanQty(nextDayPlanQty);
        scheduleVo.setIsDayProductSpec(isDayProductSpec);

        //计算中班总计划量 和 夜班总计划量
//        this.groupTotalPlanQtyMap(scheduleVo, totalPlanQtyMap);
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalNextDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNextDayPlanQty(), nextDayPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty(), totalPlanQtyVo.getTotalNextDayPlanQty()));

        this.computeTcPlanQtyLog(oldScheduleResult, scheduleVo, lossMap, paramLossRate, lossRate);  //添加日志
    }

    /**
     * 增加损耗量
     *
     * @param dayPlanQty 计划量
     * @param lossRate   损耗率
     * @return
     */
    private double addLossRate(double dayPlanQty, double lossRate) {
        return BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
    }

    /**
     * 获取各班需求量的累计值（从前日早班开始）
     *
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getCxClassPlanCumulative(TcScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型前日早班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型夜班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型早班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日夜班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日早班的计划量
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass1Plan);
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass2Plan);
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass3Plan);
        if (classNum == OpenMachineClassEnums.CLASS_THREE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass4Plan);
        if (classNum == OpenMachineClassEnums.CLASS_FOUR) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, cxClass5Plan);
    }

    /**
     * 计划量取整
     *
     * @param scheduleVo      排产记录
     * @param planQty         原计划量
     * @param toolCapacity    一车可以放的胎面量
     * @param totalConsumeQty 总需期间量，用于判断收尾规格是否超量
     * @param classNum        当前班次，从前日早班开始
     * @return
     */
    private double planQtyRounding(TcScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
                                   Double totalConsumeQty, OpenMachineClassEnums classNum,
                                   String tcVmMinRollNum) {
        if (planQty <= 0D) { // 不排的情况直接返回0即可
            return 0D;
        }
        BigDecimal divideResult = BigDecimalUtils.valueOf(planQty).divide(toolCapacity, 0, RoundingMode.CEILING);
        String sidewallCode = scheduleVo.getSidewallCode();
        BigDecimal oneRollNum = null;
        if (!this.checkIsVMI(sidewallCode)) {
            oneRollNum = new BigDecimal(tcVmMinRollNum);
        } else {
            oneRollNum = BigDecimal.ONE;
        }
        // 一次法一车一卷，二次法一车四卷
        int remainder = divideResult.intValue() % oneRollNum.intValue();
        if (remainder != 0) {
            divideResult = divideResult.add(BigDecimalUtils.sub(oneRollNum, BigDecimal.valueOf(remainder)));
        }
        double roudingPlanQty = divideResult
                .multiply(toolCapacity).doubleValue(); // 取整车
        if (classNum == null) {
            return roudingPlanQty;
        }
        OpenMachineClassEnums lastClass = classNum;
        if (classNum != OpenMachineClassEnums.CLASS_ONE) { // 取出上一班的班次
            Integer classIndex = classNum.getClassIndex();
            lastClass = OpenMachineClassEnums.getClassEnums(classIndex - 1);
        }
        double lastPlanCumulative = this.getTcClassPlanCumulative(scheduleVo, lastClass); // 到上个班次班次班的累计已排计划量
        double result = roudingPlanQty;
        double newPlanQty = BigDecimalUtil.add(lastPlanCumulative, roudingPlanQty, scheduleVo.getStockQty()); // 库存+已排计划+本班计划
        // 如果库存+计划已经超过总需求量，则本班的计划量要限制住不允许超量
        if (newPlanQty > totalConsumeQty) {
            Double increaseMidPlanQty = BigDecimalUtil.sub(newPlanQty, totalConsumeQty);
            result = BigDecimalUtil.sub(roudingPlanQty, increaseMidPlanQty);
            result = Math.max(result, 0D);
        }
        scheduleVo.setCloseOutSpecFlag(newPlanQty >= totalConsumeQty ? ApsConstant.STATUS_ENABLE : ApsConstant.STATUS_DISABLE);
        return result;
    }

    /**
     * 获取各班计划量的累计值（从前日早班开始）
     *
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getTcClassPlanCumulative(TcScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getLastMidPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getDayPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getNightPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_THREE) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, scheduleVo.getNextDayPlanQty());
    }

    private void computeTcPlanQtyLog(String oldScheduleResult, TcScheduleResultVo scheduleVo, Map<String, Double> lossMap, double paramLossRate, double lossRate) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("开始计算中班和夜班计划量").append(division);
        logDetail.append("计算前排程数据：" + oldScheduleResult).append(division);
        logDetail.append("计算中班计划量dayPlanQty = （成型一班消耗胎侧计划量cxClass1Plan + 成型二班消耗胎侧计划量CxClass2Plan）").append(division);
        logDetail.append("计算夜班计划量nightPlanQty =（成型三班消耗胎侧计划量cxClass3Plan + 成型次日一班消耗胎侧计划量cxClass4Plan）").append(division);
        logDetail.append("根据库存重新计算中班计划量dayPlanQty：（原中班计划量dayPlanQty > 库存stockQty） ？（ 原中班计划量-库存） ： 0").append(division);
        logDetail.append("根据库存重新计算夜班计划量nightPlanQty：（原中班计划量dayPlanQty>库存stockQty） ？原夜班计划量nightPlanQty ： （原中班计划量dayPlanQty + 原夜班计划量nightPlanQty - 库存stockQty）").append(division);
        logDetail.append("胎侧耗损率集合：" + toJSONString(lossMap) + "  参数配置耗损率：" + paramLossRate).append(division);
        logDetail.append("获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 胎侧代码 > 机台 >工序参数配置），耗损率：" + lossRate).append(division);
        logDetail.append("重新计算中班计划量和夜班计划量(计划量 = 计划量 + 计划量 * 耗损率)，计划量要加上耗损率的损耗数").append(division);
        logDetail.append("如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）").append(division);
        logDetail.append("计划量计算好后的排程数据：" + toJSONString(scheduleVo));
        autoScheduleLogService.insertTcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算各班计划量", logDetail.toString());
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.TC_BATCH_NO_PREFIX + scheduleDate);
    }

    /**
     * 创建工单号
     * @param batchNo 批次号
     * @return
     */
    private String createOrderNo(String batchNo) {
        return incrementService.getSequence4(batchNo);
    }

    /**
     * 获取工序参数map
     * @return
     */
    private Map<String, String> getParamsMap() {
        List<TcParamsVo> list = this.tcEngineMapper.listTcParams();
        Map<String, String> map = list.stream().collect(Collectors.toMap(TcParamsVo::getParamCode, TcParamsVo::getParamValue));
        return map == null ? new HashMap<>() : map;
    }

    /**
     * 自动排程基础表的数据日志
     * @param batchNo 自动排程批次号
     * @param glueSeqMap 胶料顺序集合
     * @param mouthPlateMachineMap 口型板和机台关系集合
     * @param specifyCanMachineMap 定点机台和机台的限制作业集合
     * @param specifyNotMachineMap 定点集合和机台的不可作业集合
     * @param planStockMap 16点预计库存集合
     * @param lossRateMap 耗损率集合
     * @param monthSurplus 月度计划剩余量、完成量集合
     * @param paramsMap 参数设置集合
     */
    private void baseDataLog(String batchNo, Map<String, String> glueSeqMap, Map<String, String> mouthPlateMachineMap, Map<String, String> specifyCanMachineMap,
                             Map<String, String> specifyNotMachineMap, Map<String, Double> planStockMap, Map<String, Double> lossRateMap,
                             Map<String, TcMonthSurplusVo> monthSurplus,Map<String, String> paramsMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("胶料顺序集合：" + toJSONString(glueSeqMap)).append(division);
        logDetail.append("口型板和机台关系集合：" + toJSONString(mouthPlateMachineMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点集合和机台的不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(paramsMap)).append(division);
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }

    /**
     * 批量设置批次号和订单号
     *
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     */
    @Override
    public void batchUpdateBatchNoAndOrderNo(String scheduleDate) {
        List<TcScheduleResultVo> scheduleResultVoList = tcEngineMapper.listTcEnginSchedule(scheduleDate);
        //查询当前排程的批次号
        String batchNo = tcEngineMapper.getTcCurrentBatchNo(scheduleDate);
        if (StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“或者做的批量导入（需要删掉已排的数据），那么自己生成一个排程批次号
            //胎面排程批次号
            batchNo = this.createBatchNo(scheduleDate);
            //创建自动排程记录
            this.createScheduleRecord(scheduleDate, "", batchNo);
            //把排程数据同步到log表
            this.syncTcScheduleToLog(scheduleDate);
        }
        for (TcScheduleResultVo scheduleResult : scheduleResultVoList) {
            //批次号
            scheduleResult.setBatchNo(batchNo);
            //工单号
            String orderNo = this.createOrderNo(batchNo);
            scheduleResult.setOrderNo(orderNo);
        }
        if (CollectionUtils.isNotEmpty(scheduleResultVoList)) {
            tcEngineMapper.batchUpdateBatchNoAndOrderNo(scheduleResultVoList);
        }
    }
}
