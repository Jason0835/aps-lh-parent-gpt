package com.zlt.aps.tm.engine.service.impl;

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
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.engine.mapper.TmEngineGlueMapper;
import com.zlt.aps.tm.engine.mapper.TmEngineMapper;
import com.zlt.aps.tm.engine.mapper.TmEngineStockMapper;
import com.zlt.aps.tm.engine.service.*;
import com.zlt.aps.tm.engine.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.*;

@Slf4j
@Service
public class TmEngineServiceImpl implements TmEngineService {

    @Resource
    private TmEngineMapper tmEngineMapper;
    @Resource
    private TmEngineGlueService tmEngineGlueService;
    @Resource
    private TmEngineStockService tmEngineStockService;
    @Resource
    private TmEngineMachineService tmEngineMachineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private TmEngineLossService tmEngineLossService;
    @Resource
    private TmEngineMonthSurplusService tmEngineMonthSurplusService;
    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    @Resource
    private TmEngineCurlRollService tmEngineCurlRollService;
    @Resource
    private TmEngineStockMapper tmEngineStockMapper;
    /**
     * 一次生产卷数
     */
    private final static String DEFAULT_ONE_ROLL_NUM = "2";
    private final static String DEFAULT_GLUE_LARGE_DEMAND = "40"; // 胶料大需求量卷数
//    private final static String DEFAULT_ONE_SPEC_LARGE_DEMAND = "20"; // 单规格大需求量卷数
    private final static String DEFAULT_REPLENISH_INVENTORY = "2"; // 库存缺口补齐卷数卷数

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 排程参数预设值，参数设置取不到值时使用这些预设值
     */
    private final static String DEFAULT_STANDARD_CRIMP_LENGTH = "85"; // 卷曲标准长度
//    private final static String DEFAULT_CURL_DECIMAL_ROUNDING = "0.3"; // 卷曲数小数取整值
//    private final static String DEFAULT_CLOSE_OUT_DAYS = "1"; // 共用规格收尾判断天数
    private final static String DEFAULT_PLAN_DIFFERENCE_RATE = "15"; // 均衡差异率阈值
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "12"; // 保库存供应时长
    @Autowired
    private TmEngineGlueMapper tmEngineGlueMapper;
	/**
	 * 生产阶段校验开关状态：打开
	 */
	private final static String PRODUCTION_STAGE_ON = "1";

    /**
     * 胎面胶自动排程
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    @Transactional(rollbackFor=Exception.class)
    public void autoTmSchedule(String scheduleDate) {
        String username = SecurityUtils.getUsername(); //用户账号
        String cxBatchNo = "";  //成型批次号
        String batchNo = this.createBatchNo(scheduleDate);  //胎面排程批次号
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        Map<String, String> mapAssistSpec = this.mapAssistSpec(); //获得外协规格Map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        BigDecimal standardCurlLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        standardCurlLength = standardCurlLength.compareTo(BigDecimal.ZERO) > 0? standardCurlLength: new BigDecimal(DEFAULT_STANDARD_CRIMP_LENGTH); // 卷曲标准长度防错处理，不合法的配置都按默认值处理
//        BigDecimal curlDecimalRounding = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CURL_DECIMAL_ROUNDING, DEFAULT_CURL_DECIMAL_ROUNDING)); // 卷曲数小数取整值，小数部分大于等于该值的进位，否则舍弃
//        BigDecimal midPlanQtyReference = new BigDecimal(paramsMap.getOrDefault(EngineConstants.MID_PLAN_QTY_REFERENCE, DEFAULT_MID_PLAN_QTY_REFERENCE)); // 夜班计划参考值，用于均衡
//        BigDecimal closeOutDays = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CLOSE_OUT_DAYS, DEFAULT_CLOSE_OUT_DAYS)); // 共用规格收尾判断天数
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
        double mergeThreshold = getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD));
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault("PRODUCT_STOCK_HOUR", DEFAULT_PRODUCT_STOCK_HOUR));
        double productStockDay = BigDecimalUtils.div(productStockHour, BigDecimalUtils.HOUR24).doubleValue();
        // 查询机台维修计划，如果有，则需扣减机台对应生产定额，
        // K：GenerageMapKeyUtils.createMapKey(机台ID, 停机班次)，V：机台需扣减的生产定额
        Map<String, BigDecimal> machineSubQuotaMap = tmEngineMachineService.selectMachineSubQuota(scheduleDate);
        List<TmScheduleResultVo> scheduleList = tmEngineMapper.statTmScheduleBase(scheduleDate, productionStage);  //根据成型排程记录 统计出 胎面胶排程记录基础数据
        if (scheduleList == null || scheduleList.isEmpty()) {
            log.info("根据成型排程记录 统计出 胎面胶排程记录基础数据 为空");
            autoScheduleLogService.insertTmScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料"); //添加日志
            throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
        }
        //过滤掉成型2个班的计划量都为0的数据
//        scheduleList = scheduleList.stream().filter(s -> (s.getCxClass2Plan()+s.getCxClass3Plan()+s.getCxClass4Plan())>0).collect(Collectors.toList());
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "根据成'型排程记录'统计出胎面胶排程记录基础数据",  toJSONString(scheduleList));
//        this.ValidatedConstruction(scheduleDate, batchNo, productionStage, mapAssistSpec);   //证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
        Map<String, String> glueSeqMap = tmEngineGlueService.getGlueSeqMap();  //获取胶料序号map
        Map<String, String> mouthPlateMachineMap = tmEngineMachineService.getMouthPlateMachineMap(); //获得口型板代码map
        Map<String, String> specifyCanMachineMap = tmEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN); //获得胎面代码和定点机台的限制作业map
        Map<String, String> specifyNotMachineMap = tmEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT); //获得胎面代码和定点机台的不可作业map
        Map<String, Double> planStockMap = tmEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算胎面16点预计库存
        Map<String, Double> stockMap = this.loadTmStock(scheduleDate); // 加载库存
        Map<String, Double> lastDayMidPlanMap = this.loadLastDayMidPlan(scheduleDate); // 加载昨日早班计划
        Map<String, String> lastDayGlueMachineMap = this.loadLastDayMidPlan4Glue(scheduleDate); // 加载昨日胶料对应机台
        Map<String, Double> lossRateMap = tmEngineLossService.getLossRateMap();   //损耗率map
        Map<String, TmMonthSurplusVo> monthSurplus = tmEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
//        List<String> closeOutSpecList = this.getCloseOutSpecList(scheduleDate, closeOutDays, productionStage); // 获取当天的收尾规格列表
        Map<String, BigDecimal> tmCurlLengthMap = tmEngineCurlRollService.getTmCurlLengthMap(); // 胎面卷曲设置
        this.baseDataLog(batchNo, glueSeqMap, mouthPlateMachineMap, specifyCanMachineMap, specifyNotMachineMap, planStockMap, lossRateMap, monthSurplus, paramsMap); //把基础数据假如到日志中
//        Map<String, TmTotalPlanQtyVo> totalPlanQtyMap = new HashMap<>();  //每个生产线的计划量汇总MAP
        List<TmMachineInfo> allMachineList = tmEngineMachineService.listTmMachine();
        TmTotalPlanQtyVo totalPlanQtyVo = new TmTotalPlanQtyVo();  //胎面中班和夜班总计划量Vo
        for (TmScheduleResultVo scheduleVo : scheduleList) {
            cxBatchNo = scheduleVo.getCxBatchNo();
            scheduleVo.setBatchNo(batchNo);    //批次号
            String orderNo = this.createOrderNo(batchNo);   //创建工单号
            scheduleVo.setOrderNo(orderNo);
            BigDecimal curlLength = tmCurlLengthMap.getOrDefault(scheduleVo.getTreadCode(), standardCurlLength); // 卷曲长度
            scheduleVo.setCurlLength(curlLength);
//            scheduleVo.setGlueSeq(glueSeqMap.get(scheduleVo.getGlueCode()));  //胶料序号
//            autoScheduleLogService.insertTmScheduleLog(batchNo, orderNo, "根据'胶料顺序集合'设置胶料序号",
//                    logSplit("胶料顺序集合：" + toJSONString(glueSeqMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

//            this.chooseMachine(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap);  //选择生产线
            scheduleVo.setPlanStockQty(planStockMap.getOrDefault(scheduleVo.getTreadCode(), 0D));  //16点预计库存
            scheduleVo.setStockQty(stockMap.getOrDefault(scheduleVo.getTreadCode(), 0D));  // 库存
            scheduleVo.setLastMidPlanQty(lastDayMidPlanMap.getOrDefault(scheduleVo.getTreadCode(), 0D)); // 上一天早班库存
            scheduleVo.setSurplusQty(Optional.ofNullable(monthSurplus.get(scheduleVo.getTreadCode())).map(TmMonthSurplusVo::getMonthRemainQty).orElse(0D)); // 剩余量
            autoScheduleLogService.insertTmScheduleLog(batchNo, orderNo, "根据'16点预计库存集合'设置库存",
                    logSplit("16点预计库存集合：" + toJSONString(planStockMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.newComputeSupplyTime(scheduleVo, scheduleVo.getPlanStockQty());  //库存供应时长
            this.computeTmPlanQty(scheduleVo, totalPlanQtyVo, lossRateMap, paramLossRate, mergeThreshold, curlLength, productStockDay, paramsMap);  //计算胎面中班和夜班计划量
//            this.computeTmCurlRoll(scheduleVo, tmCurlLengthMap, standardCurlLength, closeOutSpecList, curlDecimalRounding, totalPlanQtyMap); // 计算卷曲数
            this.setStatusAndCloseTip(scheduleVo, monthSurplus.get(scheduleVo.getTreadCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段

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
//        this.equilibriumDay1(scheduleList, totalPlanQtyVo, midPlanQtyReference);
//        this.equilibriumDay2(scheduleList, totalPlanQtyVo);  //中班和夜班计排程计划量均衡处理
//        this.glueMerge(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX));  //同胶料合并生产
//        TmScheduleResultVo scheduleVo = scheduleList.stream().filter(s -> s.getTreadCode().equals("1076")).findAny().get();
//        this.glueMerge1(batchNo, scheduleList, paramsMap.get(EngineConstants.SUPPLY_TIME_PASS), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX), paramsMap, tmCurlLengthMap, standardCurlLength);
        this.chooseMachineByCapacityGlueSortAndMouthPlate(scheduleList, allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, lastDayGlueMachineMap, glueSeqMap);  //选择生产线
        // 机台产能不足，重新选机台，不考虑同胶料优先同机台
//        List<TmScheduleResultVo> nullMachineList = scheduleList.stream().filter(item -> item.getMachineId() == null).collect(Collectors.toList());
//        if (CollectionUtils.isNotEmpty(nullMachineList)) {
//            this.chooseMachineByCapacity(scheduleList, allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, lastDayGlueMachineMap);  //选择生产线
//        }
        // 添加小批量的计划，产能不足不添加，如果产能不足的情况，成型第二顺位的计划也可以推迟到成型需求那班再做
//        this.equilibriumMachineQuota(scheduleList, paramsMap, allMachineList, tmCurlLengthMap, standardCurlLength);
        this.setGlueSeq(scheduleList, glueSeqMap);
        Boolean isExceedTotalRoll = this.getIsExceedTotalRoll(scheduleDate, scheduleList, paramsMap);
        this.allocationPlanQty(scheduleDate, scheduleList, lastDayGlueMachineMap, glueSeqMap, tmCurlLengthMap, paramsMap, isExceedTotalRoll);
        this.equilibriumMachineQuota2(scheduleList, allMachineList, tmCurlLengthMap, paramsMap, machineSubQuotaMap, isExceedTotalRoll); // 第二次均衡机台产能
//        this.equalShare(batchNo, scheduleList, paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD));  //单规格排产数量达到设定值时，中夜班数量对半分
        this.setProduceOrder(scheduleList, lastDayGlueMachineMap);  //设置白班和夜班的生产顺序

        List<TmScheduleResultVo> existScheduleList = this.tmEngineMapper.listTmEnginSchedule(scheduleDate);  //查询当天已经存在的排产记录
        this.syncTmScheduleToLog(scheduleDate);  //把排程数据同步到log表，删除历史外协排程数据
        this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);  //创建自动排程记录
        List<TmScheduleResultVo> assistScheduleList = scheduleList.stream().filter(r -> mapAssistSpec.containsKey(r.getTreadCode())).collect(Collectors.toList()); //过滤出外协排程数据
        scheduleList = scheduleList.stream().filter(r -> !mapAssistSpec.containsKey(r.getTreadCode())).collect(Collectors.toList());  //过滤出非外协的排产数据
        if(StringUtils.isNotEmpty(assistScheduleList)) {
            tmEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);   //批量新增外协排程结果数据
        }

        scheduleList = this.mergeExistSchedule(batchNo, scheduleList, existScheduleList);  //如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
        if(StringUtils.isNotEmpty(scheduleList)) {
            tmEngineMapper.batchCreateScheduleResult(scheduleList);   //批量新增非外协排程结果数据
        }
    }

    private Boolean getIsExceedTotalRoll(String scheduleDate, List<TmScheduleResultVo> scheduleList, Map<String, String> paramsMap) {
        // 总工装数量
        BigDecimal totalRollNum = BigDecimalUtils.valueOf(paramsMap.get(EngineConstants.TOTAL_ROLL_NUM));
        // SUM(昨日早班计划+夜班+库存-成型预计消耗)>总工装数量
        Map<String, Double> cxConsumeMap = new HashMap<>(16);
        List<TmScheduleResult> cxConsumeList = tmEngineMapper.getCxConsume4List(scheduleDate);
        if (CollectionUtils.isNotEmpty(cxConsumeList)) {
            cxConsumeMap = cxConsumeList.stream().collect(Collectors.toMap(TmScheduleResult::getTreadCode, TmScheduleResult::getCxConsumeQty));
        }

        // 计算总的工装数量
        BigDecimal planSumRollNum = BigDecimal.ZERO;
        for (TmScheduleResultVo scheduleResultVo : scheduleList) {
            Double lastMidPlanQty = scheduleResultVo.getLastMidPlanQty();
            Double stockQty = scheduleResultVo.getStockQty();
            Double dayPlanQty = scheduleResultVo.getDayPlanQty();

            String treadCode = scheduleResultVo.getTreadCode();
            Double cxConsume = cxConsumeMap.getOrDefault(treadCode, 0D);

            double planQty = lastMidPlanQty + stockQty + dayPlanQty - cxConsume;
            BigDecimal result = BigDecimalUtils.div(planQty, scheduleResultVo.getCurlLength());
            planSumRollNum = planSumRollNum.add(result);
        }

        return planSumRollNum.compareTo(totalRollNum) > 0;
    }

    /**
     * 添加小批量的计划，产能不足不添加。如果产能不足的情况，成型第二顺位的计划也可以推迟到成型需求那班再做
     *
     * @param scheduleList       排程结果列表
     * @param paramsMap          参数
     * @param allMachineList     机台列表
     * @param curlLengthMap      卷曲长度
     * @param standardCurlLength 标准卷曲长度
     */
    private void equilibriumMachineQuota(List<TmScheduleResultVo> scheduleList, Map<String, String> paramsMap, List<TmMachineInfo> allMachineList, Map<String, BigDecimal> curlLengthMap, BigDecimal standardCurlLength) {
        List<TmScheduleResultVo> machineNullList = scheduleList.stream().filter(item -> item.getMachineId() == null).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(machineNullList)) {
            log.error("机台为空的列表:{}", JSON.toJSONString(machineNullList));
            return;
        }
        Map<String, Double> machineDayPlanQtyMap = scheduleList.stream().collect(Collectors.groupingBy(TmScheduleResultVo::getMachineId, Collectors.summingDouble(TmScheduleResultVo::getDayPlanQty)));
        Map<String, Double> machineNightPlanQtyMap = scheduleList.stream().collect(Collectors.groupingBy(TmScheduleResultVo::getMachineId, Collectors.summingDouble(TmScheduleResultVo::getNightPlanQty)));
        Map<String, Boolean> machineDayCapacityMoreMap = new HashMap<>(16);
        for (TmMachineInfo machineInfo : allMachineList) {
            String id = String.valueOf(machineInfo.getId());
            BigDecimal quata = ObjectUtils.defaultIfNull(machineInfo.getQuata(), BigDecimal.ZERO);
            if (machineDayPlanQtyMap.containsKey(id)) {
                Double sumDayPlanQty = machineDayPlanQtyMap.get(id);
                if (sumDayPlanQty > quata.doubleValue()) {
                    machineDayCapacityMoreMap.put(id, true);
                }
            }
        }
        Map<Long, BigDecimal> machineQuotaMap = allMachineList.stream().collect(Collectors.toMap(TmMachineInfo::getId, item -> ObjectUtils.defaultIfNull(item.getQuata(), BigDecimal.ZERO)));

        String mergeMaxRoll = paramsMap.get(EngineConstants.MERGE_MAX_ROLL);
        BigDecimal mergeMaxRollSubOne = new BigDecimal(mergeMaxRoll).subtract(BigDecimal.ONE);
        Integer cxMergeMaxSort = Integer.valueOf(paramsMap.getOrDefault(EngineConstants.CX_MERGE_MAX_SORT, "3"));
        BigDecimal oneRollNum = new BigDecimal(paramsMap.getOrDefault(EngineConstants.ONE_ROLL_NUM, DEFAULT_ONE_ROLL_NUM));
        String mergeMaxRollStr = paramsMap.get(EngineConstants.MERGE_MAX_ROLL);
        // 根据胶料分组，如果计划卷数小于mergeMaxRoll算小批量，且成型二班顺序大于等于2，将计划量从夜班移到早班
        Map<String, List<TmScheduleResultVo>> groupMap1 = scheduleList.stream().collect(Collectors.groupingBy(TmScheduleResultVo::getMachineId));
        Set<Map.Entry<String, List<TmScheduleResultVo>>> entrySet = groupMap1.entrySet();
        for (Map.Entry<String, List<TmScheduleResultVo>> entry : entrySet) {
            String machineId = entry.getKey();
            if (!machineDayCapacityMoreMap.containsKey(machineId)) {
                continue;
            }
            BigDecimal quota = machineQuotaMap.get(Long.valueOf(machineId));
            Double dayTotalPlanQty = machineDayPlanQtyMap.get(machineId);
            Double nightTotalPlanQty = machineNightPlanQtyMap.get(machineId);
            List<TmScheduleResultVo> value = entry.getValue();
            List<TmScheduleResultVo> sortedList = value.stream().sorted(Comparator.comparing(TmScheduleResultVo::getDayPlanQty).reversed()).collect(Collectors.toList());
            for (TmScheduleResultVo scheduleVo : sortedList) {
                // 通过计划量小于机台定额了，则直接跳出
                if (BigDecimal.valueOf(dayTotalPlanQty).compareTo(quota) <= 0) {
                    break;
                }
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();

                BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getTreadCode(), standardCurlLength);
                BigDecimal dayRollNum = BigDecimalUtils.valueOf(dayPlanQty).divide(curlLength, 0, RoundingMode.CEILING);

                Double stockQty = scheduleVo.getStockQty();
                Double lastMidPlanQty = scheduleVo.getLastMidPlanQty();
                double totalStock = stockQty + lastMidPlanQty;
                Double cxClass1Plan = scheduleVo.getCxClass1Plan();
                Double cxClass2Plan = scheduleVo.getCxClass2Plan();
                double cxTotalPlan = cxClass1Plan + cxClass2Plan;

                if (scheduleVo.getClass2Sort() != null
                        && scheduleVo.getClass2Sort() >= cxMergeMaxSort
                        && dayRollNum.compareTo(mergeMaxRollSubOne) <= 0
                        && totalStock >= cxTotalPlan) {
                    scheduleVo.setDayPlanQty(0D);
                    scheduleVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    dayTotalPlanQty -= dayPlanQty;
                    nightTotalPlanQty += dayPlanQty;
                }
            }
            machineDayPlanQtyMap.put(machineId, dayTotalPlanQty);
            machineNightPlanQtyMap.put(machineId, nightTotalPlanQty);
        }

        // 小批量多生产2卷，多生产的卷数看参数：ONE_ROLL_NUM，夜班产能足够加到夜班，早班产能足够加到早班
        for (TmScheduleResultVo scheduleResultVo : scheduleList) {
            String machineId = scheduleResultVo.getMachineId();
            // 夜班产能足够
            BigDecimal dayPlanQty = BigDecimal.valueOf(scheduleResultVo.getDayPlanQty());
            BigDecimal nightPlanQty = BigDecimal.valueOf(scheduleResultVo.getNightPlanQty());
            BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleResultVo.getTreadCode(), standardCurlLength);
            BigDecimal dayRollNum = dayPlanQty.divide(curlLength, 0, RoundingMode.CEILING);
            BigDecimal nightRollNum = nightPlanQty.divide(curlLength, 0, RoundingMode.CEILING);
            int totalRollNum = nightRollNum.intValue() + dayRollNum.intValue();
            // 夜班总产能
            Double dayTotalPlanQty = machineDayPlanQtyMap.get(machineId);
            Double nightTotalPlanQty = machineNightPlanQtyMap.get(machineId);
            BigDecimal quota = machineQuotaMap.get(Long.valueOf(machineId));

            if (totalRollNum != 0 && totalRollNum <= Integer.parseInt(mergeMaxRollStr)) {
                // 夜班产能足够，加到夜班，早班产能足够加到早班（已经有计划量的情况下，才会加，否则不加）
                BigDecimal addPlan = oneRollNum.multiply(curlLength);
                if (dayTotalPlanQty < quota.doubleValue() && dayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    scheduleResultVo.setDayPlanQty(BigDecimalUtils.add(dayPlanQty, addPlan).doubleValue());
                    machineDayPlanQtyMap.put(machineId, dayTotalPlanQty + addPlan.doubleValue());
                } else if (nightTotalPlanQty < quota.doubleValue() && nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    scheduleResultVo.setNightPlanQty(BigDecimalUtils.add(nightPlanQty, addPlan).doubleValue());
                    machineNightPlanQtyMap.put(machineId, nightTotalPlanQty + addPlan.doubleValue());
                } else if (dayPlanQty.compareTo(BigDecimal.ZERO) <= 0 && nightPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
                    // 夜班早班计划都为0，先加到早班，产能满了才加到夜班
                    if (nightTotalPlanQty < quota.doubleValue()) {
                        scheduleResultVo.setNightPlanQty(BigDecimalUtils.add(nightPlanQty, addPlan).doubleValue());
                        machineNightPlanQtyMap.put(machineId, nightTotalPlanQty + addPlan.doubleValue());
                    } else if (dayTotalPlanQty < quota.doubleValue()) {
                        scheduleResultVo.setDayPlanQty(BigDecimalUtils.add(dayPlanQty, addPlan).doubleValue());
                        machineDayPlanQtyMap.put(machineId, dayTotalPlanQty + addPlan.doubleValue());
                    }
                }
            }
        }

        // 看夜班还有没有富余产能，如果还有，就把早班的量从大到小的顺序移到早班生产，直到产能满足
        List<TmScheduleResultVo> nightPlanSortList = scheduleList.stream().sorted(Comparator.comparing(TmScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
        for (TmScheduleResultVo scheduleResultVo : nightPlanSortList) {
            String machineId = scheduleResultVo.getMachineId();
            Double dayTotalPlanQty = machineDayPlanQtyMap.get(machineId);
            BigDecimal quota = machineQuotaMap.get(Long.valueOf(machineId));
            Double dayPlanQty = scheduleResultVo.getDayPlanQty();
            Double nightPlanQty = scheduleResultVo.getNightPlanQty();
            double addResultPlan = dayPlanQty + nightPlanQty;
            if (dayTotalPlanQty >= quota.doubleValue()) {
                // 如果成型顺序大于2的，且总量超过定额360，将夜班移到早班
                double moreThanPlan = dayTotalPlanQty - quota.doubleValue();
                if (scheduleResultVo.getClass3Sort() != null
                        && scheduleResultVo.getClass3Sort() > 1
                        && moreThanPlan >= 360
                        && scheduleResultVo.getSupplyTime() >= 6
                ) {
                    scheduleResultVo.setDayPlanQty(0D);
                    scheduleResultVo.setNightPlanQty(addResultPlan);
                    machineDayPlanQtyMap.put(machineId, dayTotalPlanQty - dayPlanQty);
                    machineNightPlanQtyMap.put(machineId, machineNightPlanQtyMap.get(machineId) + nightPlanQty);
                }
                continue;
            }
            scheduleResultVo.setDayPlanQty(addResultPlan);
            scheduleResultVo.setNightPlanQty(0D);
            machineDayPlanQtyMap.put(machineId, dayTotalPlanQty + nightPlanQty);
            machineNightPlanQtyMap.put(machineId, machineNightPlanQtyMap.get(machineId) - nightPlanQty);
        }

        // 看早班还有没有富余产能，如果还有，就把次日夜班的量从大到小的顺序移到早班生产，直到产能满足
        List<TmScheduleResultVo> nextDayPlanSortList = scheduleList.stream().sorted(Comparator.comparing(TmScheduleResultVo::getNextDayPlanQty).reversed()).collect(Collectors.toList());
        for (TmScheduleResultVo scheduleResultVo : nextDayPlanSortList) {
            String machineId = scheduleResultVo.getMachineId();
            Double nightTotalPlanQty = machineNightPlanQtyMap.get(machineId);
            BigDecimal quota = machineQuotaMap.get(Long.valueOf(machineId));
            if (nightTotalPlanQty >= quota.doubleValue()) {
                continue;
            }
            Double nightPlanQty = scheduleResultVo.getNightPlanQty();
            Double nextDayPlanQty = scheduleResultVo.getNextDayPlanQty();
            BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleResultVo.getTreadCode(), standardCurlLength);
            BigDecimal nightRollNum = BigDecimal.valueOf(nightPlanQty).divide(curlLength, 0, RoundingMode.CEILING);
            BigDecimal nextDayRollNum = BigDecimal.valueOf(nextDayPlanQty).divide(curlLength, 0, RoundingMode.CEILING);
            // 大于十卷，取十卷
            BigDecimal addRollNum = nightRollNum.add(nextDayRollNum);
            if (addRollNum.compareTo(BigDecimal.TEN) > 0) {
                double nextDayPlanResult = addRollNum.subtract(BigDecimal.TEN).multiply(curlLength).doubleValue();
                double nightPlanResult = BigDecimal.TEN.multiply(curlLength).doubleValue();
                double addPlanResult = BigDecimal.TEN.subtract(nightRollNum).multiply(curlLength).doubleValue();
                scheduleResultVo.setNightPlanQty(nightPlanResult);
                scheduleResultVo.setNextDayPlanQty(nextDayPlanResult);
                machineNightPlanQtyMap.put(machineId, nightTotalPlanQty + addPlanResult);
            } else {
                double addResultPlan = nightPlanQty + nextDayPlanQty;
                if (nextDayRollNum.compareTo(BigDecimal.valueOf(2)) > 0) {
                    scheduleResultVo.setNightPlanQty(addResultPlan);
                    scheduleResultVo.setNextDayPlanQty(0D);
                    machineNightPlanQtyMap.put(machineId, nightTotalPlanQty + nextDayPlanQty);
                }
            }
        }
    }

    /**
     * 赋值胶料序号
     *
     * @param scheduleList 排程结果列表
     * @param glueSeqMap   胶料序号map
     */
    private void setGlueSeq(List<TmScheduleResultVo> scheduleList, Map<String, String> glueSeqMap) {
        if (CollectionUtils.isNotEmpty(scheduleList)) {
            scheduleList.forEach(s -> s.setGlueSeq(this.getGlueSeq(s, glueSeqMap)));
        }
    }

    /**
     * 获取胶料次序
     * @param tmScheduleResultVo
     * @param glueSeqMap
     * @return
     */
    private String getGlueSeq(TmScheduleResultVo tmScheduleResultVo, Map<String, String> glueSeqMap) {
        String machineIdStr = StringUtils.defaultIfBlank(tmScheduleResultVo.getMachineId(), "");
        // 取第一个机台
        if (machineIdStr.contains(",")) {
            machineIdStr = machineIdStr.split(",")[0];
        }
        String mapKey = String.join("|", machineIdStr, tmScheduleResultVo.getGlueCode());

        return glueSeqMap.getOrDefault(mapKey, "");
    }

    /**
     * 加载当天库存
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadTmStock(String scheduleDate) {
        return tmEngineStockMapper.listTmStock(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getTreadCode()))
                .collect(Collectors.toMap(TmStockVo::getTreadCode, TmStockVo::getStockNum));
    }

    /**
     * 加载上一天的早班计划
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadLastDayMidPlan(String scheduleDate) {
        return tmEngineStockMapper.listLastDayMidPlan(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getTreadCode()))
                .collect(Collectors.toMap(TmStockConsumeVo::getTreadCode, TmStockConsumeVo::getConsume));
    }

    /**
     * 加载胶料代码对应上一天的早班计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    private Map<String, String> loadLastDayMidPlan4Glue(String scheduleDate) {
        return tmEngineStockMapper.listLastDayMidPlan4Glue(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getGlueCode()))
                .collect(Collectors.toMap(TmGlueOrderVo::getGlueCode, TmGlueOrderVo::getMachineId,
                        (v1, v2) -> String.join(",", v1, v2)));
    }

    /**
     * 均衡第一天夜班与第二天的计划
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 胎面中班和夜班总计划量Vo
     */
    private void equilibriumDay1(List<TmScheduleResultVo> scheduleList, TmTotalPlanQtyVo totalPlanQtyVo, double midPlanQtyReference) {
        double totalDayPlanQty = totalPlanQtyVo.getTotalDayPlanQty(); // 夜班总计划量
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划量
        double totalNextDayPlanQty = totalPlanQtyVo.getTotalNextDayPlanQty(); // 次日夜班总计划量
        if (totalDayPlanQty > midPlanQtyReference) {
            return;
        }
        if (totalDayPlanQty > totalNightPlanQty && totalDayPlanQty > totalNextDayPlanQty) {
            return;
        }
        // 需要从供需比例较小的（库存比较小的）开始调整
        scheduleList = scheduleList.stream().sorted(new Comparator<TmScheduleResultVo>() {
            @Override
            public int compare(TmScheduleResultVo o1, TmScheduleResultVo o2) {
                Double day2Plan1 = BigDecimalUtil.add(o1.getNightPlanQty(), o1.getNextDayPlanQty());
                Double day2Plan2 = BigDecimalUtil.add(o2.getNightPlanQty(), o2.getNextDayPlanQty());
                return day2Plan1.compareTo(day2Plan2);
            }

        }).collect(Collectors.toList());

        for (TmScheduleResultVo scheduleVo: scheduleList) {
            double dayPlanQty = scheduleVo.getDayPlanQty();
            double nightPlanQty = scheduleVo.getNightPlanQty();
            double nextDayPlanQty = scheduleVo.getNextDayPlanQty();
            double addPlanQty;

            if (dayPlanQty <= 0) {
                continue;
            }
            if (nightPlanQty > 0) {
                addPlanQty = nightPlanQty;
                nightPlanQty = 0D;
                totalNightPlanQty = BigDecimalUtil.sub(totalNightPlanQty, addPlanQty);
                dayPlanQty = BigDecimalUtil.add(dayPlanQty, addPlanQty);
            } else if (nextDayPlanQty > 0) {
                addPlanQty = nextDayPlanQty;
                nextDayPlanQty = 0D;
                totalNextDayPlanQty = BigDecimalUtil.sub(totalNextDayPlanQty, addPlanQty);
                nightPlanQty = BigDecimalUtil.add(nightPlanQty, addPlanQty);
            } else {
                continue;
            }
            scheduleVo.setDayPlanQty(dayPlanQty);
            scheduleVo.setNightPlanQty(nightPlanQty);
            scheduleVo.setNextDayPlanQty(nextDayPlanQty);
            totalDayPlanQty = BigDecimalUtil.add(totalDayPlanQty, addPlanQty);
            if (totalDayPlanQty > midPlanQtyReference) {
                break;
            }
            if (totalDayPlanQty > totalNightPlanQty && totalDayPlanQty > totalNextDayPlanQty) {
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
     * @param scheduleList   排程列表
     * @param totalPlanQtyVo 中班和夜班总计划量Vo
     */
    private void equilibriumDay2(List<TmScheduleResultVo> scheduleList, TmTotalPlanQtyVo totalPlanQtyVo) {
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划里量
        double totalNextDayPlanQty = totalPlanQtyVo.getTotalNextDayPlanQty(); // 次日夜班总计划量
        double difNum = BigDecimalUtil.sub(totalNextDayPlanQty, totalNightPlanQty); //早班和次日夜班的计划量差额
        if (difNum == 0) {
            return;
        }
//        double bigSizeNightPlanQty = 0D; // 早班大尺寸规格数量
//        double bigSizeDayPlanQty = 0D; // 夜班大尺寸规格数量

        boolean isDayClassPass = (difNum < 0);  //true：早班超量，false：次日夜班超量
        if (isDayClassPass) {
            // 早班超量，说明库存不足，需要从供需比例较大的（库存比较足的）开始调整
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(TmScheduleResultVo::getSupplyDemandRatio, Comparator.reverseOrder())).collect(Collectors.toList());
        } else {
            // 次日夜班超量，说明库存充足，都再提前做隔天的，需要从供需比例较小的（库存比较小的）开始调整
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(TmScheduleResultVo::getSupplyDemandRatio)).collect(Collectors.toList());
        }

        for (TmScheduleResultVo scheduleVo : scheduleList) {
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
            } else if (classStock2 < cxPlanQty2 && isNightPlanQtyLarger) { // 如果交接班库存不足，且早班计划量较大，则不动
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
     * @param lastDayGlueMachineMap 昨日早班胶料与机台信息
     */
    private void chooseMachineByCapacity(List<TmScheduleResultVo> scheduleList, List<TmMachineInfo> allMachineList,
                                         Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
                                         Map<String, String> mouthPlateMachineMap, Map<String, String> lastDayGlueMachineMap) {
        // 机台夜班已占用产能
        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(16);
        // 机台白班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(16);

        // 查询胶料机台关系
        Map<String, List<TmGlueMachineReal>> glueMaichineMap = new HashMap<>(16);
        Map<Long, List<TmGlueMachineReal>> maichineGlueMap = new HashMap<>(16);
        List<TmGlueMachineReal> glueMachineRealList = tmEngineGlueMapper.listGlueMachineReal();
        if (CollectionUtils.isNotEmpty(glueMachineRealList)) {
            glueMaichineMap = glueMachineRealList.stream().collect(Collectors.groupingBy(TmGlueMachineReal::getGlueCode));
            maichineGlueMap = glueMachineRealList.stream().collect(Collectors.groupingBy(TmGlueMachineReal::getMachineId));
        }

        // 获取规格仅可选择一个机台的 map:<班次, List<规格>>
        Map<String, List<String>> classCodeMap = new HashMap<>(16);
        for (TmScheduleResultVo scheduleVo : scheduleList) {
            // 胎面代码
            String beadCode = scheduleVo.getTreadCode();
            // 口型板code
            String mouthPlateCode = scheduleVo.getMouthPlateCode();
            // 定点机台ID列表
            String specifyMachineIds = specifyCanMachineMap.get(beadCode);
            String mouthPlateMachineIds = mouthPlateMachineMap.getOrDefault(mouthPlateCode, StringUtils.EMPTY);
            List<String> machineIds;
            // 如果有设置定点机台，需要把非定点全部过滤掉
            if (StringUtils.isNotEmpty(specifyMachineIds)) {
                machineIds = Arrays.asList(specifyMachineIds.split(","));
            } else {
                machineIds = new ArrayList<>(0);
            }
            String glueCode = scheduleVo.getGlueCode();
            List<TmGlueMachineReal> matchGlueMachineRealList = glueMaichineMap.get(glueCode);
            // 可选机台
            List<TmMachineInfo> oneFilterList = allMachineList.stream().filter(m -> {
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
                    }).filter(item -> {
                        if (CollectionUtils.isNotEmpty(matchGlueMachineRealList)) {
                            return matchGlueMachineRealList.stream().anyMatch(glueMachineReal -> Objects.equals(glueMachineReal.getMachineId(), item.getId()));
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
                        // 口型板机台
                        return mouthPlateMachineIds.contains(String.valueOf(m.getId()));
                    })
                    .collect(Collectors.toList());
            List<String> glueMachineClassList = matchGlueMachineRealList.stream().map(TmGlueMachineReal::getMachineClass)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(glueMachineClassList)) {
                glueMachineClassList = new ArrayList<>();
            }
            List<String> finalGlueMachineClassList = glueMachineClassList;
            List<TmMachineInfo> nightClassMachineList = oneFilterList.stream()
                    .filter(item ->
                            item.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))
                                    && finalGlueMachineClassList.contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))
                    ).collect(Collectors.toList());
            List<TmMachineInfo> dayClassMachineList = oneFilterList.stream()
                    .filter(item ->
                            item.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))
                                    && finalGlueMachineClassList.contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))
                    ).collect(Collectors.toList());
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

        Map<String, List<TmGlueMachineReal>> finalGlueMaichineMap = glueMaichineMap;
        List<TmScheduleResultVo> chooseMachineScheduleList = scheduleList.stream()
                .sorted((o1, o2) -> {
                    // 先看哪个只有一个机台，只有一个机台的先选机台
                    Integer oneMachineFlag1 = nightClassOneMachineList.contains(o1.getTreadCode()) ? 1 : 2;
                    Integer oneMachineFlag2 = nightClassOneMachineList.contains(o2.getTreadCode()) ? 1 : 2;
                    if (oneMachineFlag1.compareTo(oneMachineFlag2) != 0) {
                        return oneMachineFlag1.compareTo(oneMachineFlag2);
                    }

                    Integer flag1 = specifyCanMachineMap.containsKey(o1.getTreadCode()) ? 1 : 2;
                    Integer flag2 = specifyCanMachineMap.containsKey(o2.getTreadCode()) ? 1 : 2;
                    if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                        return flag1.compareTo(flag2);
                    }
                    // 如果定点机台设置一样，则按计划量从大到小
                    BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o2.getNightPlanQty());
                    BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
                    return planQty2.compareTo(planQty1);
        }).collect(Collectors.toList());

        // 胶料对应机台Map
        Map<String, TmMachineInfo> glueMachineMap = new HashMap<>(16);

        // 根据夜班计划分配机台
        for (TmScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            Double midPlanQty = scheduleVo.getDayPlanQty();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            List<TmMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, glueMaichineMap, maichineGlueMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 直接取第一个机台赋值
            TmMachineInfo machine;
            String glueCode = scheduleVo.getGlueCode();
            machine = CollectionUtil.firstElement(optionalMachineList);
            if (!glueMachineMap.containsKey(glueCode)) {
                glueMachineMap.put(glueCode, machine);
            }
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
                    // 先看哪个只有一个机台，只有一个机台的先选机台
                    Integer oneMachineFlag1 = dayClassOneMachineList.contains(o1.getTreadCode()) ? 1 : 2;
                    Integer oneMachineFlag2 = dayClassOneMachineList.contains(o2.getTreadCode()) ? 1 : 2;
                    if (oneMachineFlag1.compareTo(oneMachineFlag2) != 0) {
                        return oneMachineFlag1.compareTo(oneMachineFlag2);
                    }

                    Integer flag1 = specifyCanMachineMap.containsKey(o1.getTreadCode()) ? 1 : 2;
                    Integer flag2 = specifyCanMachineMap.containsKey(o2.getTreadCode()) ? 1 : 2;
                    if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                        return flag1.compareTo(flag2);
                    }
                    // 如果定点机台设置一样，则按计划量从大到小
                    BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o2.getNightPlanQty());
                    BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
                    return planQty2.compareTo(planQty1);
                }).collect(Collectors.toList());

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (TmScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                continue;
            }
            List<TmMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    // 检索当班可选机台
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, glueMaichineMap, maichineGlueMap);
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 直接取第一个机台赋值
            TmMachineInfo machine;
            String glueCode = scheduleVo.getGlueCode();
            machine = CollectionUtil.firstElement(optionalMachineList);
            if (!glueMachineMap.containsKey(glueCode)) {
                glueMachineMap.put(glueCode, machine);
            }
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

        for (TmScheduleResultVo scheduleVo : scheduleList) {
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
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleList          排程列表
     * @param specifyCanMachineMap  定点机台中限制作业map
     * @param specifyNotMachineMap  定点机台中不可作业
     * @param mouthPlateMachineMap  口型板代码map
     * @param lastDayGlueMachineMap 昨日早班胶料与机台信息
     */
    private void chooseMachineByCapacityGlueSort(List<TmScheduleResultVo> scheduleList, List<TmMachineInfo> allMachineList,
                                                 Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
                                                 Map<String, String> mouthPlateMachineMap, Map<String, String> lastDayGlueMachineMap) {
        // 机台夜班已占用产能
        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(16);
        // 机台白班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(16);

        // 查询胶料机台关系
        Map<String, List<TmGlueMachineReal>> glueMaichineMap = new HashMap<>(16);
        Map<Long, List<TmGlueMachineReal>> maichineGlueMap = new HashMap<>(16);
        List<TmGlueMachineReal> glueMachineRealList = tmEngineGlueMapper.listGlueMachineReal();
        if (CollectionUtils.isNotEmpty(glueMachineRealList)) {
            glueMaichineMap = glueMachineRealList.stream().collect(Collectors.groupingBy(TmGlueMachineReal::getGlueCode));
            maichineGlueMap = glueMachineRealList.stream().collect(Collectors.groupingBy(TmGlueMachineReal::getMachineId));
        }

        // 获取规格仅可选择一个机台的 map:<班次, List<规格>>
        Map<String, List<String>> classCodeMap = new HashMap<>(16);
        for (TmScheduleResultVo scheduleVo : scheduleList) {
            // 胎面代码
            String beadCode = scheduleVo.getTreadCode();
            // 口型板code
            String mouthPlateCode = scheduleVo.getMouthPlateCode();
            // 定点机台ID列表
            String specifyMachineIds = specifyCanMachineMap.get(beadCode);
            String mouthPlateMachineIds = mouthPlateMachineMap.getOrDefault(mouthPlateCode, StringUtils.EMPTY);
            List<String> machineIds;
            // 如果有设置定点机台，需要把非定点全部过滤掉
            if (StringUtils.isNotEmpty(specifyMachineIds)) {
                machineIds = Arrays.asList(specifyMachineIds.split(","));
            } else {
                machineIds = new ArrayList<>(0);
            }
            String glueCode = scheduleVo.getGlueCode();
            List<TmGlueMachineReal> matchGlueMachineRealList = glueMaichineMap.get(glueCode);
            // 可选机台
            List<TmMachineInfo> oneFilterList = allMachineList.stream().filter(m -> {
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
                    }).filter(item -> {
                        if (CollectionUtils.isNotEmpty(matchGlueMachineRealList)) {
                            return matchGlueMachineRealList.stream().anyMatch(glueMachineReal -> Objects.equals(glueMachineReal.getMachineId(), item.getId()));
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
                        // 口型板机台
                        return mouthPlateMachineIds.contains(String.valueOf(m.getId()));
                    })
                    .collect(Collectors.toList());
            List<String> glueMachineClassList = matchGlueMachineRealList.stream().map(TmGlueMachineReal::getMachineClass)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(glueMachineClassList)) {
                glueMachineClassList = new ArrayList<>();
            }
            List<String> finalGlueMachineClassList = glueMachineClassList;
            List<TmMachineInfo> nightClassMachineList = oneFilterList.stream()
                    .filter(item ->
                            (item.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))
                                    && finalGlueMachineClassList.contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex())))
                                    || finalGlueMachineClassList.isEmpty()
                    ).collect(Collectors.toList());
            List<TmMachineInfo> dayClassMachineList = oneFilterList.stream()
                    .filter(item ->
                            (item.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))
                                    && finalGlueMachineClassList.contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex())))
                                    || finalGlueMachineClassList.isEmpty()
                    ).collect(Collectors.toList());
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

        List<TmScheduleResultVo> chooseMachineScheduleList = scheduleList.stream()
                .sorted((o1, o2) -> {
                    // 先看哪个只有一个机台，只有一个机台的先选机台
                    Integer oneMachineFlag1 = nightClassOneMachineList.contains(o1.getTreadCode()) ? 1 : 2;
                    Integer oneMachineFlag2 = nightClassOneMachineList.contains(o2.getTreadCode()) ? 1 : 2;
                    if (oneMachineFlag1.compareTo(oneMachineFlag2) != 0) {
                        return oneMachineFlag1.compareTo(oneMachineFlag2);
                    }
                    Integer glueFlag1 = "15172".equals(o1.getGlueCode()) ? 1 : 2;
                    Integer glueFlag2 = "15172".equals(o2.getGlueCode()) ? 1 : 2;
                    if (glueFlag1.compareTo(glueFlag2) != 0) {
                        return glueFlag1.compareTo(glueFlag2);
                    }

                    Integer flag1 = specifyCanMachineMap.containsKey(o1.getTreadCode()) ? 1 : 2;
                    Integer flag2 = specifyCanMachineMap.containsKey(o2.getTreadCode()) ? 1 : 2;
                    if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                        return flag1.compareTo(flag2);
                    }
                    // 成型顺序小的先
                    BigDecimal class2Sort1 = BigDecimal.valueOf(ObjectUtils.defaultIfNull(o1.getClass2Sort(), 0D));
                    BigDecimal class2Sort2 = BigDecimal.valueOf(ObjectUtils.defaultIfNull(o2.getClass2Sort(), 0D));
                    if (class2Sort1.compareTo(class2Sort2) != 0) {
                        return class2Sort1.compareTo(class2Sort2);
                    }
                    // 如果定点机台设置一样，则按计划量从大到小
                    BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o2.getNightPlanQty());
                    BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
                    return planQty2.compareTo(planQty1);
                }).collect(Collectors.toList());

        // 胶料对应机台Map
        Map<String, TmMachineInfo> glueMachineMap = new HashMap<>(16);

        // 根据夜班计划分配机台
        for (TmScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            Double midPlanQty = scheduleVo.getDayPlanQty();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            List<TmMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, glueMaichineMap, maichineGlueMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，先取同胶料机台，则直接取第一个机台赋值
            TmMachineInfo machine;
            String glueCode = scheduleVo.getGlueCode();
            if (optionalMachineList.size() > 1 && glueMachineMap.containsKey(glueCode)) {
                // 如果胶料已有机台，优先用对应机台
                machine = glueMachineMap.get(glueCode);
                // 如果机台产能过半，才选另一个
                if (midCapacityMap.containsKey(machine.getId()) && machine.getQuata() != null
                        && midCapacityMap.get(machine.getId()).compareTo(machine.getQuata().divide(BigDecimal.valueOf(5))) >= 0) {
                    machine = CollectionUtil.firstElement(optionalMachineList);
                    if (!glueMachineMap.containsKey(glueCode)) {
                        glueMachineMap.put(glueCode, machine);
                    }
                }
            } else {
                machine = CollectionUtil.firstElement(optionalMachineList);
                if (!glueMachineMap.containsKey(glueCode)) {
                    glueMachineMap.put(glueCode, machine);
                }
            }
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
                    // 先看哪个只有一个机台，只有一个机台的先选机台
                    Integer oneMachineFlag1 = dayClassOneMachineList.contains(o1.getTreadCode()) ? 1 : 2;
                    Integer oneMachineFlag2 = dayClassOneMachineList.contains(o2.getTreadCode()) ? 1 : 2;
                    if (oneMachineFlag1.compareTo(oneMachineFlag2) != 0) {
                        return oneMachineFlag1.compareTo(oneMachineFlag2);
                    }
                    Integer glueFlag1 = "15172".equals(o1.getGlueCode()) ? 1 : 2;
                    Integer glueFlag2 = "15172".equals(o2.getGlueCode()) ? 1 : 2;
                    if (glueFlag1.compareTo(glueFlag2) != 0) {
                        return glueFlag1.compareTo(glueFlag2);
                    }

                    Integer flag1 = specifyCanMachineMap.containsKey(o1.getTreadCode()) ? 1 : 2;
                    Integer flag2 = specifyCanMachineMap.containsKey(o2.getTreadCode()) ? 1 : 2;
                    if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                        return flag1.compareTo(flag2);
                    }
                    // 成型顺序小的先
                    BigDecimal class2Sort1 = BigDecimal.valueOf(ObjectUtils.defaultIfNull(o1.getClass2Sort(), 0D));
                    BigDecimal class2Sort2 = BigDecimal.valueOf(ObjectUtils.defaultIfNull(o2.getClass2Sort(), 0D));
                    if (class2Sort1.compareTo(class2Sort2) != 0) {
                        return class2Sort1.compareTo(class2Sort2);
                    }
                    // 如果定点机台设置一样，则按计划量从大到小
                    BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o2.getNightPlanQty());
                    BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
                    return planQty2.compareTo(planQty1);
                }).collect(Collectors.toList());

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (TmScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                continue;
            }
            List<TmMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    // 检索当班可选机台
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, glueMaichineMap, maichineGlueMap);
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，先取同胶料机台，则直接取第一个机台赋值
            TmMachineInfo machine;
            String glueCode = scheduleVo.getGlueCode();
            if (optionalMachineList.size() > 1 && glueMachineMap.containsKey(glueCode)) {
                // 如果胶料已有机台，优先用对应机台
                machine = glueMachineMap.get(glueCode);
                // 如果机台产能已满，才选另一个
                if (nightCapacityMap.containsKey(machine.getId()) && machine.getQuata() != null
                        && nightCapacityMap.get(machine.getId()).compareTo(machine.getQuata().divide(BigDecimal.valueOf(5))) >= 0) {
                    machine = CollectionUtil.firstElement(optionalMachineList);
                    if (!glueMachineMap.containsKey(glueCode)) {
                        glueMachineMap.put(glueCode, machine);
                    }
                }
            } else {
                machine = CollectionUtil.firstElement(optionalMachineList);
                if (!glueMachineMap.containsKey(glueCode)) {
                    glueMachineMap.put(glueCode, machine);
                }
            }
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

        for (TmScheduleResultVo scheduleVo : scheduleList) {
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
     * 第二次均衡机台产能
     *
     * @param scheduleList
     * @param allMachineList
     * @param curlLengthMap
     * @param paramsMap
     */
    private void equilibriumMachineQuota2(List<TmScheduleResultVo> scheduleList, List<TmMachineInfo> allMachineList,
                                          Map<String, BigDecimal> curlLengthMap, Map<String, String> paramsMap, Map<String, BigDecimal> machineSubQuotaMap, Boolean isExceedTotalRoll) {
        Map<String, List<TmScheduleResultVo>> scheduleMachineMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMachineId()))
                .collect(Collectors.groupingBy(TmScheduleResultVo::getMachineId)); // 将排产计划按机台分组
        Map<String, Map<OpenMachineClassEnums, BigDecimal>> classMachinePlanQtyMap = this.initClassMachinePlanQtyMap(scheduleMachineMap); // 获取各班已排计划
        Map<String, BigDecimal> machineQuata = allMachineList.stream()
                .collect(Collectors.toMap(m -> String.valueOf(m.getId()), TmMachineInfo::getQuata)); // 各机台单班产能
        BigDecimal standardCurlLength = new BigDecimal(
                paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        String dayProductGlueParamValue = paramsMap.getOrDefault(EngineConstants.DAY_PRODUCT_GLUE, "");
        BigDecimal diffRateParam = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.PLAN_DIFFERENCE_RATE, DEFAULT_PLAN_DIFFERENCE_RATE)); // 均衡差异率阈值
        diffRateParam = BigDecimalUtils.percentages2Decimals(diffRateParam);
        List<String> dayProductGlueArr = Arrays.asList(dayProductGlueParamValue.split(",")); // 限制早班生产胶料
        Map<String, List<TmScheduleResultVo>> glueScheduleMap = scheduleList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getGlueCode()))
                .collect(Collectors.groupingBy(TmScheduleResultVo::getGlueCode)); // 按胶料分组的排产记录
        Map<String, Map<OpenMachineClassEnums, BigDecimal>> limitQuataMap = this.initLimitQuataMap(machineQuata,
                scheduleList, allMachineList, machineSubQuotaMap); // 各机台各班的产能限制列表

        Integer minSupplyTime = Integer.parseInt(paramsMap.get(EngineConstants.LESS_SUPPLY_TIME));

        for (Entry<String, Map<OpenMachineClassEnums, BigDecimal>> entry : classMachinePlanQtyMap.entrySet()) {
            String machineId = entry.getKey();
            Map<OpenMachineClassEnums, BigDecimal> classPlanQtyMap = entry.getValue();
            List<TmScheduleResultVo> scheduleMachineList = scheduleMachineMap.get(machineId);
            BigDecimal quata = machineQuata.get(machineId); // 机台产能
            BigDecimal planQtyReference = quata; // 平衡基准值与产能一致（计划量往最大产能靠）
            // 机台各班已排计划量
            OpenMachineClassEnums currentClass = OpenMachineClassEnums.CLASS_TWO;
            while (currentClass.getClassIndex() < OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) { // 遍历2个班的计划
                if (limitQuataMap.containsKey(machineId)) {
                    planQtyReference = limitQuataMap.get(machineId).getOrDefault(currentClass, quata); // 如果有产能限制，按经过限制的产能计算
                }
                OpenMachineClassEnums previousClass = currentClass.getPreviousClass();
                OpenMachineClassEnums nextClass = currentClass.getNextClass();
                BigDecimal classTotalPlanQty = classPlanQtyMap.getOrDefault(currentClass, BigDecimal.ZERO);  // 本班计划量统计
                BigDecimal nextClassTotalPlanQty = classPlanQtyMap.getOrDefault(nextClass, BigDecimal.ZERO); // 下一个班计划量统计
                BigDecimal diff = classTotalPlanQty.subtract(planQtyReference); // 与基准值比较
                if (diff.compareTo(BigDecimal.ZERO) == 0) { // 一致则处理下一个班次
                    continue;
                }
                boolean isPlanQtyPass = diff.compareTo(BigDecimal.ZERO) > 0; // 是否超产能
                OpenMachineClassEnums sortClass = currentClass;
                // 只处理本机台的计划
                List<TmScheduleResultVo> filterList = scheduleMachineList.stream()
                        .sorted((s1, s2) -> {
                            // 第一次序，成型可供时长不足1小时的优先
                            Integer supplyTimeSort1 = s1.getSupplyTime() < minSupplyTime ? 0 : 1;
                            Integer supplyTimeSort2 = s2.getSupplyTime() < minSupplyTime ? 0 : 1;
                            int supplyTimeSortResult = supplyTimeSort1.compareTo(supplyTimeSort2);
                            if (supplyTimeSortResult != 0) {
                                return supplyTimeSortResult;
                            }

                            // 第一次序，按成型需求顺位排序
                            Double sort1 = this.getCxClassSort(s1, nextClass);
                            Double sort2 = this.getCxClassSort(s2, nextClass);
                            int result = isPlanQtyPass? sort2.compareTo(sort1): sort1.compareTo(sort2); // 超量，倒序；产能没满，顺序
                            if (result != 0) {
                                return result;
                            }
                            // 第二次序，库存缺口，下个班为止总计划量之和 - 本班之前库存之和
                            Double cxPlanQty1 = this.getCxClassPlanCumulative(s1, sortClass);
                            Double cxPlanQty2 = this.getCxClassPlanCumulative(s2, sortClass);
                            Double tmPlanQty1 = this.getTmClassPlanCumulative(s1, previousClass);
                            Double tmPlanQty2 = this.getTmClassPlanCumulative(s2, previousClass);
                            Double stock1 = s1.getStockQty();
                            Double stock2 = s2.getStockQty();
                            Double lackStock1 = BigDecimalUtil.sub(BigDecimalUtil.add(stock1, tmPlanQty1), cxPlanQty1);
                            Double lackStock2 = BigDecimalUtil.sub(BigDecimalUtil.add(stock2, tmPlanQty2), cxPlanQty2);
                            result = isPlanQtyPass? lackStock1.compareTo(lackStock2): lackStock2.compareTo(lackStock1);
                            return result;
                        }).collect(Collectors.toList());
                // 上个班正在生产胶料
                List<String> previousProductionGlueList = filterList.stream().filter(s -> this.getPlanQty(s, previousClass) > 0)
                        .map(TmScheduleResultVo::getGlueCode).filter(StringUtils::isNotEmpty).distinct()
                        .collect(Collectors.toList());
                // 本班正在生产胶料
                List<String> inProductionGlueList = filterList.stream().filter(s -> this.getPlanQty(s, sortClass) > 0)
                        .map(TmScheduleResultVo::getGlueCode).filter(StringUtils::isNotEmpty).distinct()
                        .collect(Collectors.toList());
                // 下个班正在生产胶料
                List<String> nextProductionGlueList = filterList.stream().filter(s -> this.getPlanQty(s, nextClass) > 0)
                        .map(TmScheduleResultVo::getGlueCode).filter(StringUtils::isNotEmpty).distinct()
                        .collect(Collectors.toList());
                for (TmScheduleResultVo scheduleVo : filterList) {
                    if (scheduleVo.getIsEqualShare()) { // 已均分的规格不处理
                        continue;
                    }
                    if (dayProductGlueArr.contains(scheduleVo.getGlueCode())) { // 限制早班生产的胶料规格不处理
                        continue;
                    }
                    BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getTreadCode(), standardCurlLength); // 标准长度
                    // 计划量低于基准值，将下个班的部分计划提前
                    if (diff.abs().compareTo(curlLength) <= 0) { // 差异在一卷以内，则直接结束
                        break;
                    }

                    // 超过总工装，不把计划提前到本班生产
                    if (isExceedTotalRoll) {
                        break;
                    }

                    Double planQtyCumulative = this.getTmClassPlanCumulative(scheduleVo, previousClass); // 到上个班次的累计已排计划量
                    Double classStock = BigDecimalUtil.add(planQtyCumulative, scheduleVo.getStockQty()); // 交班库存
                    Double cxPlanQtyCumulative = this.getCxClassPlanCumulative(scheduleVo, currentClass); // 本班为止的成型累计需求
                    Double nextCxPlanQtyCumulative = this.getCxClassPlanCumulative(scheduleVo, nextClass); // 下一个班为止的成型累计需求
                    Double currentClassPlanQty = this.getPlanQty(scheduleVo, currentClass);
                    if (diff.compareTo(BigDecimal.ZERO) > 0) { // 计划量高于基准值，则尝试推迟到下一班
                        if (currentClassPlanQty <= 0) { // 本班没有排计划的跳过
                            continue;
                        }
                        boolean isForceFlag = currentClassPlanQty > planQtyReference.doubleValue(); // 单规格计划量比产能基准值还大，不需要做其他判断直接推迟（通常是当前班次关机）
                        if (!isForceFlag) { // 非强制推迟情况，再判断其他情况是否可推迟
                            if (classStock < cxPlanQtyCumulative) { // 本班的需求量无法满足则不能推迟
                                continue;
                            }
                            if (classStock < nextCxPlanQtyCumulative) { // 下个班的需求量不满足
                                if (this.getCxClassSort(scheduleVo, nextClass) <= 1) { // 下个班成型需求顺位1的不能推迟
                                    continue;
                                }
                            }
                        }

                        if (!nextProductionGlueList.contains(scheduleVo.getGlueCode())) { // 下个班没有生产该胶料的胎面，如果使用相同胶料的规格都符合条件，则把整个胶料转移到下个班
                            List<TmScheduleResultVo> glueScheduleList = glueScheduleMap.get(scheduleVo.getGlueCode());
                            boolean isAllGlueMatch = glueScheduleList.stream().allMatch(s -> {
                                if (this.getCxClassSort(s, nextClass) <= 1) { // 下个班成型需求顺位1的不能推迟
                                    return false;
                                }
                                Double planQtyCumulative1 = this.getTmClassPlanCumulative(s, sortClass); // 到当前班次班的累计已排计划量
                                Double classStock1 = BigDecimalUtil.add(planQtyCumulative1, s.getStockQty()); // 交接班库存
                                if (classStock1 > 0) {
                                    return false;
                                }
                                return true;
                            });
                            if (!isAllGlueMatch) {
                                continue;
                            }
                        }

                        BigDecimal newCLassTotalPlanQty = BigDecimalUtils.sub(classTotalPlanQty, currentClassPlanQty); // 本班计划添加上下个班的计划量
                        BigDecimal newDiff = newCLassTotalPlanQty.subtract(planQtyReference); // 与基准值比较
                        // 符合条件的，下个班的计划提前到本班
                        Double nextClassPlanQty = this.getPlanQty(scheduleVo, nextClass);
                        this.setPlanQty(scheduleVo, currentClass, 0D);
                        this.setPlanQty(scheduleVo, nextClass, BigDecimalUtil.add(nextClassPlanQty, currentClassPlanQty));
                        diff = newDiff;
                        classTotalPlanQty = newCLassTotalPlanQty;
                        nextClassTotalPlanQty = BigDecimalUtils.add(nextClassTotalPlanQty, currentClassPlanQty);
                        if (!nextProductionGlueList.contains(scheduleVo.getGlueCode())) {
                            nextProductionGlueList.add(scheduleVo.getGlueCode());
                        }
                        if (diff.abs().compareTo(curlLength) <= 0 || newCLassTotalPlanQty.compareTo(planQtyReference) <= 0) { // 新差异在一卷以内、或者总计划量已经达到基准值，则直接结束
                            break;
                        }
                    } else {
                        if (!inProductionGlueList.contains(scheduleVo.getGlueCode())
                                && previousProductionGlueList.contains(scheduleVo.getGlueCode())) { // 本班没有生产该胶料的胎面且上个班有生产该胶料，跳过
                            continue;
                        }
                        if (this.getPlanQty(scheduleVo, previousClass) > 0 && currentClassPlanQty == 0) { // 如果上个班有排计划本班没有，为防止重复投产，跳过
                            continue;
                        }

                        // 尝试把下个班成型有需求但交接班库存不足的计划提前
                        Double nextClassPlanQty = this.getPlanQty(scheduleVo, nextClass);
                        if (nextClassPlanQty <= 0) { // 下个班无计划量的跳过
                            continue;
                        }
                        Double cxPlanQty = this.getCxClassPlanCumulative(scheduleVo, nextClass); // 下个班为止的成型需求量
                        if (classStock < cxPlanQty) { // 如果交接班库存不足，将计划量提前
                            BigDecimal newCLassTotalPlanQty = BigDecimalUtils.add(classTotalPlanQty, nextClassPlanQty); // 本班计划添加上下个班的计划量
                            BigDecimal newDiff = newCLassTotalPlanQty.subtract(planQtyReference); // 与基准值比较
                            if (newCLassTotalPlanQty.compareTo(quata) > 0) { // 处理后超产能则不处理
                                continue;
                            }
                            // 符合条件的，下个班的计划提前到本班
                            this.setPlanQty(scheduleVo, currentClass, BigDecimalUtil.add(currentClassPlanQty, nextClassPlanQty));
                            this.setPlanQty(scheduleVo, nextClass, 0D);
                            diff = newDiff;
                            classTotalPlanQty = newCLassTotalPlanQty;
                            nextClassTotalPlanQty = BigDecimalUtils.sub(nextClassTotalPlanQty, nextClassPlanQty);
                            if (!inProductionGlueList.contains(scheduleVo.getGlueCode())) {
                                inProductionGlueList.add(scheduleVo.getGlueCode());
                            }
                            if (diff.abs().compareTo(curlLength) <= 0 || newCLassTotalPlanQty.compareTo(planQtyReference) >= 0) { // 新差异在一卷以内、或者总计划量已经达到基准值，则直接结束
                                break;
                            }
                        }
                    }
                }

                // 基准值大于0（开机），则本班与下半都有生产的胶料强制转移
//                if (planQtyReference.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(BigDecimal.ZERO) < 0) {
//                    BigDecimal diffRate = diff.abs().divide(planQtyReference, 4, RoundingMode.HALF_UP);
//                    if (diffRate.compareTo(diffRateParam) >= 0) {
//                        filterList.sort((s1, s2) -> {
//                            int result = 0;
//                            // 有同种胶料在产的优先调整
//                            Integer inProduct1 = inProductionGlueList.contains(s1.getGlueCode())? 0: 1;
//                            Integer inProduct2 = inProductionGlueList.contains(s2.getGlueCode())? 0: 1;
//                            result = inProduct1.compareTo(inProduct2);
//                            if (result != 0) {
//                                return result;
//                            }
//                            return this.getPlanQty(s1, nextClass).compareTo(this.getPlanQty(s2, nextClass));
//                        }); // 根据下个计划量重新排序，从小计划量开始往前提
//                        for (TmScheduleResultVo scheduleVo : filterList) {
//                            if (scheduleVo.getIsEqualShare()) { // 已均分的规格不处理
//                                continue;
//                            }
//                            BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getTreadCode(), standardCurlLength); // 标准长度
//                            Double nextClassPlanQty = this.getPlanQty(scheduleVo, nextClass);
//                            if (nextClassPlanQty <= 0) { // 下个班无计划量的跳过
//                                continue;
//                            }
////                            if (!inProductionGlueList.contains(scheduleVo.getGlueCode())) { // 本班不在产的胶料跳过
////                                continue;
////                            }
//                            BigDecimal newCLassTotalPlanQty = BigDecimalUtils.add(classTotalPlanQty, nextClassPlanQty); // 本班计划添加上下个班的计划量
//                            BigDecimal newDiff = newCLassTotalPlanQty.subtract(planQtyReference); // 与基准值比较
//                            if (newCLassTotalPlanQty.compareTo(quata) > 0) { // 提前后会导致超产能则不处理
//                                continue;
//                            }
//                            // 符合条件的，下个班的计划提前到本班
//                            Double currentClassPlanQty = this.getPlanQty(scheduleVo, currentClass); // 本班计划量
//                            this.setPlanQty(scheduleVo, currentClass, BigDecimalUtil.add(currentClassPlanQty, nextClassPlanQty));
//                            this.setPlanQty(scheduleVo, nextClass, 0D);
//                            diff = newDiff;
//                            classTotalPlanQty = newCLassTotalPlanQty;
//                            nextClassTotalPlanQty = BigDecimalUtils.sub(nextClassTotalPlanQty, nextClassPlanQty);
//                            if (diff.abs().compareTo(curlLength) <= 0 || newCLassTotalPlanQty.compareTo(planQtyReference) >= 0) { // 新差异在一卷以内、或者总计划量已经达到基准值，则直接结束
//                                break;
//                            }
//                            diffRate = diff.abs().divide(planQtyReference, 4, RoundingMode.HALF_UP); // 计划提前后重新计算差异率
//                            if (diffRate.compareTo(diffRateParam) < 0) { // 差异率低于参数则视为已平衡
//                                break;
//                            }
//                        }
//                    }
//                }

                classPlanQtyMap.put(currentClass, classTotalPlanQty);
                classPlanQtyMap.put(nextClass, nextClassTotalPlanQty);
                currentClass = currentClass.getNextClass();
                if (currentClass == null) {
                    break;
                }
            }
        }
    }

    /**
     * 更新计划量统计对象
     * @param scheduleList
     * @param totalPlanQtyVo
     */
    private void refreshTotalPlanQtyVo(List<TmScheduleResultVo> scheduleList, TmTotalPlanQtyVo totalPlanQtyVo) {
        Double totalDayPlanQty = scheduleList.stream().mapToDouble(TmScheduleResultVo::getDayPlanQty).sum();
        Double totalNightPlanQty = scheduleList.stream().mapToDouble(TmScheduleResultVo::getNightPlanQty).sum();
        Double totalNextDayPlanQty = scheduleList.stream().mapToDouble(TmScheduleResultVo::getNextDayPlanQty).sum();
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty);
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty);
        totalPlanQtyVo.setTotalNextDayPlanQty(totalNextDayPlanQty);
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalDayPlanQty, totalNightPlanQty, totalNextDayPlanQty));
    }

    /**
     * 初始化各机台各班的产能限制列表
     *
     * @param machineQuata   机台产能
     * @param scheduleList   排产计划
     * @param allMachineList 机台列表
     * @return
     */
    private Map<String, Map<OpenMachineClassEnums, BigDecimal>> initLimitQuataMap(Map<String, BigDecimal> machineQuata,
                                                                                  List<TmScheduleResultVo> scheduleList, List<TmMachineInfo> allMachineList, Map<String, BigDecimal> machineSubQuotaMap) {
        Map<String, Map<OpenMachineClassEnums, BigDecimal>> limitQuataMap = new HashMap<>();
//        // 如果夜班计划量比所有机台的可用产能小，则限制可用产能最大的机台的计划量
//        Double totalCxPlanQty = scheduleList.stream()
//                .mapToDouble(s -> this.getCxClassPlanCumulative(s, OpenMachineClassEnums.CLASS_FOUR)).sum(); // 两天的总需求量
//        Double totalStockQty = scheduleList.stream().mapToDouble(TmScheduleResultVo::getStockQty).sum(); // 总库存量
//        Double lastMidPlanQty = scheduleList.stream().mapToDouble(TmScheduleResultVo::getLastMidPlanQty).sum(); // 总早班已拍量
//        // 夜班可排产计划量 = 第一天可排产计划量 - 早班已排计划量。其中：第一天可排产计划量 = 两天总需求量 - 库存。由于第一天早班计划已确定，要扣减掉
//        Double day1Class2PlanQty = BigDecimalUtil.sub(BigDecimalUtil.sub(totalCxPlanQty, totalStockQty),
//                lastMidPlanQty);
//        BigDecimal totalQuata = machineQuata.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add); // 总产能
//        if (day1Class2PlanQty > 0 && day1Class2PlanQty < totalQuata.doubleValue()) { // 如果夜班可排产计划量
//            Entry<String, BigDecimal> entry = machineQuata.entrySet().stream()
//                    .max((e1, e2) -> e1.getValue().compareTo(e2.getValue())).orElse(null);
//            BigDecimal quata = entry.getValue(); // 产能
//            BigDecimal newQuata = BigDecimalUtils.sub(quata, BigDecimalUtils.sub(totalQuata, day1Class2PlanQty)); // 产能要扣除超库容量的部分
//            Map<OpenMachineClassEnums, BigDecimal> classQuataMap = new HashMap<>();
//            classQuataMap.put(OpenMachineClassEnums.CLASS_TWO, BigDecimalUtils.greatest(BigDecimal.ZERO, newQuata)); // 早班的产能分配限制
//            limitQuataMap.put(entry.getKey(), classQuataMap);
//            // 第二天可排产计划量 = 第一天总需求量 + 第二天总需求量 * 2 - 库存 - 第一天总计划量
//        }
        // 限制关机班次的产能
        for (TmMachineInfo machine : allMachineList) {
            String openMachineClass = machine.getOpenMachineClass();
            String machineId = String.valueOf(machine.getId());
            Map<OpenMachineClassEnums, BigDecimal> classQuataMap = limitQuataMap.get(machineId);
            if (classQuataMap == null) {
                classQuataMap = new HashMap<>();
                limitQuataMap.put(machineId, classQuataMap);
            }
            OpenMachineClassEnums currentClass = OpenMachineClassEnums.CLASS_TWO; // 从夜班开始
            while (currentClass.getClassIndex() < OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) {
                BigDecimal quata = machine.getQuata();
                if (StringUtils.isEmpty(openMachineClass)
                        || !openMachineClass.contains(String.valueOf(currentClass.getClassIndex()))) { // 检查如果本班不开机则产能归0
                    quata = BigDecimal.ZERO;
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
            Map<String, List<TmScheduleResultVo>> scheduleMachineMap) {
        Map<String, Map<OpenMachineClassEnums, BigDecimal>> classMachinePlanQtyMap = new HashMap<>();
        OpenMachineClassEnums currentClass = OpenMachineClassEnums.CLASS_TWO;
        while (currentClass.getClassIndex() <= OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) { // 遍历3个班的计划
            for (Entry<String, List<TmScheduleResultVo>> entry : scheduleMachineMap.entrySet()) {
                String machineId = entry.getKey();
                List<TmScheduleResultVo> scheduleMachineList = entry.getValue();
                Map<OpenMachineClassEnums, BigDecimal> classPlanQtyMap = classMachinePlanQtyMap.get(machineId);
                if (classPlanQtyMap == null) {
                    classPlanQtyMap = new HashMap<>();
                    classMachinePlanQtyMap.put(machineId, classPlanQtyMap);
                }
                for (TmScheduleResultVo scheduleVo : scheduleMachineList) {
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
     * 分配计划量到各班次
     * <p/>
     * 原则上上个班已经生产过的胶料，本班就不继续生产<br/>
     * 例外：1、库存无法满足本班成型需求<br/>
     * 2、大需求量规格
     *
     * @param scheduleDate          排产日
     * @param scheduleList          排产计划
     * @param lastDayGlueMachineMap 昨日早班胶料与机台信息<胶料号，机台id>
     * @param glueSeqMap            胶料顺序配置
     *
     */
    private void allocationPlanQty(String scheduleDate, List<TmScheduleResultVo> scheduleList,
            Map<String, String> lastDayGlueMachineMap, Map<String, String> glueSeqMap,
                                   Map<String, BigDecimal> curlLengthMap, Map<String, String> paramsMap, Boolean isExceedTotalRoll) {
//        BigDecimal oneSpecLargeDemand = BigDecimalUtils.valueOf(DEFAULT_ONE_SPEC_LARGE_DEMAND); // 单规格大需求量卷数
//        BigDecimal replenishInventory = BigDecimalUtils.valueOf(DEFAULT_REPLENISH_INVENTORY); // 库存缺口补齐卷数阈值
//        Integer cxMergeMinSort = Integer.parseInt(paramsMap.getOrDefault(EngineConstants.CX_MERGE_MIN_SORT, "0")); // 成型需提前生产顺位
        BigDecimal glueLargeDemand = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.GLUE_LARGE_DEMAND, DEFAULT_GLUE_LARGE_DEMAND)); // 胶料大需求量卷数
        BigDecimal standardCurlLength = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        String dayProductGlueParamValue = paramsMap.getOrDefault(EngineConstants.DAY_PRODUCT_GLUE, "");
        List<String> dayProductGlueArr = Arrays.asList(dayProductGlueParamValue.split(",")); // 限制早班生产胶料

        // 查询上一天排产计划，并汇总每个机台胶料分布情况控制每个班的胶料分配
        String lastDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(DateUtils.parseDate(scheduleDate), -1));
        List<TmScheduleResultVo> lastDayScheduleList = tmEngineMapper.listTmEnginSchedule(lastDate);

        // 根据机台分别重新分配各班计划量
        Map<String, List<TmScheduleResultVo>> scheduleMachineMap = scheduleList.stream()
        .filter(s -> StringUtils.isNotEmpty(s.getMachineId()))
        .collect(Collectors.groupingBy(TmScheduleResultVo::getMachineId));

        for (Entry<String, List<TmScheduleResultVo>> entry: scheduleMachineMap.entrySet()) {
            String machineId = entry.getKey();
            List<TmScheduleResultVo> scheduleMachineList = entry.getValue();

            // 统计本机台早班的已排产胶料
            List<String> lastDayMachineGlueList = lastDayScheduleList.stream()
                    .filter(s -> machineId.equals(s.getMachineId()) && s.getNightPlanQty() > 0
                            && StringUtils.isNotEmpty(s.getGlueCode()))
                    .map(TmScheduleResultVo::getGlueCode).distinct().collect(Collectors.toList());
            Map<OpenMachineClassEnums, List<String>> producedGlueClassMap = new HashMap<>(); // 各班已生产胶料
            producedGlueClassMap.put(OpenMachineClassEnums.CLASS_ONE, lastDayMachineGlueList); // 上一天早班是初始第一个班的排产量

            // 统计胶料计划量，用于判断是否大需求量胶料
            Map<String, Double> gluePlanQtyMap = scheduleMachineList.stream().filter(s -> StringUtils.isNotEmpty(s.getGlueCode()))
                    .collect(Collectors.groupingBy(TmScheduleResultVo::getGlueCode,
                            Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                    .mapToDouble(s -> BigDecimalUtil.add(s.getDayPlanQty(), s.getNightPlanQty())).sum())));
            // 各班续做胶料
    //        Map<OpenMachineClassEnums, Map<String, String>> continueGlueMap = new HashMap<>();
    //        continueGlueMap.put(OpenMachineClassEnums.CLASS_ONE, lastDayGlueMachineMap);

            // 轮询每个班次
            OpenMachineClassEnums checkClass = OpenMachineClassEnums.CLASS_TWO;
            while (checkClass.getClassIndex() < OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) { // 目前仅需遍历夜班与早班
                this.equalShareMachineClass(scheduleMachineList, curlLengthMap, paramsMap, checkClass); // 轮询每一个班次之前都做计划量合并
                // 统计生产本班为止，各胶料的“库存 + 昨日早班计划量”最晚可支持到的班次
                Map<String, Map<String, OpenMachineClassEnums>> machineGlueProductClassMap = this.initMachineGlueProductClassMap(scheduleMachineList, checkClass);
                OpenMachineClassEnums lastClass = checkClass.getPreviousClass(); // 上个班次
                OpenMachineClassEnums currentClass = checkClass;
                OpenMachineClassEnums nextClass = currentClass.getNextClass(); // 下个班次
    //            List<String> inProductionGlueList = scheduleList.stream().filter(s -> {
    //                if (StringUtils.isNotEmpty(s.getGlueCode())) {
    //                    return false;
    //                }
    //                return this.getPlanQty(s, currentClass) > 0;
    //            }).map(TmScheduleResultVo::getGlueCode).distinct().collect(Collectors.toList());
                for (TmScheduleResultVo scheduleVo : scheduleMachineList) {
                    String glueCode = scheduleVo.getGlueCode();
                    if (StringUtils.isEmpty(glueCode) || StringUtils.isEmpty(machineId)) {
                        continue;
                    }
                    if (dayProductGlueArr.contains(glueCode)) { // 限制早班生产的胶料规格不处理
                        continue;
                    }
                    if (scheduleVo.getIsEqualShare()) {
                        continue;
                    }
                    BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getTreadCode(), standardCurlLength); // 卷曲长度
                    boolean isLargeDemand = gluePlanQtyMap.getOrDefault(glueCode, 0D) >= glueLargeDemand.multiply(curlLength).doubleValue(); // 是否大需求量
    //                boolean isContinueGlue = machineId.equals(continueGlueMap.get(lastClass).get(glueCode)); // 是否续做胶料
                    // 是否胶料最晚可生产班次，超过本班不生产会导致成型待料
                    boolean isLatestProdctClass = false;
                    Map<String, OpenMachineClassEnums> glueProductClassMap = machineGlueProductClassMap.get(machineId);
                    if (glueProductClassMap != null) {
                        isLatestProdctClass = checkClass == glueProductClassMap.get(glueCode);
                    }
                    boolean isRepeatGlue = producedGlueClassMap.get(lastClass).contains(glueCode); // 是否重复胶料
    //                boolean isInProductGlue = inProductionGlueList.contains(glueCode); // 是否本班正在生产胶料

                    boolean isInAdvance = !isRepeatGlue; // 是否提前，原则上一种胶料不要分成两个班做
                    if (!isLargeDemand && isLatestProdctClass // 非大需求量胶料，本班为最晚可生产班次则提前生产
                            // 2025-9-17 未达到满工装的情况才处理
                            && !isExceedTotalRoll) {
                        isInAdvance = true;
                    } else { // 其他情况不处理
                        continue;
                    }

                    Double currentClassPlanQty = this.getPlanQty(scheduleVo, currentClass);
                    Double nextClassPlanQty = this.getPlanQty(scheduleVo, nextClass);
                    if (isInAdvance) { // 需要提前到本班
                        this.setPlanQty(scheduleVo, currentClass, BigDecimalUtil.add(currentClassPlanQty, nextClassPlanQty));
                        this.setPlanQty(scheduleVo, nextClass, 0D);
                    } else { // 其他情推迟到下个班
                        this.setPlanQty(scheduleVo, currentClass, 0D);
                        this.setPlanQty(scheduleVo, nextClass, BigDecimalUtil.add(currentClassPlanQty, nextClassPlanQty));
                    }
                }
                // 更新已排胶料列表
                Map<String, List<TmScheduleResultVo>> glueMachineMap = scheduleMachineList.stream()
                        .filter(s -> this.getPlanQty(s, currentClass) > 0 && s.getMachineId() != null) // 当前班次有计划量
                        .collect(Collectors.groupingBy(TmScheduleResultVo::getMachineId));
                List<String> glueList = new ArrayList<>();
                for (List<TmScheduleResultVo> glueMachineList: glueMachineMap.values()) {
                    glueList.addAll(glueMachineList.stream().map(TmScheduleResultVo::getGlueCode).collect(Collectors.toList()));
                }
                producedGlueClassMap.put(checkClass, glueList.stream().distinct().collect(Collectors.toList()));

                // 更新续做胶料，根据胶料顺序决定最后一个胶料
    //            Map<String, String> continueGlue = new HashMap<>();
    //            for (Entry<String, List<TmScheduleResultVo>> entry : glueMachineMap.entrySet()) {
    //                String machineId = entry.getKey();
    //                String glueCode = entry.getValue().stream().max( // 取胶料组顺序最大的（最后的）
    //                        Comparator.comparing(TmScheduleResultVo::getGlueSeq, Comparator.nullsFirst(String::compareTo)))
    //                        .map(TmScheduleResultVo::getGlueCode).get();
    //                continueGlue.put(glueCode, machineId);
    //            }
    //            continueGlueMap.put(checkClass, continueGlue);
                checkClass = checkClass.getNextClass(); // 轮询下一个班次
                if (checkClass == null) {
                    break;
                }
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
    private void equalShareMachineClass(List<TmScheduleResultVo> scheduleList, Map<String, BigDecimal> curlLengthMap,
            Map<String, String> paramsMap, OpenMachineClassEnums currentClass) {
//        BigDecimal oneSpecLargeDemand = BigDecimalUtils.valueOf(DEFAULT_ONE_SPEC_LARGE_DEMAND); // 单规格大需求量卷数
        BigDecimal equalShareThreshold = BigDecimalUtils.valueOf(paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD));
        BigDecimal standardCurlLength = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        BigDecimal replenishInventory = BigDecimalUtils.valueOf(DEFAULT_REPLENISH_INVENTORY); // 库存缺口补齐卷数阈值
        Integer cxMergeMinSort = Integer.parseInt(paramsMap.getOrDefault(EngineConstants.CX_MERGE_MIN_SORT, "9")); // 成型需提前生产顺位
        BigDecimal oneRollNum = new BigDecimal(paramsMap.getOrDefault(EngineConstants.ONE_ROLL_NUM, DEFAULT_ONE_ROLL_NUM));
        OpenMachineClassEnums previousClass = currentClass.getPreviousClass();
        OpenMachineClassEnums nextClass = currentClass.getNextClass();

        // 次日早夜班总计划量超过阈值的平分中夜班计划量
        for (TmScheduleResultVo scheduleVo : scheduleList) {
            BigDecimal stockQty = BigDecimalUtils.valueOf(scheduleVo.getStockQty());
            Double planQtyCumulative = this.getTmClassPlanCumulative(scheduleVo, previousClass); // 上个班为止的累计已排计划量
            Double cxPlanQtyCumulative = this.getCxClassPlanCumulative(scheduleVo, currentClass); // 本班为止的成型累计需求
            Double nextCxPlanQtyCumulative = this.getCxClassPlanCumulative(scheduleVo, nextClass); // 下个班为止的成型累计需求

            BigDecimal currentPlanQty = BigDecimalUtils.valueOf(this.getPlanQty(scheduleVo, currentClass)); // 当前班次计划
            BigDecimal nextPlanQty = BigDecimalUtils.valueOf(this.getPlanQty(scheduleVo, nextClass)); // 下一班次计划
            BigDecimal oneDayPlanQty = currentPlanQty.add(nextPlanQty);
            BigDecimal twoClassPlanQty = BigDecimalUtils.add(currentPlanQty, nextPlanQty); // 两个班的计划量合计值
            BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getTreadCode(), standardCurlLength); // 标准卷长
            BigDecimal toolCapacity = curlLength.multiply(oneRollNum); // 满工装生产数
            BigDecimal planStockQty = BigDecimalUtils.add(stockQty, planQtyCumulative); // 上个班为止的预计库存
            BigDecimal currentLackStock = BigDecimalUtils.sub(cxPlanQtyCumulative, planStockQty); // 当班库存缺口
            BigDecimal nextLackStock = BigDecimalUtils.sub(nextCxPlanQtyCumulative, planStockQty); // 下个班库存缺口
            boolean isCxFirstSort = this.getCxClassSort(scheduleVo, nextClass) <= cxMergeMinSort; // 成型需求是否第一顺位
            BigDecimal newCurrentPlanQty = currentPlanQty; // 计算后的当班计划量
            BigDecimal newNextPlanQty = nextPlanQty; // 计算后的下个班计划量
            BigDecimal replenishInventoryLength = curlLength.multiply(replenishInventory); // 库存缺口补齐卷数阈值换，算成长度
            boolean isEqualShare = scheduleVo.getIsEqualShare() != null? scheduleVo.getIsEqualShare(): false;
            if (oneDayPlanQty.compareTo(equalShareThreshold) >= 0) { // 计划量超过均分阈值，则以工装的为单位平分
                BigDecimal newPlanQty = BigDecimalUtils.ceil(BigDecimalUtils.half(twoClassPlanQty), toolCapacity); // 平分后的计划量，先换算成工装数，平分后再换算成米数
                if (currentLackStock.compareTo(BigDecimal.ZERO) > 0) { // 如果当班库存就有缺口，则均分后本班计划不能造成库存缺口
                    newPlanQty = BigDecimalUtils.greatest(BigDecimalUtils.ceil(currentLackStock, toolCapacity), newPlanQty);
                } else if (isCxFirstSort && nextLackStock.compareTo(BigDecimal.ZERO) > 0) { // 如果下一班成型需求顺位为1，则均分后本班计划不能造成下一班有库存缺口
                    newPlanQty = BigDecimalUtils.greatest(BigDecimalUtils.ceil(nextLackStock, toolCapacity), newPlanQty);
                }
                newCurrentPlanQty = BigDecimalUtils.least(newPlanQty, twoClassPlanQty); // 取整后的量不能超过总量
                newNextPlanQty = twoClassPlanQty.subtract(newCurrentPlanQty); // 下个班计划 = 总计划 - 当班计划
                if (newNextPlanQty.compareTo(replenishInventoryLength) <= 0) { // 如果剩余一班的计划量不足最小排产量，则直接合并到本班班（库存缺口太大的情况）
                    newCurrentPlanQty = twoClassPlanQty;
                    newNextPlanQty = BigDecimal.ZERO;
                }
                isEqualShare = true;
            } else { // 没有达到阈值，且有一班没有达到最低排产量的，合并
//                if (currentLackStock.compareTo(BigDecimal.ZERO) > 0) { // 如果当班库存就有缺口，则均分后本班计划不能造成库存缺口
//                    newCurrentPlanQty = twoClassPlanQty;
//                    newNextPlanQty = BigDecimal.ZERO;
//                } else if (isCxFirstSort && nextLackStock.compareTo(BigDecimal.ZERO) > 0) { // 如果下一班成型需求顺位为1，则均分后本班计划不能造成下一班有库存缺口
//                    newCurrentPlanQty = twoClassPlanQty;
//                    newNextPlanQty = BigDecimal.ZERO;
//                } else {
//                    newCurrentPlanQty = BigDecimal.ZERO;
//                    newNextPlanQty = twoClassPlanQty;
//                }
                if (currentPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    newCurrentPlanQty = twoClassPlanQty;
                    newNextPlanQty = BigDecimal.ZERO;
                } else {
                    newCurrentPlanQty = BigDecimal.ZERO;
                    newNextPlanQty = twoClassPlanQty;
                }
            }
            scheduleVo.setIsEqualShare(isEqualShare);
            this.setPlanQty(scheduleVo, currentClass, newCurrentPlanQty.doubleValue());
            this.setPlanQty(scheduleVo, nextClass, newNextPlanQty.doubleValue());
        }
    }

    /**
     * 统计每个机台各胶料的“库存 + 昨日早班计划量”最晚可支持到的班次
     * @param scheduleList
     * @return
     */
    private Map<String, Map<String, OpenMachineClassEnums>> initMachineGlueProductClassMap(List<TmScheduleResultVo> scheduleList, OpenMachineClassEnums startClass) {
        Map<String, Map<String, OpenMachineClassEnums>> machineGlueProductClassMap = new HashMap<>();
        OpenMachineClassEnums lastClass = startClass.getPreviousClass(); // 上一个班次
        for (TmScheduleResultVo scheduleVo : scheduleList) {
            Map<String, OpenMachineClassEnums> glueProductClassMap = machineGlueProductClassMap.get(scheduleVo.getMachineId());
            if (glueProductClassMap == null) {
                glueProductClassMap = new HashMap<>();
                machineGlueProductClassMap.put(scheduleVo.getMachineId(), glueProductClassMap);
            }
            OpenMachineClassEnums prodctClass = glueProductClassMap.getOrDefault(scheduleVo.getGlueCode(), OpenMachineClassEnums.CLASS_FOUR); // 默认可支持到最后一个班
            Double lastPlanCumulative = this.getTmClassPlanCumulative(scheduleVo, lastClass); // 本班开始前的累计已排计划量;
            lastPlanCumulative = BigDecimalUtil.add(lastPlanCumulative, scheduleVo.getStockQty()); // 加上库存
            OpenMachineClassEnums checkClass = startClass; // 检查班次
            Double surplusQty = scheduleVo.getSurplusQty(); // 剩余量
            // 按顺序遍历每个班，检查库存是否足够支撑到本班次
            while (checkClass != null && prodctClass.getClassIndex() > checkClass.getClassIndex()) { // 已排胶料更晚则继续
                Double cxPlanQty = this.getCxClassPlanCumulative(scheduleVo, checkClass); // 成型累计需求量
                Double limitPlanQty = BigDecimalUtils.least(surplusQty, cxPlanQty).doubleValue(); // 取成型累计需求量与剩余量的较小值
                if (lastPlanCumulative < limitPlanQty) { // 与轮询班次的累计需求量比较，不足则说明本班即为最晚生产班次
                    break;
                }
                checkClass = checkClass.getNextClass(); // 轮询下个班次
            }
            glueProductClassMap.put(scheduleVo.getGlueCode(), checkClass);
        }
        return machineGlueProductClassMap;
    }

    /**
     * 获取排产计划指定班次的计划量
     * @param scheduleVo
     * @param currentClass
     * @return
     */
    private Double getPlanQty(TmScheduleResultVo scheduleVo, OpenMachineClassEnums currentClass) {
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
     * @param scheduleVo
     * @param currentClass
     */
    private Double getCxClassSort(TmScheduleResultVo scheduleVo, OpenMachineClassEnums currentClass) {
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
     * 设定计划量到排产计划指定班次中
     * @param scheduleVo
     * @param currentClass
     * @param planQty
     * @return
     */
    private void setPlanQty(TmScheduleResultVo scheduleVo, OpenMachineClassEnums currentClass, Double planQty) {
        if (currentClass == OpenMachineClassEnums.CLASS_TWO) {
            scheduleVo.setDayPlanQty(planQty);
        } else if (currentClass == OpenMachineClassEnums.CLASS_THREE) {
            scheduleVo.setNightPlanQty(planQty);
        } else if (currentClass == OpenMachineClassEnums.CLASS_FOUR) {
            scheduleVo.setNextDayPlanQty(planQty);
        }
    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleList          排程列表
     * @param specifyCanMachineMap  定点机台中限制作业map
     * @param specifyNotMachineMap  定点机台中不可作业
     * @param mouthPlateMachineMap  口型板代码map
     * @param lastDayGlueMachineMap 昨日早班胶料与机台信息
     * @param glueSeqMap            胶料次序
     */
    private void chooseMachineByCapacityGlueSortAndMouthPlate(List<TmScheduleResultVo> scheduleList, List<TmMachineInfo> allMachineList,
                                                              Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
                                                              Map<String, String> mouthPlateMachineMap, Map<String, String> lastDayGlueMachineMap,
                                                              Map<String, String> glueSeqMap) {
        // 机台夜班已占用产能
        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(16);
        // 机台白班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(16);

        // 查询胶料机台关系
        Map<String, List<TmGlueMachineReal>> glueMaichineMap = new HashMap<>(16);
        Map<Long, List<TmGlueMachineReal>> maichineGlueMap = new HashMap<>(16);
        List<TmGlueMachineReal> glueMachineRealList = tmEngineGlueMapper.listGlueMachineReal();
        if (CollectionUtils.isNotEmpty(glueMachineRealList)) {
            glueMaichineMap = glueMachineRealList.stream().collect(Collectors.groupingBy(TmGlueMachineReal::getGlueCode));
            maichineGlueMap = glueMachineRealList.stream().collect(Collectors.groupingBy(TmGlueMachineReal::getMachineId));
        }

        // 获取规格仅可选择一个机台的 map:<班次, List<规格>>
        Map<String, List<String>> classCodeMap = new HashMap<>(16);
        for (TmScheduleResultVo scheduleVo : scheduleList) {
            // 胎面代码
            String beadCode = scheduleVo.getTreadCode();
            // 口型板code
            String mouthPlateCode = scheduleVo.getMouthPlateCode();
            // 定点机台ID列表
            String specifyMachineIds = specifyCanMachineMap.get(beadCode);
            String mouthPlateMachineIds = mouthPlateMachineMap.getOrDefault(mouthPlateCode, StringUtils.EMPTY);
            List<String> machineIds;
            // 如果有设置定点机台，需要把非定点全部过滤掉
            if (StringUtils.isNotEmpty(specifyMachineIds)) {
                machineIds = Arrays.asList(specifyMachineIds.split(","));
            } else {
                machineIds = new ArrayList<>(0);
            }
            String glueCode = scheduleVo.getGlueCode();
            List<TmGlueMachineReal> matchGlueMachineRealList = glueMaichineMap.getOrDefault(glueCode, new ArrayList<>());
            // 可选机台
            List<TmMachineInfo> oneFilterList = allMachineList.stream().filter(m -> {
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
                    }).filter(item -> {
                        if (CollectionUtils.isNotEmpty(matchGlueMachineRealList)) {
                            return matchGlueMachineRealList.stream().anyMatch(glueMachineReal -> Objects.equals(glueMachineReal.getMachineId(), item.getId()));
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
                        // 不支持全口型的机台，要看口型设置，没有设置的就不能在该机台排产
                        return mouthPlateMachineIds.contains(String.valueOf(m.getId()));
                    })
                    .collect(Collectors.toList());
            List<String> glueMachineClassList = matchGlueMachineRealList.stream().map(TmGlueMachineReal::getMachineClass)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(glueMachineClassList)) {
                glueMachineClassList = new ArrayList<>();
            }
            List<String> finalGlueMachineClassList = glueMachineClassList;
            List<TmMachineInfo> nightClassMachineList = oneFilterList.stream()
                    .filter(item ->
                            (item.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))
                                    && finalGlueMachineClassList.contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex())))
                                    || finalGlueMachineClassList.isEmpty()
                    ).collect(Collectors.toList());
            List<TmMachineInfo> dayClassMachineList = oneFilterList.stream()
                    .filter(item ->
                            (item.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))
                                    && finalGlueMachineClassList.contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex())))
                                    || finalGlueMachineClassList.isEmpty()
                    ).collect(Collectors.toList());
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

        List<TmScheduleResultVo> chooseMachineScheduleList = scheduleList.stream()
                .sorted((o1, o2) -> {
                    int result;
                    // 先看哪个只有一个机台，只有一个机台的先选机台
                    Integer oneMachineFlag1 = nightClassOneMachineList.contains(o1.getTreadCode()) ? 1 : 2;
                    Integer oneMachineFlag2 = nightClassOneMachineList.contains(o2.getTreadCode()) ? 1 : 2;
                    result = oneMachineFlag1.compareTo(oneMachineFlag2);
                    if (result != 0) {
                        return result;
                    }
                    // 按胶料次序选机台
                    result = this.getGlueSeq(o1, glueSeqMap).compareTo(this.getGlueSeq(o2, glueSeqMap));
                    if (result != 0) {
                        return result;
                    }

                    Integer flag1 = specifyCanMachineMap.containsKey(o1.getTreadCode()) ? 1 : 2;
                    Integer flag2 = specifyCanMachineMap.containsKey(o2.getTreadCode()) ? 1 : 2;
                    result = flag1.compareTo(flag2);
                    if (result != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                        return result;
                    }
                    // 成型顺序小的先
                    BigDecimal class2Sort1 = BigDecimal.valueOf(ObjectUtils.defaultIfNull(o1.getClass2Sort(), 0D));
                    BigDecimal class2Sort2 = BigDecimal.valueOf(ObjectUtils.defaultIfNull(o2.getClass2Sort(), 0D));
                    result = class2Sort1.compareTo(class2Sort2);
                    if (result != 0) {
                        return result;
                    }
                    // 如果定点机台设置一样，则按计划量从大到小
                    BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o2.getNightPlanQty());
                    BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
                    return planQty2.compareTo(planQty1);
                }).collect(Collectors.toList());

        // 胶料对应机台Map
        Map<String, TmMachineInfo> glueMachineMap = new HashMap<>(16);
        // 口型对应机台Map
        Map<String, TmMachineInfo> mouthMachineMap = new HashMap<>(16);

        // 根据夜班计划分配机台
        for (TmScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            Double midPlanQty = scheduleVo.getDayPlanQty();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            List<TmMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, glueMaichineMap, maichineGlueMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，先取同胶料机台，则直接取第一个机台赋值
            TmMachineInfo machine;
            String glueCode = scheduleVo.getGlueCode();
            String mouthPlateCode = scheduleVo.getMouthPlateCode();
            if (optionalMachineList.size() > 1 && glueMachineMap.containsKey(glueCode)) {
                // 如果胶料已有机台，优先用对应机台
                machine = glueMachineMap.get(glueCode);
                // 如果机台产能过半，才选另一个
                if (midCapacityMap.containsKey(machine.getId()) && machine.getQuata() != null
                        && midCapacityMap.get(machine.getId()).compareTo(machine.getQuata().divide(BigDecimal.valueOf(5))) >= 0) {
                    machine = CollectionUtil.firstElement(optionalMachineList);
                    if (!glueMachineMap.containsKey(glueCode)) {
                        glueMachineMap.put(glueCode, machine);
                    }
                }
                if (optionalMachineList.size() > 1 && mouthMachineMap.containsKey(mouthPlateCode)) {
                    // 如果口型已有机台，优先用对应机台
                    machine = mouthMachineMap.get(mouthPlateCode);
                    // 如果机台产能过半，才选另一个
                    if (midCapacityMap.containsKey(machine.getId()) && machine.getQuata() != null
                            && midCapacityMap.get(machine.getId()).compareTo(machine.getQuata().divide(BigDecimal.valueOf(5))) >= 0) {
                        machine = CollectionUtil.firstElement(optionalMachineList);
                        if (!mouthMachineMap.containsKey(mouthPlateCode)) {
                            mouthMachineMap.put(mouthPlateCode, machine);
                        }
                    }
                } else {
                    machine = CollectionUtil.firstElement(optionalMachineList);
                    if (!mouthMachineMap.containsKey(mouthPlateCode)) {
                        mouthMachineMap.put(mouthPlateCode, machine);
                    }
                }
            } else {
                machine = CollectionUtil.firstElement(optionalMachineList);
                if (!glueMachineMap.containsKey(glueCode)) {
                    glueMachineMap.put(glueCode, machine);
                }
                if (!mouthMachineMap.containsKey(mouthPlateCode)) {
                    mouthMachineMap.put(mouthPlateCode, machine);
                }
            }
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
                    // 先看哪个只有一个机台，只有一个机台的先选机台
                    Integer oneMachineFlag1 = dayClassOneMachineList.contains(o1.getTreadCode()) ? 1 : 2;
                    Integer oneMachineFlag2 = dayClassOneMachineList.contains(o2.getTreadCode()) ? 1 : 2;
                    if (oneMachineFlag1.compareTo(oneMachineFlag2) != 0) {
                        return oneMachineFlag1.compareTo(oneMachineFlag2);
                    }
                    Integer glueFlag1 = "15172".equals(o1.getGlueCode()) ? 1 : 2;
                    Integer glueFlag2 = "15172".equals(o2.getGlueCode()) ? 1 : 2;
                    if (glueFlag1.compareTo(glueFlag2) != 0) {
                        return glueFlag1.compareTo(glueFlag2);
                    }

                    Integer flag1 = specifyCanMachineMap.containsKey(o1.getTreadCode()) ? 1 : 2;
                    Integer flag2 = specifyCanMachineMap.containsKey(o2.getTreadCode()) ? 1 : 2;
                    if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                        return flag1.compareTo(flag2);
                    }
                    // 成型顺序小的先
                    BigDecimal class2Sort1 = BigDecimal.valueOf(ObjectUtils.defaultIfNull(o1.getClass2Sort(), 0D));
                    BigDecimal class2Sort2 = BigDecimal.valueOf(ObjectUtils.defaultIfNull(o2.getClass2Sort(), 0D));
                    if (class2Sort1.compareTo(class2Sort2) != 0) {
                        return class2Sort1.compareTo(class2Sort2);
                    }
                    // 如果定点机台设置一样，则按计划量从大到小
                    BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o2.getNightPlanQty());
                    BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
                    return planQty2.compareTo(planQty1);
                }).collect(Collectors.toList());

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (TmScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                continue;
            }
            List<TmMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    // 检索当班可选机台
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, glueMaichineMap, maichineGlueMap);
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，先取同胶料机台，则直接取第一个机台赋值
            TmMachineInfo machine;
            String glueCode = scheduleVo.getGlueCode();
            String mouthPlateCode = scheduleVo.getMouthPlateCode();
            if (optionalMachineList.size() > 1 && glueMachineMap.containsKey(glueCode)) {
                // 如果胶料已有机台，优先用对应机台
                machine = glueMachineMap.get(glueCode);
                // 如果机台产能已满，才选另一个
                if (nightCapacityMap.containsKey(machine.getId()) && machine.getQuata() != null
                        && nightCapacityMap.get(machine.getId()).compareTo(machine.getQuata().divide(BigDecimal.valueOf(5))) >= 0) {
                    machine = CollectionUtil.firstElement(optionalMachineList);
                    if (!glueMachineMap.containsKey(glueCode)) {
                        glueMachineMap.put(glueCode, machine);
                    }
                }
                if (optionalMachineList.size() > 1 && mouthMachineMap.containsKey(mouthPlateCode)) {
                    // 如果口型已有机台，优先用对应机台
                    machine = mouthMachineMap.get(mouthPlateCode);
                    // 如果机台产能过半，才选另一个
                    if (midCapacityMap.containsKey(machine.getId()) && machine.getQuata() != null
                            && midCapacityMap.get(machine.getId()).compareTo(machine.getQuata().divide(BigDecimal.valueOf(5))) >= 0) {
                        machine = CollectionUtil.firstElement(optionalMachineList);
                        if (!mouthMachineMap.containsKey(mouthPlateCode)) {
                            mouthMachineMap.put(mouthPlateCode, machine);
                        }
                    }
                }
            } else {
                machine = CollectionUtil.firstElement(optionalMachineList);
                if (!glueMachineMap.containsKey(glueCode)) {
                    glueMachineMap.put(glueCode, machine);
                }
                if (!mouthMachineMap.containsKey(mouthPlateCode)) {
                    mouthMachineMap.put(mouthPlateCode, machine);
                }
            }
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

        for (TmScheduleResultVo scheduleVo : scheduleList) {
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
     * 选择排程对应机台列表
     *
     * @param scheduleVo           排程
     * @param classCode            班制
     * @param capacityMap          机台产能map
     * @param allMachineList       所有机台
     * @param specifyCanMachineMap 定点机台
     * @param specifyNotMachineMap 不可作业机台
     * @param mouthPlateMachineMap 口型板机台
     * @param glueMaichineMap       胶料机台关系
     * @return 机台列表
     */
    private List<TmMachineInfo> searchOptionalMachineList(TmScheduleResultVo scheduleVo, String classCode,
                                                          Map<Long, BigDecimal> capacityMap,
                                                          List<TmMachineInfo> allMachineList,
                                                          Map<String, String> specifyCanMachineMap,
                                                          Map<String, String> specifyNotMachineMap,
                                                          Map<String, String> mouthPlateMachineMap,
                                                          Map<String, List<TmGlueMachineReal>> glueMaichineMap,
                                                          Map<Long, List<TmGlueMachineReal>> maichineGlueMap) {
//        BigDecimal dimension = BigDecimalUtils.valueOf(scheduleVo.getDimension()); // 寸口
        String beadCode = scheduleVo.getTreadCode(); // 胎面代码
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
        List<TmGlueMachineReal> glueMachineRealList = glueMaichineMap.get(glueCode);
//        Map<Long, Integer> machineSeqMap = glueMachineRealList.stream().filter(m -> m.getSeq() != null)
//                .collect(Collectors.toMap(TmGlueMachineReal::getMachineId, TmGlueMachineReal::getSeq, (m1, m2) -> m1));

        // 可选机台
        List<TmMachineInfo> optionalMachineList = allMachineList.stream()
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
                }).filter(item -> {
                    if (CollectionUtils.isNotEmpty(glueMachineRealList)) {
                        List<String> machineClassList = glueMachineRealList.stream().map(TmGlueMachineReal::getMachineClass)
                                .filter(StringUtils::isNotBlank).collect(Collectors.toList());
                        boolean isGlueMachineMatch = glueMachineRealList.stream().anyMatch(glueMachineReal -> Objects.equals(glueMachineReal.getMachineId(), item.getId())); // 胶料与机台配置吻合
                        if (CollectionUtils.isEmpty(machineClassList)) {
                            return isGlueMachineMatch;
                        } else {
                            return machineClassList.contains(classCode) && isGlueMachineMatch;
                        }
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
                    // 口型板机台
                    return mouthPlateMachineIds.contains(String.valueOf(m.getId()));
                }).filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode)) // 对应班次可用
                /*.filter(m -> {// 寸口需要在工装范围内
                    String toolingInfo = m.getToolingInfo(); // 工装信息
                    if (StringUtils.isNotEmpty(toolingInfo)) {
                        String[] toolingInfoArr = toolingInfo.split("-");
                        BigDecimal minDimension = BigDecimal.ZERO;
                        BigDecimal maxDimension = BigDecimal.ZERO;
                        if (toolingInfoArr.length > 0) {
                            String minDimensionStr = toolingInfoArr[0];
                            minDimension = NumberUtils.isDigits(minDimensionStr) ? new BigDecimal(minDimensionStr)
                                    : BigDecimal.ZERO; // 寸口限制最小
                        }
                        if (toolingInfoArr.length > 1) {
                            String maxDimensionStr = toolingInfoArr[1];
                            maxDimension = NumberUtils.isDigits(maxDimensionStr) ? new BigDecimal(maxDimensionStr)
                                    : BigDecimal.ZERO; // 寸口限制最大
                            if (minDimension.compareTo(maxDimension) > 0) {
                                maxDimension = minDimension;
                            }
                        }
                        if (minDimension.compareTo(dimension) > 0) {
                            return false;
                        }
                        if (maxDimension.compareTo(dimension) < 0) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                })*/
//                .filter(item -> {
//                    if (item.getQuata() != null) {
//                        // 机台产能小于定额
//                        return capacityMap.getOrDefault(item.getId(), BigDecimal.ZERO).compareTo(item.getQuata()) <= 0;
//                    }
//                    return true;
//                })
                .sorted(new Comparator<TmMachineInfo>() {
                    @Override
                    public int compare(TmMachineInfo m1, TmMachineInfo m2) {
                        // 看哪个能生产的胶料少，优先选择
                        List<TmGlueMachineReal> m1GlueList = maichineGlueMap.get(m1.getId());
                        List<TmGlueMachineReal> m2GlueList = maichineGlueMap.get(m2.getId());
                        int m1Size = m1GlueList.size();
                        int m2Size = m2GlueList.size();
                        if (m1Size != m2Size) {
                            return m1Size > m2Size ? 1 : -1;
                        }
                        // 按剩余产能升序排序
                        int compareTo = capacityMap.getOrDefault(m1.getId(), BigDecimal.ZERO)
                                .compareTo(capacityMap.getOrDefault(m2.getId(), BigDecimal.ZERO));
                        if (compareTo != 0) {
                            return compareTo;
                        }
                        return 0;
                    }
                }).collect(Collectors.toList());
        return optionalMachineList;
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
		return tmEngineMapper.listCloseOutSpec(DateUtils.parseDate(scheduleDate), queryCloseOutDays.intValue(), isProductionStage);
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
	private void computeTmCurlRoll(TmScheduleResultVo scheduleVo, Map<String, BigDecimal> tmCurlLengthMap,
			BigDecimal standardCurlLength, List<String> closeOutSpecList, BigDecimal curlDecimalRounding,
			Map<String, TmTotalPlanQtyVo> totalPlanQtyMap) {
		String treadCode = scheduleVo.getTreadCode();
		if (closeOutSpecList.contains(treadCode)) { // 收尾规格，则直接返回
	        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_ENABLE); // 打上收尾标记
			return;
		}
		BigDecimal curlLength = tmCurlLengthMap.get(treadCode); // 本规格的卷曲长度
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
		TmTotalPlanQtyVo totalPlanQtyVo = totalPlanQtyMap.getOrDefault(key, new TmTotalPlanQtyVo()); // 取出对应生产线的计划量汇总对象
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
        List<EngineConstructionInfo> list = tmEngineMapper.listTmNeedConstruction(scheduleDate, productionStage);
        list = list.stream().filter(r -> !mapAssistSpec.containsKey(r.getTreadCode())).collect(Collectors.toList());  //校验忽略掉 外协规格，只校验 不是外协的规格
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
            if(StringUtils.isBlank(construction.getTreadCode())) {
                //施工表胎面代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.treadCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getTreadRubberCategory())) {
                //施工表胎面胶料为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.treadRubberCategory") + "\"");
            }
            if(StringUtils.isBlank(construction.getTreadMouthPlate())) {
                //施工表胎面口型板为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.treadMouthPlate") + "\"");
            }
            if(construction.getTreadShoulderLength() == null || construction.getTreadShoulderLength() == 0) {
                //施工表胎面长为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.treadShoulderLength") + "\"");
            }
            if(!errorColumns.isEmpty()) {
                String tip = StringUtils.format(I18nUtil.getMessage("engine.auto.scheule.construction.validate"), embryoCode, embryoVersion, String.join(",", errorColumns));
                autoScheduleLogService.insertTmScheduleLog(batchNo, "", "自动排程失败", tip); //添加日志
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
        List<String> listAssistSpec = this.tmEngineMapper.listAssistSpec();
        if(listAssistSpec == null || listAssistSpec.size() == 0) {
            return map;
        }
        for(String assistSpec : listAssistSpec) {
            map.put(assistSpec, "1");
        }
        return map;
    }

    /**
     * 胎面插单
     * @param scheduleVo
     */
    public int inertTmOrder(TmScheduleResultVo scheduleVo) {
        String scheduleDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleVo.getScheduleDate()); //排程日期
        List<TmScheduleResultVo> scheduleList = new ArrayList<>();
        scheduleList.add(scheduleVo);
        return this.batchSaveTmSchedule(scheduleDate, scheduleList, true);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    public int batchSaveTmSchedule(String scheduleDate, List<TmScheduleResultVo> scheduleList) {
        return this.batchSaveTmSchedule(scheduleDate, scheduleList, false);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     * @param isUpdate 相同唯一键是否做更新操作。true：是，
     */
    @Transactional(rollbackFor=Exception.class)
    public int batchSaveTmSchedule(String scheduleDate, List<TmScheduleResultVo> scheduleList, boolean isUpdate) {
        if(scheduleList == null || scheduleList.isEmpty()) {
            return -1;
        }
        String batchNo = "";
        if(isUpdate) {
            batchNo = tmEngineMapper.getTmCurrentBatchNo(scheduleDate);  //查询当前排程的批次号
        }
        if(StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“或者做的批量导入（需要删掉已排的数据），那么自己生成一个排程批次号
            batchNo = this.createBatchNo(scheduleDate);  //胎面排程批次号
            this.createScheduleRecord(scheduleDate, "", batchNo);  //创建自动排程记录
            this.syncTmScheduleToLog(scheduleDate);  //把排程数据同步到log表
        }
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "插单或批量导入初始数据", toJSONString(scheduleList));  //添加日志

        List<String> treadCodes = scheduleList.stream().map(TmScheduleResultVo::getTreadCode).collect(Collectors.toList());
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        Map<String, TmScheduleBaseInfoVo> scheduleBaseInfoMap = getScheduleBaseInfoMap(scheduleDate, treadCodes, productionStage);  //根据胎面代码查询对应的胎面基础信息
        Map<String, String> glueSeqMap = tmEngineGlueService.getGlueSeqMap();  //获取胶料序号map
        Map<String, Double> planStockMap = tmEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算胎面16点预计库存
        Map<String, TmMonthSurplusVo> monthSurplus = tmEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "插单或批量导入基础数据", logSplit("半部件基础数据信息:" + toJSONString(scheduleBaseInfoMap),
                "胶料序号map：" + glueSeqMap, "16点预计库存：" + planStockMap, "月度计划剩余量、完成量：" + monthSurplus, "工序参数map：" + paramsMap));  //添加日志

        for(TmScheduleResultVo schedule : scheduleList) {
            schedule.setBatchNo(batchNo);  //批次号
            String orderNo = this.createOrderNo(batchNo); //工单号
            schedule.setOrderNo(orderNo);  //工单号
            TmScheduleBaseInfoVo baseInfoVo = scheduleBaseInfoMap.get(schedule.getTreadCode());
            if(baseInfoVo != null) {
                BeanUtils.copyProperties(baseInfoVo, schedule);
            }
            Double dayPlanQty = schedule.getDayPlanQty();  //中班计划量
            schedule.setDayPlanQty(dayPlanQty == null ? 0D : dayPlanQty);
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            schedule.setNightPlanQty(nightPlanQty == null ? 0D : nightPlanQty);

            schedule.setGlueSeq(glueSeqMap.get(schedule.getGlueCode()));  //胶料序号
            schedule.setStockQty(planStockMap.getOrDefault(schedule.getTreadCode(), 0D));  //16点预计库存
            this.newComputeSupplyTime(schedule, schedule.getStockQty());  //库存供应时长
            this.setStatusAndCloseTip(schedule, monthSurplus.get(schedule.getTreadCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段
            schedule.setIsRelease(ApsConstant.NO_RELEASE);
            schedule.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            schedule.setCreateTime(new Date());
            schedule.setCreateBy(SecurityUtils.getUsername());
            schedule.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE);
        }
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "插单或批量导入最终数据", toJSONString(scheduleList));  //添加日志
        return tmEngineMapper.mergeTmScheduleResult(scheduleList);  //批量合并排程结果表（根据唯一字段，做更新或新增）
    }

    /**
     * 根据胎面代码查询对应的胎面基础信息
     * @param scheduleDate
     * @return
     */
    private Map<String, TmScheduleBaseInfoVo> getScheduleBaseInfoMap(String scheduleDate, List<String> treadCodes, String productionStage) {
        Map<String, TmScheduleBaseInfoVo> map = new HashMap<>();
        List<TmScheduleBaseInfoVo> list = tmEngineMapper.listTmScheduleBaseInfo(treadCodes, ""); //查询出胎面在施工表的基础信息
        if(!StringUtils.isEmpty(list)) {
            map = list.stream().collect(Collectors.toMap(TmScheduleBaseInfoVo::getTreadCode, baseInfoVo->baseInfoVo));
        }

        Map<String, TmScheduleBaseInfoVo> hasCxMap = new HashMap<>();
        List<TmScheduleResultVo> hasCxlist = tmEngineMapper.statTmScheduleBase(scheduleDate, productionStage); //查询出在有对应成型排程的胎面基础信息
        for(TmScheduleResultVo info : hasCxlist) {
            TmScheduleBaseInfoVo baseInfoVo = new TmScheduleBaseInfoVo();
            BeanUtils.copyProperties(info, baseInfoVo);
            hasCxMap.put(info.getTreadCode(), baseInfoVo);
        }

        map.putAll(hasCxMap);  //有对应成型排程的胎面基础信息 覆盖掉，没有成型排程的胎面基础信息
        return map;
    }

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    public void changeTmMachine(String oldMachineIds, TmScheduleResult scheduleResult) {
//        String batchNo = scheduleResult.getBatchNo();  //批次号
//        String orderNo = scheduleResult.getOrderNo();  //工单号
//        autoScheduleLogService.insertTmScheduleLog(batchNo, orderNo, "转机台初始数据", logSplit("转机台前的机台ID：" + oldMachineIds, "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
//        Map<String, Double> lossRateMap = tmEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
//        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
//
//        //转机台后，不同机台的损耗率不一样，需要重新计算计划量
//        double oldLossRate = tmEngineLossService.getLossRate(scheduleResult.getTreadCode(), oldMachineIds, lossRateMap, paramLossRate);  //计算出转机台前的耗损率
//        double lossRate = tmEngineLossService.getLossRate(scheduleResult.getTreadCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
//        autoScheduleLogService.insertTmScheduleLog(batchNo, orderNo, "转机台需要根据不同机台耗损率重新计算计划量",
//                logSplit("重新计算计划量规则：先要根据之前机台的耗损率推算出之前在没有加上耗损率之前的计划量A，然后再用计划量A * 当前机台对应的耗损率，计算出最终的计划量", "转机台前的耗损率：" + oldLossRate + "转机台后的耗损率：" + lossRate));  //添加日志
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
//        autoScheduleLogService.insertTmScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
//    }

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    public void confirmTmMachine(TmScheduleResult scheduleResult) {
        String batchNo = scheduleResult.getBatchNo();  //批次号
        String orderNo = scheduleResult.getOrderNo();  //工单号
        autoScheduleLogService.insertTmScheduleLog(batchNo, orderNo, "确认机台初始数据", logSplit( "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
        Map<String, Double> lossRateMap = tmEngineLossService.getLossRateMap();   //损耗率map
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));

        //耗损率
        double lossRate = tmEngineLossService.getLossRate(scheduleResult.getTreadCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
        autoScheduleLogService.insertTmScheduleLog(batchNo, orderNo, "确认机台耗损率", "耗损率：" + lossRate);  //添加日志

        Double dayPlanQty = scheduleResult.getDayPlanQty();  //中班计划量
        if(dayPlanQty != null) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            scheduleResult.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty, 0)); //计划量向上取整
        }
        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
        if(nightPlanQty != null) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            scheduleResult.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty, 0));  //计划量向上取整
        }
        autoScheduleLogService.insertTmScheduleLog(batchNo, orderNo, "确认机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
    }

    /**
     * 手动均衡和重新设置生产顺序
     * @param scheduleDate 排程日期,格式：yyyy-mm-dd
     */
    public void handEquilibriumAndProduceOrder(String scheduleDate) {
        List<TmScheduleResultVo> scheduleList = tmEngineMapper.listTmEnginSchedule(scheduleDate);
        if(StringUtils.isEmpty(scheduleList)) {
           return;
        }

        String batchNo = "";
        Map<String, TmTotalPlanQtyVo> totalPlanQtyMap = new HashMap<>();  //每个生产线的计划量汇总MAP
        for(TmScheduleResultVo schedule : scheduleList ) {
            batchNo = schedule.getBatchNo();
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
        this.equalShare(batchNo, scheduleList, paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD));  //单规格排产数量达到设定值时，中夜班数量对半分
        this.setProduceOrder(scheduleList, new HashMap<>());  //生产顺序重新计算

        tmEngineMapper.createTempTable();
        tmEngineMapper.insertTempTable(scheduleList);
        tmEngineMapper.batchUpdateProduceOrder(scheduleDate, scheduleList);  //批量更新各班的计划量和生产顺序
//        tmEngineMapper.dropTempTable();
    }

    /**
     * 手动 同胶料合并生产
     * @param scheduleDate
     */
    public void handGlueMerge(String scheduleDate) {
        List<TmScheduleResultVo> scheduleList = tmEngineMapper.listTmEnginSchedule(scheduleDate);
        if(StringUtils.isEmpty(scheduleList)) {
            return;
        }
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String batchNo = scheduleList.get(0).getBatchNo();  //批次号
        this.glueMerge(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX));  //同胶料合并生产
        tmEngineMapper.createTempTable();
        tmEngineMapper.insertTempTable(scheduleList);
        tmEngineMapper.batchUpdatePlanQty(scheduleDate, scheduleList);  //批量更新各班的计划量
//        tmEngineMapper.dropTempTable();
    }

    /**
     * 中班和夜班计排程计划量均衡处理(根据生产线进行分组均衡)
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyMap 每个生产线的计划量汇总MAP
     */
    private void equilibrium(List<TmScheduleResultVo> scheduleList, Map<String, String> paramsMap, Map<String, TmTotalPlanQtyVo> totalPlanQtyMap) {
        Map<String, List<TmScheduleResultVo>> map = scheduleList.stream().collect(Collectors.groupingBy(s->s.getMachineId()));
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
     * @param totalPlanQtyVo 胎面中班和夜班总计划量Vo
     */
    private void equilibriumOne(List<TmScheduleResultVo> scheduleList, Map<String, String> paramsMap, TmTotalPlanQtyVo totalPlanQtyVo) {
        String batchNo = "";  //批次号
        String oldScheduleList = toJSONString(scheduleList);
        double difRate = getDoubleOrDefault(paramsMap.get(EngineConstants.PLAN_DIFFERENCE_RATE), 100D) ;  //参数配置：中班总量和夜班总量差额百分比
        double supplyTimePass = getDoubleOrDefault(paramsMap.get(EngineConstants.SUPPLY_TIME_PASS), 12D);  //参数配置：库存供应时长小时数
        double difNum = BigDecimalUtil.sub(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()); //中班和夜班计划量差额
        double actualDifRate = Math.abs(difNum) / totalPlanQtyVo.getTotalPlanQty() * 100;  //实际中班和夜班总计划量差额百分比
        if (actualDifRate > difRate) {
            //中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理
            boolean isDayClassPass = (difNum > 0);  //true：中班超量，false：夜班超量
            if (isDayClassPass) {
                //中班超量，排程结果按中班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(TmScheduleResultVo::getDayPlanQty)).collect(Collectors.toList());
            } else {
                //夜班超量，排程结果按夜班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(TmScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
            }
            //开始计划量均衡处理
            double lastDifRate = actualDifRate;  //上一次的中班总量和夜班总量的差额百分比
            for (TmScheduleResultVo resultVo : scheduleList) {
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
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     * @param scheduleList 排程列表
     * @param equalShareThreshold  各班计划量均分阈值
     */
    private void equalShare(String batchNo, List<TmScheduleResultVo> scheduleList, String equalShareThreshold) {
        if(StringUtils.isBlank(equalShareThreshold)) {
            return;
        }
        Integer threshold = Integer.parseInt(equalShareThreshold);
        scheduleList.forEach(schedule->{
            Double totalPlay = BigDecimalUtil.add(schedule.getDayPlanQty(), schedule.getNightPlanQty());  //一天总计划量
            if(totalPlay >= threshold) {
                //单规格排产数量达到参数配置的阈值,中夜班数量对半分
                Double equalSharePlan = BigDecimalUtil.div(totalPlay, 2);
                schedule.setDayPlanQty(BigDecimalUtil.roundUp(equalSharePlan, 0));   //均分后，中班向上取整
                schedule.setNightPlanQty(BigDecimalUtil.roundDown(equalSharePlan, 0));  //均分后，夜班向下取整
            }
        });
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "单规格排产数量达到设定值时，中夜班数量对半分", logSplit("班计划量均分阈值:" + equalShareThreshold,
                "均分后排程数据：" + toJSONString(scheduleList)));  //添加日志
    }

    /**
     * 同一个机台，胶料一样的排程记录，供应时长有一个小于等于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到中班;
     * 反之如果供应时长都大于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到夜班，在此情况下其中要是有记录的供应时长又大于{GLUE_MERGE_THRESHOLD_MAX}参数，则把计划量归并到【预计划】字段中，中班和夜班计划量变0
     *
     * @param scheduleList 排程列表
     * @param glueMergethreshold  同胶料合并生产预计库存可供应时长参数
     * @param glueMergethresholdMax  同胶料归并生产可供应时长(MAX)
     */
    private void glueMerge(String batchNo, List<TmScheduleResultVo> scheduleList, String glueMergethreshold, String glueMergethresholdMax) {
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
        Map<String, List<TmScheduleResultVo>> groupMap = scheduleList.stream().collect(Collectors.groupingBy(v -> v.getGlueCode() + v.getMachineId()));
        scheduleList.clear();

        for(List<TmScheduleResultVo> list : groupMap.values()) {
            boolean isPassParam = this.compareSupplyTime(list, threshold);  //判断集合中的库存供应时长 是否 有小于参数值的

            for(TmScheduleResultVo scheduleVo : list) {
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
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "同胶料合并生产", logSplit("同胶料合并生产预计库存可供应时长参数:" + glueMergethreshold,
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
    private void glueMerge1(String batchNo, List<TmScheduleResultVo> scheduleList, String glueMergethreshold,
                            String glueMergethresholdMax, Map<String, String> paramsMap, Map<String, BigDecimal> curlLengthMap,
                            BigDecimal standardCurlLength) {
        Double threshold = 12D;
        Double thresholdMax = 28D;
        try {
            threshold = Double.parseDouble(glueMergethreshold);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长参数转换错误");
        }
//        try {
//            thresholdMax = Double.parseDouble(glueMergethresholdMax);
//        } catch (NumberFormatException e) {
//            log.error("同胶料合并生产预计库存可供应时长(Max)参数转换错误");
//        }

        String mergeMaxRoll = paramsMap.get(EngineConstants.MERGE_MAX_ROLL);
        Integer cxMergeMaxSort = Integer.valueOf(paramsMap.getOrDefault(EngineConstants.CX_MERGE_MAX_SORT, "3"));
        // 根据胶料分组，如果计划卷数小于mergeMaxRoll算小批量，且成型二班顺序大于等于3，将计划量从夜班移到早班
        Map<String, List<TmScheduleResultVo>> groupMap1 = scheduleList.stream().collect(Collectors.groupingBy(TmScheduleResultVo::getGlueCode));
        Set<Map.Entry<String, List<TmScheduleResultVo>>> entrySet = groupMap1.entrySet();
        for (Map.Entry<String, List<TmScheduleResultVo>> entry : entrySet) {
            List<TmScheduleResultVo> value = entry.getValue();
            for (TmScheduleResultVo scheduleVo : value) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();

                BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getTreadCode(), standardCurlLength);
                BigDecimal dayRollNum = BigDecimalUtils.valueOf(dayPlanQty).divide(curlLength, 0, RoundingMode.CEILING);

                Double stockQty = scheduleVo.getStockQty();
                Double lastMidPlanQty = scheduleVo.getLastMidPlanQty();
                double totalStock = stockQty + lastMidPlanQty;
                Double cxClass1Plan = scheduleVo.getCxClass1Plan();
                Double cxClass2Plan = scheduleVo.getCxClass2Plan();
                double cxTotalPlan = cxClass1Plan + cxClass2Plan;

                if (scheduleVo.getClass2Sort() != null && scheduleVo.getClass2Sort() >= cxMergeMaxSort
                        && dayRollNum.compareTo(new BigDecimal(mergeMaxRoll)) <= 0 && totalStock >= cxTotalPlan) {
                    scheduleVo.setDayPlanQty(0D);
                    scheduleVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                }
            }
        }

        //根据机台+胶料进行分组
        Map<String, List<TmScheduleResultVo>> groupMap = scheduleList.stream().collect(Collectors.groupingBy(v -> v.getGlueCode() + v.getMachineId()));
        scheduleList.clear();

        for (List<TmScheduleResultVo> list : groupMap.values()) {
            boolean isPassParam = this.compareSupplyTime(list, threshold);  //判断集合中的库存供应时长 是否 有小于参数值的

            for (TmScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();

                BigDecimal curlLength = curlLengthMap.getOrDefault(scheduleVo.getTreadCode(), standardCurlLength);
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
    private boolean compareSupplyTime(List<TmScheduleResultVo> list, Double equalShareThreshold) {
        for(TmScheduleResultVo schedule : list) {
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
    private void groupTotalPlanQtyMap(TmScheduleResultVo scheduleVo, Map<String, TmTotalPlanQtyVo> totalPlanQtyMap) {
        String key = scheduleVo.getMachineId();  //机台id作为Map的key
        key = StringUtils.isBlank(key) ? "" : key;
        TmTotalPlanQtyVo totalPlanQtyVo = totalPlanQtyMap.getOrDefault(key, new TmTotalPlanQtyVo());  //取出对应生产线的计划量汇总对象

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
    private void equilibriumLog(String batchNo, String oldScheduleList, List<TmScheduleResultVo> scheduleList, Map<String, String> paramsMap, TmTotalPlanQtyVo totalPlanQtyVo) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，" +
                "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。").append(division);
        logDetail.append("各班总计划量：" + toJSONString(totalPlanQtyVo)).append(division);
        logDetail.append("参数配置集合，这里要用到‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’和‘SUPPLY_TIME_PASS（库存供应时长小时数）’：" + toJSONString(paramsMap)).append(division);
        logDetail.append("均衡前的排程数据列表：" + oldScheduleList).append(division);
        logDetail.append("均衡后的排产数据列表：" + toJSONString(scheduleList));
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "计划量均衡处理", logDetail.toString());
    }

    /**
     * 根据机台+胶料进行分组，然后在根据库存供应时长，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）
     * @param scheduleList
     */
    private void setProduceOrder(List<TmScheduleResultVo> scheduleList, Map<String, String> lastDayGlueMachineMap) {
        //根据机台进行分组
        Map<String, List<TmScheduleResultVo>> groupMap = scheduleList.stream()
                .filter(item -> item.getMachineId() != null)
                .collect(Collectors.groupingBy(TmScheduleResultVo::getMachineId));
        scheduleList.clear();

        // 各班续做胶料
//        Map<OpenMachineClassEnums, Map<String, String>> continueGlueMap = new HashMap<>();
//        continueGlueMap.put(OpenMachineClassEnums.CLASS_ONE, lastDayGlueMachineMap);

        for (Map.Entry<String, List<TmScheduleResultVo>> entry : groupMap.entrySet()) {
            List<TmScheduleResultVo> list = entry.getValue();
            int dayProduceOrder = 1; //白班生产顺序
            int nightProduceOrder = 1;  //夜班生产顺序
            String dayLastGlueCode = "";

            // 按胶料计算夜班有计划量的胶料最小供应时长
            list.forEach(s -> {
                s.setSupplyTime(this.computeSupplyTime2(s, OpenMachineClassEnums.CLASS_TWO)); // 重算可供时长
                String machineId = lastDayGlueMachineMap.get(s.getGlueCode());
                if (Objects.equals(machineId, s.getMachineId())) {
                    s.setDayProduceOrderFlag(ApsConstant.PRODUCT_ORDER_FLAG); // 打上夜班提前生产标记
                }
            });
            Map<String, Double> glueDaySupplyTimeMap = list.stream().filter(s -> s.getDayPlanQty() > 0)
                    .collect(Collectors.groupingBy(TmScheduleResultVo::getGlueCode, Collectors.collectingAndThen(
                            Collectors.toList(),
                            l -> l.stream().map(TmScheduleResultVo::getSupplyTime).min(Double::compareTo).get())));
            // 按胶料统计当班计划量
            Map<String, Double> dayGluePlanQtyMap = list.stream().filter(s -> StringUtils.isNotEmpty(s.getGlueCode()))
                    .collect(Collectors.groupingBy(TmScheduleResultVo::getGlueCode,
                            Collectors.summingDouble(TmScheduleResultVo::getDayPlanQty)));
            this.sortScheduleResultList(list, glueDaySupplyTimeMap, dayGluePlanQtyMap, OpenMachineClassEnums.CLASS_TWO);

            log.debug("-----夜班排序后列表start-----");
            for (int i = 0; i < list.size(); i++) {
                TmScheduleResultVo tmScheduleResultVo = list.get(i);
                log.debug(tmScheduleResultVo.printDebugSortLogInfo() + "|,顺序：{}", i);
            }
            for(TmScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                if(dayPlanQty > 0) {
                    scheduleVo.setDayProduceOrder(dayProduceOrder++);
                    dayLastGlueCode = scheduleVo.getGlueCode();
                }
            }

            // 抓取早班续做胶料
            Map<String, List<TmScheduleResultVo>> nightGlueMachineMap= scheduleList.stream()
                    .filter(s -> this.getPlanQty(s, OpenMachineClassEnums.CLASS_THREE) > 0) // 早班有计划量
                    .collect(Collectors.groupingBy(TmScheduleResultVo::getMachineId));
            Map<String, String> nightContinueGlue = new HashMap<>();
            for (Entry<String, List<TmScheduleResultVo>> glueMchineEntry : nightGlueMachineMap.entrySet()) {
                String machineId = glueMchineEntry.getKey();
                String glueCode = glueMchineEntry.getValue().stream().filter(s -> s.getDayProduceOrder() != null)
                        .max(Comparator.comparing(TmScheduleResultVo::getDayProduceOrder)) // 取最后的
                        .map(TmScheduleResultVo::getGlueCode).orElse(null);
                if (StringUtils.isNotEmpty(glueCode)) {
                    nightContinueGlue.put(glueCode, machineId);
                }
            }

            // 按胶料计算夜班有计划量的胶料最小供应时长
            list.forEach(s -> {
                s.setSupplyTime(this.computeSupplyTime2(s, OpenMachineClassEnums.CLASS_THREE)); // 重算可供时长
                String machineId = nightContinueGlue.get(s.getGlueCode());
                if (Objects.equals(machineId, s.getMachineId())) {
                    s.setNightProduceOrderFlag(ApsConstant.PRODUCT_ORDER_FLAG); // 打上早班提前生产标记
                }
            });
            Map<String, Double> glueNightSupplyTimeMap = list.stream().filter(s -> s.getNightPlanQty() > 0)
                    .collect(Collectors.groupingBy(TmScheduleResultVo::getGlueCode, Collectors.collectingAndThen(
                            Collectors.toList(),
                            l -> l.stream().map(TmScheduleResultVo::getSupplyTime).min(Double::compareTo).get())));
            // 按胶料统计当班计划量
            Map<String, Double> nightGluePlanQtyMap = list.stream().filter(s -> StringUtils.isNotEmpty(s.getGlueCode()))
                    .collect(Collectors.groupingBy(TmScheduleResultVo::getGlueCode,
                            Collectors.summingDouble(TmScheduleResultVo::getNightPlanQty)));
            // 先根据标识、库存供应时长升序排序，赋值顺序，并记录当前
            this.sortScheduleResultList(list, glueNightSupplyTimeMap, nightGluePlanQtyMap, OpenMachineClassEnums.CLASS_THREE);
            log.debug("-----早班排序后列表start-----");
            for (int i = 0; i < list.size(); i++) {
                TmScheduleResultVo tmScheduleResultVo = list.get(i);
                log.debug(tmScheduleResultVo.printDebugSortLogInfo() + "|,顺序：{}", i);
            }
            for (TmScheduleResultVo scheduleVo : list) {
                Double nightPlanQty = scheduleVo.getNightPlanQty();
                if(nightPlanQty > 0) {
                    scheduleVo.setNightProduceOrder(nightProduceOrder++);
                }
                autoScheduleLogService.insertTmScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产顺序字段",
                        logSplit("根据机台进行分组，然后在根据班次顺序标识库存供应时长(从小到大)，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）", "设置后的排程数据：" + toJSONString(scheduleVo)));  //添加日志
            }
            scheduleList.addAll(list);
        }
    }

    /**
     * 计划排序
     *
     * @param list
     * @param glueNightSupplyTimeMap
     * @param checkClass
     */
    private void sortScheduleResultList(List<TmScheduleResultVo> list, Map<String, Double> glueSupplyTimeMap,
            Map<String, Double> gluePlanQtyMap, OpenMachineClassEnums checkClass) {
        list.sort((s1, s2) -> {
            int result = 0;
            // 检查是否续做胶料，续做的排最前
//            if (checkClass == OpenMachineClassEnums.CLASS_TWO) { // 夜班
//                Integer orderFlag1 = s1.getDayProduceOrderFlag() == ApsConstant.PRODUCT_ORDER_FLAG ? 0 : 1;
//                Integer orderFlag2 = s2.getDayProduceOrderFlag() == ApsConstant.PRODUCT_ORDER_FLAG ? 0 : 1;
//                result = orderFlag1.compareTo(orderFlag2);
//                if (result != 0) {
//                    return result;
//                }
//            } else if (checkClass == OpenMachineClassEnums.CLASS_THREE) { // 早班
//                Integer orderFlag1 = s1.getNightProduceOrderFlag() == ApsConstant.PRODUCT_ORDER_FLAG ? 0 : 1;
//                Integer orderFlag2 = s2.getNightProduceOrderFlag() == ApsConstant.PRODUCT_ORDER_FLAG ? 0 : 1;
//                result = orderFlag1.compareTo(orderFlag2);
//                if (result != 0) {
//                    return result;
//                }
//            }
            // 欠料的先排（供应时长小于12小时），再排非欠料的
//            Integer lackFlag1 = s1.getSupplyTime() >= 12D? 1: 0;
//            Integer lackFlag2 = s2.getSupplyTime() >= 12D? 1: 0;
//            result = lackFlag1.compareTo(lackFlag2);
//            if (result != 0) {
//                return result;
//            }

            // 先比较胶料班次供应时长是否超过12小时，不到12小时的优先
            String glueCode1 = s1.getGlueCode();
            String glueCode2 = s2.getGlueCode();
            if (!glueCode1.equals(glueCode2)) { // 胶料不一样时先比较胶料优先级
                Double classSupplyTime1 = glueSupplyTimeMap.getOrDefault(glueCode1, 24D);
                Double classSupplyTime2 = glueSupplyTimeMap.getOrDefault(glueCode2, 24D);
                Integer oneClassFLag1 = classSupplyTime1 < 12? 0: 1;
                Integer oneClassFLag2 = classSupplyTime2 < 12? 0: 1;
                result = oneClassFLag1.compareTo(oneClassFLag2);
                if (result != 0) {
                    return result;
                }
                // 按胶料当班总计划量排序
                Double planQty1 = gluePlanQtyMap.getOrDefault(glueCode1, 0D);
                Double planQty2 = gluePlanQtyMap.getOrDefault(glueCode2, 0D);
                result = planQty1.compareTo(planQty2);
                if (result != 0) {
                    return result;
                }
                // 按胶料组排序
                String glueSeq1 = s1.getGlueSeq();
                String glueSeq2 = s2.getGlueSeq();
                result = glueSeq1.compareTo(glueSeq2);
                if (result != 0) {
                    return result;
                }
            }
            // 同供应时长组的，按各自的供应时长排序
            Double supplyTime1 = s1.getSupplyTime();
            Double supplyTime2 = s2.getSupplyTime();
            result = supplyTime1.compareTo(supplyTime2);
            if (result != 0) {
                return result;
            }
            return result;
        }
        );
//        String changeGlue = null; // 切换顺序的胶料
//        int insertIndex = 0; // 转移下标
        // 排序按胶料分了段，有可能后面的胶料等待时间很长
//        for (int i = list.size() / 2, size = list.size(); i < size; i++) { // 从中间位开始往后找
//            TmScheduleResultVo previousScheduleVo = list.get(i - 1); // 上一个下标的规格
//            TmScheduleResultVo scheduleVo = list.get(i); // 当前下标的规格
//            String glueCode = scheduleVo.getGlueCode();
//            if (scheduleVo.getSupplyTime() >= 12 && i < size - 3) { // 找到欠料的规格，或者还剩不到3个规格
//                continue;
//            }
//            if (Objects.equals(previousScheduleVo.getGlueCode(), glueCode)) { // 如果欠料规格与上一个规格胶料相同，跳过
//                continue;
//            }
//            if (changeGlue == null) {
//                changeGlue = glueCode;
//            } else if (!changeGlue.equals(glueCode)) {
//                break; // 如果已经切换过顺序，且胶料也切换掉了，则直接结束
//            }
//            // 直接将该规格提前第一位
//            list.remove(i);
//            list.add(insertIndex, scheduleVo);
//            insertIndex++; // 新增下标后移一位
//        }
    }

    /**
     * 设置收尾提示标识 和 生产状态字段
     * @param scheduleResultVo
     * @param monthSurplusVo
     * @param closeOutNum  参数配置表设置的 提示收尾阈值
     */
    private void setStatusAndCloseTip(TmScheduleResultVo scheduleResultVo, TmMonthSurplusVo monthSurplusVo, Double closeOutNum) {
        if(monthSurplusVo == null) {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
//            log.error("月计划汇总数据为空，物料编号为：", scheduleResultVo.getTreadCode());
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
        autoScheduleLogService.insertTmScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "设置收尾提示标识markCloseOutTip",
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
        autoScheduleLogService.insertTmScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "修改生产状态productionStatus",
                logSplit("①完成量为0，对应生产状态：未生产;②完成量大于0，月度计划量也大于0，说明处于生产中;③月度计划量小于等于0，说明处于生产完成",
                        "月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
     * @param batchNo   批次号
     * @param autoScheduleList   自动排程列表
     * @param existScheduleList  当天已经存在的排产记录
     */
    private List<TmScheduleResultVo> mergeExistSchedule(String batchNo, List<TmScheduleResultVo> autoScheduleList, List<TmScheduleResultVo> existScheduleList) {
        if(StringUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }
        List<TmScheduleResultVo> mergeList = new ArrayList<>();

        Map<String, List<TmScheduleResultVo>> existScheduleMap = existScheduleList.stream().filter(s->s.getPublishSuccessCount()>0)
                .collect(Collectors.groupingBy(TmScheduleResultVo::getTreadCode)); //拿到重排前，已经有发布给MES的排产数据。key为 半部件规格代码

        for(TmScheduleResultVo autoSchedule : autoScheduleList) {
            List<TmScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getTreadCode());

            if(existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                //对应规格重排前已经发布，并且此规格重排前只有一条排程记录（只对应了一个机台）
                TmScheduleResultVo existSchedule = existScheduleGroupList.get(0);
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
                for(TmScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                //对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getTreadCode());
        }

        //重排前的已发布的规格如果没有在重排后的列表中，则需要把对应的规格也加入到最新的排程列表中
        for(List<TmScheduleResultVo> list : existScheduleMap.values()) {
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
     * @param batchNo      胎面批次号
     */
    private void createScheduleRecord(String scheduleDate, String cxBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("cxBatchNo", cxBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());  //用户名
        tmEngineMapper.createScheduleRecord(params);
    }

    /**
     * 把排程数据同步到log表,删除历史外协排程数据
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    private void syncTmScheduleToLog(String scheduleDate) {
        tmEngineMapper.syncTmScheduleToLog(scheduleDate);
        tmEngineMapper.deleteTmSchedule(scheduleDate);
        tmEngineMapper.deleteTmAssistSchedule(scheduleDate);
    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleVo
     * @param specifyCanMachineMap  定点机台中限制作业map
     * @param specifyNotMachineMap  定点机台中不可作业
     * @param mouthPlateMachineMap
     */
    private void chooseMachine(TmScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap, Map<String, String> mouthPlateMachineMap) {
        String treadCode = scheduleVo.getTreadCode();  //胎面代码
        String mouthPlateCode = scheduleVo.getMouthPlateCode();  //口型板code

        String machineIds = specifyCanMachineMap.get(treadCode);
        machineIds = StringUtils.isBlank(machineIds) ? mouthPlateMachineMap.get(mouthPlateCode) : machineIds;  //从口型板中找机台
        //过滤掉 定点机台中 设置的不可作业的机台
        String notMachineIds = specifyNotMachineMap.get(treadCode);  //定点机台中不可作业的机台
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
    private void chooseMachineLog(TmScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap, Map<String, String> mouthPlateMachineMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“口型板与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
        logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("口型板与机台对应关系集合：" + toJSONString(mouthPlateMachineMap)).append(division);
        logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
        autoScheduleLogService.insertTmScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
    }

    /**
     * （新）计算并设置供成型库存供应时长（小时）。
     * 具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；
     *         预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）
     * @param scheduleVo
     * @param stockQty
     */
    private void newComputeSupplyTime(TmScheduleResultVo scheduleVo,  Double stockQty) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型一班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型二班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型三班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日一班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日一班的计划量
        autoScheduleLogService.insertTmScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长前数据",
                logSplit("具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+12小时；预计库存-1班计划-2班计划大于等于0时，供应时长+24小时；预计库存-1班计划-2班计划-3班计划小于0，供应时长=24个小时+（((预计库存-1班计划-2班计划)/3班计划)*12）；以此类推到第5班",
                        "物料编号：" + scheduleVo.getTreadCode() + ",7点预计库存：" + stockQty + "，对应成型一班的计划量：" + 0 + "，对应成型二班的计划量：" + cxClass2Plan + "，对应成型三班的计划量：" + cxClass3Plan + "，对应成型次日一班的计划量：" + cxClass4Plan + "，对应成型次日二班的计划量：" + cxClass5Plan));

        //根据1班计算库存供应时长
        double remnantStock = stockQty;    //剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass2Plan, scheduleVo.getClass2Sort())) {
            return;
        }

        //根据3班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass2Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass3Plan, scheduleVo.getClass3Sort())) {
            return;
        }

        //根据次日1班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass3Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass4Plan, scheduleVo.getClass4Sort())) {
            return;
        }

        //根据次日2班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass4Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass5Plan, null)) {
            return;
        }
        autoScheduleLogService.insertTmScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getTreadCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }

    /**
     * 重算库存可供时长
     */
    private Double computeSupplyTime2(TmScheduleResultVo scheduleVo, OpenMachineClassEnums checkClass) {
        Double supplyTime = 0D;
        Double stock = scheduleVo.getStockQty(); // 库存
        while (checkClass.getClassIndex() <= OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) { // 只需遍历到第四个班
            Double tmPlanQty = this.getTmClassPlanCumulative(scheduleVo, checkClass.getPreviousClass()); // 上个班为止的胎面总计划量
            Double cxPlanQty = this.getCxClassPlanCumulative(scheduleVo, checkClass); // 本班为止的成型总消耗量
            if (BigDecimalUtil.add(stock, tmPlanQty) >= cxPlanQty) { // 满足一个班需求，直接加12小时
                supplyTime = BigDecimalUtil.add(supplyTime, 12D);
            } else { // 不满足一个班需求，直接成型顺位计算（一个班最多6个规格，一顺位按2小时算）
                Double cxClassSort = BigDecimalUtils.least(this.getCxClassSort(scheduleVo, checkClass), 6D).doubleValue(); // 顺位超过6的，按6算
                supplyTime = BigDecimalUtil.add(supplyTime, BigDecimalUtil.mul(cxClassSort, 2D));
                break;
            }
            checkClass = checkClass.getNextClass();

        }
        return supplyTime;
    }

    /**
     * 根据对应成型每班计划量，计算库存供应时长
     * @param scheduleVo  排程实体
     * @param remnantStock 剩余库存
     * @param classPlan 对应成型的计划量
     * @return false：不需要再根据其他班在计算了。 true：还需要根据其他班计划量，继续计算库存供应时长
     */
    private boolean oneComputeSupplyTime(TmScheduleResultVo scheduleVo,Double remnantStock, Double classPlan, Double cxSort) {
        Double supplyTime = scheduleVo.getSupplyTime();
        supplyTime = (supplyTime == null ? 0D : supplyTime);
        if(BigDecimalUtil.sub(remnantStock, classPlan) >= 0) {
            //如果剩余库存 大于 对应班次库存，则库存供应时长直接+12小时
            scheduleVo.setSupplyTime(BigDecimalUtil.add(supplyTime, 12));  //库存供应时长加12小时
            return true;
        } else {
            if (cxSort == null || cxSort > 1) { // 如果成型是第一顺位，则相当于这个班的可供时长为0
                //如果剩余库存 小于 对应班次库存，则库存供应时长在加上：((剩余库存)/对应班班计划)*12小时
                double classSupplyTime = BigDecimalUtil.mul(BigDecimalUtil.div(remnantStock, classPlan), 12);
                supplyTime = BigDecimalUtil.add(supplyTime, BigDecimalUtil.roundDown(classSupplyTime, 1));  //设置库存供应时长向下保留1位小数
            }
            scheduleVo.setSupplyTime(supplyTime);
            autoScheduleLogService.insertTmScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getTreadCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
            return false;
        }
    }

    /**
     * 计算并设置库存供应时长（小时）= 库存/(成型定额*单耗)*8小时
     * @param quotaKeys 成型机台code和胎胚代码，格式：成型机台code$胎胚代码
     * @param stockQty 16点预计库存
     * @param unitConsume 单耗
     */
    private void computeSupplyTime(TmScheduleResultVo scheduleVo, String quotaKeys, Double stockQty, Double unitConsume) {
        if(StringUtils.isBlank(quotaKeys)) {
            scheduleVo.setSupplyTime(0D);
            autoScheduleLogService.insertTmScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长", "库存供应时长为空，原因：没找到对应的成型排程记录");
            return;
        }
        String [] quotaKeyArray = quotaKeys.split(",");
        Integer cxQuota = cxEngineQuotaCommonService.getCxMachineQuota(quotaKeyArray);  //成型定额
        unitConsume = BigDecimalUtil.div(unitConsume, 1000);   //单耗把毫米转成米
        Double quota = BigDecimalUtil.mul(cxQuota, unitConsume);   //胎面定额
        if(quota == 0) {
            scheduleVo.setSupplyTime(0D);
        } else {
            Double supplyTime = stockQty / quota * 8;  //库存可供成型连续生产的时长
            supplyTime = BigDecimalUtil.add(supplyTime, addComputeSupplyTime(scheduleVo)); //如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
            scheduleVo.setSupplyTime(BigDecimalUtil.roundDown(supplyTime, 1)); //设置库存公用时长向下保留1位小数
        }
        autoScheduleLogService.insertTmScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长",
                logSplit("库存供应时长supplyTime（小时）= 库存/(成型定额*单耗)*8小时;其中成型定额取成型定额的平均值，单耗也是取平均单耗", "成型定额：" + cxQuota + "，半制品平均单耗：" + unitConsume,
                        "计算后的结果数据：" + toJSONString(scheduleVo)));
    }

    /**
     * 如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
     * @param scheduleVo
     * @return
     */
    private int addComputeSupplyTime(TmScheduleResultVo scheduleVo) {
        int count = 0;
        int addTime = 12;  //每班12小时
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
     *
     * @param scheduleVo
     * @param totalPlanQtyVo  计划量总计VO
     * @param lossMap         耗损率map
     * @param paramLossRate   工序参数中配置的耗损率
     * @param mergeThreshold  往前一班合并计划量阈值
     * @param toolCapacity    取整数
     * @param productStockDay 预生产库存天数
     */
    private void computeTmPlanQty(TmScheduleResultVo scheduleVo, TmTotalPlanQtyVo totalPlanQtyVo,
                                  Map<String, Double> lossMap, double paramLossRate, double mergeThreshold, BigDecimal toolCapacity,
                                  double productStockDay, Map<String, String> paramsMap) {
        BigDecimal oneRollNum = new BigDecimal(paramsMap.getOrDefault(EngineConstants.ONE_ROLL_NUM, DEFAULT_ONE_ROLL_NUM));
        String dayProductGlueParamValue = paramsMap.getOrDefault(EngineConstants.DAY_PRODUCT_GLUE, "");

        String oldScheduleResult = toJSONString(scheduleVo); // 没计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); // 库存
        Double lastMidPlanQty = scheduleVo.getLastMidPlanQty(); // 前日白班计划
        Double totalConsumeQty = scheduleVo.getSurplusQty(); // 剩余量
        double supplyClass = productStockDay; // 预生产库存天数
        double lossRate = tmEngineLossService.getLossRate(scheduleVo.getTreadCode(), null, lossMap, paramLossRate); // 损耗率

        // 每个早班计算交接班库存 = 上一天交接班库存 + 上一天胎面计划量总量 - 上一天成型两个班的消耗量
        // 交接班库存要按生产几个小时库存算，例如预生产12小时库存，则交接班库存要 > 当天成型需求量 / 2，最多超过一卷
        // 上一天胎面计划总量原则上平均分配给两个班，但是早班的计划量要 > 上一天成型两个班的需求量 - 上一天交接班库存
        double cxPlanQty1 = BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());// 第一天成型两个班消耗量
        double cxPlanQty2 = BigDecimalUtil.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan());// 第二天成型两个班消耗量
        double cxPlanQty3 = cxPlanQty2;// 第三天成型两个班消耗量（成型没有，如果未收尾暂时先预计与第二天一样）
        double classStock1 = stockQty; // 第一天交接班库存，初始为当天库存
        double classStock2 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty2, supplyClass), 0); // 第二天交接班库存，第二天成型两个班的消耗量 * 预生产天数
        // 计算第一天相关数值
        double planQty1 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock2, classStock1), cxPlanQty1);// 第一天胎面计划量 = 第二天交接班库存 - 第一天交接班库存 + 第一天成型两个班的消耗量
        planQty1 = planQty1 > 0 ? planQty1 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double class1PlanQty1 = lastMidPlanQty;// 第一天早班计划 = 前日早班计划
        double class2PlanQty1;
        if (BigDecimalUtil.add(stockQty, lastMidPlanQty) >= BigDecimalUtil.add(cxPlanQty1, scheduleVo.getCxClass3Plan())) {
            class2PlanQty1 = 0D; // 如果库存+早班计划已经满足今天+明天早班的需求，则夜班不需要排产
        } else {
            class2PlanQty1 = BigDecimalUtil.sub(planQty1, class1PlanQty1); // 第一天夜班计划 = 等于第一天胎面计划 - 第一天早班计划
        }
        class2PlanQty1 = this.planQtyRounding(scheduleVo, class2PlanQty1, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_TWO, oneRollNum, lossRate); // 整车取整
        double dayPlanQty = class2PlanQty1; // 夜班计划
        scheduleVo.setDayPlanQty(dayPlanQty);
        // 根据排好的计划量重算相关数值
        planQty1 = BigDecimalUtil.add(class1PlanQty1, class2PlanQty1); // 刷新第一天胎面计划量
        classStock2 = BigDecimalUtil.sub(BigDecimalUtil.add(planQty1, classStock1), cxPlanQty1);// 刷新第二天交接班库存
        scheduleVo.setClassStock(classStock2); // 保存交接班库存，用于均衡计算
        scheduleVo.setSupplyDemandRatio(BigDecimalUtil.div(classStock2, cxPlanQty2, 4)); // 计算交接班库存供需比率，第二天交接班库存 / 成型第二天需求量，用于均衡计算
//        double subPlanQty = classStock2 + dayPlanQty - cxPlanQty2;
        // （单规格第二天的理论库存 + 早班计划量 - 计划用量）不超过10卷，超过则扣减交接班库存，减少早班的计划量
        /*if (subPlanQty > 10) {
            classStock2 = classStock2 - (subPlanQty - 10);
        }*/
        // 计算第二天相关数值
        double classStock3 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty3, supplyClass), 0); // 第三天交接班库存，第三天成型两个班的消耗量 * 预生产天数
        double planQty2 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock3, classStock2), cxPlanQty2);// 第二天胎面计划量 = 第三天交接班库存 - 第二天交接班库存 + 第二天成型两个班的消耗量
        planQty2 = planQty2 > 0 ? planQty2 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double class1PlanQty2;// 第二天早班计划，“第二天胎面计划量的一半” 与 “(第二天成型两个班的需求量 - 第二天交接班库存)的一半”的较大值
        double lackPlanQty = BigDecimalUtil.sub(cxPlanQty2, classStock2); // 早班先补交接班库存缺口
        class1PlanQty2 = this.planQtyRounding(scheduleVo, lackPlanQty, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_THREE, oneRollNum, lossRate); // 整车取整
        double nightPlanQty = class1PlanQty2; // 早班计划
        scheduleVo.setNightPlanQty(nightPlanQty);
        double class2PlanQty2 = BigDecimalUtil.sub(planQty2, class1PlanQty2);// 第二天夜班计划 = 等于第二天胎面计划 - 第二天早班计划
        double nextDayPlanQty = this.planQtyRounding(scheduleVo, class2PlanQty2, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_FOUR, oneRollNum, lossRate); // 次日夜班计划 = 第二天夜班计划整车取整
        scheduleVo.setNextDayPlanQty(nextDayPlanQty);

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
        nightPlanQty = (nightPlanQty < 0) ? 0D : nightPlanQty;

        //如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）
        if (dayPlanQty > 0) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
            nightPlanQty = 0D;
        }*/

//        //计划量要加上耗损量
//        double lossRate = tmEngineLossService.getLossRate(scheduleVo.getTreadCode(), null, lossMap, paramLossRate);
//        dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
//        nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
//        nextDayPlanQty = BigDecimalUtil.add(nextDayPlanQty, BigDecimalUtil.mul(nextDayPlanQty, lossRate));

        // 如果成型的夜班最小的顺序小于参数值，且计划量小于参数值，则将早班计划量移到夜班
        String cxMergeMinSortStr = paramsMap.get(EngineConstants.CX_MERGE_MIN_SORT);
        String mergeMaxRollStr = paramsMap.get(EngineConstants.MERGE_MAX_ROLL);
        BigDecimal nightRollNum = BigDecimalUtils.valueOf(nightPlanQty).divide(toolCapacity, 0, RoundingMode.CEILING);
        if (scheduleVo.getClass3Sort() != null &&
                scheduleVo.getClass3Sort() <= Integer.parseInt(cxMergeMinSortStr) &&
                nightRollNum.intValue() != 0 &&
                nightRollNum.intValue() <= Integer.parseInt(mergeMaxRollStr)) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
            nightPlanQty = 0D;
        }

//        // 如果成型的夜班最小的顺序小于参数值，则将早班计划量移到夜班
//        if (scheduleVo.getClass3Sort() != null &&
//                scheduleVo.getClass3Sort() <= Integer.parseInt(cxMergeMinSortStr)) {
//            dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
//            nightPlanQty = 0D;
//        }
//
//        // 成型顺序小于等于1，夜班计划量小于5卷的，从早班移到夜班
//        // 成型顺序大于1，夜班早班计划合计小于5卷的，从夜班移到早班
//        double totalPlan = nightPlanQty != 0 ? dayPlanQty + nightPlanQty : dayPlanQty + nextDayPlanQty;
//        BigDecimal totalRollNum = BigDecimalUtils.div(totalPlan, toolCapacity);
//        if (totalRollNum.compareTo(BigDecimal.valueOf(5)) < 0) {
//            if ((scheduleVo.getClass3Sort() != null && scheduleVo.getClass3Sort() <= 1)
////                    || scheduleVo.getSupplyTime() <= 12
//            ) {
//                if (nightPlanQty == 0) {
//                    if (dayPlanQty != 0) {
//                        dayPlanQty = dayPlanQty + nextDayPlanQty;
//                        nextDayPlanQty = 0;
//                    }
//                } else if (dayPlanQty == 0) {
//                    nightPlanQty = nightPlanQty + nextDayPlanQty;
//                    nextDayPlanQty = 0;
//                } else {
//                    dayPlanQty = dayPlanQty + nightPlanQty;
//                    nightPlanQty = 0;
//                }
//            } else if ((scheduleVo.getClass3Sort() != null && scheduleVo.getClass3Sort() > 1)
////                    || scheduleVo.getSupplyTime() > 12
//            ) {
//                if (nightPlanQty != 0) {
//                    nightPlanQty = dayPlanQty + nightPlanQty;
//                    dayPlanQty = 0;
//                }
//            }
//        }

        // 如果限制早班生产胶料，将夜班的计划量移到早班，不考虑硫化时间是否大于等于12
        String glueCode = scheduleVo.getGlueCode();
        List<String> dayProductGlueArr = Arrays.asList(dayProductGlueParamValue.split(","));
        if (dayProductGlueArr.contains(glueCode)) {
            // 把计划量移到早班，夜班计划量清0
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, dayPlanQty);
            dayPlanQty = 0D;
        }

        //计划量向上取整
        dayPlanQty = BigDecimalUtil.roundUp(dayPlanQty, 0);
        nightPlanQty = BigDecimalUtil.roundUp(nightPlanQty, 0);
        nextDayPlanQty = BigDecimalUtil.roundUp(nextDayPlanQty, 0);
        scheduleVo.setDayPlanQty(dayPlanQty);
        scheduleVo.setNightPlanQty(nightPlanQty);
        scheduleVo.setNextDayPlanQty(nextDayPlanQty);
        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE);

        //计算中班总计划量 和 夜班总计划量
//        this.groupTotalPlanQtyMap(scheduleVo, totalPlanQtyMap);
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalNextDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNextDayPlanQty(), nextDayPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty(), totalPlanQtyVo.getTotalNextDayPlanQty()));

        this.computeTmPlanQtyLog(oldScheduleResult, scheduleVo, lossMap, paramLossRate, lossRate);  //添加日志
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
    private Double getCxClassPlanCumulative(TmScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
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
     * @param oneRollNum      一次生产卷数
     * @param lossRate        损耗率
     * @return 结果
     */
    private double planQtyRounding(TmScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
                                   Double totalConsumeQty, OpenMachineClassEnums classNum,
                                   BigDecimal oneRollNum, Double lossRate) {
        if (planQty <= 0D) { // 不排的情况直接返回0即可
            return 0D;
        }

        BigDecimal divideResult = BigDecimalUtils.valueOf(this.addLossRate(planQty, lossRate)).divide(toolCapacity, 0, RoundingMode.CEILING);
        // 如果卷数不是一次生产卷数倍数，需要取整到一次生产卷数的倍数
        int remainder = divideResult.intValue() % oneRollNum.intValue();
        if (remainder != 0) {
            divideResult = divideResult.add(BigDecimal.valueOf(remainder));
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
        double lastPlanCumulative = this.getTmClassPlanCumulative(scheduleVo, lastClass); // 到上个班次班次班的累计已排计划量
        double result = roudingPlanQty;
        double newPlanQty = BigDecimalUtil.add(lastPlanCumulative, roudingPlanQty, scheduleVo.getStockQty()); // 库存+已排计划+本班计划
        // 如果库存+计划已经超过总需求量，则本班的计划量要限制住不允许超量
        if (newPlanQty > totalConsumeQty) {
            Double increaseMidPlanQty = BigDecimalUtil.sub(newPlanQty, totalConsumeQty);
            result = BigDecimalUtil.sub(roudingPlanQty, increaseMidPlanQty);
            result = Math.max(result, 0D);
        }
        scheduleVo.setCloseOutSpecFlag(newPlanQty >= totalConsumeQty? ApsConstant.STATUS_ENABLE: ApsConstant.STATUS_DISABLE);
        return result;
    }

    /**
     * 获取各班计划量的累计值（从前日早班开始）
     *
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getTmClassPlanCumulative(TmScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
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

    private void computeTmPlanQtyLog(String oldScheduleResult, TmScheduleResultVo scheduleVo, Map<String, Double> lossMap, double paramLossRate, double lossRate) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("开始计算中班和夜班计划量").append(division);
        logDetail.append("计算前排程数据：" + oldScheduleResult).append(division);
        logDetail.append("计算中班计划量dayPlanQty = （成型一班消耗胎面计划量cxClass1Plan + 成型二班消耗胎面计划量CxClass2Plan）").append(division);
        logDetail.append("计算夜班计划量nightPlanQty =（成型三班消耗胎面计划量cxClass3Plan + 成型次日一班消耗胎面计划量cxClass4Plan）").append(division);
        logDetail.append("根据库存重新计算中班计划量dayPlanQty：（原中班计划量dayPlanQty > 库存stockQty） ？（ 原中班计划量-库存） ： 0").append(division);
        logDetail.append("根据库存重新计算夜班计划量nightPlanQty：（原中班计划量dayPlanQty>库存stockQty） ？原夜班计划量nightPlanQty ： （原中班计划量dayPlanQty + 原夜班计划量nightPlanQty - 库存stockQty）").append(division);
        logDetail.append("胎面耗损率集合：" + toJSONString(lossMap) + "  参数配置耗损率：" + paramLossRate).append(division);
        logDetail.append("获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 胎面代码 > 机台 >工序参数配置），耗损率：" + lossRate).append(division);
        logDetail.append("重新计算中班计划量和夜班计划量(计划量 = 计划量 + 计划量 * 耗损率)，计划量要加上耗损率的损耗数").append(division);
        logDetail.append("如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）").append(division);
        logDetail.append("计划量计算好后的排程数据：" + toJSONString(scheduleVo));
        autoScheduleLogService.insertTmScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算各班计划量", logDetail.toString());
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.TM_BATCH_NO_PREFIX + scheduleDate);
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
        List<TmParamsVo> list = this.tmEngineMapper.listTmParams();
        Map<String, String> map = list.stream().collect(Collectors.toMap(TmParamsVo::getParamCode, TmParamsVo::getParamValue));
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
                             Map<String, TmMonthSurplusVo> monthSurplus,Map<String, String> paramsMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("胶料顺序集合：" + toJSONString(glueSeqMap)).append(division);
        logDetail.append("口型板和机台关系集合：" + toJSONString(mouthPlateMachineMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点集合和机台的不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(paramsMap)).append(division);
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }

    /**
     * 批量设置生产顺序
     *
     * @param scheduleDate 排程日期
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchSetProduceOrder(String scheduleDate, String targetMachineId) {
        // 加载昨日胶料对应机台
        Map<String, String> lastDayGlueMachineMap = this.loadLastDayMidPlan4Glue(scheduleDate);
        Map<String, Double> stockDateMap = this.loadTmStock(scheduleDate);
        List<TmScheduleResultVo> scheduleList = tmEngineMapper.listTmEnginSchedule(scheduleDate);
        if (CollectionUtils.isEmpty(scheduleList)) {
            return;
        }
        // 加载昨日早班计划
        Map<String, Double> lastDayMidPlanMap = this.loadLastDayMidPlan(scheduleDate);
        scheduleList = scheduleList.stream().filter(item -> targetMachineId.equals(item.getMachineId())).collect(Collectors.toList());
        for (TmScheduleResultVo scheduleVo : scheduleList) {
            // 上一天早班库存
            scheduleVo.setLastMidPlanQty(lastDayMidPlanMap.getOrDefault(scheduleVo.getTreadCode(), 0D));
            scheduleVo.setStockQty(stockDateMap.getOrDefault(scheduleVo.getTreadCode(), 0D));
        }
        this.setProduceOrder(scheduleList, lastDayGlueMachineMap);
        //查询当天已经存在的排产记录
        List<TmScheduleResultVo> existScheduleList = this.tmEngineMapper.listTmEnginSchedule(scheduleDate);
        String batchNo = scheduleList.get(0).getBatchNo();
        //如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
        scheduleList = this.mergeExistSchedule(batchNo, scheduleList, existScheduleList);
        if (StringUtils.isNotEmpty(scheduleList)) {
            //批量新增非外协排程结果数据
            tmEngineMapper.batchCreateScheduleResult(scheduleList);
        }
    }

    @Override
    public void batchUpdateBatchNoAndOrderNo(String scheduleDate) {
        List<TmScheduleResultVo> scheduleResultVoList = tmEngineMapper.listTmEnginSchedule(scheduleDate);
        //查询当前排程的批次号
        String batchNo = tmEngineMapper.getTmCurrentBatchNo(scheduleDate);
        if (StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“或者做的批量导入（需要删掉已排的数据），那么自己生成一个排程批次号
            //胎面排程批次号
            batchNo = this.createBatchNo(scheduleDate);
            //创建自动排程记录
            this.createScheduleRecord(scheduleDate, "", batchNo);
            //把排程数据同步到log表
            this.syncTmScheduleToLog(scheduleDate);
        }
        for (TmScheduleResultVo scheduleResult : scheduleResultVoList) {
            //批次号
            scheduleResult.setBatchNo(batchNo);
            //工单号
            String orderNo = this.createOrderNo(batchNo);
            scheduleResult.setOrderNo(orderNo);
        }
        if (CollectionUtils.isNotEmpty(scheduleResultVoList)) {
            tmEngineMapper.batchUpdateBatchNoAndOrderNo(scheduleResultVoList);
        }
    }
}
